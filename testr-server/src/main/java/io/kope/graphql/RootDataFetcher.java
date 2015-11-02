package io.kope.graphql;

import java.lang.reflect.Method;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import jersey.repackaged.com.google.common.base.Throwables;

public class RootDataFetcher<T, V> implements DataFetcher {

	final Method method;
	final T root;

	public RootDataFetcher(Method method, T root) {
		this.method = method;
		this.root = root;
	}

	@Override
	public Object get(DataFetchingEnvironment environment) {
		try {
			return method.invoke(root);
		} catch (ReflectiveOperationException e) {
			throw Throwables.propagate(e);
		}
	}

	public static <T, V> RootDataFetcher<T, V> build(Method method, T root) {
		return new RootDataFetcher<>(method, root);
	}
}
