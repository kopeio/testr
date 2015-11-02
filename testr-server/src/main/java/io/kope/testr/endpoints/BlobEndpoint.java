package io.kope.testr.endpoints;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.kope.testr.protobuf.auth.Auth.AuthPermission;
import io.kope.testr.protobuf.model.Model.ExecutionKey;
import io.kope.testr.protobuf.model.Model.JobData;
import io.kope.testr.services.BlobService;
import io.kope.testr.services.JobService;

@Path("/api/blob")
public class BlobEndpoint extends EndpointBase {

	private static final Logger log = LoggerFactory.getLogger(BlobEndpoint.class);

	@PathParam("job")
	String job;
	@PathParam("revision")
	String revision;
	@PathParam("timestamp")
	long timestamp;

	@Inject
	BlobService blobService;
	@Inject
	JobService jobService;

	@Inject
	HttpServletRequest request;

	@PUT
	@Path("{job}/{revision}/{timestamp}/{artifactPath: .*}")
	public Response uploadBlob(@PathParam("artifactPath") String artifactPath) throws IOException {
		ExecutionKey executionKey = buildExecutionKey(job, revision, timestamp);

		requireExecutorToken(executionKey);

		blobService.uploadBlob(executionKey, artifactPath, request.getInputStream());

		return Response.ok().build();
	}

	@GET
	@Path("{job}/{revision}/{timestamp}/{artifactPath: .*}")
	public Response getBlob(@PathParam("artifactPath") String artifactPath) throws IOException {
		ExecutionKey executionKey = buildExecutionKey(job, revision, timestamp);

		JobData job = jobService.findJob(executionKey.getJob());
		if (job == null) {
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		requireAuthorized(job, AuthPermission.READ);

		// TODO: Verify that artifactPath is in the JobData manifest (to save an
		// S3 call if not a valid file)?

		InputStream blob = blobService.findBlob(executionKey, artifactPath);
		if (blob == null) {
			return Response.status(Status.NOT_FOUND).build();
		}

		// Closing the InputStream is hard-coded. Sigh.
		return Response.ok(blob).build();
	}

}
