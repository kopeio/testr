package io.kope.testr.endpoints;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteSource;

import io.kope.testr.protobuf.model.Model.Execution;
import io.kope.testr.protobuf.model.Model.ExecutionKey;
import io.kope.testr.protobuf.model.Model.JobData;
import io.kope.testr.protobuf.model.Model.Step;
import io.kope.testr.services.ExecutionService;
import io.kope.testr.services.JobService;
import io.kope.testr.services.ScriptBuilder;

@Path("/api/system")
public class SystemEndpoint extends EndpointBase {

	private static final Logger log = LoggerFactory.getLogger(SystemEndpoint.class);

	@PathParam("job")
	String job;
	@PathParam("revision")
	String revision;
	@PathParam("timestamp")
	long timestamp;

	@Inject
	JobService jobs;

	@Inject
	ExecutionService executions;

	@Inject
	ScriptBuilder scriptBuilder;

	@GET
	@Path("{job}/{revision}/{timestamp}/{key}")
	public Response getSystemResource(@PathParam("key") String key) {
		ExecutionKey executionKey = buildExecutionKey(job, revision, timestamp);

		// We want this to give clear errors if crypto is broken etc
		requireExecutorToken(executionKey);

		// TODO: Take Authentication?
		JobData job = jobs.findJob(executionKey.getJob());
		if (job == null) {
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		Execution execution = executions.findExecution(executionKey);
		if (execution == null) {
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		switch (key) {
		case "plan":
			Step plan = scriptBuilder.getPlan(job, executionKey);
			return Response.ok(plan).build();

		case "bootstrap":
			String token = getExecutorToken();
			String script = scriptBuilder.getBoostrapScript(token, executionKey);
			return Response.ok(script, MediaType.TEXT_PLAIN).build();

		case "executor":
			ByteSource blob = scriptBuilder.getExecutor();
			return Response.ok(blob).build();

		default:
			throw new WebApplicationException(Status.NOT_FOUND);
		}
	}

}
