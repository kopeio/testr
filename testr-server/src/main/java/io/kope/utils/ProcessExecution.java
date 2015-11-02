package io.kope.utils;

public class ProcessExecution {
    public final int exitCode;
    public final String stdout;
    public final String stderr;

    public ProcessExecution(int exitCode, String stdout, String stderr) {
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

}