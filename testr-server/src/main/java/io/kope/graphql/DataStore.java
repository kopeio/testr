package io.kope.graphql;

public interface DataStore {

	GraphQLNode getRoot(String name);

	GraphQLNode getById(String id);

}
