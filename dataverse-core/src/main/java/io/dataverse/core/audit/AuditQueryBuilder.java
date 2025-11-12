package io.dataverse.core.audit;

import java.time.Instant;
import java.util.List;

/**
 * Fluent query builder for audit trail entries.
 *
 * <p>Provides a chainable API for building complex audit queries with multiple filters.
 *
 * <p>Example usage:
 * <pre>{@code
 * List<AuditEntry<Product>> updates = auditRepository
 *     .forEntityId(productId)
 *     .operation(AuditOperation.UPDATE)
 *     .between(startDate, endDate)
 *     .modifiedBy("admin@example.com")
 *     .orderByTimestamp(false)  // descending
 *     .limit(50)
 *     .execute();
 * }</pre>
 *
 * @param <T> the entity type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface AuditQueryBuilder<T> {

  /**
   * Filter by time range.
   *
   * @param start the start time (inclusive)
   * @param end the end time (inclusive)
   * @return this builder for chaining
   */
  AuditQueryBuilder<T> between(Instant start, Instant end);

  /**
   * Filter by operation type.
   *
   * @param operation the operation type (INSERT, UPDATE, DELETE)
   * @return this builder for chaining
   */
  AuditQueryBuilder<T> operation(AuditOperation operation);

  /**
   * Filter by user/principal who made the change.
   *
   * @param modifiedBy the user identifier
   * @return this builder for chaining
   */
  AuditQueryBuilder<T> modifiedBy(String modifiedBy);

  /**
   * Filter by entries after a specific timestamp.
   *
   * @param timestamp the timestamp (exclusive)
   * @return this builder for chaining
   */
  AuditQueryBuilder<T> after(Instant timestamp);

  /**
   * Filter by entries before a specific timestamp.
   *
   * @param timestamp the timestamp (exclusive)
   * @return this builder for chaining
   */
  AuditQueryBuilder<T> before(Instant timestamp);

  /**
   * Order results by timestamp.
   *
   * @param ascending true for ascending order, false for descending
   * @return this builder for chaining
   */
  AuditQueryBuilder<T> orderByTimestamp(boolean ascending);

  /**
   * Limit the number of results.
   *
   * @param limit the maximum number of results
   * @return this builder for chaining
   */
  AuditQueryBuilder<T> limit(int limit);

  /**
   * Skip the first N results (for pagination).
   *
   * @param offset the number of results to skip
   * @return this builder for chaining
   */
  AuditQueryBuilder<T> offset(int offset);

  /**
   * Execute the query and return results.
   *
   * @return list of audit entries matching the query
   */
  List<AuditEntry<T>> execute();

  /**
   * Execute the query and return the count of matching entries.
   *
   * @return the count of matching entries
   */
  long count();

  /**
   * Execute the query and return only the first result.
   *
   * @return the first audit entry, or null if no results
   */
  AuditEntry<T> first();

  /**
   * Execute the query and return only the last result (most recent).
   *
   * @return the last audit entry, or null if no results
   */
  AuditEntry<T> last();
}
