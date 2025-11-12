package io.dataverse.core.loading;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field or collection to be loaded eagerly (immediately with parent).
 *
 * <p>Eager loading fetches related entities in the same query as the parent,
 * or in a separate but immediate query, avoiding N+1 query problems at the
 * cost of loading more data upfront.
 *
 * <p><strong>Field-Level Usage:</strong>
 * <pre>{@code
 * public class User implements Entity<Long> {
 *     private Long id;
 *     private String username;
 *
 *     // Always loaded with user
 *     @EagerLoad
 *     private Profile profile;
 *
 *     // Small collection - always needed
 *     @EagerLoad
 *     private List<Role> roles;
 *
 *     // Eager load with LEFT JOIN
 *     @EagerLoad(joinType = JoinType.LEFT)
 *     private Address address;
 * }
 * }</pre>
 *
 * <p><strong>Query-Level Usage:</strong>
 * <pre>{@code
 * // Explicitly eager load relationships for this query
 * List<User> users = repository.query()
 *     .where("active").isTrue()
 *     .fetch("orders", FetchStrategy.EAGER)
 *     .fetch("orders.items", FetchStrategy.EAGER)  // Nested eager loading
 *     .execute();
 * }</pre>
 *
 * <p><strong>Repository Method Usage:</strong>
 * <pre>{@code
 * public interface UserRepository extends DataRepository<User, Long> {
 *     // Load orders eagerly with users
 *     @EagerLoad("orders")
 *     List<User> findByActiveTrue();
 *
 *     // Multiple eager loads
 *     @EagerLoad({"orders", "profile", "roles"})
 *     User findById(Long id);
 *
 *     // Nested eager loading
 *     @EagerLoad({"orders", "orders.items"})
 *     List<User> findUsersWithOrders();
 * }
 * }</pre>
 *
 * <p><strong>SQL Join Strategies:</strong>
 * <pre>{@code
 * // INNER JOIN - only users with orders
 * @EagerLoad(joinType = JoinType.INNER)
 * private List<Order> orders;
 *
 * // LEFT JOIN - all users, even without orders
 * @EagerLoad(joinType = JoinType.LEFT)
 * private List<Order> orders;
 *
 * // SEPARATE SELECT - two queries instead of join
 * @EagerLoad(joinType = JoinType.SELECT)
 * private List<Order> orders;
 * }</pre>
 *
 * <p><strong>When to Use Eager Loading:</strong>
 * <ul>
 *   <li>Small collections (< 100 items typically)</li>
 *   <li>Always accessed relationships</li>
 *   <li>Single relationships (one-to-one)</li>
 *   <li>Iterating over parent collection</li>
 *   <li>Detached/serialized entities</li>
 * </ul>
 *
 * <p><strong>When NOT to Use:</strong>
 * <ul>
 *   <li>Large collections (thousands of items)</li>
 *   <li>Rarely accessed relationships</li>
 *   <li>Deep nesting (> 2-3 levels)</li>
 *   <li>Memory-constrained environments</li>
 * </ul>
 *
 * <p><strong>Performance Examples:</strong>
 * <pre>{@code
 * // Without eager loading: N+1 queries
 * List<User> users = repository.findAll();  // 1 query
 * for (User user : users) {
 *     List<Order> orders = user.getOrders();  // N queries
 * }
 * // Total: 1 + N queries
 *
 * // With eager loading: 1-2 queries
 * @EagerLoad("orders")
 * List<User> users = repository.findAll();  // 1-2 queries (JOIN or SELECT)
 * for (User user : users) {
 *     List<Order> orders = user.getOrders();  // No query - already loaded
 * }
 * // Total: 1-2 queries
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface EagerLoad {

  /**
   * The field paths to load eagerly (for method annotations).
   *
   * <p>Use dot notation for nested paths: "orders.items"
   *
   * @return field paths to load eagerly
   */
  String[] value() default {};

  /**
   * Join type for SQL databases.
   *
   * <p>Controls how the join is performed:
   * <ul>
   *   <li>INNER - only parents with children</li>
   *   <li>LEFT - all parents, even without children</li>
   *   <li>SELECT - separate SELECT query instead of JOIN</li>
   * </ul>
   *
   * @return join type
   */
  JoinType joinType() default JoinType.LEFT;

  /**
   * Maximum depth for recursive eager loading.
   *
   * <p>Prevents infinite loops in self-referencing relationships.
   *
   * @return maximum depth, 0 for unlimited
   */
  int maxDepth() default 0;

  /**
   * Join type enumeration.
   */
  enum JoinType {
    /** INNER JOIN - only parents with children */
    INNER,

    /** LEFT JOIN - all parents, even without children */
    LEFT,

    /** Separate SELECT query instead of JOIN */
    SELECT
  }
}
