package io.dataverse.core.query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for native database queries with type safety.
 *
 * <p>Allows executing raw database queries while maintaining type safety for results.
 * Supports named parameters, pagination, and automatic result mapping.
 *
 * <p><strong>Key Features:</strong>
 * <ul>
 *   <li>Execute raw SQL, NoSQL, or database-specific queries</li>
 *   <li>Type-safe result mapping to entity classes or DTOs</li>
 *   <li>Named parameter binding with :paramName syntax</li>
 *   <li>Support for pagination and streaming results</li>
 *   <li>Compile-time query validation (optional)</li>
 * </ul>
 *
 * <p><strong>SQL Example (PostgreSQL):</strong>
 * <pre>{@code
 * public interface UserRepository extends DataRepository<User, Long> {
 *
 *     @NativeQuery("""
 *         SELECT * FROM users
 *         WHERE status = :status
 *         AND created_at > :since
 *         ORDER BY created_at DESC
 *         """)
 *     List<User> findActiveUsersSince(String status, LocalDateTime since);
 *
 *     @NativeQuery(value = """
 *         SELECT u.id, u.username, COUNT(o.id) as order_count
 *         FROM users u
 *         LEFT JOIN orders o ON u.id = o.user_id
 *         WHERE u.created_at > :since
 *         GROUP BY u.id, u.username
 *         HAVING COUNT(o.id) > :minOrders
 *         """,
 *         resultClass = UserOrderSummary.class)
 *     List<UserOrderSummary> findUsersWithMinOrders(LocalDateTime since, int minOrders);
 *
 *     @NativeQuery("SELECT * FROM users WHERE id = :id")
 *     Optional<User> findUserById(Long id);
 * }
 * }</pre>
 *
 * <p><strong>MongoDB Example:</strong>
 * <pre>{@code
 * public interface ProductRepository extends DataRepository<Product, String> {
 *
 *     @NativeQuery("""
 *         {
 *           "category": ":category",
 *           "price": { "$gte": :minPrice, "$lte": :maxPrice },
 *           "inStock": true
 *         }
 *         """)
 *     List<Product> findProductsByPriceRange(String category, double minPrice, double maxPrice);
 *
 *     @NativeQuery(value = """
 *         [
 *           { "$match": { "category": ":category" } },
 *           { "$group": {
 *               "_id": "$brand",
 *               "avgPrice": { "$avg": "$price" },
 *               "count": { "$sum": 1 }
 *             }
 *           },
 *           { "$sort": { "avgPrice": -1 } }
 *         ]
 *         """,
 *         resultClass = BrandStats.class,
 *         isAggregation = true)
 *     List<BrandStats> getAveragePriceByBrand(String category);
 * }
 * }</pre>
 *
 * <p><strong>Named Parameters:</strong>
 * <ul>
 *   <li>Use :paramName syntax in queries</li>
 *   <li>Method parameters matched by position or name</li>
 *   <li>Supports all primitive types, strings, dates, enums</li>
 *   <li>Collection parameters for IN clauses</li>
 * </ul>
 *
 * <p><strong>Return Types:</strong>
 * <ul>
 *   <li>{@code List<T>} - Multiple results</li>
 *   <li>{@code Optional<T>} - Single result or empty</li>
 *   <li>{@code T} - Single result (throws if not found)</li>
 *   <li>{@code Stream<T>} - Streaming results</li>
 *   <li>{@code Page<T>} - Paginated results</li>
 *   <li>Primitive types for counts/aggregations</li>
 * </ul>
 *
 * <p><strong>Result Mapping:</strong>
 * <pre>{@code
 * // Automatic mapping to entity
 * @NativeQuery("SELECT * FROM users WHERE active = true")
 * List<User> findActiveUsers();
 *
 * // Map to custom DTO
 * @NativeQuery(value = "SELECT id, username FROM users",
 *             resultClass = UserSummary.class)
 * List<UserSummary> getAllUserSummaries();
 *
 * // Scalar results
 * @NativeQuery("SELECT COUNT(*) FROM users WHERE active = true")
 * long countActiveUsers();
 *
 * @NativeQuery("SELECT username FROM users WHERE id = :id")
 * Optional<String> getUsernameById(Long id);
 * }</pre>
 *
 * <p><strong>Pagination:</strong>
 * <pre>{@code
 * @NativeQuery(value = "SELECT * FROM users WHERE active = true",
 *             countQuery = "SELECT COUNT(*) FROM users WHERE active = true")
 * Page<User> findActiveUsers(Pageable pageable);
 * }</pre>
 *
 * <p><strong>Best Practices:</strong>
 * <ul>
 *   <li>Use for complex queries that can't be expressed with QueryBuilder</li>
 *   <li>Prefer QueryBuilder for simple queries (better cross-database compatibility)</li>
 *   <li>Always use named parameters instead of string concatenation</li>
 *   <li>Specify resultClass when returning DTOs</li>
 *   <li>Test queries against production-like data volumes</li>
 *   <li>Consider adding database-specific comments for query hints</li>
 * </ul>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NativeQuery {

  /**
   * The native query string.
   *
   * <p>Supports named parameters using :paramName syntax.
   *
   * @return the query string
   */
  String value();

  /**
   * The result class for mapping query results.
   *
   * <p>Optional - defaults to the entity class or inferred from return type.
   *
   * @return the result class
   */
  Class<?> resultClass() default void.class;

  /**
   * Count query for pagination support.
   *
   * <p>Required when returning {@code Page<T>} results.
   *
   * @return the count query
   */
  String countQuery() default "";

  /**
   * Whether this is a native aggregation query (for MongoDB, etc.).
   *
   * @return true if aggregation query
   */
  boolean isAggregation() default false;

  /**
   * Whether this is a modification query (INSERT, UPDATE, DELETE).
   *
   * @return true if modification query
   */
  boolean isModification() default false;

  /**
   * Maximum number of results to return.
   *
   * <p>-1 means unlimited.
   *
   * @return max results
   */
  int maxResults() default -1;

  /**
   * Query timeout in milliseconds.
   *
   * <p>-1 means use default timeout.
   *
   * @return timeout in ms
   */
  int timeout() default -1;

  /**
   * Whether to use read-only connection (for SQL databases).
   *
   * @return true for read-only
   */
  boolean readOnly() default true;
}
