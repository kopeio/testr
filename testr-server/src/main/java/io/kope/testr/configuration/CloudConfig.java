package io.kope.testr.configuration;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.config.java.AbstractCloudConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.kope.testr.keys.KubernetesDynamicKeyStore;

@Configuration
class CloudConfig extends AbstractCloudConfig {
	@Bean
	public DataSource dataSource() {
		return connectionFactory().dataSource();
	}

	@Value("${keystore.kubernetes.path}")
	public String keystoreKubernetesPath;

	@Bean
	public KubernetesDynamicKeyStore keyStore(KubernetesClient client) {
		KubernetesId id = KubernetesId.parse(keystoreKubernetesPath);
		return new KubernetesDynamicKeyStore(client, id);
	}
}