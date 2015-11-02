package io.kope.graphql;

import java.lang.reflect.Method;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import jersey.repackaged.com.google.common.base.Throwables;

public class ReflectionDataFetcher<T, V> implements DataFetcher {

	final Method method;

	public ReflectionDataFetcher(Method method) {
		this.method = method;
	}

	@Override
	public Object get(DataFetchingEnvironment environment) {
		T t = (T) environment.getSource();
		try {
			return method.invoke(t);
		} catch (ReflectiveOperationException e) {
			throw Throwables.propagate(e);
		}
	}

	public static <T, V> ReflectionDataFetcher<T, V> build(Method method) {
		return new ReflectionDataFetcher<>(method);
	}
}
