package io.kope.graphql;

import java.lang.reflect.Method;

import com.google.common.base.Throwables;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

abstract class DataFetcherBase<T, V> implements DataFetcher {

	final Method method;
	final String[] parameterNames;

	protected DataFetcherBase(Method method, String[] parameterNames) {
		this.method = method;
		this.parameterNames = parameterNames;
	}

	protected Object get(DataFetchingEnvironment environment, T target) {
		try {
			if (parameterNames.length == 0) {
				return method.invoke(target);
			} else {
				Object[] args = new Object[parameterNames.length];
				for (int i = 0; i < args.length; i++) {
					Object argument = environment.getArgument(parameterNames[i]);
					args[i] = argument;
				}
				return method.invoke(target, args);
			}
		} catch (ReflectiveOperationException e) {
			throw Throwables.propagate(e);
		}
	}

}
