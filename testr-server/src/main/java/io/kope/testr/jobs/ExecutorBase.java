package io.kope.testr.jobs;

import io.kope.testr.protobuf.auth.Auth.AuthenticationTokenData;
import io.kope.testr.protobuf.auth.Auth.TokenType;
import io.kope.testr.protobuf.model.Model.ExecutionKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ExecutorBase implements Executor {
    private static final Logger log = LoggerFactory.getLogger(Executor.class);

    final JobManager manager;

    public ExecutorBase(JobManager manager) {
        this.manager = manager;
    }

    protected String buildToken(ExecutionKey executionKey) {
        // TODO: Refactor to ExecutionStore?
        AuthenticationTokenData authenticationTokenData;
        {
            AuthenticationTokenData.Builder b = AuthenticationTokenData.newBuilder();
            b.setJob(executionKey.getJob());
            b.setRevision(executionKey.getRevision());
            b.setTimestamp(executionKey.getTimestamp());
            b.setTokenType(TokenType.EXECUTOR);
            authenticationTokenData = b.build();
        }

        String token = manager.authenticator.createToken(authenticationTokenData);
        return token;
    }
}
