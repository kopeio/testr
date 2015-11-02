package io.kope.graphql;

import java.util.Map;

import io.kope.utils.DebugUtils;

public class GraphQLQuery {
	public String query;
	public Map<String, String> variables;

	@Override
	public String toString() {
		return DebugUtils.toJson(this);
	}
}
