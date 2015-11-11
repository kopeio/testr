// package io.kope.testr.auth;
//
// import org.keyczar.Signer;
// import org.keyczar.Verifier;
// import org.keyczar.exceptions.KeyczarException;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
//
// import com.google.common.base.Throwables;
// import com.google.common.io.BaseEncoding;
// import com.google.protobuf.ByteString;
// import com.google.protobuf.InvalidProtocolBufferException;
//
// import io.kope.testr.protobuf.auth.Auth.AuthenticationTokenData;
// import io.kope.testr.protobuf.auth.Auth.SignedToken;
//
// public class SignedTokenAuthenticator implements Authenticator {
//
// private static final Logger log =
// LoggerFactory.getLogger(SignedTokenAuthenticator.class);
//
// final Signer signer;
// final Verifier verifier;
//
// public SignedTokenAuthenticator(Signer signer, Verifier verifier) {
// this.signer = signer;
// this.verifier = verifier;
// }
//
// public class SignedTokenAuthentication extends
// AuthenticationTokenDataAuthentication {
// public SignedTokenAuthentication(AuthenticationTokenData data, String token)
// {
// super(data, token);
// }
// }
//
// @Override
// public Authentication authenticate(String tokenString) {
// byte[] tokenBytes;
// try {
// tokenBytes = BaseEncoding.base64Url().decode(tokenString);
// } catch (IllegalArgumentException e) {
// log.debug("Rejecting auth token with bad base64 encoding");
// return null;
// }
//
// SignedToken signedToken;
// try {
// signedToken = SignedToken.parseFrom(tokenBytes);
// } catch (InvalidProtocolBufferException e) {
// log.debug("Rejecting auth token with bad protobuf encoding");
// return null;
// }
//
// // TODO: Verify should take a []byte and is generally different from the
// // Java implementation
// // TODO: Verify matches multiple keys?
// boolean verified;
// try {
// verified = verifier.verify(signedToken.getData().asReadOnlyByteBuffer(),
// signedToken.getSignature().asReadOnlyByteBuffer());
// } catch (KeyczarException e) {
// log.debug("Rejecting auth token which gave error validating signature");
// return null;
// }
//
// if (!verified) {
// log.debug("Rejecting auth token which failed signature validation");
// return null;
// }
//
// AuthenticationTokenData authTokenData;
// try {
// authTokenData = AuthenticationTokenData.parseFrom(signedToken.getData());
// } catch (InvalidProtocolBufferException e) {
// log.debug("Rejecting auth token which failed unmarshaling (but passed
// validation)", e);
// return null;
// }
//
// return new SignedTokenAuthentication(authTokenData, tokenString);
// }
//
// @Override
// public String createToken(AuthenticationTokenData data) {
// ByteString rawBytes = data.toByteString();
//
// byte[] signature;
// try {
// signature = signer.sign(rawBytes.toByteArray());
// } catch (KeyczarException e) {
// throw Throwables.propagate(e);
// }
//
// SignedToken signedToken;
// {
// SignedToken.Builder b = SignedToken.newBuilder();
// b.setData(rawBytes);
// b.setSignature(ByteString.copyFrom(signature));
// signedToken = b.build();
// }
//
// byte[] signedBytes = signedToken.toByteArray();
//
// String signedString = BaseEncoding.base64Url().encode(signedBytes);
// return signedString;
// }
// }
