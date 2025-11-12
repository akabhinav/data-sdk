package io.dataverse.core.query;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Executor for native database queries with type-safe result mapping.
 *
 * <p>Provides a fluent API for building and executing native queries programmatically,
 * with support for named parameters, pagination, and result streaming.
 *
 * <p><strong>Usage Example (SQL):</strong>
 * <pre>{@code
 * // Execute native SQL query
 * List<User> users = repository.nativeQuery(User.class)
 *     .sql("""
 *         SELECT * FROM users
 *         WHERE status = :status
 *         AND created_at > :since
 *         ORDER BY created_at DESC
 *         """)
 *     .parameter("status", "ACTIVE")
 *     .parameter("since", LocalDateTime.now().minusDays(7))
 *     .execute();
 *
 * // Map to DTO
 * List<UserSummary> summaries = repository.nativeQuery(UserSummary.class)
 *     .sql("""
 *         SELECT id, username, email
 *         FROM users
 *         WHERE department = :dept
 *         """)
 *     .parameter("dept", "Engineering")
 *     .execute();
 *
 * // Single result
 * Optional<User> user = repository.nativeQuery(User.class)
 *     .sql("SELECT * FROM users WHERE id = :id")
 *     .parameter("id", 123L)
 *     .executeSingle();
 *
 * // Streaming results
 * try (Stream<User> stream = repository.nativeQuery(User.class)
 *         .sql("SELECT * FROM users WHERE active = true")
 *         .executeStream()) {
 *     stream.forEach(System.out::println);
 * }
 *
 * // Scalar result
 * long count = repository.nativeQuery(Long.class)
 *     .sql("SELECT COUNT(*) FROM users WHERE active = :active")
 *     .parameter("active", true)
 *     .executeSingle()
 *     .orElse(0L);
 * }</pre>
 *
 * <p><strong>Usage Example (MongoDB):</strong>
 * <pre>{@code
 * // MongoDB find query
 * List<Product> products = repository.nativeQuery(Product.class)
 *     .mongoQuery("""
 *         {
 *           "category": ":category",
 *           "price": { "$gte": :minPrice },
 *           "inStock": true
 *         }
 *         """)
 *     .parameter("category", "Electronics")
 *     .parameter("minPrice", 100.0)
 *     .execute();
 *
 * // MongoDB aggregation
 * List<CategoryStats> stats = repository.nativeQuery(CategoryStats.class)
 *     .mongoAggregation("""
 *         [
 *           { "$match": { "active": true } },
 *           { "$group": {
 *               "_id": "$category",
 *               "count": { "$sum": 1 },
 *               "avgPrice": { "$avg": "$price" }
 *             }
 *           },
 *           { "$sort": { "count": -1 } }
 *         ]
 *         """)
 *     .execute();
 * }</pre>
 *
 * <p><strong>Parameter Binding:</strong>
 * <pre>{@code
 * // Named parameters
 * query.parameter("userId", 123L)
 *      .parameter("status", "ACTIVE")
 *      .parameter("since", LocalDateTime.now());
 *
 * // Map of parameters
 * Map<String, Object> params = Map.of(
 *     "userId", 123L,
 *     "status", "ACTIVE"
 * );
 * query.parameters(params);
 *
 * // Collection parameters for IN clauses
 * query.parameter("ids", List.of(1L, 2L, 3L));
 * }</pre>
 *
 * @param <T> the result type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface NativeQueryExecutor<T> {

  /**
   * Sets the native SQL query.
   *
   * @param sql the SQL query with named parameters
   * @return this executor
   */
  NativeQueryExecutor<T> sql(String sql);

  /**
   * Sets a MongoDB find query.
   *
   * @param query the MongoDB query JSON
   * @return this executor
   */
  NativeQueryExecutor<T> mongoQuery(String query);

  /**
   * Sets a MongoDB aggregation pipeline.
   *
   * @param pipeline the aggregation pipeline JSON
   * @return this executor
   */
  NativeQueryExecutor<T> mongoAggregation(String pipeline);

  /**
   * Sets a named parameter value.
   *
   * @param name the parameter name (without :)
   * @param value the parameter value
   * @return this executor
   */
  NativeQueryExecutor<T> parameter(String name, Object value);

  /**
   * Sets multiple parameters at once.
   *
   * @param parameters map of parameter names to values
   * @return this executor
   */
  NativeQueryExecutor<T> parameters(Map<String, Object> parameters);

  /**
   * Sets the maximum number of results.
   *
   * @param maxResults maximum results
   * @return this executor
   */
  NativeQueryExecutor<T> maxResults(int maxResults);

  /**
   * Sets the query timeout.
   *
   * @param timeoutMs timeout in milliseconds
   * @return this executor
   */
  NativeQueryExecutor<T> timeout(int timeoutMs);

  /**
   * Marks this as a read-only query.
   *
   * @param readOnly true for read-only
   * @return this executor
   */
  NativeQueryExecutor<T> readOnly(boolean readOnly);

  /**
   * Executes the query and returns all results.
   *
   * @return list of results
   */
  List<T> execute();

  /**
   * Executes the query and returns the first result if any.
   *
   * @return optional containing first result
   */
  Optional<T> executeFirst();

  /**
   * Executes the query and returns a single result.
   *
   * @return optional containing single result
   * @throws IllegalStateException if more than one result
   */
  Optional<T> executeSingle();

  /**
   * Executes the query and streams results.
   *
   * <p>The returned stream must be closed to release resources.
   *
   * @return stream of results
   */
  Stream<T> executeStream();

  /**
   * Executes an update/delete query.
   *
   * @return number of rows affected
   */
  int executeUpdate();
}
