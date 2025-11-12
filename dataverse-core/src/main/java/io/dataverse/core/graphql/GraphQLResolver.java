package io.dataverse.core.graphql;

import java.util.Map;

/**
 * Interface for custom GraphQL resolvers.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * public class UserResolver implements GraphQLResolver<User> {
 *     private final UserRepository repository;
 *
 *     public UserResolver(UserRepository repository) {
 *         this.repository = repository;
 *     }
 *
 *     @Override
 *     public User resolve(String fieldName, Map<String, Object> arguments) {
 *         return switch (fieldName) {
 *             case "user" -> {
 *                 Long id = (Long) arguments.get("id");
 *                 yield repository.findById(id).orElse(null);
 *             }
 *             case "users" -> {
 *                 Integer limit = (Integer) arguments.getOrDefault("limit", 100);
 *                 Integer offset = (Integer) arguments.getOrDefault("offset", 0);
 *                 yield repository.findAll(limit, offset);
 *             }
 *             default -> null;
 *         };
 *     }
 * }
 * }</pre>
 *
 * @param <T> entity type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
@FunctionalInterface
public interface GraphQLResolver<T> {

  /**
   * Resolves a GraphQL field.
   *
   * @param fieldName the field name
   * @param arguments field arguments
   * @return resolved value
   */
  Object resolve(String fieldName, Map<String, Object> arguments);
}
