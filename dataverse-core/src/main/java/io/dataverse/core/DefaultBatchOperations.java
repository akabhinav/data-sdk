package io.dataverse.core;

import io.dataverse.api.BatchOperations;
import io.dataverse.api.Entity;
import io.dataverse.api.Repository;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Default implementation of BatchOperations.
 *
 * <p>This implementation provides efficient bulk operations with error tracking.
 * Operations are executed using the repository's virtual thread executor for
 * optimal performance with Java 21.
 *
 * @param <T> the entity type
 * @param <ID> the identifier type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class DefaultBatchOperations<T extends Entity<ID>, ID extends Serializable>
    implements BatchOperations<T, ID> {

  private final Repository<T, ID> repository;
  private final ExecutorService executor;

  /**
   * Constructs a new batch operations instance.
   *
   * @param repository the repository to use for operations
   * @param executor the executor for async operations
   */
  public DefaultBatchOperations(Repository<T, ID> repository, ExecutorService executor) {
    this.repository = repository;
    this.executor = executor;
  }

  @Override
  public BatchResult<T> upsertAll(Iterable<T> entities) {
    if (entities == null) {
      throw new IllegalArgumentException("Entities must not be null");
    }

    var resultBuilder = DefaultBatchResult.<T>builder();

    for (T entity : entities) {
      try {
        boolean isNew = (entity.getId() == null || !repository.existsById(entity.getId()));

        T saved = repository.save(entity);
        resultBuilder.addSuccessful(saved);

        if (isNew) {
          resultBuilder.incrementInserted();
        } else {
          resultBuilder.incrementUpdated();
        }
      } catch (Exception e) {
        resultBuilder.addFailure(entity, e.getMessage());
      }
    }

    return resultBuilder.build();
  }

  @Override
  public BatchResult<T> updateAll(Iterable<T> entities) {
    if (entities == null) {
      throw new IllegalArgumentException("Entities must not be null");
    }

    var resultBuilder = DefaultBatchResult.<T>builder();

    for (T entity : entities) {
      try {
        if (entity.getId() == null) {
          resultBuilder.addFailure(entity, "Cannot update entity without ID");
          continue;
        }

        if (!repository.existsById(entity.getId())) {
          resultBuilder.addFailure(entity, "Entity not found with ID: " + entity.getId());
          continue;
        }

        T updated = repository.save(entity);
        resultBuilder.addSuccessful(updated);
        resultBuilder.incrementUpdated();
      } catch (Exception e) {
        resultBuilder.addFailure(entity, e.getMessage());
      }
    }

    return resultBuilder.build();
  }

  @Override
  public BatchResult<T> updateAll(List<T> entitiesToUpdate, Function<T, T> updateFunction) {
    if (entitiesToUpdate == null) {
      throw new IllegalArgumentException("Entities must not be null");
    }
    if (updateFunction == null) {
      throw new IllegalArgumentException("Update function must not be null");
    }

    var resultBuilder = DefaultBatchResult.<T>builder();

    for (T entity : entitiesToUpdate) {
      try {
        T updated = updateFunction.apply(entity);
        if (updated == null) {
          resultBuilder.addFailure(entity, "Update function returned null");
          continue;
        }

        T saved = repository.save(updated);
        resultBuilder.addSuccessful(saved);
        resultBuilder.incrementUpdated();
      } catch (Exception e) {
        resultBuilder.addFailure(entity, e.getMessage());
      }
    }

    return resultBuilder.build();
  }

  @Override
  public BatchResult<T> deleteAllById(Iterable<ID> ids) {
    if (ids == null) {
      throw new IllegalArgumentException("IDs must not be null");
    }

    var resultBuilder = DefaultBatchResult.<T>builder();
    int successCount = 0;

    for (ID id : ids) {
      try {
        // Try to find the entity first for tracking
        var optionalEntity = repository.findById(id);

        repository.deleteById(id);

        // If we found it, add to successful list
        optionalEntity.ifPresent(resultBuilder::addSuccessful);
        successCount++;
      } catch (Exception e) {
        // Create a placeholder entity for the failed deletion
        // This is not ideal but BatchResult expects entities, not IDs
        // Subclasses can override this behavior
        resultBuilder.addFailure(null, "Failed to delete ID " + id + ": " + e.getMessage());
      }
    }

    return resultBuilder.build();
  }

  @Override
  public BatchResult<T> findAllById(Iterable<ID> ids) {
    if (ids == null) {
      throw new IllegalArgumentException("IDs must not be null");
    }

    List<T> found = repository.findAllById(ids);
    return DefaultBatchResult.success(found);
  }

  @Override
  public CompletableFuture<BatchResult<T>> upsertAllAsync(Iterable<T> entities) {
    return CompletableFuture.supplyAsync(() -> upsertAll(entities), executor);
  }

  @Override
  public CompletableFuture<BatchResult<T>> updateAllAsync(Iterable<T> entities) {
    return CompletableFuture.supplyAsync(() -> updateAll(entities), executor);
  }

  @Override
  public CompletableFuture<BatchResult<T>> deleteAllByIdAsync(Iterable<ID> ids) {
    return CompletableFuture.supplyAsync(() -> deleteAllById(ids), executor);
  }
}
