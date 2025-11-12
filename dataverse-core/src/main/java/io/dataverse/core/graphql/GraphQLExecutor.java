package io.dataverse.core.graphql;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executes GraphQL queries against a schema.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Create executor with schema
 * GraphQLExecutor executor = new GraphQLExecutor(schema);
 *
 * // Register resolvers
 * executor.registerResolver("User", (fieldName, arguments) -> {
 *     return switch (fieldName) {
 *         case "user" -> userRepository.findById((Long) arguments.get("id"));
 *         case "users" -> userRepository.findAll();
 *         default -> null;
 *     };
 * });
 *
 * // Execute query
 * GraphQLResult result = executor.execute(
 *     "query { users { id name email } }",
 *     Map.of()
 * );
 *
 * if (result.isSuccessful()) {
 *     Map<String, Object> data = result.getData();
 *     List<User> users = (List<User>) data.get("users");
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class GraphQLExecutor {

  private static final Logger logger = System.getLogger(GraphQLExecutor.class.getName());

  private static final Pattern QUERY_PATTERN = Pattern.compile(
      "(?:query|mutation|subscription)\\s*\\{\\s*(\\w+)(?:\\(([^)]*)\\))?",
      Pattern.CASE_INSENSITIVE
  );

  private final GraphQLSchema schema;
  private final Map<String, GraphQLResolver<?>> resolvers = new ConcurrentHashMap<>();

  /**
   * Creates executor with schema.
   *
   * @param schema the GraphQL schema
   */
  public GraphQLExecutor(GraphQLSchema schema) {
    this.schema = schema;
  }

  /**
   * Registers a resolver for a type.
   *
   * @param typeName the type name
   * @param resolver the resolver
   */
  public void registerResolver(String typeName, GraphQLResolver<?> resolver) {
    resolvers.put(typeName, resolver);
    logger.log(Level.INFO, "Registered resolver for type: " + typeName);
  }

  /**
   * Unregisters a resolver.
   *
   * @param typeName the type name
   */
  public void unregisterResolver(String typeName) {
    resolvers.remove(typeName);
    logger.log(Level.INFO, "Unregistered resolver for type: " + typeName);
  }

  /**
   * Executes a GraphQL query.
   *
   * @param query the GraphQL query string
   * @param variables query variables
   * @return execution result
   */
  public GraphQLResult execute(String query, Map<String, Object> variables) {
    try {
      logger.log(Level.DEBUG, "Executing GraphQL query: " + query);

      // Parse query to extract operation and field name
      Matcher matcher = QUERY_PATTERN.matcher(query);
      if (!matcher.find()) {
        return GraphQLResult.error("Invalid GraphQL query format");
      }

      String fieldName = matcher.group(1);
      String argumentsStr = matcher.group(2);

      logger.log(Level.DEBUG, "Field name: " + fieldName);
      logger.log(Level.DEBUG, "Arguments: " + argumentsStr);

      // Parse arguments
      Map<String, Object> arguments = parseArguments(argumentsStr, variables);

      // Find appropriate resolver
      GraphQLResolver<?> resolver = findResolver(fieldName);
      if (resolver == null) {
        return GraphQLResult.error("No resolver found for field: " + fieldName);
      }

      // Execute resolver
      Object result = resolver.resolve(fieldName, arguments);

      // Build response
      Map<String, Object> data = new HashMap<>();
      data.put(fieldName, result);

      return GraphQLResult.success(data);

    } catch (Exception e) {
      logger.log(Level.ERROR, "Error executing GraphQL query", e);
      return GraphQLResult.error("Execution error: " + e.getMessage());
    }
  }

  /**
   * Gets the schema.
   *
   * @return schema
   */
  public GraphQLSchema getSchema() {
    return schema;
  }

  /**
   * Gets all registered resolvers.
   *
   * @return resolver map
   */
  public Map<String, GraphQLResolver<?>> getResolvers() {
    return Map.copyOf(resolvers);
  }

  /**
   * Parses query arguments.
   */
  private Map<String, Object> parseArguments(String argumentsStr, Map<String, Object> variables) {
    Map<String, Object> arguments = new HashMap<>();

    if (argumentsStr == null || argumentsStr.trim().isEmpty()) {
      return arguments;
    }

    // Simple parsing - split by comma
    String[] parts = argumentsStr.split(",");
    for (String part : parts) {
      part = part.trim();
      if (part.isEmpty()) continue;

      String[] keyValue = part.split(":");
      if (keyValue.length != 2) continue;

      String key = keyValue[0].trim();
      String value = keyValue[1].trim();

      // Handle variable references
      if (value.startsWith("$")) {
        String varName = value.substring(1);
        arguments.put(key, variables.get(varName));
      } else {
        // Parse literal value
        arguments.put(key, parseLiteralValue(value));
      }
    }

    return arguments;
  }

  /**
   * Parses literal value.
   */
  private Object parseLiteralValue(String value) {
    // Remove quotes for strings
    if (value.startsWith("\"") && value.endsWith("\"")) {
      return value.substring(1, value.length() - 1);
    }

    // Try parsing as integer
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ignored) {
    }

    // Try parsing as double
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException ignored) {
    }

    // Try parsing as boolean
    if ("true".equalsIgnoreCase(value)) {
      return true;
    }
    if ("false".equalsIgnoreCase(value)) {
      return false;
    }

    // Default to string
    return value;
  }

  /**
   * Finds resolver for a field.
   */
  private GraphQLResolver<?> findResolver(String fieldName) {
    // Try exact match first
    if (resolvers.containsKey(fieldName)) {
      return resolvers.get(fieldName);
    }

    // Try to find by checking queries
    for (GraphQLSchema.QueryDefinition query : schema.getQueries()) {
      if (query.getName().equals(fieldName)) {
        // Extract type name from return type
        String returnType = query.getReturnType()
            .replace("[", "")
            .replace("]", "")
            .replace("!", "");

        return resolvers.get(returnType);
      }
    }

    // Try to find by checking mutations
    for (GraphQLSchema.MutationDefinition mutation : schema.getMutations()) {
      if (mutation.getName().equals(fieldName)) {
        String returnType = mutation.getReturnType()
            .replace("[", "")
            .replace("]", "")
            .replace("!", "");

        return resolvers.get(returnType);
      }
    }

    return null;
  }
}
