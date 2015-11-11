package io.kope.springframework.cloud.kubernetes;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

public class Kubernetes {
	private static final Logger log = LoggerFactory.getLogger(Kubernetes.class);
	public static Gson GSON = new Gson();

	public static final Kubernetes INSTANCE = new Kubernetes();

	KubernetesClient kubernetesClient;

	public synchronized KubernetesClient getKubernetesClient() {
		if (kubernetesClient == null) {
			kubernetesClient = new DefaultKubernetesClient();
		}
		return kubernetesClient;
	}

	public boolean isRunningInKubernetes() {
		String host = System.getenv("KUBERNETES_SERVICE_HOST");
		return host != null && !host.isEmpty();
	}

	public Optional<Pod> findPodByIp(InetAddress podIp) {
		KubernetesClient client = getKubernetesClient();

		String podIpString = podIp.getHostAddress();

		log.warn("Finding pod by IP is inefficient");

		List<Pod> matches = new ArrayList<Pod>();

		PodList pods = client.pods().list();
		for (Pod pod : pods.getItems()) {
			if (podIpString.equals(pod.getStatus().getPodIP())) {
				matches.add(pod);
			}
		}

		if (matches.size() == 0) {
			return Optional.empty();
		}
		if (matches.size() == 1) {
			return Optional.of(matches.get(0));
		}
		throw new IllegalStateException("Found multiple pods with IP: " + podIp);
	}

	private Optional<InetAddress> selfPodIp = null;

	public Optional<InetAddress> getSelfPodIp() {
		if (selfPodIp != null) {
			return selfPodIp;
		}

		List<InetAddress> matches = new ArrayList<>();

		try {
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
			for (NetworkInterface networkInterface : Collections.list(networkInterfaces)) {
				if (!networkInterface.isUp()) {
					continue;
				}
				if (networkInterface.isLoopback()) {
					continue;
				}

				Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
				for (InetAddress inetAddress : Collections.list(inetAddresses)) {
					if (inetAddress.isLoopbackAddress()) {
						continue;
					}
					if (inetAddress.isLinkLocalAddress()) {
						continue;
					}
					matches.add(inetAddress);
				}
			}
		} catch (SocketException e) {
			throw new IllegalStateException("Error querying network addresses", e);
		}

		if (matches.size() == 0) {
			return Optional.empty();
		}
		if (matches.size() != 1) {
			log.warn("Found multiple potential pod ip addresses; picking arbitrarily: {}", matches);
		}

		selfPodIp = Optional.of(matches.get(0));
		return Optional.of(matches.get(0));
	}

	Pod selfPod = null;

	public Pod getSelfPod() {
		if (selfPod != null) {
			return selfPod;
		}

		log.info("Querying kubernetes for self-pod");

		Optional<InetAddress> selfPodIp = getSelfPodIp();
		if (!selfPodIp.isPresent()) {
			throw new IllegalStateException("Cannot determine self-pod IP");
		}

		for (int i = 0; i < 10; i++) {
			// We wait and retry in case of a delay before the pod is sent to
			// the API
			Optional<Pod> pod = findPodByIp(selfPodIp.get());

			if (pod.isPresent()) {
				selfPod = pod.get();
				return pod.get();
			}

			log.warn("Did not find self-pod; will wait and retry");
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted while retrying");
			}
		}

		throw new IllegalStateException("Could not find self-pod in kubernetes");

	}
}
