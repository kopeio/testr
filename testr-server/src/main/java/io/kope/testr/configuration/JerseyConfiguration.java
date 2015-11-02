package io.kope.testr.configuration;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;

import io.kope.testr.endpoints.BlobEndpoint;
import io.kope.testr.endpoints.ExecutionEndpoint;
import io.kope.testr.endpoints.StepEventsMessageBodyWriter;
import io.kope.testr.endpoints.SystemEndpoint;
import io.kope.utils.ByteSourceMessageBodyWriter;
import io.kope.utils.ProtobufGeneratedMessageBodyReader;
import io.kope.utils.ProtobufMessageBodyWriter;

@Configuration
public class JerseyConfiguration extends ResourceConfig {
	public JerseyConfiguration() {
		register(BlobEndpoint.class);
		register(ExecutionEndpoint.class);
		register(SystemEndpoint.class);
		// register(GraphqlEndpoint.class);

		register(ProtobufMessageBodyWriter.class);
		register(ProtobufGeneratedMessageBodyReader.class);
		register(StepEventsMessageBodyWriter.class);
		register(ByteSourceMessageBodyWriter.class);
	}
}