package io.kope.testr.jobs;

import io.kope.testr.protobuf.model.Model.ExecutionKey;
import io.kope.utils.ProcessExecution;
import io.kope.utils.Processes;
import io.kope.utils.TimeSpan;

import java.util.List;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class LocalDockerExecutor extends ExecutorBase {

    private static final Logger log = LoggerFactory.getLogger(LocalDockerExecutor.class);

    public LocalDockerExecutor(JobManager manager) {
        super(manager);
    }

    @Override
    public String startJob(ExecutionKey executionKey) {
        String token = buildToken(executionKey);
        manager.executionStore.createExecution(executionKey);

        String bootstrapURL = manager.scriptBuilder.buildBootstrapUrl(executionKey);

        List<String> args = Lists.newArrayList();
        args.add("docker");
        args.add("run");
        args.add("-d");
        args.add("golang:1.4");
        args.add("bash");
        args.add("-c");
        args.add("curl -s -H 'Authorization: " + token + "' " + bootstrapURL + " | bash");

        log.info("Starting process: {}", args);

        ProcessBuilder pb = new ProcessBuilder(args);

        ProcessExecution execution;
        try {
            execution = Processes.runSync(pb, TimeSpan.minutes(1));
        } catch (TimeoutException e) {
            throw new IllegalStateException("Timeout while starting runner");
        }

        int exitCode = execution.getExitCode();
        if (exitCode != 0) {
            log.warn("Unable to start runner");
            log.warn(" exitcode: {}", execution.getExitCode());
            log.warn(" stdout: {}", execution.getStdout());
            log.warn(" srderr: {}", execution.getStderr());

            throw new IllegalStateException("Unable to start runner");
        }

        String containerId = execution.getStdout();
        return containerId;
    }

    @Override
    public ExecutionStatus getJobStatus(String jobId) {
        throw new UnsupportedOperationException("querying docker not yet implemented");
    }

}
