package io.dataverse.core.audit;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * Repository for querying and managing audit trail entries.
 *
 * <p>Provides a fluent API for querying audit history with filters for:
 * <ul>
 *   <li>Entity or entity ID</li>
 *   <li>Time range</li>
 *   <li>Operation type (INSERT, UPDATE, DELETE)</li>
 *   <li>User/principal who made changes</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Query all changes to a specific product
 * List<AuditEntry<Product>> history = auditRepository
 *     .forEntityId(productId)
 *     .execute();
 *
 * // Query updates in a specific time range
 * List<AuditEntry<Product>> updates = auditRepository
 *     .forEntityType(Product.class)
 *     .operation(AuditOperation.UPDATE)
 *     .between(startDate, endDate)
 *     .execute();
 * }</pre>
 *
 * @param <T> the entity type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface AuditRepository<T> {

  /**
   * Start building a query for audit entries of a specific entity.
   *
   * @param entity the entity to query audit history for
   * @return a query builder for further filtering
   */
  AuditQueryBuilder<T> forEntity(T entity);

  /**
   * Start building a query for audit entries with a specific entity ID.
   *
   * @param entityId the entity ID to query
   * @return a query builder for further filtering
   */
  AuditQueryBuilder<T> forEntityId(Serializable entityId);

  /**
   * Start building a query for all audit entries of a specific entity type.
   *
   * @param entityType the entity class
   * @return a query builder for further filtering
   */
  AuditQueryBuilder<T> forEntityType(Class<T> entityType);

  /**
   * Find all audit entries (use with caution in production).
   *
   * @return all audit entries
   */
  List<AuditEntry<T>> findAll();

  /**
   * Find all audit entries for a specific user.
   *
   * @param modifiedBy the user/principal who made changes
   * @return all audit entries for that user
   */
  List<AuditEntry<T>> findByModifiedBy(String modifiedBy);

  /**
   * Find all audit entries within a time range.
   *
   * @param start the start time (inclusive)
   * @param end the end time (inclusive)
   * @return all audit entries in the time range
   */
  List<AuditEntry<T>> findByTimestampBetween(Instant start, Instant end);

  /**
   * Save an audit entry.
   *
   * @param entry the audit entry to save
   */
  void save(AuditEntry<T> entry);

  /**
   * Count total audit entries.
   *
   * @return the total count
   */
  long count();

  /**
   * Delete audit entries older than a specific timestamp.
   * Useful for audit retention policies.
   *
   * @param before the timestamp before which entries should be deleted
   * @return the number of entries deleted
   */
  long deleteEntriesOlderThan(Instant before);
}
