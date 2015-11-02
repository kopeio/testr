package io.kope.graphql;

import java.lang.reflect.Method;

import graphql.schema.DataFetchingEnvironment;

public class ReflectionDataFetcher<T, V> extends DataFetcherBase<T, V> {

	public ReflectionDataFetcher(Method method, String[] parameterNames) {
		super(method, parameterNames);
	}

	@Override
	public Object get(DataFetchingEnvironment environment) {
		T t = (T) environment.getSource();
		return get(environment, t);
	}

	public static <T, V> ReflectionDataFetcher<T, V> build(Method method, String[] parameterNames) {
		return new ReflectionDataFetcher<>(method, parameterNames);
	}
}
