package io.kope.graphql;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class IdFetcher implements DataFetcher {

	final GraphQLMapper mapper;

	public IdFetcher(GraphQLMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public Object get(DataFetchingEnvironment environment) {
		GraphQLNode node = (GraphQLNode) environment.getSource();
		if (node == null) {
			return null;
		}
		String id = node.getId();
		String graphqlTypeName = mapper.getGraphQLTypeName(node);
		String qualifiedId = graphqlTypeName + ":" + id;
		return BaseEncoding.base64Url().encode(qualifiedId.getBytes(Charsets.UTF_8));
	}

}
