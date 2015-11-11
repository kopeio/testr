package io.kope.testr.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

import io.kope.testr.auth.Authenticator.Authentication;
import io.kope.testr.protobuf.auth.Auth.AuthPermission;
import io.kope.testr.protobuf.auth.Auth.AuthenticationTokenData;
import io.kope.testr.protobuf.model.Model.ExecutionKey;
import io.kope.testr.protobuf.model.Model.JobData;

public class AuthenticationTokenDataAuthentication implements Authentication {

	private static final Logger log = LoggerFactory.getLogger(AuthenticationTokenDataAuthentication.class);

	final AuthenticationTokenData data;
	final String token;

	public AuthenticationTokenDataAuthentication(AuthenticationTokenData data, String token) {
		this.data = data;
		this.token = token;
	}

	@Override
	public boolean isExecutorToken(ExecutionKey executionKey) {
		switch (data.getTokenType()) {
		case EXECUTOR:
			return Objects.equal(data.getJob(), executionKey.getJob()) && Objects.equal(data.getRevision(), executionKey.getRevision())
					&& data.getTimestamp() == executionKey.getTimestamp();

		case USER:
			return false;

		default:
			log.warn("unhandled token type {}", data.getTokenType());
			return false;
		}
	}

	@Override
	public boolean isAuthorized(AuthPermission level, JobData job) {

		switch (data.getTokenType()) {
		case EXECUTOR:
			return false;

		case USER:
			if (job.getIsPublic() && level == AuthPermission.READ) {
				return true;
			}
			if (!Objects.equal(data.getJob(), job.getJob())) {
				return false;
			}
			// TODO: Explicit read/write permissions?
			return true;

		default:
			log.warn("unhandled token type {}", data.getTokenType());
			return false;
		}
	}

	@Override
	public String getToken() {
		return token;
	}
}
