package io.kope.testr.configuration;

import java.util.List;

import com.google.common.base.Splitter;

public class KubernetesId {

	final String namespace;
	final String name;

	public KubernetesId(String namespace, String name) {
		this.namespace = namespace;
		this.name = name;
	}

	public static KubernetesId parse(String kubernetesPath) {
		List<String> pathTokens = Splitter.on('/').splitToList(kubernetesPath);
		if (pathTokens.size() != 2) {
			throw new IllegalArgumentException("Unable to parse kubernetes path: " + kubernetesPath);
		}
		String namespace = pathTokens.get(0);
		String name = pathTokens.get(1);

		return new KubernetesId(namespace, name);
	}

	@Override
	public String toString() {
		return namespace + "/" + name;
	}

	public String getName() {
		return name;
	}

	public String getNamespace() {
		return namespace;
	}

}
