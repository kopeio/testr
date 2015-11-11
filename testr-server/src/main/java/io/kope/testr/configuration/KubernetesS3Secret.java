package io.kope.testr.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;

public class KubernetesS3Secret {
	private static final Logger log = LoggerFactory.getLogger(KubernetesS3Secret.class);

	static final Gson gson = new Gson();

	public static AWSCredentials read(KubernetesClient client, KubernetesId awsCredentialsSecret) {
		log.info("Reading AWS credentials from {}", awsCredentialsSecret);

		Secret secret = client.secrets().inNamespace(awsCredentialsSecret.getNamespace()).withName(awsCredentialsSecret.getName()).get();
		if (secret == null) {
			throw new IllegalArgumentException("Secret not found: " + awsCredentialsSecret);
		}

		String configBase64 = secret.getData().get("config.json");
		if (configBase64 == null) {
			throw new IllegalArgumentException("config.json not found in secret: " + awsCredentialsSecret);
		}

		String json = new String(BaseEncoding.base64().decode(configBase64), Charsets.UTF_8);

		Config config = gson.fromJson(json, Config.class);
		if (Strings.isNullOrEmpty(config.accessKey)) {
			throw new IllegalArgumentException("AWS accessKey not set in secret: " + awsCredentialsSecret);
		}
		if (Strings.isNullOrEmpty(config.secretKey)) {
			throw new IllegalArgumentException("AWS secretKey not set in secret: " + awsCredentialsSecret);
		}

		return new BasicAWSCredentials(config.accessKey, config.secretKey);
	}

	public static class Config {
		public String accessKey;
		public String secretKey;
	}
}
