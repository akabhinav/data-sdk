package io.dataverse.core.loading;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field or collection to be loaded lazily (on first access).
 *
 * <p>Lazy loading defers loading of related entities until they are accessed,
 * reducing initial query time and memory usage. However, it can lead to N+1
 * query problems if not used carefully.
 *
 * <p><strong>Field-Level Usage:</strong>
 * <pre>{@code
 * public class User implements Entity<Long> {
 *     private Long id;
 *     private String username;
 *
 *     // Lazy-loaded collection - only loaded when accessed
 *     @LazyLoad
 *     private List<Order> orders;
 *
 *     // Lazy-loaded single relationship
 *     @LazyLoad
 *     private Profile profile;
 *
 *     // Lazy-loaded with batch size (load 100 at a time)
 *     @LazyLoad(batchSize = 100)
 *     private List<Activity> activities;
 * }
 * }</pre>
 *
 * <p><strong>Query-Level Usage:</strong>
 * <pre>{@code
 * // Override eager loading at query time
 * List<User> users = repository.query()
 *     .where("active").isTrue()
 *     .fetch("orders", FetchStrategy.LAZY)  // Force lazy
 *     .execute();
 * }</pre>
 *
 * <p><strong>Repository Method Usage:</strong>
 * <pre>{@code
 * public interface UserRepository extends DataRepository<User, Long> {
 *     // Orders loaded lazily by default
 *     List<User> findByActiveTrue();
 *
 *     // Explicitly specify lazy loading
 *     @LazyLoad("orders")
 *     List<User> findByDepartment(String department);
 * }
 * }</pre>
 *
 * <p><strong>When to Use Lazy Loading:</strong>
 * <ul>
 *   <li>Large collections that are rarely needed</li>
 *   <li>Optional relationships (might not be accessed)</li>
 *   <li>Memory-constrained environments</li>
 *   <li>Initial page load needs to be fast</li>
 * </ul>
 *
 * <p><strong>When NOT to Use:</strong>
 * <ul>
 *   <li>Always accessed relationships (use @EagerLoad)</li>
 *   <li>Iterating over collections (causes N+1 problem)</li>
 *   <li>Detached/serialized entities</li>
 *   <li>No active database connection available</li>
 * </ul>
 *
 * <p><strong>Avoiding N+1 Problem:</strong>
 * <pre>{@code
 * // BAD: N+1 queries
 * List<User> users = repository.findAll();  // 1 query
 * for (User user : users) {
 *     // N queries - one per user!
 *     List<Order> orders = user.getOrders();
 *     process(orders);
 * }
 *
 * // GOOD: Batch loading
 * @LazyLoad(batchSize = 100)
 * private List<Order> orders;
 *
 * // OR: Use eager loading for this query
 * List<User> users = repository.query()
 *     .fetch("orders", FetchStrategy.EAGER)
 *     .execute();
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LazyLoad {

  /**
   * The field paths to load lazily (for method annotations).
   *
   * <p>Use dot notation for nested paths: "orders.items"
   *
   * @return field paths to load lazily
   */
  String[] value() default {};

  /**
   * Batch size for batch loading.
   *
   * <p>If set, uses batch loading strategy instead of pure lazy loading.
   * Loads related entities in batches of this size to avoid N+1 problem.
   *
   * <p>For example, with batchSize=100:
   * <ul>
   *   <li>Load first 100 parent entities</li>
   *   <li>On first child access, load ALL children for those 100 parents in one query</li>
   *   <li>Repeat for next batch</li>
   * </ul>
   *
   * @return batch size, 0 for pure lazy loading
   */
  int batchSize() default 0;

  /**
   * Whether to throw an exception if accessed after session is closed.
   *
   * <p>If false, returns null/empty collection when accessed without active connection.
   *
   * @return true to fail on access without connection
   */
  boolean failOnDetachedAccess() default true;
}
