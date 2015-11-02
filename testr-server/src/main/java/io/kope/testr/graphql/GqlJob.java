package io.kope.testr.graphql;

import io.kope.graphql.GqlName;
import io.kope.graphql.GraphQLNode;
import io.kope.testr.protobuf.model.Model.JobData;

@GqlName("Job")
public class GqlJob implements GraphQLNode {
	final JobData data;

	public GqlJob(JobData data) {
		this.data = data;
	}

	@Override
	public String getId() {
		return data.getJob();
	}

	public String getName() {
		return data.getJob();
	}

	public String getRepo() {
		return data.getRepo();
	}
}
