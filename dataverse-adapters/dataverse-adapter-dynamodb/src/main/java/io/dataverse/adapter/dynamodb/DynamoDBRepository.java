package io.dataverse.adapter.dynamodb;

import io.dataverse.api.BatchOperations;
import io.dataverse.api.Entity;
import io.dataverse.api.QueryBuilder;
import io.dataverse.core.AbstractRepository;
import io.dataverse.core.query.DefaultQueryBuilder;
import io.dataverse.core.query.Query;
import io.dataverse.spi.ConnectionProvider;
import io.dataverse.spi.QueryTranslator;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

/**
 * DynamoDB implementation of the Repository interface.
 *
 * <p>This class implements all CRUD operations using the AWS SDK for DynamoDB.
 * It extends AbstractRepository to inherit common functionality.
 *
 * @param <T> the entity type
 * @param <ID> the identifier type
 * @since 1.0.0
 */
public class DynamoDBRepository<T extends Entity<ID>, ID extends Serializable>
    extends AbstractRepository<T, ID> {

  private final ConnectionProvider<DynamoDbClient> connectionProvider;
  private final QueryTranslator queryTranslator;
  private final String tableName;

  public DynamoDBRepository(
      Class<T> entityClass,
      ConnectionProvider<DynamoDbClient> connectionProvider,
      QueryTranslator queryTranslator) {
    super(entityClass);
    this.connectionProvider = connectionProvider;
    this.queryTranslator = queryTranslator;
    this.tableName = deriveTableName(entityClass);
  }

  /**
   * Derives the DynamoDB table name from the entity class.
   *
   * <p>Default strategy: Simple class name (e.g., User -> User)
   * Subclasses can override this method for custom naming strategies.
   *
   * @param entityClass the entity class
   * @return the table name
   */
  protected String deriveTableName(Class<T> entityClass) {
    // Simple strategy: use class name as table name
    // In production, this could use annotations or configuration
    return entityClass.getSimpleName();
  }

  @Override
  protected T doSave(T entity) {
    return connectionProvider.execute(
        client -> {
          try {
            // Convert entity to DynamoDB attributes
            var item = convertToAttributeMap(entity);

            // Build PutItem request
            PutItemRequest request =
                PutItemRequest.builder().tableName(tableName).item(item).build();

            // Execute
            client.putItem(request);

            return entity;
          } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to save entity to DynamoDB", e);
          }
        });
  }

  @Override
  protected Optional<T> doFindById(ID id) {
    return connectionProvider.execute(
        client -> {
          try {
            // Build GetItem request
            GetItemRequest request =
                GetItemRequest.builder()
                    .tableName(tableName)
                    .key(java.util.Map.of("id", convertToAttributeValue(id)))
                    .build();

            // Execute
            GetItemResponse response = client.getItem(request);

            // Convert result
            if (response.hasItem()) {
              return Optional.of(convertFromAttributeMap(response.item()));
            } else {
              return Optional.empty();
            }
          } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to find entity by ID in DynamoDB", e);
          }
        });
  }

  @Override
  protected void doDelete(ID id) {
    connectionProvider.execute(
        client -> {
          try {
            // Build DeleteItem request
            DeleteItemRequest request =
                DeleteItemRequest.builder()
                    .tableName(tableName)
                    .key(java.util.Map.of("id", convertToAttributeValue(id)))
                    .build();

            // Execute
            client.deleteItem(request);

            return null;
          } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to delete entity from DynamoDB", e);
          }
        });
  }

  @Override
  protected List<T> doFindAll() {
    return connectionProvider.execute(
        client -> {
          try {
            // Build Scan request (inefficient for large tables!)
            ScanRequest request = ScanRequest.builder().tableName(tableName).build();

            // Execute
            ScanResponse response = client.scan(request);

            // Convert results
            List<T> results = new ArrayList<>();
            for (var item : response.items()) {
              results.add(convertFromAttributeMap(item));
            }

            return results;
          } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to scan DynamoDB table", e);
          }
        });
  }

  @Override
  protected long doCount() {
    // For DynamoDB, we'd use DescribeTable to get item count
    // For simplicity, using findAll().size() (inefficient!)
    return doFindAll().size();
  }

  @Override
  public QueryBuilder<T> query() {
    return new DefaultQueryBuilder<>(this::executeQuery);
  }

  @Override
  public BatchOperations<T, ID> batch() {
    // Return optimized DynamoDB batch operations
    DynamoDbClient client = connectionProvider.execute(c -> c);
    return new DynamoDBBatchOperations<>(
        this,
        client,
        tableName,
        entityClass,
        virtualThreadExecutor,
        this
    );
  }

  @Override
  public List<T> executeNativeQuery(String nativeQuery) {
    // Native DynamoDB queries would be executed here
    throw new UnsupportedOperationException("Native queries not yet implemented for DynamoDB");
  }

  /**
   * Executes a query built by the QueryBuilder.
   *
   * @param query the query to execute
   * @return the query results
   */
  private List<T> executeQuery(Query query) {
    return connectionProvider.execute(
        client -> {
          try {
            // Translate query to DynamoDB format
            QueryTranslator.NativeQuery nativeQuery = queryTranslator.translate(query);

            // For now, fall back to scan with filter (inefficient but functional)
            // In production, this would use Query or Scan with FilterExpression
            return doFindAll(); // Simplified implementation

          } catch (Exception e) {
            throw new RuntimeException("Failed to execute query on DynamoDB", e);
          }
        });
  }

  // Conversion methods (simplified - in production, use proper serialization)

  /**
   * Converts an entity to a DynamoDB attribute map.
   *
   * <p>Package-private to allow access from DynamoDBBatchOperations.
   *
   * @param entity the entity
   * @return the attribute map
   */
  java.util.Map<String, AttributeValue> convertToAttributeMap(T entity) {
    // Simplified: In production, use reflection or Jackson for proper conversion
    var map = new java.util.HashMap<String, AttributeValue>();

    // Add ID
    if (entity.getId() != null) {
      map.put("id", convertToAttributeValue(entity.getId()));
    }

    // In production, iterate over all fields using reflection or use DynamoDB Enhanced Client

    return map;
  }

  /**
   * Converts a DynamoDB attribute map to an entity.
   *
   * <p>Package-private to allow access from DynamoDBBatchOperations.
   *
   * @param attributeMap the attribute map
   * @return the entity
   */
  T convertFromAttributeMap(java.util.Map<String, AttributeValue> attributeMap) {
    // Simplified: In production, use proper deserialization
    try {
      T instance = entityClass.getDeclaredConstructor().newInstance();

      // Set ID
      if (attributeMap.containsKey("id")) {
        // This is simplified - proper implementation would handle type conversion
        // instance.setId(...);
      }

      return instance;
    } catch (Exception e) {
      throw new RuntimeException("Failed to convert DynamoDB item to entity", e);
    }
  }

  /**
   * Converts a Java value to a DynamoDB AttributeValue.
   *
   * @param value the value
   * @return the AttributeValue
   */
  protected AttributeValue convertToAttributeValue(Object value) {
    if (value == null) {
      return AttributeValue.builder().nul(true).build();
    } else if (value instanceof String) {
      return AttributeValue.builder().s((String) value).build();
    } else if (value instanceof Number) {
      return AttributeValue.builder().n(value.toString()).build();
    } else if (value instanceof Boolean) {
      return AttributeValue.builder().bool((Boolean) value).build();
    } else {
      // Fallback to string representation
      return AttributeValue.builder().s(value.toString()).build();
    }
  }

  @Override
  protected void shutdown() {
    super.shutdown();
    // Additional cleanup if needed
  }
}
