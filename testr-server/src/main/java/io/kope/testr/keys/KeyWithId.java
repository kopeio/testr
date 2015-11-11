package io.kope.testr.keys;

import java.security.PrivateKey;
import java.security.PublicKey;

public class KeyWithId {
	public final String id;
	public final PrivateKey privateKey;
	public final PublicKey publicKey;

	public KeyWithId(String id, PrivateKey privateKey, PublicKey publicKey) {
		this.id = id;
		this.privateKey = privateKey;
		this.publicKey = publicKey;
	}

}
