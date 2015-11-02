package io.kope.testr.graphql;

import java.util.List;

import com.google.common.collect.Lists;

import io.kope.graphql.GqlName;
import io.kope.graphql.GraphQLNode;
import io.kope.testr.protobuf.model.Model.Execution;
import io.kope.testr.protobuf.model.Model.ExecutionKey;

@GqlName("Execution")
public class GqlExecution implements GraphQLNode {

	final GqlDataStore dataStore;
	Execution data;
	final ExecutionKey key;

	public GqlExecution(GqlDataStore dataStore, Execution data) {
		this.dataStore = dataStore;
		this.data = data;
		this.key = data.getKey();
	}

	public GqlExecution(GqlDataStore dataStore, ExecutionKey key) {
		this.dataStore = dataStore;
		this.data = null;
		this.key = key;
	}

	@Override
	public String getId() {
		return key.getJob() + ":" + key.getRevision() + ":" + key.getTimestamp();
	}

	public String getRevision() {
		return key.getRevision();
	}

	public String getTimestamp() {
		// We cast to string because JS isn't great at longs
		return Long.toString(key.getTimestamp());
	}

	synchronized Execution getData() {
		if (data == null) {
			data = dataStore.executionService.findExecution(key);
			if (data == null) {
				throw new IllegalStateException();
			}
		}
		return data;
	}

	public boolean getSuccess() {
		return getData().getSuccess();
	}

	public List<GqlArtifact> getArtifacts() {
		return Lists.transform(getData().getArtifactsList(), (a) -> new GqlArtifact(a));
	}

	public List<GqlStepEvent> getLog() {
		return dataStore.getLog(key);
	}
}
