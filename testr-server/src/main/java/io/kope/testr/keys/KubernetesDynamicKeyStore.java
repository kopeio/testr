package io.kope.testr.keys;

import java.io.Reader;
import java.io.StringReader;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.util.Map;
import java.util.Optional;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.Maps;
import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.kope.testr.configuration.KubernetesId;

public class KubernetesDynamicKeyStore implements DynamicKeyStore {

	private static final Logger log = LoggerFactory.getLogger(KubernetesDynamicKeyStore.class);

	final KubernetesClient client;
	final KubernetesId secretId;

	Map<String, KeyWithId> keys;

	KeyWithId activeKey;

	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	public KubernetesDynamicKeyStore(KubernetesClient client, KubernetesId secretId) {
		this.client = client;
		this.secretId = secretId;

		Map<String, KeyWithId> keys = refresh();
		KeyWithId activeKey = determineActive(keys);

		/* synchronized (this) */ {
			this.keys = keys;
			this.activeKey = activeKey;
		}

		// TODO: Watch or poll
	}

	private static KeyWithId determineActive(Map<String, KeyWithId> keys) {
		long max = Long.MIN_VALUE;
		for (String id : keys.keySet()) {
			long value = Long.parseLong(id);
			if (value > max) {
				max = value;
			}
		}

		if (max == Long.MIN_VALUE) {
			return null;
		}

		String key = Long.toString(max);
		return keys.get(key);
	}

	@Override
	public synchronized Optional<PublicKey> findPublicKey(String keyId) {
		KeyWithId key = keys.get(keyId);
		if (key == null) {
			return Optional.empty();
		}
		return Optional.of(key.publicKey);
	}

	@Override
	public synchronized KeyWithId getActiveKey() {
		return activeKey;
	}

	public Map<String, KeyWithId> refresh() {
		Secret secret = client.secrets().inNamespace(secretId.getNamespace()).withName(secretId.getName()).get();
		if (secret == null) {
			throw new IllegalArgumentException("Secret not found: " + secretId);
		}

		Map<String, KeyWithId> keys = Maps.newHashMap();

		// TODO: We could auto-create / rotate keys
		Map<String, String> data = secret.getData();
		for (Map.Entry<String, String> entry : data.entrySet()) {
			String key = entry.getKey();
			if (!key.endsWith(".pem")) {
				log.debug("Skipping secret file that does not end with recognized suffix: {}", key);
				continue;
			}

			int lastDot = key.lastIndexOf('.');
			key = key.substring(0, lastDot);

			long t = Long.parseLong(key);
			byte[] keyBytes = BaseEncoding.base64().decode(entry.getValue());

			Reader reader = new StringReader(new String(keyBytes, Charsets.UTF_8));
			try (PEMParser pemParser = new PEMParser(reader)) {
				Object pemObject = pemParser.readObject();
				if (pemObject == null) {
					log.warn("Ignoring secret file that could not be parsed: {}", key);
					continue;
				}
				PEMKeyPair pemKeyPair;
				if (pemObject instanceof PEMKeyPair) {
					pemKeyPair = (PEMKeyPair) pemObject;
				} else if (pemObject instanceof PEMEncryptedKeyPair) {
					String keyPairPassword = "";
					PEMEncryptedKeyPair pemEncKeyPair = (PEMEncryptedKeyPair) pemObject;
					PEMDecryptorProvider pemDecryptorProvider = new JcePEMDecryptorProviderBuilder().build(keyPairPassword.toCharArray());
					pemKeyPair = pemEncKeyPair.decryptKeyPair(pemDecryptorProvider);
				} else {
					log.warn("Ignoring pem file of unknown type: {}", pemObject.getClass());
					continue;
				}
				JcaPEMKeyConverter jcaPEMKeyConverter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
				KeyPair keyPair = jcaPEMKeyConverter.getKeyPair(pemKeyPair);

				PrivateKey privateKey = keyPair.getPrivate();
				PublicKey publicKey = keyPair.getPublic();

				String id = Long.toString(t);
				KeyWithId keyWithId = new KeyWithId(id, privateKey, publicKey);
				keys.put(id, keyWithId);
			} catch (Exception e) {
				log.warn("Skipping secret file that gave error during parsing: {}", key);
				continue;
			}
		}

		return keys;
	}

}
