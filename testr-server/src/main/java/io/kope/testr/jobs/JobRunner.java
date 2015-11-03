package io.kope.testr.jobs;

import io.kope.testr.git.GitBranch;
import io.kope.testr.git.GitCli;
import io.kope.testr.protobuf.model.Model.ExecutionKey;
import io.kope.testr.protobuf.model.Model.JobData;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class JobRunner {

    private static final Logger log = LoggerFactory.getLogger(JobRunner.class);

    final JobData job;
    final GitCli git;

    final Map<String, String> heads = Maps.newHashMap();

    final JobManager manager;

    public JobRunner(JobManager manager, JobData job) {
        this.manager = manager;
        this.job = job;
        this.git = new GitCli(job.getRepo());
    }

    ListenableFuture<List<GitBranch>> getBranches() {
        log.info("Listing revisions for {}", job.getRepo());
        return git.listRevisions();
    }

    public void run(List<GitBranch> branches) {
        for (GitBranch branch : branches) {
            String branchName = branch.name;

            boolean watchingBranch = false;
            for (String watchBranch : this.job.getBranchesList()) {
                if (watchBranch.equals("*") || watchBranch.equals(branchName)) {
                    watchingBranch = true;
                    break;
                }
            }
            if (!watchingBranch) {
                log.debug("Ignoring branch {}", branchName);
                continue;
            }

            String lastRevision = this.heads.get(branchName);
            if (Objects.equals(lastRevision, branch.revision)) {
                log.debug("Branch unchanged {}", branchName);
                continue;
            }
            log.info("Detected update of branch {} from {} -> {}", branchName, lastRevision, branch.revision);

            List<ExecutionKey> executions = manager.executionStore.listExecutionsForRevision(this.job.getJob(),
                    branch.revision);
            if (executions.isEmpty()) {
                String jobId = startJob(branch);
                // TODO: Write status to DB
                log.info("Started job for {}@{} jobId={}", branchName, branch.revision, jobId);
            } else {
                log.info("Already executed build for revision {}", branch.revision);
            }

            this.heads.put(branchName, branch.revision);
        }
    }

    String startJob(GitBranch branch) {
        long timestamp = System.currentTimeMillis();

        ExecutionKey executionKey;
        {
            ExecutionKey.Builder b = ExecutionKey.newBuilder();
            b.setJob(job.getJob());
            b.setRevision(branch.revision);
            b.setTimestamp(timestamp);
            executionKey = b.build();
        }

        return manager.executor.startJob(executionKey);
    }

    void poll() {
        ListenableFuture<Object> result = Futures.transform(this.getBranches(), (List<GitBranch> branches) -> {
            run(branches);
            return branches;
        });
        Futures.addCallback(result, new FutureCallback<Object>() {
            @Override
            public void onSuccess(Object result) {
                manager.scheduledExecutorService.schedule(JobRunner.this::poll, 60, TimeUnit.SECONDS);

            }

            @Override
            public void onFailure(Throwable t) {
                log.error("Error polling for changes for job {}", job.getJob(), t);
                manager.scheduledExecutorService.schedule(JobRunner.this::poll, 60, TimeUnit.SECONDS);
            }
        });
    }

    public void start() {
        manager.scheduledExecutorService.schedule(this::poll, 10, TimeUnit.SECONDS);
    }
}
