package io.kope.testr.jobs;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.kope.testr.protobuf.model.Model.ExecutionKey;

public class KubernetesExecutor extends ExecutorBase {

	private static final Logger log = LoggerFactory.getLogger(KubernetesExecutor.class);

	final KubernetesClient kube;

	public KubernetesExecutor(JobManager manager, KubernetesClient kube) {
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
		// TODO: We could use the restart policy to restart in case of execution
		// problems (vs a failed test)
		podSpec.setRestartPolicy("Never");

		Map<String, String> labels = Maps.newHashMap();
		labels.put("testr.kope.io/job", executionKey.getJob());
		labels.put("testr.kope.io/revision", executionKey.getRevision());
		labels.put("testr.kope.io/timestamp", Long.toString(executionKey.getTimestamp()));

		String name = "testr-runner-" + executionKey.getJob() + "-" + executionKey.getRevision() + "-" + executionKey.getTimestamp();
		name = "testr-runner-" + Hashing.md5().hashString(name, Charsets.UTF_8).toString();

		ObjectMeta metadata = new ObjectMeta();
		metadata.setLabels(labels);
		metadata.setName(name);
		metadata.setNamespace("default"); // TODO: Namespacing
											// executionKey.getJob());

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
