package io.kope.testr.endpoints;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import io.kope.testr.protobuf.auth.Auth.AuthPermission;
import io.kope.testr.protobuf.model.Model.Execution;
import io.kope.testr.protobuf.model.Model.ExecutionKey;
import io.kope.testr.protobuf.model.Model.JobData;
import io.kope.testr.protobuf.model.Model.StepEvent;
import io.kope.testr.services.BlobService;
import io.kope.testr.services.ExecutionService;
import io.kope.testr.services.JobService;

@Path("/api/execution")
public class ExecutionEndpoint extends EndpointBase {

	private static final Logger log = LoggerFactory.getLogger(ExecutionEndpoint.class);

	@PathParam("job")
	String job;
	@PathParam("revision")
	String revision;
	@PathParam("timestamp")
	long timestamp;

	@Inject
	ExecutionService executionService;
	@Inject
	JobService jobService;
	@Inject
	BlobService blobService;

	@PUT
	@Path("{job}/{revision}/{timestamp}")
	public Response putExecution(Execution execution) throws IOException {
		ExecutionKey executionKey = buildExecutionKey(job, revision, timestamp);

		requireExecutorToken(executionKey);

		Execution.Builder b = Execution.newBuilder(execution);
		b.setKey(executionKey);

		executionService.recordExecution(b.build());

		return Response.ok().build();
	}

	@GET
	@Path("{job}/{revision}/{timestamp}")
	public Response getExecution() throws IOException {
		ExecutionKey executionKey = buildExecutionKey(job, revision, timestamp);

		JobData jobData = jobService.findJob(executionKey.getJob());
		requireAuthorized(jobData, AuthPermission.READ);

		Execution execution = executionService.findExecution(executionKey);
		if (execution == null) {
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		return Response.ok(execution).build();
	}

	@GET
	@Path("{job}/{revision}/{timestamp}/log")
	public Response getExecutionLog() throws IOException {
		ExecutionKey executionKey = buildExecutionKey(job, revision, timestamp);

		JobData jobData = jobService.findJob(executionKey.getJob());
		requireAuthorized(jobData, AuthPermission.READ);

		Execution execution = executionService.findExecution(executionKey);
		if (execution == null) {
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		List<StepEvent> events = Lists.newArrayList();

		// TODO: Stream (make StepEvent an interface)
		InputStream is = blobService.findBlob(executionKey, "output.log");
		while (true) {
			StepEvent event = StepEvent.parseDelimitedFrom(is);
			if (event == null) {
				// EOF
				break;
			}
			events.add(event);
		}

		StepEvents stepEvents = new StepEvents(events);
		return Response.ok(stepEvents).build();
	}

}
