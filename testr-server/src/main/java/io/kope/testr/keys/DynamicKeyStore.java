package io.kope.testr.keys;

import java.security.PublicKey;
import java.util.Optional;

public interface DynamicKeyStore {

	Optional<PublicKey> findPublicKey(String keyId);

	KeyWithId getActiveKey();

}
