package io.dataverse.adapter.redis;

import io.dataverse.spi.ConnectionException;
import io.dataverse.spi.ConnectionProvider;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Connection provider for Redis using Jedis.
 *
 * @since 1.0.0
 */
public class RedisConnectionProvider implements ConnectionProvider<Jedis> {

  private final JedisPool jedisPool;

  public RedisConnectionProvider(JedisPool jedisPool) {
    this.jedisPool = jedisPool;
  }

  @Override
  public Jedis acquire() {
    try {
      return jedisPool.getResource();
    } catch (Exception e) {
      throw new ConnectionException("Failed to acquire Redis connection", e);
    }
  }

  @Override
  public Jedis acquire(Duration timeout) {
    // Jedis doesn't support timeout on acquire, use default
    return acquire();
  }

  @Override
  public CompletableFuture<Jedis> acquireAsync() {
    return CompletableFuture.supplyAsync(this::acquire);
  }

  @Override
  public void release(Jedis connection) {
    if (connection != null) {
      connection.close(); // Returns to pool
    }
  }

  @Override
  public boolean validate(Jedis connection) {
    try {
      return "PONG".equals(connection.ping());
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public <R> R execute(ConnectionOperation<Jedis, R> operation) {
    Jedis jedis = null;
    try {
      jedis = acquire();
      return operation.execute(jedis);
    } catch (Exception e) {
      throw new ConnectionException("Failed to execute operation", e);
    } finally {
      release(jedis);
    }
  }

  @Override
  public PoolStatistics getPoolStatistics() {
    return new PoolStatistics() {
      @Override
      public int getTotalConnections() {
        return jedisPool.getNumActive() + jedisPool.getNumIdle();
      }

      @Override
      public int getActiveConnections() {
        return jedisPool.getNumActive();
      }

      @Override
      public int getIdleConnections() {
        return jedisPool.getNumIdle();
      }

      @Override
      public int getWaitingThreads() {
        return jedisPool.getNumWaiters();
      }

      @Override
      public long getAverageAcquisitionTimeMs() {
        return 0; // Not tracked by Jedis
      }

      @Override
      public long getMaxAcquisitionTimeMs() {
        return 0; // Not tracked by Jedis
      }

      @Override
      public long getTotalAcquisitions() {
        return 0; // Not tracked by Jedis
      }

      @Override
      public long getTimeoutCount() {
        return 0; // Not tracked by Jedis
      }
    };
  }

  @Override
  public int evictIdleConnections() {
    // Jedis pool manages idle connections automatically
    return 0;
  }

  @Override
  public void shutdown() {
    if (jedisPool != null && !jedisPool.isClosed()) {
      jedisPool.close();
    }
  }
}
