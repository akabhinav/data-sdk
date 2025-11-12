package io.dataverse.adapter.redis;

import io.dataverse.api.Entity;
import io.dataverse.api.Repository;
import io.dataverse.spi.*;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import java.io.Serializable;
import java.time.Duration;

/**
 * Redis adapter implementation.
 *
 * <p>This adapter provides connectivity to Redis, optimized for key-value operations
 * and caching use cases.
 *
 * <p><strong>Configuration Keys:</strong>
 * <ul>
 *   <li>host - Redis host (default: localhost)</li>
 *   <li>port - Redis port (default: 6379)</li>
 *   <li>password - Redis password (optional)</li>
 *   <li>database - Redis database number (default: 0)</li>
 *   <li>pool.maxTotal - Maximum pool connections (default: 100)</li>
 *   <li>pool.maxIdle - Maximum idle connections (default: 10)</li>
 *   <li>pool.minIdle - Minimum idle connections (default: 5)</li>
 *   <li>timeout - Connection timeout in seconds (default: 30)</li>
 * </ul>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class RedisAdapter implements DataSourceAdapter {

  private static final String ADAPTER_ID = "redis";
  private static final String VERSION = "1.0.0";

  private JedisPool jedisPool;
  private RedisConnectionProvider connectionProvider;
  private RedisQueryTranslator queryTranslator;
  private AdapterConfig config;

  @Override
  public String getAdapterId() {
    return ADAPTER_ID;
  }

  @Override
  public DataSourceType getDataSourceType() {
    return DataSourceType.CACHE;
  }

  @Override
  public String getVersion() {
    return VERSION;
  }

  @Override
  public void initialize(AdapterConfig config) {
    this.config = config;

    // Configure Jedis pool
    JedisPoolConfig poolConfig = new JedisPoolConfig();
    poolConfig.setMaxTotal(config.getInt("pool.maxTotal", 100));
    poolConfig.setMaxIdle(config.getInt("pool.maxIdle", 10));
    poolConfig.setMinIdle(config.getInt("pool.minIdle", 5));
    poolConfig.setTestOnBorrow(true);
    poolConfig.setTestOnReturn(true);
    poolConfig.setTestWhileIdle(true);

    // Create Jedis pool
    String host = config.getString("host", "localhost");
    int port = config.getInt("port", 6379);
    int timeout = config.getInt("timeout", 30) * 1000; // Convert to milliseconds
    int database = config.getInt("database", 0);

    if (config.has("password")) {
      String password = config.getString("password").orElse(null);
      this.jedisPool = new JedisPool(poolConfig, host, port, timeout, password, database);
    } else {
      this.jedisPool = new JedisPool(poolConfig, host, port, timeout);
    }

    this.connectionProvider = new RedisConnectionProvider(jedisPool);
    this.queryTranslator = new RedisQueryTranslator();
  }

  @Override
  public <T extends Entity<ID>, ID extends Serializable> Repository<T, ID> createRepository(
      Class<T> entityClass) {
    return new RedisRepository<>(entityClass, connectionProvider, queryTranslator);
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
  public io.dataverse.api.TransactionManager getTransactionManager() {
    // TODO: Implement Redis MULTI/EXEC transaction support
    // Redis supports optimistic locking via WATCH/MULTI/EXEC
    return null;
  }

  @Override
  public boolean isHealthy() {
    try (var jedis = jedisPool.getResource()) {
      return "PONG".equals(jedis.ping());
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public HealthCheckResult healthCheck() {
    try (var jedis = jedisPool.getResource()) {
      String pong = jedis.ping();
      var info = jedis.info("server");

      return HealthCheckResult.healthy()
          .withMessage("Redis connection is healthy")
          .withDetail("ping", pong)
          .withDetail("host", config.getString("host", "localhost"))
          .withDetail("port", config.getInt("port", 6379))
          .withDetail("database", config.getInt("database", 0))
          .withDetail("poolActive", jedisPool.getNumActive())
          .withDetail("poolIdle", jedisPool.getNumIdle())
          .build();
    } catch (Exception e) {
      return HealthCheckResult.unhealthy()
          .withMessage("Redis connection failed")
          .withError(e)
          .build();
    }
  }

  @Override
  public void shutdown() {
    if (connectionProvider != null) {
      connectionProvider.shutdown();
    }
    if (jedisPool != null && !jedisPool.isClosed()) {
      jedisPool.close();
    }
  }

  @Override
  public AdapterCapabilities getCapabilities() {
    return new RedisCapabilities();
  }

  /**
   * Redis-specific capabilities.
   */
  private static class RedisCapabilities implements AdapterCapabilities {
    @Override
    public boolean supportsTransactions() {
      return true; // Redis has MULTI/EXEC transactions
    }

    @Override
    public boolean supportsBatchOperations() {
      return true; // Pipeline and MGET/MSET
    }

    @Override
    public boolean supportsSorting() {
      return true; // Sorted sets
    }

    @Override
    public boolean supportsPagination() {
      return true; // Via SCAN cursor
    }

    @Override
    public boolean supportsFullTextSearch() {
      return true; // RediSearch module (if available)
    }

    @Override
    public boolean supportsAggregations() {
      return false; // Limited aggregation support
    }

    @Override
    public boolean supportsSecondaryIndexes() {
      return false; // Redis is primarily key-value
    }

    @Override
    public boolean supportsTTL() {
      return true; // Native TTL support (EXPIRE command)
    }

    @Override
    public boolean supportsOptimisticLocking() {
      return true; // WATCH command
    }

    @Override
    public boolean supportsAsync() {
      return true;
    }

    @Override
    public int getMaxBatchSize() {
      return 10000; // Practical limit for pipelines
    }

    @Override
    public int getMaxQueryConditions() {
      return 1; // Key-value store - single key lookups
    }
  }
}
