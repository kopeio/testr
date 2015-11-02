package io.kope.testr.services;

import java.util.List;

import io.kope.testr.protobuf.model.Model.Execution;
import io.kope.testr.protobuf.model.Model.ExecutionKey;

public interface ExecutionService {

	Execution findExecution(ExecutionKey executionKey);

	void createExecution(ExecutionKey executionKey);

	List<ExecutionKey> listExecutionsForRevision(String job, String revision);

	void recordExecution(Execution execution);

	List<ExecutionKey> listExecutions(String job);

}
