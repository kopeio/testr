package io.kope.graphql;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import graphql.Scalars;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.TypeResolver;
import jersey.repackaged.com.google.common.collect.Lists;
import jersey.repackaged.com.google.common.collect.Maps;

public class GraphQLMapper implements TypeResolver {

	private static final Logger log = LoggerFactory.getLogger(GraphQLMapper.class);

	final IdFetcher idFetcher;

	final Map<Class<?>, GraphQLObjectType> types = Maps.newHashMap();

	private GraphQLObjectType queryType;

	private GraphQLInterfaceType nodeInterface;

	public GraphQLMapper() {
		this.idFetcher = new IdFetcher(this);
	}

	public String getGraphQLTypeName(GraphQLNode node) {
		return getType(node).getName();
	}

	public <T> GraphQLObjectType addByReflection(Class<T> clazz) {
		GraphQLObjectType.Builder b = buildByReflection(clazz, null);
		GraphQLObjectType objectType = b.build();
		add(clazz, objectType);
		return objectType;
	}

	public void add(Class<?> clazz, GraphQLObjectType objectType) {
		types.put(clazz, objectType);
	}

	public GraphQLSchema getSchema() {
		Set<GraphQLType> allTypes = Sets.newHashSet();
		for (GraphQLType t : types.values()) {
			allTypes.add(t);
		}

		allTypes.add(getNodeInterface());

		GraphQLSchema schema = GraphQLSchema.newSchema().query(queryType).build(allTypes);
		return schema;
	}

	public void setQueryType(GraphQLObjectType queryType) {
		this.queryType = queryType;

	}

	@Override
	public GraphQLObjectType getType(Object object) {
		Class<?> clazz = object.getClass();
		GraphQLObjectType graphQLType = types.get(clazz);
		if (graphQLType == null) {
			throw new IllegalArgumentException("Unknown class: " + clazz.getName());
		}
		return graphQLType;
	}

	public GraphQLInterfaceType getNodeInterface() {
		if (nodeInterface == null) {
			GraphQLInterfaceType.Builder b = GraphQLInterfaceType.newInterface().name("Node");
			b.field(GraphQLFieldDefinition.newFieldDefinition().type(Scalars.GraphQLID).name("id").build());
			b.typeResolver(this);
			nodeInterface = b.build();
		}
		return nodeInterface;
	}

	<T> GraphQLObjectType.Builder buildByReflection(Class<T> clazz, Object root) {
		String name = toGqlName(clazz);

		GraphQLObjectType.Builder b = GraphQLObjectType.newObject().name(name);

		for (Method method : clazz.getDeclaredMethods()) {
			String methodName = method.getName();

			String fieldName = null;
			if (methodName.startsWith("get")) {
				fieldName = methodName.substring(3);
			} else if (methodName.startsWith("is")) {
				fieldName = methodName.substring(2);
			}
			if (fieldName == null) {
				continue;
			}

			fieldName = Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);

			GraphQLOutputType outputType = null;
			Class<?> returnType = method.getReturnType();
			if (String.class.isAssignableFrom(returnType)) {
				outputType = Scalars.GraphQLString;
			} else if (Long.class.isAssignableFrom(returnType) || long.class.isAssignableFrom(returnType)) {
				outputType = Scalars.GraphQLLong;
			} else if (Integer.class.isAssignableFrom(returnType) || int.class.isAssignableFrom(returnType)) {
				outputType = Scalars.GraphQLInt;
			} else if (List.class.isAssignableFrom(returnType)) {
				Type genericReturnType = method.getGenericReturnType();
				if (genericReturnType instanceof ParameterizedType) {
					ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
					Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
					if (actualTypeArguments.length == 1) {
						Type elementType = actualTypeArguments[0];
						outputType = new GraphQLList(new GraphQLTypeReference(toGqlName((Class) elementType)));
					}
				}
			} else {
				outputType = new GraphQLTypeReference(toGqlName(returnType));
			}

			if (outputType == null) {
				log.warn("Unknown return type for method {}: {}", methodName, returnType);
				continue;
			}

			boolean special = false;
			// Special case GraphQLNode implementation
			if (GraphQLNode.class.isAssignableFrom(clazz)) {
				if (fieldName.equals("id")) {
					special = true;
					b.field(GraphQLFieldDefinition.newFieldDefinition().type(Scalars.GraphQLID).name(fieldName)
							.dataFetcher(idFetcher).build());
				}
			}

			if (!special) {
				GraphQLFieldDefinition.Builder fb = GraphQLFieldDefinition.newFieldDefinition().type(outputType)
						.name(fieldName);

				if (root != null) {
					fb.dataFetcher(RootDataFetcher.build(method, root));
				} else {
					fb.dataFetcher(ReflectionDataFetcher.build(method));
				}
				b.field(fb.build());
			}
		}

		if (GraphQLNode.class.isAssignableFrom(clazz)) {
			b.withInterface(getNodeInterface());
		}
		return b;
	}

	private String toGqlName(Class<?> clazz) {
		GqlName annotation = clazz.getAnnotation(GqlName.class);
		if (annotation != null) {
			return annotation.value();
		}
		return clazz.getSimpleName();
	}

	public <T> GraphQLObjectType addRootByReflection(final DataStore dataStore, T root) {
		// TODO: Does name need to be Query?
		// GraphQLObjectType.Builder b =
		// GraphQLObjectType.newObject().name("Query");

		Class<?> clazz = root.getClass();

		GraphQLObjectType.Builder b = buildByReflection(clazz, root);

		{
			List<GraphQLArgument> arguments = Lists.newArrayList();
			arguments.add(new GraphQLArgument("id", Scalars.GraphQLID));
			b.field(GraphQLFieldDefinition.newFieldDefinition().type(getNodeInterface()).argument(arguments)
					.dataFetcher(new DataFetcher() {

						@Override
						public Object get(DataFetchingEnvironment environment) {
							String id = environment.getArgument("id");
							return dataStore.getById(id);
						}
					}).name("node").build());
		}

		GraphQLObjectType objectType = b.build();
		add(clazz, objectType);
		return objectType;
	}
}
