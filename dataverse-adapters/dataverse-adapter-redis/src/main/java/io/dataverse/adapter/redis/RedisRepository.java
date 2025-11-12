package io.dataverse.adapter.redis;

import io.dataverse.api.Entity;
import io.dataverse.api.QueryBuilder;
import io.dataverse.core.AbstractRepository;
import io.dataverse.core.query.DefaultQueryBuilder;
import io.dataverse.core.query.Query;
import io.dataverse.spi.ConnectionProvider;
import io.dataverse.spi.QueryTranslator;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Redis implementation of the Repository interface.
 *
 * <p>This implementation treats Redis as a key-value store for entities.
 * Entities are serialized to JSON and stored with keys prefixed by entity class name.
 *
 * @param <T> the entity type
 * @param <ID> the identifier type
 * @since 1.0.0
 */
public class RedisRepository<T extends Entity<ID>, ID extends Serializable>
    extends AbstractRepository<T, ID> {

  private final ConnectionProvider<Jedis> connectionProvider;
  private final QueryTranslator queryTranslator;
  private final String keyPrefix;

  public RedisRepository(
      Class<T> entityClass,
      ConnectionProvider<Jedis> connectionProvider,
      QueryTranslator queryTranslator) {
    super(entityClass);
    this.connectionProvider = connectionProvider;
    this.queryTranslator = queryTranslator;
    this.keyPrefix = entityClass.getSimpleName() + ":";
  }

  /**
   * Builds the Redis key for an entity ID.
   */
  private String buildKey(ID id) {
    return keyPrefix + id.toString();
  }

  @Override
  protected T doSave(T entity) {
    return connectionProvider.execute(jedis -> {
      try {
        String key = buildKey(entity.getId());
        String value = serializeEntity(entity);

        jedis.set(key, value);

        return entity;
      } catch (Exception e) {
        throw new RuntimeException("Failed to save entity to Redis", e);
      }
    });
  }

  @Override
  protected Optional<T> doFindById(ID id) {
    return connectionProvider.execute(jedis -> {
      try {
        String key = buildKey(id);
        String value = jedis.get(key);

        if (value != null) {
          return Optional.of(deserializeEntity(value));
        } else {
          return Optional.empty();
        }
      } catch (Exception e) {
        throw new RuntimeException("Failed to find entity by ID in Redis", e);
      }
    });
  }

  @Override
  protected void doDelete(ID id) {
    connectionProvider.execute(jedis -> {
      try {
        String key = buildKey(id);
        jedis.del(key);
        return null;
      } catch (Exception e) {
        throw new RuntimeException("Failed to delete entity from Redis", e);
      }
    });
  }

  @Override
  protected List<T> doFindAll() {
    return connectionProvider.execute(jedis -> {
      try {
        List<T> results = new ArrayList<>();
        String pattern = keyPrefix + "*";

        // Use SCAN for safe iteration
        String cursor = "0";
        ScanParams params = new ScanParams().match(pattern).count(100);

        do {
          ScanResult<String> scanResult = jedis.scan(cursor, params);
          List<String> keys = scanResult.getResult();

          // Get values for all keys
          if (!keys.isEmpty()) {
            for (String key : keys) {
              String value = jedis.get(key);
              if (value != null) {
                results.add(deserializeEntity(value));
              }
            }
          }

          cursor = scanResult.getCursor();
        } while (!"0".equals(cursor));

        return results;
      } catch (Exception e) {
        throw new RuntimeException("Failed to find all entities in Redis", e);
      }
    });
  }

  @Override
  protected long doCount() {
    return connectionProvider.execute(jedis -> {
      try {
        long count = 0;
        String pattern = keyPrefix + "*";
        String cursor = "0";
        ScanParams params = new ScanParams().match(pattern).count(100);

        do {
          ScanResult<String> scanResult = jedis.scan(cursor, params);
          count += scanResult.getResult().size();
          cursor = scanResult.getCursor();
        } while (!"0".equals(cursor));

        return count;
      } catch (Exception e) {
        throw new RuntimeException("Failed to count entities in Redis", e);
      }
    });
  }

  @Override
  public QueryBuilder<T> query() {
    return new DefaultQueryBuilder<>(this::executeQuery);
  }

  @Override
  public List<T> executeNativeQuery(String nativeQuery) {
    // Native Redis commands could be executed here
    throw new UnsupportedOperationException("Native queries not yet implemented for Redis");
  }

  /**
   * Executes a query (limited support for Redis).
   */
  private List<T> executeQuery(Query query) {
    // Redis doesn't support complex queries well
    // For now, fall back to scanning all keys
    return doFindAll();
  }

  /**
   * Serializes an entity to a string (simplified - use JSON in production).
   */
  protected String serializeEntity(T entity) {
    // Simplified: In production, use Jackson or other JSON library
    // For now, return a simple representation
    return entity.toString();
  }

  /**
   * Deserializes a string to an entity (simplified - use JSON in production).
   */
  protected T deserializeEntity(String value) {
    // Simplified: In production, use proper deserialization
    try {
      return entityClass.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      throw new RuntimeException("Failed to deserialize entity from Redis", e);
    }
  }

  @Override
  protected void shutdown() {
    super.shutdown();
    // Additional cleanup if needed
  }
}
