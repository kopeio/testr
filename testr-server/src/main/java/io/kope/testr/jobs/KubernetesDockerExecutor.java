package io.kope.testr.jobs;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.kope.testr.protobuf.model.Model.ExecutionKey;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;

public class KubernetesDockerExecutor extends ExecutorBase {

    private static final Logger log = LoggerFactory.getLogger(KubernetesDockerExecutor.class);

    final KubernetesClient kube;

    public KubernetesDockerExecutor(JobManager manager, KubernetesClient kube) {
        super(manager);
        this.kube = kube;
    }

    @Override
    public String startJob(ExecutionKey executionKey) {
        String token = buildToken(executionKey);
        manager.executionStore.createExecution(executionKey);

        String bootstrapURL = manager.scriptBuilder.buildBootstrapUrl(executionKey);

        List<String> command = Lists.newArrayList();
        command.add("bash");
        command.add("-c");
        command.add("curl -s -H 'Authorization: " + token + "' " + bootstrapURL + " | bash");

        // TOOD: Move to Job API?

        Container container = new Container();
        container.setCommand(command);
        container.setImage("golang:1.4");
        container.setName("testr");

        PodSpec podSpec = new PodSpec();
        podSpec.setContainers(Lists.newArrayList(container));

        Map<String, String> labels = Maps.newHashMap();
        labels.put("kope.io/testr/job", executionKey.getJob());
        labels.put("kope.io/testr/revision", executionKey.getRevision());
        labels.put("kope.io/testr/timestamp", Long.toString(executionKey.getTimestamp()));

        String name = "testr-runner-" + executionKey.getJob() + "-" + executionKey.getRevision() + "-"
                + executionKey.getTimestamp();
        name = "testr-runner-" + Hashing.md5().hashString(name, Charsets.UTF_8).toString();

        ObjectMeta metadata = new ObjectMeta();
        metadata.setLabels(labels);
        metadata.setName(name);

        Pod pod = new Pod();
        pod.setSpec(podSpec);
        pod.setMetadata(metadata);

        log.info("Starting pod: {}", pod);

        Pod created = kube.pods().create(pod);

        String containerId = created.getMetadata().getUid();
        return containerId;
    }

    @Override
    public ExecutionStatus getJobStatus(String jobId) {
        throw new UnsupportedOperationException("querying docker not yet implemented");
    }

}
