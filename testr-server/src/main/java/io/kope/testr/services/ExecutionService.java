package io.kope.testr.services;

import io.kope.testr.protobuf.model.Model.Execution;
import io.kope.testr.protobuf.model.Model.ExecutionKey;

import java.util.List;

public interface ExecutionService {

    Execution findExecution(ExecutionKey executionKey);

    void createExecution(ExecutionKey executionKey);

    List<ExecutionKey> listExecutionsForRevision(String job, String revision);

    void recordExecution(Execution execution);

}
