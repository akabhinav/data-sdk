package io.dataverse.core.replica;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a query method to use read replicas instead of the primary database.
 *
 * <p>Read replicas improve scalability by distributing read queries across multiple
 * database instances while write operations go to the primary.
 *
 * <p><strong>Benefits:</strong>
 * <ul>
 *   <li>Horizontal scaling of read capacity</li>
 *   <li>Reduced load on primary database</li>
 *   <li>Geographic distribution for lower latency</li>
 *   <li>Isolation of analytics queries from production traffic</li>
 * </ul>
 *
 * <p><strong>Consistency Considerations:</strong>
 * <ul>
 *   <li>Read replicas may lag behind primary (replication lag)</li>
 *   <li>Typical lag: 0-5 seconds</li>
 *   <li>Use primary for real-time data requirements</li>
 *   <li>Replicas suitable for: reports, analytics, list views</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * public interface UserRepository extends DataRepository<User, Long> {
 *     // Reads from replica - eventual consistency OK
 *     @ReadReplica
 *     List<User> findByActiveTrue();
 *
 *     @ReadReplica
 *     long countByDepartment(String department);
 *
 *     // Writes always go to primary
 *     User save(User user);  // No annotation needed
 *
 *     // Force primary read for consistency
 *     @ReadReplica(preferPrimary = true)
 *     User findById(Long id);
 * }
 * }</pre>
 *
 * <p><strong>Routing Strategy:</strong>
 * <ul>
 *   <li><strong>Round-robin</strong> - Distribute evenly across replicas</li>
 *   <li><strong>Least-connections</strong> - Route to least busy replica</li>
 *   <li><strong>Geographic</strong> - Route to nearest replica</li>
 *   <li><strong>Failover</strong> - Automatic fallback to primary if replica fails</li>
 * </ul>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReadReplica {

  /**
   * Whether to prefer primary even when replicas are available.
   *
   * <p>Use when read-your-writes consistency is required
   * (e.g., immediately after updating a user, reading that user's profile).
   *
   * @return true to prefer primary
   */
  boolean preferPrimary() default false;

  /**
   * Maximum replication lag tolerance in seconds.
   *
   * <p>If a replica's lag exceeds this threshold, it will not be used.
   * Set to 0 for strictest consistency (may reduce replica usage).
   *
   * @return max lag in seconds, -1 for no limit
   */
  int maxLagSeconds() default -1;

  /**
   * Routing strategy for selecting among multiple replicas.
   *
   * @return the routing strategy
   */
  RoutingStrategy strategy() default RoutingStrategy.ROUND_ROBIN;

  /**
   * Replica routing strategies.
   */
  enum RoutingStrategy {
    /** Distribute evenly in round-robin fashion */
    ROUND_ROBIN,

    /** Route to replica with least active connections */
    LEAST_CONNECTIONS,

    /** Route to geographically nearest replica */
    GEOGRAPHIC,

    /** Random selection among healthy replicas */
    RANDOM
  }
}
