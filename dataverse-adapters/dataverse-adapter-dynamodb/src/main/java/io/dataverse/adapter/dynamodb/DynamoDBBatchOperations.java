package io.dataverse.adapter.dynamodb;

import io.dataverse.api.BatchOperations;
import io.dataverse.api.Entity;
import io.dataverse.api.Repository;
import io.dataverse.core.DefaultBatchResult;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Optimized DynamoDB batch operations using BatchWriteItem and BatchGetItem.
 *
 * <p>This implementation leverages DynamoDB's native batch APIs for maximum performance:
 * <ul>
 *   <li>BatchWriteItem - Write up to 25 items per request</li>
 *   <li>BatchGetItem - Read up to 100 items per request</li>
 *   <li>Automatic chunking for large batches</li>
 *   <li>Automatic retry for unprocessed items</li>
 * </ul>
 *
 * @param <T> the entity type
 * @param <ID> the identifier type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class DynamoDBBatchOperations<T extends Entity<ID>, ID extends Serializable>
    implements BatchOperations<T, ID> {

  private static final int BATCH_WRITE_MAX_SIZE = 25;
  private static final int BATCH_GET_MAX_SIZE = 100;
  private static final int MAX_RETRY_ATTEMPTS = 3;

  private final Repository<T, ID> repository;
  private final DynamoDbClient dynamoDbClient;
  private final String tableName;
  private final Class<T> entityClass;
  private final ExecutorService executor;
  private final DynamoDBRepository<T, ID> dynamoRepository;

  public DynamoDBBatchOperations(
      Repository<T, ID> repository,
      DynamoDbClient dynamoDbClient,
      String tableName,
      Class<T> entityClass,
      ExecutorService executor,
      DynamoDBRepository<T, ID> dynamoRepository) {
    this.repository = repository;
    this.dynamoDbClient = dynamoDbClient;
    this.tableName = tableName;
    this.entityClass = entityClass;
    this.executor = executor;
    this.dynamoRepository = dynamoRepository;
  }

  @Override
  public BatchResult<T> upsertAll(Iterable<T> entities) {
    if (entities == null) {
      throw new IllegalArgumentException("Entities must not be null");
    }

    List<T> entityList = new ArrayList<>();
    entities.forEach(entityList::add);

    if (entityList.isEmpty()) {
      return DefaultBatchResult.<T>builder().build();
    }

    var resultBuilder = DefaultBatchResult.<T>builder();

    // Determine which are inserts vs updates
    Map<Boolean, List<T>> partitioned = entityList.stream()
        .collect(Collectors.partitioningBy(entity ->
            entity.getId() == null || !repository.existsById(entity.getId())));

    List<T> toInsert = partitioned.get(true);
    List<T> toUpdate = partitioned.get(false);

    // Process in chunks of BATCH_WRITE_MAX_SIZE
    processWriteBatch(toInsert, resultBuilder, true);
    processWriteBatch(toUpdate, resultBuilder, false);

    return resultBuilder.build();
  }

  @Override
  public BatchResult<T> updateAll(Iterable<T> entities) {
    if (entities == null) {
      throw new IllegalArgumentException("Entities must not be null");
    }

    List<T> entityList = new ArrayList<>();
    entities.forEach(entityList::add);

    var resultBuilder = DefaultBatchResult.<T>builder();

    // Validate all have IDs
    for (T entity : entityList) {
      if (entity.getId() == null) {
        resultBuilder.addFailure(entity, "Cannot update entity without ID");
        continue;
      }

      if (!repository.existsById(entity.getId())) {
        resultBuilder.addFailure(entity, "Entity not found with ID: " + entity.getId());
        continue;
      }
    }

    // Process valid updates
    List<T> validEntities = entityList.stream()
        .filter(e -> e.getId() != null)
        .collect(Collectors.toList());

    processWriteBatch(validEntities, resultBuilder, false);

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
    List<T> transformedEntities = new ArrayList<>();

    for (T entity : entitiesToUpdate) {
      try {
        T updated = updateFunction.apply(entity);
        if (updated == null) {
          resultBuilder.addFailure(entity, "Update function returned null");
          continue;
        }
        transformedEntities.add(updated);
      } catch (Exception e) {
        resultBuilder.addFailure(entity, e.getMessage());
      }
    }

    // Process transformed entities
    processWriteBatch(transformedEntities, resultBuilder, false);

    return resultBuilder.build();
  }

  @Override
  public BatchResult<T> deleteAllById(Iterable<ID> ids) {
    if (ids == null) {
      throw new IllegalArgumentException("IDs must not be null");
    }

    List<ID> idList = new ArrayList<>();
    ids.forEach(idList::add);

    if (idList.isEmpty()) {
      return DefaultBatchResult.<T>builder().build();
    }

    var resultBuilder = DefaultBatchResult.<T>builder();

    // Process deletes in chunks
    for (int i = 0; i < idList.size(); i += BATCH_WRITE_MAX_SIZE) {
      List<ID> chunk = idList.subList(i, Math.min(i + BATCH_WRITE_MAX_SIZE, idList.size()));

      try {
        List<WriteRequest> writeRequests = chunk.stream()
            .map(id -> {
              Map<String, AttributeValue> key = Map.of(
                  "id", AttributeValue.builder().s(id.toString()).build()
              );
              return WriteRequest.builder()
                  .deleteRequest(DeleteRequest.builder().key(key).build())
                  .build();
            })
            .collect(Collectors.toList());

        BatchWriteItemRequest request = BatchWriteItemRequest.builder()
            .requestItems(Map.of(tableName, writeRequests))
            .build();

        BatchWriteItemResponse response = dynamoDbClient.batchWriteItem(request);

        // Handle unprocessed items
        retryUnprocessedWrites(response.unprocessedItems(), resultBuilder);

      } catch (Exception e) {
        chunk.forEach(id -> resultBuilder.addFailure(null,
            "Failed to delete ID " + id + ": " + e.getMessage()));
      }
    }

    return resultBuilder.build();
  }

  @Override
  public BatchResult<T> findAllById(Iterable<ID> ids) {
    if (ids == null) {
      throw new IllegalArgumentException("IDs must not be null");
    }

    List<ID> idList = new ArrayList<>();
    ids.forEach(idList::add);

    if (idList.isEmpty()) {
      return DefaultBatchResult.success(Collections.emptyList());
    }

    List<T> foundEntities = new ArrayList<>();

    // Process in chunks of BATCH_GET_MAX_SIZE
    for (int i = 0; i < idList.size(); i += BATCH_GET_MAX_SIZE) {
      List<ID> chunk = idList.subList(i, Math.min(i + BATCH_GET_MAX_SIZE, idList.size()));

      try {
        List<Map<String, AttributeValue>> keys = chunk.stream()
            .map(id -> Map.of("id", AttributeValue.builder().s(id.toString()).build()))
            .collect(Collectors.toList());

        KeysAndAttributes keysAndAttributes = KeysAndAttributes.builder()
            .keys(keys)
            .build();

        BatchGetItemRequest request = BatchGetItemRequest.builder()
            .requestItems(Map.of(tableName, keysAndAttributes))
            .build();

        BatchGetItemResponse response = dynamoDbClient.batchGetItem(request);

        // Convert responses to entities
        List<Map<String, AttributeValue>> items = response.responses().get(tableName);
        if (items != null) {
          for (Map<String, AttributeValue> item : items) {
            T entity = dynamoRepository.convertFromAttributeMap(item);
            foundEntities.add(entity);
          }
        }

        // Handle unprocessed keys
        if (response.hasUnprocessedKeys() && !response.unprocessedKeys().isEmpty()) {
          retryUnprocessedReads(response.unprocessedKeys(), foundEntities);
        }

      } catch (Exception e) {
        // Log error but continue processing
        System.err.println("Error in batch get: " + e.getMessage());
      }
    }

    return DefaultBatchResult.success(foundEntities);
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

  /**
   * Processes a write batch in chunks, handling DynamoDB's 25-item limit.
   */
  private void processWriteBatch(
      List<T> entities,
      DefaultBatchResult.Builder<T> resultBuilder,
      boolean isInsert) {

    for (int i = 0; i < entities.size(); i += BATCH_WRITE_MAX_SIZE) {
      List<T> chunk = entities.subList(i, Math.min(i + BATCH_WRITE_MAX_SIZE, entities.size()));

      try {
        List<WriteRequest> writeRequests = chunk.stream()
            .map(entity -> {
              Map<String, AttributeValue> item = dynamoRepository.convertToAttributeMap(entity);
              return WriteRequest.builder()
                  .putRequest(PutRequest.builder().item(item).build())
                  .build();
            })
            .collect(Collectors.toList());

        BatchWriteItemRequest request = BatchWriteItemRequest.builder()
            .requestItems(Map.of(tableName, writeRequests))
            .build();

        BatchWriteItemResponse response = dynamoDbClient.batchWriteItem(request);

        // Track successes
        chunk.forEach(entity -> {
          resultBuilder.addSuccessful(entity);
          if (isInsert) {
            resultBuilder.incrementInserted();
          } else {
            resultBuilder.incrementUpdated();
          }
        });

        // Handle unprocessed items
        retryUnprocessedWrites(response.unprocessedItems(), resultBuilder);

      } catch (Exception e) {
        chunk.forEach(entity ->
            resultBuilder.addFailure(entity, "Batch write failed: " + e.getMessage()));
      }
    }
  }

  /**
   * Retries unprocessed write items with exponential backoff.
   */
  private void retryUnprocessedWrites(
      Map<String, List<WriteRequest>> unprocessedItems,
      DefaultBatchResult.Builder<T> resultBuilder) {

    if (unprocessedItems == null || unprocessedItems.isEmpty()) {
      return;
    }

    int attempts = 0;
    Map<String, List<WriteRequest>> itemsToRetry = new HashMap<>(unprocessedItems);

    while (!itemsToRetry.isEmpty() && attempts < MAX_RETRY_ATTEMPTS) {
      attempts++;

      try {
        // Exponential backoff
        Thread.sleep((long) Math.pow(2, attempts) * 100);

        BatchWriteItemRequest retryRequest = BatchWriteItemRequest.builder()
            .requestItems(itemsToRetry)
            .build();

        BatchWriteItemResponse response = dynamoDbClient.batchWriteItem(retryRequest);
        itemsToRetry = response.unprocessedItems();

      } catch (Exception e) {
        System.err.println("Retry attempt " + attempts + " failed: " + e.getMessage());
      }
    }

    // Track final unprocessed items as failures
    if (!itemsToRetry.isEmpty()) {
      int unprocessedCount = itemsToRetry.values().stream()
          .mapToInt(List::size)
          .sum();
      resultBuilder.addFailure(null,
          "Failed to process " + unprocessedCount + " items after " + attempts + " retries");
    }
  }

  /**
   * Retries unprocessed read items with exponential backoff.
   */
  private void retryUnprocessedReads(
      Map<String, KeysAndAttributes> unprocessedKeys,
      List<T> foundEntities) {

    if (unprocessedKeys == null || unprocessedKeys.isEmpty()) {
      return;
    }

    int attempts = 0;
    Map<String, KeysAndAttributes> keysToRetry = new HashMap<>(unprocessedKeys);

    while (!keysToRetry.isEmpty() && attempts < MAX_RETRY_ATTEMPTS) {
      attempts++;

      try {
        // Exponential backoff
        Thread.sleep((long) Math.pow(2, attempts) * 100);

        BatchGetItemRequest retryRequest = BatchGetItemRequest.builder()
            .requestItems(keysToRetry)
            .build();

        BatchGetItemResponse response = dynamoDbClient.batchGetItem(retryRequest);

        // Add found items
        List<Map<String, AttributeValue>> items = response.responses().get(tableName);
        if (items != null) {
          for (Map<String, AttributeValue> item : items) {
            T entity = dynamoRepository.convertFromAttributeMap(item);
            foundEntities.add(entity);
          }
        }

        keysToRetry = response.unprocessedKeys();

      } catch (Exception e) {
        System.err.println("Retry attempt " + attempts + " failed: " + e.getMessage());
      }
    }
  }
}
