package io.kope.testr.jobs;

import io.kope.testr.protobuf.model.Model.ExecutionKey;

public interface Executor {

    String startJob(ExecutionKey executionKey);

    ExecutionStatus getJobStatus(String jobId);
}
