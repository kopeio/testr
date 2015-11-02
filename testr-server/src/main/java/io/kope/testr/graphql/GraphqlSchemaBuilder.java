package io.kope.testr.graphql;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import io.kope.graphql.DataStore;
import io.kope.graphql.GraphQLMapper;
import io.kope.graphql.SchemaBuilder;

@Component
public class GraphqlSchemaBuilder implements SchemaBuilder {

	private static final Logger log = LoggerFactory.getLogger(GraphqlSchemaBuilder.class);

	@Inject
	DataStore dataStore;

	GraphQLMapper mapper;

	@Inject
	GqlRoot gqlRoot;

	@Override
	public GraphQLSchema getSchema() {
		return getMapper().getSchema();
	}

	public GraphQLMapper getMapper() {
		if (mapper == null) {
			mapper = buildMapper();
		}
		return mapper;
	}

	GraphQLMapper buildMapper() {
		GraphQLMapper mapper = new GraphQLMapper();

		// TODO: Can we just discover these by traversal from the root
		GraphQLObjectType jobType = mapper.addByReflection(GqlJob.class);
		GraphQLObjectType userType = mapper.addByReflection(GqlUser.class);

		GraphQLObjectType queryType = mapper.addRootByReflection(dataStore, gqlRoot);
		mapper.setQueryType(queryType);

		return mapper;

	}

}
