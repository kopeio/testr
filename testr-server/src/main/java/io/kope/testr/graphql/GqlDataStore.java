package io.kope.testr.graphql;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.BaseEncoding;

import io.kope.graphql.DataStore;
import io.kope.graphql.GraphQLNode;
import io.kope.testr.protobuf.model.Model.JobData;
import io.kope.testr.services.JobService;

@Component
public class GqlDataStore implements DataStore {

	@Inject
	JobService jobService;

	@Inject
	GqlRoot root;

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

	private GqlJob getJob(String id) {
		JobData job = jobService.findJob(id);
		if (job == null) {
			return null;
		}
		return new GqlJob(job);
	}

	GqlUser getAnonymousUser() {
		return new GqlUser(this, GqlUser.ID_ANONYMOUS);

	}

}
