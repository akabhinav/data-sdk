package io.dataverse.adapter.dynamodb;

import io.dataverse.api.Entity;
import io.dataverse.api.Repository;
import io.dataverse.spi.*;
import java.io.Serializable;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Amazon DynamoDB adapter implementation.
 *
 * <p>This adapter provides connectivity to Amazon DynamoDB, supporting both AWS DynamoDB
 * service and DynamoDB Local for development/testing.
 *
 * <p><strong>Configuration Keys:</strong>
 * <ul>
 *   <li>region - AWS region (e.g., "us-east-1")</li>
 *   <li>endpoint - Custom endpoint URL (optional, for DynamoDB Local)</li>
 *   <li>accessKeyId - AWS access key ID</li>
 *   <li>secretAccessKey - AWS secret access key</li>
 *   <li>pool.minSize - Minimum connection pool size (default: 10)</li>
 *   <li>pool.maxSize - Maximum connection pool size (default: 100)</li>
 * </ul>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class DynamoDBAdapter implements DataSourceAdapter {

  private static final String ADAPTER_ID = "dynamodb";
  private static final String VERSION = "1.0.0";

  private DynamoDbClient dynamoDbClient;
  private DynamoDBConnectionProvider connectionProvider;
  private DynamoDBQueryTranslator queryTranslator;
  private AdapterConfig config;

  @Override
  public String getAdapterId() {
    return ADAPTER_ID;
  }

  @Override
  public DataSourceType getDataSourceType() {
    return DataSourceType.NOSQL;
  }

  @Override
  public String getVersion() {
    return VERSION;
  }

  @Override
  public void initialize(AdapterConfig config) {
    this.config = config;

    // Build DynamoDB client
    var clientBuilder = DynamoDbClient.builder();

    // Configure region
    String region = config.getRequiredString("region");
    clientBuilder.region(Region.of(region));

    // Configure credentials if provided
    config.getString("accessKeyId").ifPresent(accessKeyId -> {
      String secretKey = config.getRequiredString("secretAccessKey");
      clientBuilder.credentialsProvider(
          StaticCredentialsProvider.create(
              AwsBasicCredentials.create(accessKeyId, secretKey)));
    });

    // Configure custom endpoint (for DynamoDB Local)
    config.getString("endpoint").ifPresent(endpoint -> {
      clientBuilder.endpointOverride(java.net.URI.create(endpoint));
    });

    this.dynamoDbClient = clientBuilder.build();
    this.connectionProvider = new DynamoDBConnectionProvider(dynamoDbClient, config);
    this.queryTranslator = new DynamoDBQueryTranslator();
  }

  @Override
  public <T extends Entity<ID>, ID extends Serializable> Repository<T, ID> createRepository(
      Class<T> entityClass) {
    return new DynamoDBRepository<>(entityClass, connectionProvider, queryTranslator);
  }

  @Override
  public ConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  @Override
  public QueryTranslator getQueryTranslator() {
    return queryTranslator;
  }

  @Override
  public boolean isHealthy() {
    try {
      dynamoDbClient.listTables();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public HealthCheckResult healthCheck() {
    try {
      var response = dynamoDbClient.listTables();
      return HealthCheckResult.healthy()
          .withMessage("DynamoDB connection is healthy")
          .withDetail("tableCount", response.tableNames().size())
          .withDetail("region", config.getString("region").orElse("unknown"))
          .build();
    } catch (Exception e) {
      return HealthCheckResult.unhealthy()
          .withMessage("DynamoDB connection failed")
          .withError(e)
          .build();
    }
  }

  @Override
  public void shutdown() {
    if (connectionProvider != null) {
      connectionProvider.shutdown();
    }
    if (dynamoDbClient != null) {
      dynamoDbClient.close();
    }
  }

  @Override
  public AdapterCapabilities getCapabilities() {
    return new DynamoDBCapabilities();
  }

  /**
   * DynamoDB-specific capabilities.
   */
  private static class DynamoDBCapabilities implements AdapterCapabilities {
    @Override
    public boolean supportsTransactions() {
      return true; // DynamoDB supports transactions
    }

    @Override
    public boolean supportsBatchOperations() {
      return true;
    }

    @Override
    public boolean supportsSorting() {
      return true; // Limited to sort keys
    }

    @Override
    public boolean supportsPagination() {
      return true;
    }

    @Override
    public boolean supportsFullTextSearch() {
      return false; // DynamoDB doesn't support full-text search
    }

    @Override
    public boolean supportsAggregations() {
      return false; // Limited aggregation support
    }

    @Override
    public boolean supportsSecondaryIndexes() {
      return true; // GSI and LSI
    }

    @Override
    public boolean supportsTTL() {
      return true;
    }

    @Override
    public boolean supportsOptimisticLocking() {
      return true; // Via conditional writes
    }

    @Override
    public boolean supportsAsync() {
      return true;
    }

    @Override
    public int getMaxBatchSize() {
      return 25; // DynamoDB batch limit
    }

    @Override
    public int getMaxQueryConditions() {
      return -1; // No hard limit
    }
  }
}
