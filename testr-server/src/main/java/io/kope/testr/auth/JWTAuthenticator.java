package io.kope.testr.auth;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.auth.openidconnect.IdToken;
import com.google.api.client.auth.openidconnect.IdTokenVerifier;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.api.client.json.webtoken.JsonWebSignature.Header;
import com.google.api.client.json.webtoken.JsonWebToken.Payload;
import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;

import io.kope.testr.keys.DynamicKeyStore;
import io.kope.testr.keys.KeyWithId;
import io.kope.testr.protobuf.auth.Auth.AuthenticationTokenData;
import io.kope.testr.protobuf.auth.Auth.TokenType;

public class JWTAuthenticator implements Authenticator {
	private static GsonFactory gsonFactory = new GsonFactory();

	private static final Logger log = LoggerFactory.getLogger(JWTAuthenticator.class);

	private static final long EXPIRATION_SECONDS = 24 * 3600;

	final DynamicKeyStore keyStore;

	public JWTAuthenticator(DynamicKeyStore keyStore) {
		this.keyStore = keyStore;
	}

	public class JWTAuthentication extends AuthenticationTokenDataAuthentication {
		public JWTAuthentication(AuthenticationTokenData data, String token) {
			super(data, token);
		}
	}

	static class ExecutorTokenVerifier extends IdTokenVerifier {
		final DynamicKeyStore keyStore;

		public ExecutorTokenVerifier(Builder builder) {
			this.keyStore = Preconditions.checkNotNull(builder.keyStore);
		}

		public boolean verify(ExecutorToken token) throws GeneralSecurityException, IOException {
			// check the payload
			if (!super.verify(token)) {
				log.debug("Failed to verify token (base level)");
				return false;
			}

			String keyId = token.getHeader().getKeyId();
			if (Strings.isNullOrEmpty(keyId)) {
				log.debug("Failed to verify token (no key id)");
				return false;
			}

			Optional<PublicKey> publicKey = keyStore.findPublicKey(keyId);
			if (!publicKey.isPresent()) {
				log.debug("Failed to verify token (no key for key id)");
				return false;
			}

			// verify signature
			if (token.verifySignature(publicKey.get())) {
				return true;
			} else {
				log.debug("Failed to verify token (signature validation failed)");
				return false;
			}
		}

		public static class Builder extends IdTokenVerifier.Builder {
			private DynamicKeyStore keyStore;

			public Builder setKeyStore(DynamicKeyStore keyStore) {
				this.keyStore = keyStore;
				return this;
			}

			@Override
			public ExecutorTokenVerifier build() {
				return new ExecutorTokenVerifier(this);
			}
		}
	}

	class ExecutorToken extends IdToken {
		public ExecutorToken(Header header, IdToken.Payload payload, byte[] signatureBytes, byte[] signedContentBytes) {
			super(header, payload, signatureBytes, signedContentBytes);
		}

		public boolean verify(ExecutorTokenVerifier verifier) throws GeneralSecurityException, IOException {
			return verifier.verify(this);
		}
	}

	@Override
	public Authentication authenticate(String tokenString) {
		ExecutorToken token;
		try {
			JsonWebSignature jws = JsonWebSignature.parser(gsonFactory).setPayloadClass(IdToken.Payload.class).parse(tokenString);

			ExecutorTokenVerifier.Builder builder = new ExecutorTokenVerifier.Builder();
			builder.setKeyStore(keyStore);
			// builder.setIssuer("issuer.example.com").setAudience(Arrays.asList("myClientId"));
			ExecutorTokenVerifier verifier = builder.build();

			token = new ExecutorToken(jws.getHeader(), (IdToken.Payload) jws.getPayload(), jws.getSignatureBytes(),
					jws.getSignedContentBytes());

			if (!token.verify(verifier)) {
				log.debug("Rejecting auth token which did not verify");
				return null;
			}
		} catch (Exception e) {
			log.debug("Rejecting token which gave error during parsing", e);
			return null;
		}

		AuthenticationTokenData authTokenData;
		try {
			IdToken.Payload payload = token.getPayload();
			AuthenticationTokenData.Builder b = AuthenticationTokenData.newBuilder();
			TokenType tokenType = TokenType.valueOf(((Number) payload.get("tt")).intValue());
			if (tokenType == null) {
				throw new IllegalArgumentException("Unknown tt in : " + payload);
			}

			// TODO: Might be cute to reflect on protobuf
			b.setTokenType(tokenType);
			b.setJob((String) payload.get("job"));
			b.setRevision((String) payload.get("rev"));
			b.setTimestamp(((Number) payload.get("ts")).longValue());
			authTokenData = b.build();
		} catch (Exception e) {
			log.debug("Rejecting auth token which failed unmarshaling (but passed validation)", e);
			return null;
		}

		return new JWTAuthentication(authTokenData, tokenString);
	}

	@Override
	public String createToken(AuthenticationTokenData data) {
		KeyWithId key = keyStore.getActiveKey();
		if (key == null) {
			throw new IllegalArgumentException("No active key");
		}

		Header header = new Header();
		header.setKeyId(key.id);
		header.setAlgorithm("RS256");

		Payload payload = new Payload();
		payload.set("job", data.getJob());
		payload.set("rev", data.getRevision());
		payload.set("ts", data.getTimestamp());
		payload.set("tt", data.getTokenType().getNumber());
		long nowSeconds = System.currentTimeMillis() / 1000L;
		payload.setIssuedAtTimeSeconds(nowSeconds);
		payload.setExpirationTimeSeconds(nowSeconds + EXPIRATION_SECONDS);

		String sig;
		try {
			sig = JsonWebSignature.signUsingRsaSha256(key.privateKey, gsonFactory, header, payload);
		} catch (GeneralSecurityException e) {
			log.warn("Error signing token", e);
			throw Throwables.propagate(e);
		} catch (IOException e) {
			log.warn("Error signing token", e);
			throw Throwables.propagate(e);
		}
		return sig;
	}
}
