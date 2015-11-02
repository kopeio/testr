package io.kope.testr.graphql;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import io.kope.graphql.GqlName;
import io.kope.graphql.GraphQLNode;
import io.kope.testr.protobuf.model.Model.JobData;

@GqlName("Job")
public class GqlJob implements GraphQLNode {

	private static final Logger log = LoggerFactory.getLogger(GqlJob.class);

	final GqlDataStore dataStore;
	final JobData data;

	public GqlJob(GqlDataStore dataStore, JobData data) {
		this.dataStore = dataStore;
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

	public List<GqlExecution> getExecutions() {
		return Lists.transform(dataStore.executionService.listExecutions(data.getJob()),
				(key) -> new GqlExecution(dataStore, key));
	}

	public GqlExecution getExecution(String revision, String timestamp) {
		return dataStore.getExecution(data.getJob(), revision, timestamp);
	}
}
