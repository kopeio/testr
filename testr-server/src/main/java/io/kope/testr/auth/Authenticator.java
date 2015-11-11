package io.kope.testr.auth;

import io.kope.testr.protobuf.auth.Auth.AuthPermission;
import io.kope.testr.protobuf.auth.Auth.AuthenticationTokenData;
import io.kope.testr.protobuf.model.Model.ExecutionKey;
import io.kope.testr.protobuf.model.Model.JobData;

public interface Authenticator {
	public interface Authentication {

		boolean isExecutorToken(ExecutionKey executionKey);

		boolean isAuthorized(AuthPermission level, JobData job);

		String getToken();

	}

	Authentication authenticate(String token);

	String createToken(AuthenticationTokenData authenticationTokenData);
}
