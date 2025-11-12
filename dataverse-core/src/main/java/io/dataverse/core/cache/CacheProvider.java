package io.dataverse.core.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Interface for cache providers in the multi-level caching system.
 *
 * <p>Cache providers abstract the underlying cache implementation (Caffeine, Redis, Memcached, etc.)
 * and provide a uniform API for cache operations.
 *
 * <p>Supports:
 * <ul>
 *   <li>Basic get/put/evict operations</li>
 *   <li>Time-to-live (TTL) configuration</li>
 *   <li>Cache-aside pattern with computeIfAbsent</li>
 *   <li>Bulk operations for efficiency</li>
 *   <li>Cache statistics and monitoring</li>
 * </ul>
 *
 * @param <K> the cache key type
 * @param <V> the cached value type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface CacheProvider<K, V> {

  /**
   * Gets a value from the cache.
   *
   * @param key the cache key
   * @return an Optional containing the cached value, or empty if not found
   */
  Optional<V> get(K key);

  /**
   * Puts a value in the cache with default TTL.
   *
   * @param key the cache key
   * @param value the value to cache
   */
  void put(K key, V value);

  /**
   * Puts a value in the cache with specific TTL.
   *
   * @param key the cache key
   * @param value the value to cache
   * @param ttl the time-to-live duration
   */
  void put(K key, V value, Duration ttl);

  /**
   * Gets a value from cache, computing it if absent (cache-aside pattern).
   *
   * <p>This method:
   * <ol>
   *   <li>Checks if value exists in cache</li>
   *   <li>If found, returns cached value</li>
   *   <li>If not found, computes value using supplier</li>
   *   <li>Stores computed value in cache</li>
   *   <li>Returns computed value</li>
   * </ol>
   *
   * @param key the cache key
   * @param valueLoader the supplier to compute the value if not cached
   * @return the cached or computed value
   */
  V computeIfAbsent(K key, Supplier<V> valueLoader);

  /**
   * Gets a value from cache, computing it if absent with custom TTL.
   *
   * @param key the cache key
   * @param valueLoader the supplier to compute the value if not cached
   * @param ttl the time-to-live for the computed value
   * @return the cached or computed value
   */
  V computeIfAbsent(K key, Supplier<V> valueLoader, Duration ttl);

  /**
   * Removes a value from the cache.
   *
   * @param key the cache key to evict
   */
  void evict(K key);

  /**
   * Removes all entries from the cache.
   */
  void clear();

  /**
   * Checks if a key exists in the cache.
   *
   * @param key the cache key
   * @return true if the key exists, false otherwise
   */
  boolean containsKey(K key);

  /**
   * Gets the name/identifier of this cache provider.
   *
   * @return the cache provider name (e.g., "L1-Caffeine", "L2-Redis")
   */
  String getName();

  /**
   * Gets the cache level (L1, L2, etc.).
   *
   * @return the cache level
   */
  CacheLevel getLevel();

  /**
   * Gets cache statistics.
   *
   * @return the cache statistics
   */
  CacheStats getStats();

  /**
   * Checks if this cache provider is healthy and available.
   *
   * @return true if healthy, false otherwise
   */
  boolean isHealthy();

  /**
   * Shuts down the cache provider and releases resources.
   */
  void shutdown();

  /**
   * Cache level enumeration.
   */
  enum CacheLevel {
    /** Level 1 cache - typically in-memory, fast, local */
    L1,
    /** Level 2 cache - typically distributed, slower than L1, shared */
    L2,
    /** Level 3 cache - typically persistent, slowest, largest capacity */
    L3
  }

  /**
   * Cache statistics.
   */
  class CacheStats {
    private final long hitCount;
    private final long missCount;
    private final long evictionCount;
    private final long size;

    public CacheStats(long hitCount, long missCount, long evictionCount, long size) {
      this.hitCount = hitCount;
      this.missCount = missCount;
      this.evictionCount = evictionCount;
      this.size = size;
    }

    public long getHitCount() {
      return hitCount;
    }

    public long getMissCount() {
      return missCount;
    }

    public long getEvictionCount() {
      return evictionCount;
    }

    public long getSize() {
      return size;
    }

    public double getHitRate() {
      long totalRequests = hitCount + missCount;
      return totalRequests == 0 ? 0.0 : (double) hitCount / totalRequests;
    }

    @Override
    public String toString() {
      return String.format("CacheStats{hits=%d, misses=%d, hitRate=%.2f%%, evictions=%d, size=%d}",
          hitCount, missCount, getHitRate() * 100, evictionCount, size);
    }
  }
}
