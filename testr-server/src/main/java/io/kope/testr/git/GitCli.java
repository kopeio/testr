package io.kope.testr.git;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import io.kope.utils.ProcessExecution;
import io.kope.utils.Processes;
import io.kope.utils.TimeSpan;

public class GitCli {

    private static final Logger log = LoggerFactory.getLogger(GitCli.class);

    final String repo;

    public GitCli(String repo) {
        super();
        this.repo = repo;
    }

    public ListenableFuture<List<GitBranch>> listRevisions() {

        List<String> args = Lists.newArrayList();
        args.add("git");
        args.add("ls-remote");
        args.add("--heads");
        args.add(this.repo);

        ProcessBuilder pb = new ProcessBuilder(args);
        ListenableFuture<ProcessExecution> executionFuture = Processes.run(pb, TimeSpan.minutes(1));
        return Futures.transform(executionFuture, (ProcessExecution execution) -> {
            List<GitBranch> branches = Lists.newArrayList();
            // try {
            // execution = Processes.run(pb, TimeSpan.minutes(1));
            // } catch (TimeoutException e) {
            // throw new TestRunException("Timeout listing git revisions");
            // }

                int exit = execution.getExitCode();
                if (exit != 0) {
                    log.warn("Unable to list git revisions");
                    log.warn("stdout: {}", execution.getStdout());
                    log.warn("stderr: {}", execution.getStderr());

                    throw new TestRunException("Error listing git revisions");
                }

                if (!execution.getStderr().isEmpty()) {
                    log.warn("Unexpected stderr output from git ls-remote");
                    log.warn("stdout: {}", execution.getStdout());
                    log.warn("stderr: {}", execution.getStderr());

                    throw new TestRunException("Unexpected stderr output from git ls-remote");
                }

                for (String line : Splitter.on('\n').split(execution.getStdout())) {
                    if (line.isEmpty()) {
                        continue;
                    }
                    List<String> tokens = Splitter.on('\t').splitToList(line);
                    if (tokens.size() != 2) {
                        throw new TestRunException("Unexpected line from `git ls-remote`: " + line);
                    }
                    GitBranch branch = new GitBranch();
                    branch.name = tokens.get(1);
                    branch.revision = tokens.get(0);
                    branches.add(branch);
                }

                return branches;
            });

    }
}
