package io.dataverse.adapter.mongodb;

import io.dataverse.api.Entity;
import io.dataverse.api.Repository;
import io.dataverse.spi.*;
import java.io.Serializable;

/**
 * MongoDB adapter implementation.
 *
 * <p>This adapter provides connectivity to MongoDB, supporting both MongoDB Atlas
 * and self-hosted MongoDB instances.
 *
 * <p><strong>Configuration Keys:</strong>
 * <ul>
 *   <li>connectionString - MongoDB connection string (required)</li>
 *   <li>database - Database name (required)</li>
 *   <li>pool.minSize - Minimum connection pool size (default: 10)</li>
 *   <li>pool.maxSize - Maximum connection pool size (default: 100)</li>
 *   <li>connectionTimeout - Connection timeout in seconds (default: 30)</li>
 * </ul>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class MongoDBAdapter implements DataSourceAdapter {

  private static final String ADAPTER_ID = "mongodb";
  private static final String VERSION = "1.0.0";

  private com.mongodb.client.MongoClient mongoClient;
  private MongoDBConnectionProvider connectionProvider;
  private MongoDBQueryTranslator queryTranslator;
  private AdapterConfig config;
  private String databaseName;

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
    this.databaseName = config.getRequiredString("database");

    // Build MongoDB client
    String connectionString = config.getRequiredString("connectionString");

    var settingsBuilder = com.mongodb.MongoClientSettings.builder()
        .applyConnectionString(new com.mongodb.ConnectionString(connectionString));

    // Configure connection pool
    var poolSettings = com.mongodb.connection.ConnectionPoolSettings.builder()
        .minSize(config.getInt("pool.minSize", 10))
        .maxSize(config.getInt("pool.maxSize", 100))
        .build();

    settingsBuilder.applyToConnectionPoolSettings(builder ->
        builder.applySettings(poolSettings));

    // Configure socket settings
    var socketSettings = com.mongodb.connection.SocketSettings.builder()
        .connectTimeout(config.getInt("connectionTimeout", 30),
            java.util.concurrent.TimeUnit.SECONDS)
        .build();

    settingsBuilder.applyToSocketSettings(builder ->
        builder.applySettings(socketSettings));

    this.mongoClient = com.mongodb.client.MongoClients.create(settingsBuilder.build());
    this.connectionProvider = new MongoDBConnectionProvider(mongoClient, databaseName);
    this.queryTranslator = new MongoDBQueryTranslator();
  }

  @Override
  public <T extends Entity<ID>, ID extends Serializable> Repository<T, ID> createRepository(
      Class<T> entityClass) {
    return new MongoDBRepository<>(entityClass, connectionProvider, queryTranslator, databaseName);
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
      mongoClient.getDatabase(databaseName).listCollectionNames().first();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public HealthCheckResult healthCheck() {
    try {
      var database = mongoClient.getDatabase(databaseName);
      var collections = new java.util.ArrayList<String>();
      database.listCollectionNames().into(collections);

      return HealthCheckResult.healthy()
          .withMessage("MongoDB connection is healthy")
          .withDetail("database", databaseName)
          .withDetail("collectionCount", collections.size())
          .withDetail("serverAddress", mongoClient.getClusterDescription()
              .getServerDescriptions().toString())
          .build();
    } catch (Exception e) {
      return HealthCheckResult.unhealthy()
          .withMessage("MongoDB connection failed")
          .withError(e)
          .build();
    }
  }

  @Override
  public void shutdown() {
    if (connectionProvider != null) {
      connectionProvider.shutdown();
    }
    if (mongoClient != null) {
      mongoClient.close();
    }
  }

  @Override
  public AdapterCapabilities getCapabilities() {
    return new MongoDBCapabilities();
  }

  /**
   * MongoDB-specific capabilities.
   */
  private static class MongoDBCapabilities implements AdapterCapabilities {
    @Override
    public boolean supportsTransactions() {
      return true; // MongoDB 4.0+ supports multi-document transactions
    }

    @Override
    public boolean supportsBatchOperations() {
      return true;
    }

    @Override
    public boolean supportsSorting() {
      return true;
    }

    @Override
    public boolean supportsPagination() {
      return true;
    }

    @Override
    public boolean supportsFullTextSearch() {
      return true; // MongoDB has text indexes
    }

    @Override
    public boolean supportsAggregations() {
      return true; // MongoDB has powerful aggregation framework
    }

    @Override
    public boolean supportsSecondaryIndexes() {
      return true;
    }

    @Override
    public boolean supportsTTL() {
      return true; // TTL indexes
    }

    @Override
    public boolean supportsOptimisticLocking() {
      return true; // Via version fields
    }

    @Override
    public boolean supportsAsync() {
      return true;
    }

    @Override
    public int getMaxBatchSize() {
      return 1000; // MongoDB bulk write operations
    }

    @Override
    public int getMaxQueryConditions() {
      return -1; // No hard limit
    }
  }
}
