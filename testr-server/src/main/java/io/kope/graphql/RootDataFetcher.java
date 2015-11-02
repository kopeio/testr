package io.kope.graphql;

import java.lang.reflect.Method;

import graphql.schema.DataFetchingEnvironment;

public class RootDataFetcher<T, V> extends DataFetcherBase<T, V> {

	final T root;

	public RootDataFetcher(Method method, String[] parameterNames, T root) {
		super(method, parameterNames);
		this.root = root;
	}

	@Override
	public Object get(DataFetchingEnvironment environment) {
		return get(environment, root);
	}

	public static <T, V> RootDataFetcher<T, V> build(Method method, String[] parameterNames, T root) {
		return new RootDataFetcher<>(method, parameterNames, root);
	}

}
