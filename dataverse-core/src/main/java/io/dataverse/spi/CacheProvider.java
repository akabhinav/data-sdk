package io.dataverse.spi;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Service Provider Interface for cache implementations.
 *
 * <p>CacheProvider enables pluggable caching strategies for DataVerse operations.
 * Implementations can provide in-memory, distributed, or multi-level caching.
 *
 * <p><strong>Example Implementations:</strong>
 * <ul>
 *   <li>In-memory cache (Caffeine)</li>
 *   <li>Distributed cache (Redis)</li>
 *   <li>Multi-level cache (L1 + L2)</li>
 * </ul>
 *
 * @param <K> the cache key type
 * @param <V> the cache value type
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface CacheProvider<K, V> {

  /**
   * Gets a value from the cache.
   *
   * @param key the cache key, must not be {@code null}
   * @return an Optional containing the cached value if present
   * @throws IllegalArgumentException if key is {@code null}
   */
  Optional<V> get(K key);

  /**
   * Gets a value from the cache, or computes and caches it if absent.
   *
   * @param key the cache key, must not be {@code null}
   * @param valueLoader the function to compute the value if absent
   * @return the cached or computed value, never {@code null}
   * @throws IllegalArgumentException if key or valueLoader is {@code null}
   */
  V get(K key, Supplier<V> valueLoader);

  /**
   * Puts a value in the cache.
   *
   * @param key the cache key, must not be {@code null}
   * @param value the value to cache, must not be {@code null}
   * @throws IllegalArgumentException if key or value is {@code null}
   */
  void put(K key, V value);

  /**
   * Puts a value in the cache with TTL (time-to-live).
   *
   * @param key the cache key, must not be {@code null}
   * @param value the value to cache, must not be {@code null}
   * @param ttl the time-to-live duration
   * @throws IllegalArgumentException if key, value, or ttl is {@code null}
   */
  void put(K key, V value, Duration ttl);

  /**
   * Removes a value from the cache.
   *
   * @param key the cache key, must not be {@code null}
   * @throws IllegalArgumentException if key is {@code null}
   */
  void evict(K key);

  /**
   * Clears all entries from the cache.
   */
  void clear();

  /**
   * Returns the number of entries in the cache.
   *
   * @return the cache size
   */
  long size();

  /**
   * Returns cache statistics.
   *
   * @return cache statistics, never {@code null}
   */
  CacheStatistics getStatistics();

  /**
   * Asynchronously gets a value from the cache.
   *
   * @param key the cache key, must not be {@code null}
   * @return a CompletableFuture that will complete with an Optional containing the value
   */
  CompletableFuture<Optional<V>> getAsync(K key);

  /**
   * Asynchronously puts a value in the cache.
   *
   * @param key the cache key, must not be {@code null}
   * @param value the value to cache, must not be {@code null}
   * @return a CompletableFuture that will complete when the operation finishes
   */
  CompletableFuture<Void> putAsync(K key, V value);

  /**
   * Cache statistics interface.
   */
  interface CacheStatistics {
    /** Total number of cache requests. */
    long getRequestCount();

    /** Number of cache hits. */
    long getHitCount();

    /** Number of cache misses. */
    long getMissCount();

    /** Cache hit rate (0.0 to 1.0). */
    double getHitRate();

    /** Cache miss rate (0.0 to 1.0). */
    double getMissRate();

    /** Number of evictions. */
    long getEvictionCount();

    /** Average load time in nanoseconds. */
    double getAverageLoadPenalty();
  }
}
