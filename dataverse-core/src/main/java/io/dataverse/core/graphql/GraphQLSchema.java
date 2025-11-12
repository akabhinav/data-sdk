package io.dataverse.core.graphql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a generated GraphQL schema.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * GraphQLSchema schema = GraphQLSchemaGenerator.generate(User.class, Order.class);
 *
 * // Get SDL (Schema Definition Language)
 * String sdl = schema.toSDL();
 *
 * // Execute query
 * GraphQLResult result = schema.execute(
 *     "query { users { id name } }",
 *     Collections.emptyMap()
 * );
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class GraphQLSchema {

  private final Map<String, TypeDefinition> types = new LinkedHashMap<>();
  private final List<QueryDefinition> queries = new ArrayList<>();
  private final List<MutationDefinition> mutations = new ArrayList<>();
  private final List<SubscriptionDefinition> subscriptions = new ArrayList<>();

  /**
   * Adds a type definition.
   *
   * @param type the type definition
   */
  public void addType(TypeDefinition type) {
    types.put(type.getName(), type);
  }

  /**
   * Adds a query definition.
   *
   * @param query the query definition
   */
  public void addQuery(QueryDefinition query) {
    queries.add(query);
  }

  /**
   * Adds a mutation definition.
   *
   * @param mutation the mutation definition
   */
  public void addMutation(MutationDefinition mutation) {
    mutations.add(mutation);
  }

  /**
   * Adds a subscription definition.
   *
   * @param subscription the subscription definition
   */
  public void addSubscription(SubscriptionDefinition subscription) {
    subscriptions.add(subscription);
  }

  /**
   * Gets all type definitions.
   *
   * @return unmodifiable map of types
   */
  public Map<String, TypeDefinition> getTypes() {
    return Collections.unmodifiableMap(types);
  }

  /**
   * Gets all query definitions.
   *
   * @return unmodifiable list of queries
   */
  public List<QueryDefinition> getQueries() {
    return Collections.unmodifiableList(queries);
  }

  /**
   * Gets all mutation definitions.
   *
   * @return unmodifiable list of mutations
   */
  public List<MutationDefinition> getMutations() {
    return Collections.unmodifiableList(mutations);
  }

  /**
   * Gets all subscription definitions.
   *
   * @return unmodifiable list of subscriptions
   */
  public List<SubscriptionDefinition> getSubscriptions() {
    return Collections.unmodifiableList(subscriptions);
  }

  /**
   * Converts schema to SDL (Schema Definition Language).
   *
   * @return SDL representation
   */
  public String toSDL() {
    StringBuilder sdl = new StringBuilder();

    // Type definitions
    for (TypeDefinition type : types.values()) {
      sdl.append(type.toSDL()).append("\n\n");
    }

    // Query type
    if (!queries.isEmpty()) {
      sdl.append("type Query {\n");
      for (QueryDefinition query : queries) {
        sdl.append("  ").append(query.toSDL()).append("\n");
      }
      sdl.append("}\n\n");
    }

    // Mutation type
    if (!mutations.isEmpty()) {
      sdl.append("type Mutation {\n");
      for (MutationDefinition mutation : mutations) {
        sdl.append("  ").append(mutation.toSDL()).append("\n");
      }
      sdl.append("}\n\n");
    }

    // Subscription type
    if (!subscriptions.isEmpty()) {
      sdl.append("type Subscription {\n");
      for (SubscriptionDefinition subscription : subscriptions) {
        sdl.append("  ").append(subscription.toSDL()).append("\n");
      }
      sdl.append("}\n\n");
    }

    return sdl.toString().trim();
  }

  /**
   * Type definition.
   */
  public static class TypeDefinition {
    private final String name;
    private final String description;
    private final List<FieldDefinition> fields = new ArrayList<>();

    public TypeDefinition(String name, String description) {
      this.name = name;
      this.description = description;
    }

    public void addField(FieldDefinition field) {
      fields.add(field);
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

    public List<FieldDefinition> getFields() {
      return Collections.unmodifiableList(fields);
    }

    public String toSDL() {
      StringBuilder sdl = new StringBuilder();

      if (description != null && !description.isEmpty()) {
        sdl.append("\"\"\"").append(description).append("\"\"\"\n");
      }

      sdl.append("type ").append(name).append(" {\n");

      for (FieldDefinition field : fields) {
        sdl.append("  ").append(field.toSDL()).append("\n");
      }

      sdl.append("}");

      return sdl.toString();
    }
  }

  /**
   * Field definition.
   */
  public static class FieldDefinition {
    private final String name;
    private final String type;
    private final String description;
    private final boolean nullable;
    private final boolean deprecated;
    private final String deprecationReason;

    public FieldDefinition(String name, String type, String description,
                           boolean nullable, boolean deprecated, String deprecationReason) {
      this.name = name;
      this.type = type;
      this.description = description;
      this.nullable = nullable;
      this.deprecated = deprecated;
      this.deprecationReason = deprecationReason;
    }

    public String getName() {
      return name;
    }

    public String getType() {
      return type;
    }

    public String toSDL() {
      StringBuilder sdl = new StringBuilder();

      if (description != null && !description.isEmpty()) {
        sdl.append("\"\"\"").append(description).append("\"\"\" ");
      }

      sdl.append(name).append(": ").append(type);

      if (!nullable) {
        sdl.append("!");
      }

      if (deprecated) {
        sdl.append(" @deprecated");
        if (deprecationReason != null && !deprecationReason.isEmpty()) {
          sdl.append("(reason: \"").append(deprecationReason).append("\")");
        }
      }

      return sdl.toString();
    }
  }

  /**
   * Query definition.
   */
  public static class QueryDefinition {
    private final String name;
    private final String returnType;
    private final List<ArgumentDefinition> arguments = new ArrayList<>();

    public QueryDefinition(String name, String returnType) {
      this.name = name;
      this.returnType = returnType;
    }

    public void addArgument(ArgumentDefinition argument) {
      arguments.add(argument);
    }

    public String getName() {
      return name;
    }

    public String getReturnType() {
      return returnType;
    }

    public String toSDL() {
      StringBuilder sdl = new StringBuilder();
      sdl.append(name);

      if (!arguments.isEmpty()) {
        sdl.append("(");
        for (int i = 0; i < arguments.size(); i++) {
          if (i > 0) sdl.append(", ");
          sdl.append(arguments.get(i).toSDL());
        }
        sdl.append(")");
      }

      sdl.append(": ").append(returnType);

      return sdl.toString();
    }
  }

  /**
   * Mutation definition.
   */
  public static class MutationDefinition {
    private final String name;
    private final String returnType;
    private final List<ArgumentDefinition> arguments = new ArrayList<>();

    public MutationDefinition(String name, String returnType) {
      this.name = name;
      this.returnType = returnType;
    }

    public void addArgument(ArgumentDefinition argument) {
      arguments.add(argument);
    }

    public String getName() {
      return name;
    }

    public String getReturnType() {
      return returnType;
    }

    public String toSDL() {
      StringBuilder sdl = new StringBuilder();
      sdl.append(name);

      if (!arguments.isEmpty()) {
        sdl.append("(");
        for (int i = 0; i < arguments.size(); i++) {
          if (i > 0) sdl.append(", ");
          sdl.append(arguments.get(i).toSDL());
        }
        sdl.append(")");
      }

      sdl.append(": ").append(returnType);

      return sdl.toString();
    }
  }

  /**
   * Subscription definition.
   */
  public static class SubscriptionDefinition {
    private final String name;
    private final String returnType;
    private final List<ArgumentDefinition> arguments = new ArrayList<>();

    public SubscriptionDefinition(String name, String returnType) {
      this.name = name;
      this.returnType = returnType;
    }

    public void addArgument(ArgumentDefinition argument) {
      arguments.add(argument);
    }

    public String getName() {
      return name;
    }

    public String getReturnType() {
      return returnType;
    }

    public String toSDL() {
      StringBuilder sdl = new StringBuilder();
      sdl.append(name);

      if (!arguments.isEmpty()) {
        sdl.append("(");
        for (int i = 0; i < arguments.size(); i++) {
          if (i > 0) sdl.append(", ");
          sdl.append(arguments.get(i).toSDL());
        }
        sdl.append(")");
      }

      sdl.append(": ").append(returnType);

      return sdl.toString();
    }
  }

  /**
   * Argument definition.
   */
  public static class ArgumentDefinition {
    private final String name;
    private final String type;
    private final boolean required;

    public ArgumentDefinition(String name, String type, boolean required) {
      this.name = name;
      this.type = type;
      this.required = required;
    }

    public String getName() {
      return name;
    }

    public String getType() {
      return type;
    }

    public boolean isRequired() {
      return required;
    }

    public String toSDL() {
      StringBuilder sdl = new StringBuilder();
      sdl.append(name).append(": ").append(type);

      if (required) {
        sdl.append("!");
      }

      return sdl.toString();
    }
  }
}
