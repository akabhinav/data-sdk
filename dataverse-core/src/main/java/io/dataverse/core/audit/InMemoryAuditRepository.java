package io.dataverse.core.audit;

import io.dataverse.api.Entity;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * In-memory implementation of AuditRepository.
 *
 * <p>This implementation stores audit entries in memory using thread-safe collections.
 * Suitable for development, testing, or as a fallback when no persistent audit storage is configured.
 *
 * <p><strong>Note:</strong> All audit data is lost when the application restarts.
 * For production use, consider implementing a persistent audit repository using a database.
 *
 * @param <T> the entity type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class InMemoryAuditRepository<T> implements AuditRepository<T> {

  private final List<AuditEntry<T>> entries = new CopyOnWriteArrayList<>();
  private final Class<T> entityType;

  public InMemoryAuditRepository(Class<T> entityType) {
    this.entityType = entityType;
  }

  @Override
  public AuditQueryBuilder<T> forEntity(T entity) {
    if (entity instanceof Entity<?>) {
      return forEntityId(((Entity<?>) entity).getId());
    }
    throw new IllegalArgumentException("Entity must implement Entity interface");
  }

  @Override
  public AuditQueryBuilder<T> forEntityId(Serializable entityId) {
    return new InMemoryAuditQueryBuilder(entries.stream()
        .filter(entry -> entry.getEntityId().equals(entityId)));
  }

  @Override
  public AuditQueryBuilder<T> forEntityType(Class<T> entityType) {
    return new InMemoryAuditQueryBuilder(entries.stream()
        .filter(entry -> entry.getEntityType().equals(entityType.getSimpleName())));
  }

  @Override
  public List<AuditEntry<T>> findAll() {
    return new ArrayList<>(entries);
  }

  @Override
  public List<AuditEntry<T>> findByModifiedBy(String modifiedBy) {
    return entries.stream()
        .filter(entry -> entry.getModifiedBy() != null && entry.getModifiedBy().equals(modifiedBy))
        .collect(Collectors.toList());
  }

  @Override
  public List<AuditEntry<T>> findByTimestampBetween(Instant start, Instant end) {
    return entries.stream()
        .filter(entry -> !entry.getTimestamp().isBefore(start) && !entry.getTimestamp().isAfter(end))
        .collect(Collectors.toList());
  }

  @Override
  public void save(AuditEntry<T> entry) {
    entries.add(entry);
  }

  @Override
  public long count() {
    return entries.size();
  }

  @Override
  public long deleteEntriesOlderThan(Instant before) {
    long initialSize = entries.size();
    entries.removeIf(entry -> entry.getTimestamp().isBefore(before));
    return initialSize - entries.size();
  }

  /**
   * Clear all audit entries (useful for testing).
   */
  public void clear() {
    entries.clear();
  }

  /**
   * In-memory implementation of AuditQueryBuilder.
   */
  private class InMemoryAuditQueryBuilder implements AuditQueryBuilder<T> {

    private Stream<AuditEntry<T>> stream;
    private boolean ascending = false;
    private Integer limitValue = null;
    private Integer offsetValue = null;

    InMemoryAuditQueryBuilder(Stream<AuditEntry<T>> stream) {
      this.stream = stream;
    }

    @Override
    public AuditQueryBuilder<T> between(Instant start, Instant end) {
      stream = stream.filter(entry ->
          !entry.getTimestamp().isBefore(start) && !entry.getTimestamp().isAfter(end));
      return this;
    }

    @Override
    public AuditQueryBuilder<T> operation(AuditOperation operation) {
      stream = stream.filter(entry -> entry.getOperation() == operation);
      return this;
    }

    @Override
    public AuditQueryBuilder<T> modifiedBy(String modifiedBy) {
      stream = stream.filter(entry ->
          entry.getModifiedBy() != null && entry.getModifiedBy().equals(modifiedBy));
      return this;
    }

    @Override
    public AuditQueryBuilder<T> after(Instant timestamp) {
      stream = stream.filter(entry -> entry.getTimestamp().isAfter(timestamp));
      return this;
    }

    @Override
    public AuditQueryBuilder<T> before(Instant timestamp) {
      stream = stream.filter(entry -> entry.getTimestamp().isBefore(timestamp));
      return this;
    }

    @Override
    public AuditQueryBuilder<T> orderByTimestamp(boolean ascending) {
      this.ascending = ascending;
      return this;
    }

    @Override
    public AuditQueryBuilder<T> limit(int limit) {
      this.limitValue = limit;
      return this;
    }

    @Override
    public AuditQueryBuilder<T> offset(int offset) {
      this.offsetValue = offset;
      return this;
    }

    @Override
    public List<AuditEntry<T>> execute() {
      Stream<AuditEntry<T>> resultStream = stream;

      // Apply ordering
      resultStream = resultStream.sorted(
          ascending
              ? Comparator.comparing(AuditEntry::getTimestamp)
              : Comparator.comparing(AuditEntry<T>::getTimestamp).reversed()
      );

      // Apply offset
      if (offsetValue != null && offsetValue > 0) {
        resultStream = resultStream.skip(offsetValue);
      }

      // Apply limit
      if (limitValue != null && limitValue > 0) {
        resultStream = resultStream.limit(limitValue);
      }

      return resultStream.collect(Collectors.toList());
    }

    @Override
    public long count() {
      return stream.count();
    }

    @Override
    public AuditEntry<T> first() {
      List<AuditEntry<T>> results = orderByTimestamp(true).limit(1).execute();
      return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public AuditEntry<T> last() {
      List<AuditEntry<T>> results = orderByTimestamp(false).limit(1).execute();
      return results.isEmpty() ? null : results.get(0);
    }
  }
}
