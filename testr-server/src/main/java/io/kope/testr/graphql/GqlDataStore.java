package io.kope.testr.graphql;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;

import io.kope.graphql.DataStore;
import io.kope.graphql.GraphQLNode;
import io.kope.testr.protobuf.model.Model.Execution;
import io.kope.testr.protobuf.model.Model.ExecutionKey;
import io.kope.testr.protobuf.model.Model.JobData;
import io.kope.testr.protobuf.model.Model.StepEvent;
import io.kope.testr.services.BlobService;
import io.kope.testr.services.ExecutionService;
import io.kope.testr.services.JobService;

@Component
public class GqlDataStore implements DataStore {

	@Inject
	JobService jobService;

	@Inject
	GqlRoot root;

	@Inject
	ExecutionService executionService;

	@Inject
	BlobService blobService;

	@Override
	public GraphQLNode getRoot(String name) {
		// TODO: Make automatic?
		switch (name) {
		case "viewer":
			return root.getViewer();
		default:
			throw new IllegalArgumentException();
		}
	}

	@Override
	public GraphQLNode getById(String encodedId) {
		// TODO: Make automatic?
		byte[] decodedBytes = BaseEncoding.base64Url().decode(encodedId);
		String decoded = new String(decodedBytes, Charsets.UTF_8);

		List<String> tokens = Splitter.on(':').splitToList(decoded);
		if (tokens.size() != 2) {
			throw new IllegalStateException();
		}

		String typeName = tokens.get(0);
		String id = tokens.get(1);

		switch (typeName) {
		case "User":
			return getUser(id);

		case "Job":
			return getJob(id);

		default:
			throw new IllegalArgumentException();
		}

	}

	private GqlUser getUser(String id) {
		if (id.equals(GqlUser.ID_ANONYMOUS)) {
			return getAnonymousUser();
		}
		throw new IllegalArgumentException();
	}

	GqlJob getJob(String id) {
		JobData job = jobService.findJob(id);
		if (job == null) {
			return null;
		}
		return new GqlJob(this, job);
	}

	GqlUser getAnonymousUser() {
		return new GqlUser(this, GqlUser.ID_ANONYMOUS);
	}

	public GqlExecution getExecution(String job, String revision, String timestamp) {
		long timestampValue = Long.parseLong(timestamp);
		ExecutionKey executionKey = ExecutionKey.newBuilder().setJob(job).setRevision(revision)
				.setTimestamp(timestampValue).build();
		Execution execution = executionService.findExecution(executionKey);
		if (execution == null) {
			return null;
		}
		return new GqlExecution(this, execution);

	}

	public List<GqlStepEvent> getLog(ExecutionKey executionKey) {
		JobData jobData = jobService.findJob(executionKey.getJob());
		// requireAuthorized(jobData, AuthPermission.READ);

		Execution execution = executionService.findExecution(executionKey);
		if (execution == null) {
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		List<GqlStepEvent> events = Lists.newArrayList();

		// TODO: Stream (make StepEvent an interface)
		InputStream is = blobService.findBlob(executionKey, "output.log");
		if (is == null) {
			return Collections.emptyList();
		}
		while (true) {
			StepEvent event;
			try {
				event = StepEvent.parseDelimitedFrom(is);
			} catch (IOException e) {
				throw Throwables.propagate(e);
			}
			if (event == null) {
				// EOF
				break;
			}
			events.add(new GqlStepEvent(event));
		}

		return events;
	}

}
