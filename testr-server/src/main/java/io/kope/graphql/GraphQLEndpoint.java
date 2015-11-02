package io.kope.graphql;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.schema.GraphQLSchema;

@Path("/graphql")
public class GraphQLEndpoint {
	private static final Logger log = LoggerFactory.getLogger(GraphQLEndpoint.class);

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Inject
	SchemaBuilder schemaBuilder;

	@GET
	@Path("schema")
	public Response getSchema() throws IOException {
		String INTROSPECTION_QUERY = "  query IntrospectionQuery {\n    __schema {\n      queryType { name }\n      mutationType { name }\n      types {\n        ...FullType\n      }\n      directives {\n        name\n        description\n        args {\n          ...InputValue\n        }\n        onOperation\n        onFragment\n        onField\n      }\n    }\n  }\n\n  fragment FullType on __Type {\n    kind\n    name\n    description\n    fields {\n      name\n      description\n      args {\n        ...InputValue\n      }\n      type {\n        ...TypeRef\n      }\n      isDeprecated\n      deprecationReason\n    }\n    inputFields {\n      ...InputValue\n    }\n    interfaces {\n      ...TypeRef\n    }\n    enumValues {\n      name\n      description\n      isDeprecated\n      deprecationReason\n    }\n    possibleTypes {\n      ...TypeRef\n    }\n  }\n\n  fragment InputValue on __InputValue {\n    name\n    description\n    type { ...TypeRef }\n    defaultValue\n  }\n\n  fragment TypeRef on __Type {\n    kind\n    name\n    ofType {\n      kind\n      name\n      ofType {\n        kind\n        name\n        ofType {\n          kind\n          name\n        }\n      }\n    }\n  }\n";
		GraphQLQuery query = new GraphQLQuery();
		query.query = INTROSPECTION_QUERY;

		return query(query);
	}

	@POST
	public Response query(GraphQLQuery query) throws IOException {
		log.info("graphql query {}", query);

		GraphQLSchema schema = schemaBuilder.getSchema();

		ExecutionResult execution = new GraphQL(schema).execute(query.query);
		List<GraphQLError> errors = execution.getErrors();
		if (!errors.isEmpty())

		{
			log.warn("GraphQL errors: {}", errors);
		}

		Object result = execution.getData();

		Map<String, Object> wrapped = Maps.newHashMap();
		wrapped.put("data", result);

		String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(wrapped);

		log.info("response {}", json);

		return Response.ok(json).build();
	}

}
