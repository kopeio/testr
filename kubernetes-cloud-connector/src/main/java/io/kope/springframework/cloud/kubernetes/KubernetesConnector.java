package io.kope.springframework.cloud.kubernetes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.CloudConnector;
import org.springframework.cloud.app.ApplicationInstanceInfo;
import org.springframework.cloud.app.BasicApplicationInstanceInfo;
import org.springframework.cloud.service.ServiceInfo;
import org.springframework.cloud.service.common.PostgresqlServiceInfo;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import com.google.gson.JsonParseException;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.client.KubernetesClient;

public class KubernetesConnector implements CloudConnector {

	private static final Logger log = LoggerFactory.getLogger(KubernetesConnector.class);

	@Override
	public ApplicationInstanceInfo getApplicationInstanceInfo() {
		Pod selfPod = Kubernetes.INSTANCE.getSelfPod();

		String instanceId = selfPod.getMetadata().getNamespace() + "::" + selfPod.getMetadata().getName();
		// TODO: This isn't quite right... maybe get the RC name?
		String appId = instanceId;

		// TODO: Populate labels / annotations?
		Map<String, Object> properties = new HashMap<String, Object>();

		ApplicationInstanceInfo info = new BasicApplicationInstanceInfo(instanceId, appId, properties);
		return info;
	}

	@Override
	public List<ServiceInfo> getServiceInfos() {
		Pod selfPod = Kubernetes.INSTANCE.getSelfPod();

		String namespace = selfPod.getMetadata().getNamespace();
		KubernetesClient client = Kubernetes.INSTANCE.getKubernetesClient();
		// ServiceList serviceList =
		// client.services().inNamespace(namespace).list();
		// for (Service service : serviceList.getItems()) {
		//
		// }

		List<ServiceInfo> serviceInfos = new ArrayList<ServiceInfo>();
		SecretList secretList = client.secrets().inNamespace(namespace).list();
		for (Secret secret : secretList.getItems()) {
			Map<String, String> labels = secret.getMetadata().getLabels();
			if (labels == null) {
				continue;
			}

			String serviceId = labels.get(WellKnownLabels.SERVICE_ID);
			if (serviceId == null || serviceId.isEmpty()) {
				continue;
			}

			String servicetype = labels.get(WellKnownLabels.SERVICE_TYPE);
			if (servicetype == null || servicetype.isEmpty()) {
				continue;
			}

			// Service service =
			// client.services().inNamespace(namespace).withName(serviceId).get();
			// if (service == null) {
			// log.warn("Ignoring secret where service not found: {}",
			// serviceId);
			// continue;
			// }
			String hostname = serviceId;

			if (servicetype.equals("postgresql")) {
				PostgresqlServiceInfo serviceInfo = buildPostgresqlServiceInfo(hostname, secret);
				if (serviceInfo != null) {
					serviceInfos.add(serviceInfo);
				}
			} else {
				log.warn("Ignoring unknown servicetype: {}", servicetype);
			}
		}

		return serviceInfos;
	}

	private PostgresqlServiceInfo buildPostgresqlServiceInfo(String hostname, Secret secret) {

		String id = secret.getMetadata().getName();
		Map<String, String> data = secret.getData();
		if (data == null) {
			log.warn("Ignoring secret with no data: {}", id);
			return null;
		}
		String configJsonBase64 = data.get("config.json");
		if (configJsonBase64 == null) {
			log.warn("Ignoring secret with no config.json: {}", id);
			return null;
		}
		String configJson;
		try {
			byte[] configJsonBytes = BaseEncoding.base64().decode(configJsonBase64);
			configJson = new String(configJsonBytes, Charsets.UTF_8);
		} catch (IllegalArgumentException e) {
			log.warn("Ignoring secret with malformed value (not base64): {}", id);
			return null;
		}
		ServiceConfigData serviceConfig;
		try {
			serviceConfig = Kubernetes.GSON.fromJson(configJson, ServiceConfigData.class);
		} catch (JsonParseException e) {
			log.warn("Ignoring secret with malformed JSON: {}", id);
			return null;
		}

		String url = PostgresqlServiceInfo.POSTGRES_SCHEME + "://";
		if (serviceConfig.user != null) {
			url += serviceConfig.user + ":" + serviceConfig.password;
		}

		url += "@" + hostname;
		// url += ":" + port;

		if (serviceConfig.db != null) {
			url += "/" + serviceConfig.db;
		}

		PostgresqlServiceInfo serviceInfo = new PostgresqlServiceInfo(id, url);
		return serviceInfo;
	}

	@Override
	public boolean isInMatchingCloud() {
		return Kubernetes.INSTANCE.isRunningInKubernetes();
	}

}
