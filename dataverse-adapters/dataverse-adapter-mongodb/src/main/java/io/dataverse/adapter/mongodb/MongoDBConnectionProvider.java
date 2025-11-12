package io.dataverse.adapter.mongodb;

import io.dataverse.spi.ConnectionException;
import io.dataverse.spi.ConnectionProvider;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Connection provider for MongoDB.
 *
 * <p>MongoDB client is thread-safe and manages its own connection pool.
 * This implementation provides access to the database instance.
 *
 * @since 1.0.0
 */
public class MongoDBConnectionProvider implements ConnectionProvider<MongoDatabase> {

  private final MongoClient client;
  private final String databaseName;
  private final MongoDatabase database;

  public MongoDBConnectionProvider(MongoClient client, String databaseName) {
    this.client = client;
    this.databaseName = databaseName;
    this.database = client.getDatabase(databaseName);
  }

  @Override
  public MongoDatabase acquire() {
    return database;
  }

  @Override
  public MongoDatabase acquire(Duration timeout) {
    return database;
  }

  @Override
  public CompletableFuture<MongoDatabase> acquireAsync() {
    return CompletableFuture.completedFuture(database);
  }

  @Override
  public void release(MongoDatabase connection) {
    // No-op: MongoDB manages its own connection pool
  }

  @Override
  public boolean validate(MongoDatabase connection) {
    try {
      connection.listCollectionNames().first();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public <R> R execute(ConnectionOperation<MongoDatabase, R> operation) {
    try {
      return operation.execute(database);
    } catch (Exception e) {
      throw new ConnectionException("Failed to execute operation", e);
    }
  }

  @Override
  public PoolStatistics getPoolStatistics() {
    // Return dummy statistics (managed internally by MongoDB driver)
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
    return 0; // Managed by MongoDB driver
  }

  @Override
  public void shutdown() {
    // Client shutdown is handled by the adapter
  }
}
