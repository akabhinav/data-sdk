package io.dataverse.core.graphql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an entity for GraphQL schema generation.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * @GraphQLEntity(name = "User")
 * public class User {
 *     private Long id;
 *     private String name;
 *     private String email;
 *
 *     @GraphQLField(description = "User's unique identifier")
 *     public Long getId() { return id; }
 *
 *     @GraphQLField(description = "User's full name")
 *     public String getName() { return name; }
 *
 *     @GraphQLIgnore
 *     public String getInternalField() { return "internal"; }
 * }
 * }</pre>
 *
 * <p>Generated GraphQL schema:
 * <pre>{@code
 * type User {
 *   id: ID!
 *   name: String
 *   email: String
 * }
 *
 * type Query {
 *   user(id: ID!): User
 *   users(limit: Int, offset: Int): [User!]!
 * }
 *
 * type Mutation {
 *   createUser(input: UserInput!): User!
 *   updateUser(id: ID!, input: UserInput!): User!
 *   deleteUser(id: ID!): Boolean!
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GraphQLEntity {

  /**
   * GraphQL type name.
   *
   * <p>Defaults to simple class name.
   *
   * @return type name
   */
  String name() default "";

  /**
   * GraphQL type description.
   *
   * @return type description
   */
  String description() default "";

  /**
   * Whether to generate queries for this entity.
   *
   * @return true to generate queries
   */
  boolean generateQueries() default true;

  /**
   * Whether to generate mutations for this entity.
   *
   * @return true to generate mutations
   */
  boolean generateMutations() default true;

  /**
   * Whether to generate subscriptions for this entity.
   *
   * @return true to generate subscriptions
   */
  boolean generateSubscriptions() default false;
}
