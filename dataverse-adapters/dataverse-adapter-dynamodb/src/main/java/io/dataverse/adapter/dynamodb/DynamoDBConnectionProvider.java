package io.dataverse.adapter.dynamodb;

import io.dataverse.spi.AdapterConfig;
import io.dataverse.spi.ConnectionException;
import io.dataverse.spi.ConnectionProvider;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Connection provider for DynamoDB.
 *
 * <p>Since DynamoDB client is thread-safe and manages its own connection pool,
 * this implementation simply returns the shared client instance.
 *
 * @since 1.0.0
 */
public class DynamoDBConnectionProvider implements ConnectionProvider<DynamoDbClient> {

  private final DynamoDbClient client;
  private final AdapterConfig config;

  public DynamoDBConnectionProvider(DynamoDbClient client, AdapterConfig config) {
    this.client = client;
    this.config = config;
  }

  @Override
  public DynamoDbClient acquire() {
    return client;
  }

  @Override
  public DynamoDbClient acquire(Duration timeout) {
    return client;
  }

  @Override
  public CompletableFuture<DynamoDbClient> acquireAsync() {
    return CompletableFuture.completedFuture(client);
  }

  @Override
  public void release(DynamoDbClient connection) {
    // No-op: DynamoDB client is shared and manages its own lifecycle
  }

  @Override
  public boolean validate(DynamoDbClient connection) {
    try {
      connection.listTables();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public <R> R execute(ConnectionOperation<DynamoDbClient, R> operation) {
    try {
      return operation.execute(client);
    } catch (Exception e) {
      throw new ConnectionException("Failed to execute operation", e);
    }
  }

  @Override
  public PoolStatistics getPoolStatistics() {
    // Return dummy statistics for DynamoDB (managed internally by AWS SDK)
    return new PoolStatistics() {
      @Override
      public int getTotalConnections() {
        return 1;
      }

      @Override
      public int getActiveConnections() {
        return 1;
      }

      @Override
      public int getIdleConnections() {
        return 0;
      }

      @Override
      public int getWaitingThreads() {
        return 0;
      }

      @Override
      public long getAverageAcquisitionTimeMs() {
        return 0;
      }

      @Override
      public long getMaxAcquisitionTimeMs() {
        return 0;
      }

      @Override
      public long getTotalAcquisitions() {
        return 0;
      }

      @Override
      public long getTimeoutCount() {
        return 0;
      }
    };
  }

  @Override
  public int evictIdleConnections() {
    return 0; // Managed by AWS SDK
  }

  @Override
  public void shutdown() {
    // Client shutdown is handled by the adapter
  }
}
