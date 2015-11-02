package io.kope.testr.graphql;

import java.util.List;

import javax.inject.Inject;

import com.google.common.collect.Lists;

import io.kope.graphql.GqlName;
import io.kope.graphql.GraphQLNode;

@GqlName("User")
public class GqlUser implements GraphQLNode {

	public static final String ID_ANONYMOUS = "anonymous";

	final String id;
	final GqlDataStore dataStore;

	@Inject
	public GqlUser(GqlDataStore dataStore, String id) {
		this.dataStore = dataStore;
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}

	public List<GqlJob> getJobs() {
		return Lists.transform(dataStore.jobService.listAllJobs(), d -> new GqlJob(dataStore, d));
	}
}
