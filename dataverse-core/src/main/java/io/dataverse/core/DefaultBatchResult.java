package io.dataverse.core;

import io.dataverse.api.BatchOperations.BatchResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of BatchResult.
 *
 * @param <T> the entity type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class DefaultBatchResult<T> implements BatchResult<T> {

  private final List<T> successful;
  private final List<T> failed;
  private final Map<T, String> failures;
  private final int insertedCount;
  private final int updatedCount;

  private DefaultBatchResult(Builder<T> builder) {
    this.successful = List.copyOf(builder.successful);
    this.failed = List.copyOf(builder.failed);
    this.failures = Map.copyOf(builder.failures);
    this.insertedCount = builder.insertedCount;
    this.updatedCount = builder.updatedCount;
  }

  @Override
  public List<T> getSuccessful() {
    return successful;
  }

  @Override
  public List<T> getFailed() {
    return failed;
  }

  @Override
  public Map<T, String> getFailures() {
    return failures;
  }

  @Override
  public int getTotalCount() {
    return successful.size() + failed.size();
  }

  @Override
  public int getSuccessCount() {
    return successful.size();
  }

  @Override
  public int getFailureCount() {
    return failed.size();
  }

  @Override
  public int getInsertedCount() {
    return insertedCount;
  }

  @Override
  public int getUpdatedCount() {
    return updatedCount;
  }

  @Override
  public boolean isAllSuccessful() {
    return failed.isEmpty();
  }

  @Override
  public boolean hasFailures() {
    return !failed.isEmpty();
  }

  /**
   * Builder for DefaultBatchResult.
   *
   * @param <T> the entity type
   */
  public static final class Builder<T> {
    private final List<T> successful = new ArrayList<>();
    private final List<T> failed = new ArrayList<>();
    private final Map<T, String> failures = new HashMap<>();
    private int insertedCount = 0;
    private int updatedCount = 0;

    /**
     * Adds a successful entity.
     *
     * @param entity the entity
     * @return this builder
     */
    public Builder<T> addSuccessful(T entity) {
      successful.add(entity);
      return this;
    }

    /**
     * Adds multiple successful entities.
     *
     * @param entities the entities
     * @return this builder
     */
    public Builder<T> addAllSuccessful(Iterable<T> entities) {
      entities.forEach(successful::add);
      return this;
    }

    /**
     * Adds a failed entity with error message.
     *
     * @param entity the entity
     * @param errorMessage the error message
     * @return this builder
     */
    public Builder<T> addFailure(T entity, String errorMessage) {
      failed.add(entity);
      failures.put(entity, errorMessage);
      return this;
    }

    /**
     * Sets the number of inserted entities.
     *
     * @param count the insert count
     * @return this builder
     */
    public Builder<T> insertedCount(int count) {
      this.insertedCount = count;
      return this;
    }

    /**
     * Increments the inserted count.
     *
     * @return this builder
     */
    public Builder<T> incrementInserted() {
      this.insertedCount++;
      return this;
    }

    /**
     * Sets the number of updated entities.
     *
     * @param count the update count
     * @return this builder
     */
    public Builder<T> updatedCount(int count) {
      this.updatedCount = count;
      return this;
    }

    /**
     * Increments the updated count.
     *
     * @return this builder
     */
    public Builder<T> incrementUpdated() {
      this.updatedCount++;
      return this;
    }

    /**
     * Builds the batch result.
     *
     * @return a new DefaultBatchResult
     */
    public DefaultBatchResult<T> build() {
      return new DefaultBatchResult<>(this);
    }
  }

  /**
   * Creates a new builder.
   *
   * @param <T> the entity type
   * @return a new builder
   */
  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  /**
   * Creates a successful result with all entities.
   *
   * @param entities the successful entities
   * @param <T> the entity type
   * @return a batch result with all successes
   */
  public static <T> BatchResult<T> success(List<T> entities) {
    return DefaultBatchResult.<T>builder().addAllSuccessful(entities).build();
  }

  /**
   * Creates a result with all entities marked as inserted.
   *
   * @param entities the inserted entities
   * @param <T> the entity type
   * @return a batch result with insertion count
   */
  public static <T> BatchResult<T> inserted(List<T> entities) {
    return DefaultBatchResult.<T>builder()
        .addAllSuccessful(entities)
        .insertedCount(entities.size())
        .build();
  }

  /**
   * Creates a result with all entities marked as updated.
   *
   * @param entities the updated entities
   * @param <T> the entity type
   * @return a batch result with update count
   */
  public static <T> BatchResult<T> updated(List<T> entities) {
    return DefaultBatchResult.<T>builder()
        .addAllSuccessful(entities)
        .updatedCount(entities.size())
        .build();
  }
}
