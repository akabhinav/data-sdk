package io.dataverse.cache.caffeine;

import io.dataverse.spi.CacheProvider;
import com.github.ben-manes.caffeine.cache.Cache;
import com.github.ben-manes.caffeine.cache.Caffeine;
import com.github.ben-manes.caffeine.cache.stats.CacheStats;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * High-performance in-memory cache provider using Caffeine.
 *
 * <p>Caffeine is a high-performance, near-optimal caching library for Java 8+.
 * It provides automatic loading, size-based eviction, time-based expiration,
 * and comprehensive statistics.
 *
 * <p><strong>Features:</strong>
 * <ul>
 *   <li>Automatic cache loading with Supplier
 *   <li>Size-based eviction (LRU-like policy)
 *   <li>Time-based expiration (TTL)
 *   <li>Weak/soft reference values
 *   <li>Statistics tracking
 *   <li>Thread-safe operations
 * </ul>
 *
 * <p><strong>Example Usage:</strong>
 * <pre>{@code
 * CacheProvider<String, User> cache = CaffeineCacheProvider.<String, User>builder()
 *     .maximumSize(10000)
 *     .expireAfterWrite(Duration.ofMinutes(10))
 *     .recordStats()
 *     .build();
 *
 * User user = cache.get("userId123", () -> loadUserFromDatabase("userId123"));
 * }</pre>
 *
 * @param <K> the cache key type
 * @param <V> the cache value type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class CaffeineCacheProvider<K, V> implements CacheProvider<K, V> {

  private final Cache<K, V> cache;

  /**
   * Constructs a cache provider with the given Caffeine cache.
   *
   * @param cache the Caffeine cache instance
   */
  public CaffeineCacheProvider(Cache<K, V> cache) {
    this.cache = cache;
  }

  @Override
  public Optional<V> get(K key) {
    if (key == null) {
      throw new IllegalArgumentException("Cache key must not be null");
    }
    return Optional.ofNullable(cache.getIfPresent(key));
  }

  @Override
  public V get(K key, Supplier<V> valueLoader) {
    if (key == null) {
      throw new IllegalArgumentException("Cache key must not be null");
    }
    if (valueLoader == null) {
      throw new IllegalArgumentException("Value loader must not be null");
    }

    return cache.get(key, k -> valueLoader.get());
  }

  @Override
  public void put(K key, V value) {
    if (key == null) {
      throw new IllegalArgumentException("Cache key must not be null");
    }
    if (value == null) {
      throw new IllegalArgumentException("Cache value must not be null");
    }

    cache.put(key, value);
  }

  @Override
  public void put(K key, V value, Duration ttl) {
    // Caffeine doesn't support per-entry TTL in the same cache instance
    // This would require a separate cache with different TTL settings
    // For now, use the cache-wide TTL configured during build
    put(key, value);
  }

  @Override
  public void evict(K key) {
    if (key == null) {
      throw new IllegalArgumentException("Cache key must not be null");
    }
    cache.invalidate(key);
  }

  @Override
  public void clear() {
    cache.invalidateAll();
  }

  @Override
  public long size() {
    return cache.estimatedSize();
  }

  @Override
  public CacheStatistics getStatistics() {
    CacheStats stats = cache.stats();

    return new CacheStatistics() {
      @Override
      public long getRequestCount() {
        return stats.requestCount();
      }

      @Override
      public long getHitCount() {
        return stats.hitCount();
      }

      @Override
      public long getMissCount() {
        return stats.missCount();
      }

      @Override
      public double getHitRate() {
        return stats.hitRate();
      }

      @Override
      public double getMissRate() {
        return stats.missRate();
      }

      @Override
      public long getEvictionCount() {
        return stats.evictionCount();
      }

      @Override
      public double getAverageLoadPenalty() {
        return stats.averageLoadPenalty();
      }
    };
  }

  @Override
  public CompletableFuture<Optional<V>> getAsync(K key) {
    return CompletableFuture.supplyAsync(() -> get(key));
  }

  @Override
  public CompletableFuture<Void> putAsync(K key, V value) {
    return CompletableFuture.runAsync(() -> put(key, value));
  }

  /**
   * Creates a new builder for configuring a Caffeine cache provider.
   *
   * @param <K> the cache key type
   * @param <V> the cache value type
   * @return a new builder instance
   */
  public static <K, V> Builder<K, V> builder() {
    return new Builder<>();
  }

  /**
   * Builder for CaffeineCacheProvider.
   *
   * @param <K> the cache key type
   * @param <V> the cache value type
   */
  public static final class Builder<K, V> {
    private final Caffeine<Object, Object> caffeineBuilder;

    private Builder() {
      this.caffeineBuilder = Caffeine.newBuilder();
    }

    /**
     * Sets the maximum number of entries the cache may contain.
     *
     * @param maximumSize the maximum size
     * @return this builder
     */
    public Builder<K, V> maximumSize(long maximumSize) {
      caffeineBuilder.maximumSize(maximumSize);
      return this;
    }

    /**
     * Sets the maximum total weight of entries the cache may contain.
     *
     * @param maximumWeight the maximum weight
     * @return this builder
     */
    public Builder<K, V> maximumWeight(long maximumWeight) {
      caffeineBuilder.maximumWeight(maximumWeight);
      return this;
    }

    /**
     * Specifies that each entry should be automatically removed from the cache
     * once a fixed duration has elapsed after the entry's creation or replacement.
     *
     * @param duration the TTL duration
     * @return this builder
     */
    public Builder<K, V> expireAfterWrite(Duration duration) {
      caffeineBuilder.expireAfterWrite(duration.toMillis(), TimeUnit.MILLISECONDS);
      return this;
    }

    /**
     * Specifies that each entry should be automatically removed from the cache
     * once a fixed duration has elapsed after the entry's last access.
     *
     * @param duration the idle timeout duration
     * @return this builder
     */
    public Builder<K, V> expireAfterAccess(Duration duration) {
      caffeineBuilder.expireAfterAccess(duration.toMillis(), TimeUnit.MILLISECONDS);
      return this;
    }

    /**
     * Specifies that active entries are eligible for automatic refresh once a fixed
     * duration has elapsed after the entry's creation or replacement.
     *
     * @param duration the refresh duration
     * @return this builder
     */
    public Builder<K, V> refreshAfterWrite(Duration duration) {
      caffeineBuilder.refreshAfterWrite(duration.toMillis(), TimeUnit.MILLISECONDS);
      return this;
    }

    /**
     * Enables the accumulation of cache statistics.
     *
     * @return this builder
     */
    public Builder<K, V> recordStats() {
      caffeineBuilder.recordStats();
      return this;
    }

    /**
     * Specifies that each key should be wrapped in a weak reference.
     *
     * @return this builder
     */
    public Builder<K, V> weakKeys() {
      caffeineBuilder.weakKeys();
      return this;
    }

    /**
     * Specifies that each value should be wrapped in a weak reference.
     *
     * @return this builder
     */
    public Builder<K, V> weakValues() {
      caffeineBuilder.weakValues();
      return this;
    }

    /**
     * Specifies that each value should be wrapped in a soft reference.
     *
     * @return this builder
     */
    public Builder<K, V> softValues() {
      caffeineBuilder.softValues();
      return this;
    }

    /**
     * Builds the cache provider.
     *
     * @return a new CaffeineCacheProvider instance
     */
    @SuppressWarnings("unchecked")
    public CaffeineCacheProvider<K, V> build() {
      Cache<K, V> cache = (Cache<K, V>) caffeineBuilder.build();
      return new CaffeineCacheProvider<>(cache);
    }
  }
}
