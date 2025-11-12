package io.dataverse.spi;

import io.dataverse.api.Entity;
import io.dataverse.api.Repository;
import java.io.Serializable;

/**
 * Service Provider Interface for data source adapters.
 *
 * <p>DataSourceAdapter is the primary extension point for adding support for new data sources.
 * Each adapter provides connectivity to a specific data source (e.g., DynamoDB, MongoDB, Redis)
 * and translates generic DataVerse operations to native data source commands.
 *
 * <p><strong>Implementation Guidelines:</strong>
 * <ul>
 *   <li>Implement this interface in a separate module</li>
 *   <li>Register via {@code META-INF/services/io.dataverse.spi.DataSourceAdapter}</li>
 *   <li>Depend only on dataverse-core + your native client library</li>
 *   <li>Ensure thread-safety for all operations</li>
 *   <li>Support Java 21 virtual threads</li>
 * </ul>
 *
 * <p><strong>Example Registration:</strong>
 * <pre>
 * File: META-INF/services/io.dataverse.spi.DataSourceAdapter
 * Content: io.dataverse.adapter.dynamodb.DynamoDBAdapter
 * </pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface DataSourceAdapter {

  /**
   * Returns the unique identifier for this adapter.
   *
   * <p>Common identifiers: "dynamodb", "mongodb", "redis", "postgresql", "cassandra", etc.
   *
   * @return the adapter identifier, never {@code null}
   */
  String getAdapterId();

  /**
   * Returns the data source type this adapter supports.
   *
   * @return the data source type (e.g., "nosql", "sql", "cache", "search")
   */
  DataSourceType getDataSourceType();

  /**
   * Returns the version of this adapter.
   *
   * @return the adapter version, never {@code null}
   */
  String getVersion();

  /**
   * Initializes the adapter with the given configuration.
   *
   * <p>This method is called once during adapter registration. Perform any necessary
   * setup such as connection pool initialization, client configuration, etc.
   *
   * @param config the adapter configuration, never {@code null}
   * @throws AdapterException if initialization fails
   */
  void initialize(AdapterConfig config);

  /**
   * Creates a repository for the specified entity type.
   *
   * @param entityClass the entity class, never {@code null}
   * @param <T> the entity type
   * @param <ID> the identifier type
   * @return a repository instance, never {@code null}
   * @throws AdapterException if repository creation fails
   */
  <T extends Entity<ID>, ID extends Serializable> Repository<T, ID> createRepository(
      Class<T> entityClass);

  /**
   * Returns the connection provider for this adapter.
   *
   * @return the connection provider, never {@code null}
   */
  ConnectionProvider getConnectionProvider();

  /**
   * Returns the query translator for this adapter.
   *
   * @return the query translator, never {@code null}
   */
  QueryTranslator getQueryTranslator();

  /**
   * Returns the transaction manager for this adapter.
   *
   * <p>Returns {@code null} if the adapter does not support transactions.
   *
   * @return the transaction manager, or {@code null} if not supported
   */
  io.dataverse.api.TransactionManager getTransactionManager();

  /**
   * Checks if the adapter is healthy and ready to serve requests.
   *
   * @return {@code true} if healthy, {@code false} otherwise
   */
  boolean isHealthy();

  /**
   * Performs a health check and returns detailed status information.
   *
   * @return health check result, never {@code null}
   */
  HealthCheckResult healthCheck();

  /**
   * Shuts down the adapter and releases all resources.
   *
   * <p>This method is called during application shutdown. Close all connections,
   * release thread pools, and perform any necessary cleanup.
   */
  void shutdown();

  /**
   * Returns adapter-specific capabilities.
   *
   * @return the capabilities, never {@code null}
   */
  AdapterCapabilities getCapabilities();

  /**
   * Data source type enumeration.
   */
  enum DataSourceType {
    /** NoSQL databases (DynamoDB, MongoDB, Cassandra) */
    NOSQL,

    /** SQL/Relational databases (PostgreSQL, MySQL) */
    SQL,

    /** Key-Value stores and caches (Redis, Memcached) */
    CACHE,

    /** Search engines (Elasticsearch, OpenSearch) */
    SEARCH,

    /** Time-series databases */
    TIMESERIES,

    /** Graph databases */
    GRAPH,

    /** Custom/Other */
    OTHER
  }
}
