package io.dataverse.core.graphql;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Generates GraphQL schemas from annotated entities.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Generate schema from entities
 * GraphQLSchema schema = GraphQLSchemaGenerator.generate(
 *     User.class,
 *     Order.class,
 *     Product.class
 * );
 *
 * // Get SDL
 * String sdl = schema.toSDL();
 * System.out.println(sdl);
 *
 * // Register resolvers
 * GraphQLExecutor executor = new GraphQLExecutor(schema);
 * executor.registerResolver("User", new UserResolver(userRepository));
 * executor.registerResolver("Order", new OrderResolver(orderRepository));
 *
 * // Execute queries
 * GraphQLResult result = executor.execute(
 *     "query { users(limit: 10) { id name email } }",
 *     Collections.emptyMap()
 * );
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class GraphQLSchemaGenerator {

  private static final Logger logger = System.getLogger(GraphQLSchemaGenerator.class.getName());

  /**
   * Generates GraphQL schema from entity classes.
   *
   * @param entityClasses entity classes annotated with @GraphQLEntity
   * @return generated schema
   */
  public static GraphQLSchema generate(Class<?>... entityClasses) {
    GraphQLSchema schema = new GraphQLSchema();

    for (Class<?> entityClass : entityClasses) {
      if (!entityClass.isAnnotationPresent(GraphQLEntity.class)) {
        logger.log(Level.WARNING, "Class " + entityClass.getName() +
            " is not annotated with @GraphQLEntity");
        continue;
      }

      generateForEntity(schema, entityClass);
    }

    return schema;
  }

  /**
   * Generates schema elements for a single entity.
   */
  private static void generateForEntity(GraphQLSchema schema, Class<?> entityClass) {
    GraphQLEntity annotation = entityClass.getAnnotation(GraphQLEntity.class);

    String typeName = annotation.name().isEmpty() ?
        entityClass.getSimpleName() : annotation.name();

    String description = annotation.description().isEmpty() ?
        null : annotation.description();

    logger.log(Level.INFO, "Generating GraphQL schema for: " + typeName);

    // Generate type definition
    GraphQLSchema.TypeDefinition typeDefinition = new GraphQLSchema.TypeDefinition(
        typeName, description
    );

    generateFields(typeDefinition, entityClass);
    schema.addType(typeDefinition);

    // Generate queries
    if (annotation.generateQueries()) {
      generateQueries(schema, typeName);
    }

    // Generate mutations
    if (annotation.generateMutations()) {
      generateMutations(schema, typeName);
    }

    // Generate subscriptions
    if (annotation.generateSubscriptions()) {
      generateSubscriptions(schema, typeName);
    }
  }

  /**
   * Generates field definitions from entity methods.
   */
  private static void generateFields(GraphQLSchema.TypeDefinition typeDefinition,
                                     Class<?> entityClass) {
    for (Method method : entityClass.getMethods()) {
      // Skip if annotated with @GraphQLIgnore
      if (method.isAnnotationPresent(GraphQLIgnore.class)) {
        continue;
      }

      // Only process getters
      if (!isGetter(method)) {
        continue;
      }

      String fieldName = extractFieldName(method);
      String fieldType = determineFieldType(method);
      String description = null;
      boolean nullable = true;
      boolean deprecated = false;
      String deprecationReason = null;

      // Check for @GraphQLField annotation
      if (method.isAnnotationPresent(GraphQLField.class)) {
        GraphQLField fieldAnnotation = method.getAnnotation(GraphQLField.class);

        if (!fieldAnnotation.name().isEmpty()) {
          fieldName = fieldAnnotation.name();
        }

        if (!fieldAnnotation.description().isEmpty()) {
          description = fieldAnnotation.description();
        }

        if (!fieldAnnotation.type().isEmpty()) {
          fieldType = fieldAnnotation.type();
        }

        nullable = fieldAnnotation.nullable();
        deprecated = fieldAnnotation.deprecated();
        deprecationReason = fieldAnnotation.deprecationReason();
      }

      GraphQLSchema.FieldDefinition fieldDefinition = new GraphQLSchema.FieldDefinition(
          fieldName, fieldType, description, nullable, deprecated, deprecationReason
      );

      typeDefinition.addField(fieldDefinition);
    }
  }

  /**
   * Generates query definitions.
   */
  private static void generateQueries(GraphQLSchema schema, String typeName) {
    // Single entity query: user(id: ID!): User
    GraphQLSchema.QueryDefinition singleQuery = new GraphQLSchema.QueryDefinition(
        toLowerCamelCase(typeName),
        typeName
    );
    singleQuery.addArgument(new GraphQLSchema.ArgumentDefinition("id", "ID", true));
    schema.addQuery(singleQuery);

    // List query: users(limit: Int, offset: Int): [User!]!
    GraphQLSchema.QueryDefinition listQuery = new GraphQLSchema.QueryDefinition(
        toLowerCamelCase(typeName) + "s",
        "[" + typeName + "!]!"
    );
    listQuery.addArgument(new GraphQLSchema.ArgumentDefinition("limit", "Int", false));
    listQuery.addArgument(new GraphQLSchema.ArgumentDefinition("offset", "Int", false));
    schema.addQuery(listQuery);
  }

  /**
   * Generates mutation definitions.
   */
  private static void generateMutations(GraphQLSchema schema, String typeName) {
    String inputType = typeName + "Input";

    // Create: createUser(input: UserInput!): User!
    GraphQLSchema.MutationDefinition createMutation = new GraphQLSchema.MutationDefinition(
        "create" + typeName,
        typeName + "!"
    );
    createMutation.addArgument(new GraphQLSchema.ArgumentDefinition("input", inputType, true));
    schema.addMutation(createMutation);

    // Update: updateUser(id: ID!, input: UserInput!): User!
    GraphQLSchema.MutationDefinition updateMutation = new GraphQLSchema.MutationDefinition(
        "update" + typeName,
        typeName + "!"
    );
    updateMutation.addArgument(new GraphQLSchema.ArgumentDefinition("id", "ID", true));
    updateMutation.addArgument(new GraphQLSchema.ArgumentDefinition("input", inputType, true));
    schema.addMutation(updateMutation);

    // Delete: deleteUser(id: ID!): Boolean!
    GraphQLSchema.MutationDefinition deleteMutation = new GraphQLSchema.MutationDefinition(
        "delete" + typeName,
        "Boolean!"
    );
    deleteMutation.addArgument(new GraphQLSchema.ArgumentDefinition("id", "ID", true));
    schema.addMutation(deleteMutation);
  }

  /**
   * Generates subscription definitions.
   */
  private static void generateSubscriptions(GraphQLSchema schema, String typeName) {
    // Created: userCreated: User!
    GraphQLSchema.SubscriptionDefinition createdSub = new GraphQLSchema.SubscriptionDefinition(
        toLowerCamelCase(typeName) + "Created",
        typeName + "!"
    );
    schema.addSubscription(createdSub);

    // Updated: userUpdated(id: ID!): User!
    GraphQLSchema.SubscriptionDefinition updatedSub = new GraphQLSchema.SubscriptionDefinition(
        toLowerCamelCase(typeName) + "Updated",
        typeName + "!"
    );
    updatedSub.addArgument(new GraphQLSchema.ArgumentDefinition("id", "ID", false));
    schema.addSubscription(updatedSub);

    // Deleted: userDeleted(id: ID!): ID!
    GraphQLSchema.SubscriptionDefinition deletedSub = new GraphQLSchema.SubscriptionDefinition(
        toLowerCamelCase(typeName) + "Deleted",
        "ID!"
    );
    deletedSub.addArgument(new GraphQLSchema.ArgumentDefinition("id", "ID", false));
    schema.addSubscription(deletedSub);
  }

  /**
   * Checks if a method is a getter.
   */
  private static boolean isGetter(Method method) {
    String name = method.getName();

    // Must start with "get" or "is"
    if (!name.startsWith("get") && !name.startsWith("is")) {
      return false;
    }

    // Must have no parameters
    if (method.getParameterCount() != 0) {
      return false;
    }

    // Must return something
    if (method.getReturnType() == void.class) {
      return false;
    }

    return true;
  }

  /**
   * Extracts field name from getter method.
   */
  private static String extractFieldName(Method method) {
    String name = method.getName();

    if (name.startsWith("get")) {
      name = name.substring(3);
    } else if (name.startsWith("is")) {
      name = name.substring(2);
    }

    // Convert to camelCase
    if (!name.isEmpty()) {
      name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    return name;
  }

  /**
   * Determines GraphQL type from method return type.
   */
  private static String determineFieldType(Method method) {
    Class<?> returnType = method.getReturnType();

    // Handle collections
    if (Collection.class.isAssignableFrom(returnType) || returnType.isArray()) {
      Type genericReturnType = method.getGenericReturnType();

      if (genericReturnType instanceof ParameterizedType paramType) {
        Type[] typeArgs = paramType.getActualTypeArguments();
        if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?>) {
          Class<?> elementType = (Class<?>) typeArgs[0];
          String elementTypeName = mapJavaTypeToGraphQL(elementType);
          return "[" + elementTypeName + "]";
        }
      }

      // Fallback for arrays
      if (returnType.isArray()) {
        String elementTypeName = mapJavaTypeToGraphQL(returnType.getComponentType());
        return "[" + elementTypeName + "]";
      }

      return "[String]"; // Default fallback
    }

    return mapJavaTypeToGraphQL(returnType);
  }

  /**
   * Maps Java type to GraphQL type.
   */
  private static String mapJavaTypeToGraphQL(Class<?> javaType) {
    // Primitives and wrappers
    if (javaType == Long.class || javaType == long.class) {
      return "ID";
    }

    if (javaType == Integer.class || javaType == int.class ||
        javaType == Short.class || javaType == short.class ||
        javaType == Byte.class || javaType == byte.class) {
      return "Int";
    }

    if (javaType == Double.class || javaType == double.class ||
        javaType == Float.class || javaType == float.class) {
      return "Float";
    }

    if (javaType == Boolean.class || javaType == boolean.class) {
      return "Boolean";
    }

    if (javaType == String.class) {
      return "String";
    }

    // Custom types - use simple class name
    return javaType.getSimpleName();
  }

  /**
   * Converts string to lowerCamelCase.
   */
  private static String toLowerCamelCase(String str) {
    if (str == null || str.isEmpty()) {
      return str;
    }
    return Character.toLowerCase(str.charAt(0)) + str.substring(1);
  }
}
