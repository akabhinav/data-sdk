package io.dataverse.api;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Advanced batch operations for bulk data manipulation.
 *
 * <p>This interface provides efficient bulk operations with result tracking,
 * allowing applications to process large datasets with optimal performance.
 *
 * <p><strong>Example Usage:</strong>
 * <pre>{@code
 * BatchOperations<User, String> batch = repository.batch();
 *
 * // Upsert multiple entities (insert if not exists, update if exists)
 * List<User> users = List.of(user1, user2, user3);
 * BatchResult<User> result = batch.upsertAll(users);
 * System.out.println("Inserted: " + result.getInsertedCount());
 * System.out.println("Updated: " + result.getUpdatedCount());
 *
 * // Bulk update with function
 * batch.updateAll(
 *     repository.query().where("status").eq("PENDING").execute(),
 *     user -> { user.setStatus("PROCESSED"); return user; }
 * );
 *
 * // Delete by IDs with result tracking
 * List<String> ids = List.of("id1", "id2", "id3");
 * BatchResult<User> deleteResult = batch.deleteAllById(ids);
 * }</pre>
 *
 * @param <T> the entity type
 * @param <ID> the identifier type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface BatchOperations<T extends Entity<ID>, ID extends Serializable> {

  /**
   * Inserts or updates all entities (upsert operation).
   *
   * <p>If an entity's ID exists, it will be updated. Otherwise, it will be inserted.
   *
   * @param entities the entities to upsert
   * @return the batch operation result
   */
  BatchResult<T> upsertAll(Iterable<T> entities);

  /**
   * Updates all entities in the collection.
   *
   * @param entities the entities to update
   * @return the batch operation result
   */
  BatchResult<T> updateAll(Iterable<T> entities);

  /**
   * Updates all entities matching the query using the update function.
   *
   * @param entitiesToUpdate the entities to update
   * @param updateFunction the function to apply to each entity
   * @return the batch operation result
   */
  BatchResult<T> updateAll(List<T> entitiesToUpdate, Function<T, T> updateFunction);

  /**
   * Deletes all entities by their IDs with result tracking.
   *
   * @param ids the IDs of entities to delete
   * @return the batch operation result
   */
  BatchResult<T> deleteAllById(Iterable<ID> ids);

  /**
   * Finds all entities by IDs with result tracking.
   *
   * @param ids the IDs to find
   * @return the batch operation result with found entities
   */
  BatchResult<T> findAllById(Iterable<ID> ids);

  /**
   * Asynchronously upserts all entities.
   *
   * @param entities the entities to upsert
   * @return a CompletableFuture with the batch result
   */
  CompletableFuture<BatchResult<T>> upsertAllAsync(Iterable<T> entities);

  /**
   * Asynchronously updates all entities.
   *
   * @param entities the entities to update
   * @return a CompletableFuture with the batch result
   */
  CompletableFuture<BatchResult<T>> updateAllAsync(Iterable<T> entities);

  /**
   * Asynchronously deletes all entities by IDs.
   *
   * @param ids the IDs to delete
   * @return a CompletableFuture with the batch result
   */
  CompletableFuture<BatchResult<T>> deleteAllByIdAsync(Iterable<ID> ids);

  /**
   * Result of a batch operation containing success/failure information.
   *
   * @param <T> the entity type
   */
  interface BatchResult<T> {

    /**
     * Returns all successfully processed entities.
     *
     * @return list of successful entities
     */
    List<T> getSuccessful();

    /**
     * Returns entities that failed to process.
     *
     * @return list of failed entities
     */
    List<T> getFailed();

    /**
     * Returns failure reasons mapped by entity.
     *
     * @return map of failed entities to their error messages
     */
    Map<T, String> getFailures();

    /**
     * Returns the total number of entities processed.
     *
     * @return total count
     */
    int getTotalCount();

    /**
     * Returns the number of successful operations.
     *
     * @return success count
     */
    int getSuccessCount();

    /**
     * Returns the number of failed operations.
     *
     * @return failure count
     */
    int getFailureCount();

    /**
     * Returns the number of inserted entities (for upsert operations).
     *
     * @return insert count
     */
    int getInsertedCount();

    /**
     * Returns the number of updated entities (for upsert operations).
     *
     * @return update count
     */
    int getUpdatedCount();

    /**
     * Checks if all operations succeeded.
     *
     * @return true if all succeeded, false otherwise
     */
    boolean isAllSuccessful();

    /**
     * Checks if any operations failed.
     *
     * @return true if any failed, false otherwise
     */
    boolean hasFailures();
  }
}
