package io.kope.utils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class Processes {

    private static final Logger log = LoggerFactory.getLogger(Processes.class);

    public static ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors
            .newCachedThreadPool());

    public static ListenableFuture<ProcessExecution> run(ProcessBuilder pb, TimeSpan timeout) {
        return executorService.submit(() -> {
            log.warn("Processes.run is not genuinely async");

            return runSync(pb, timeout);
        });
    }

    public static ProcessExecution runSync(ProcessBuilder pb, TimeSpan timeout) throws TimeoutException {
        File stdoutFile = null;
        File stderrFile = null;

        Process process = null;
        try {
            try {
                stdoutFile = File.createTempFile("stdout", "log");
                stderrFile = File.createTempFile("stderr", "log");
            } catch (IOException e) {
                throw new IllegalStateException("Error creating temp file", e);
            }
            pb.redirectOutput(stdoutFile);
            pb.redirectError(stderrFile);

            log.info("Running process: " + Joiner.on(" ").join(pb.command()));
            try {
                process = pb.start();
            } catch (IOException e) {
                throw new IllegalStateException("Error running process", e);
            }

            boolean exited;
            try {
                exited = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw Throwables.propagate(e);
            }

            if (exited) {
                int exitCode = process.exitValue();

                try {
                    String stdout = Files.toString(stdoutFile, Charsets.UTF_8);
                    String stderr = Files.toString(stderrFile, Charsets.UTF_8);

                    return new ProcessExecution(exitCode, stdout, stderr);
                } catch (IOException e) {
                    throw new IllegalStateException("Error reading stdout/stderr from process", e);
                }
            } else {
                throw new TimeoutException();
            }
        } finally {
            if (process != null) {
                process.destroy();
            }

            if (stdoutFile != null) {
                stdoutFile.delete();
            }

            if (stderrFile != null) {
                stderrFile.delete();
            }
        }
    }
}