package io.dataverse.core.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Multi-level cache provider implementation.
 *
 * <p>Implements a cache hierarchy with multiple levels (L1, L2):
 * <ul>
 *   <li><strong>L1 (Local):</strong> Fast in-memory cache, typically small</li>
 *   <li><strong>L2 (Distributed):</strong> Shared cache (e.g., Redis), larger but slower</li>
 * </ul>
 *
 * <p><strong>Read Flow:</strong>
 * <ol>
 *   <li>Check L1 cache (fastest)</li>
 *   <li>If miss, check L2 cache</li>
 *   <li>If L2 hit, populate L1 and return</li>
 *   <li>If L2 miss, load from data source, populate L2 and L1</li>
 * </ol>
 *
 * <p><strong>Write Flow (Write-Through):</strong>
 * <ol>
 *   <li>Update L1 cache</li>
 *   <li>Update L2 cache</li>
 *   <li>Update data source</li>
 * </ol>
 *
 * <p><strong>Eviction:</strong>
 * <ul>
 *   <li>When evicting, remove from all cache levels</li>
 *   <li>Ensures consistency across cache hierarchy</li>
 * </ul>
 *
 * @param <K> the cache key type
 * @param <V> the cached value type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class MultiLevelCacheProvider<K, V> implements CacheProvider<K, V> {

  private final CacheProvider<K, V> l1Cache;
  private final CacheProvider<K, V> l2Cache;
  private final String name;

  /**
   * Creates a multi-level cache with L1 and L2.
   *
   * @param name the cache name
   * @param l1Cache the L1 (local) cache provider
   * @param l2Cache the L2 (distributed) cache provider
   */
  public MultiLevelCacheProvider(String name, CacheProvider<K, V> l1Cache, CacheProvider<K, V> l2Cache) {
    this.name = name;
    this.l1Cache = l1Cache;
    this.l2Cache = l2Cache;
  }

  /**
   * Creates a single-level cache (L1 only).
   *
   * @param name the cache name
   * @param l1Cache the L1 cache provider
   */
  public MultiLevelCacheProvider(String name, CacheProvider<K, V> l1Cache) {
    this(name, l1Cache, new NoCacheProvider<>());
  }

  @Override
  public Optional<V> get(K key) {
    // Try L1 first (fastest)
    Optional<V> l1Result = l1Cache.get(key);
    if (l1Result.isPresent()) {
      return l1Result;
    }

    // L1 miss - try L2
    Optional<V> l2Result = l2Cache.get(key);
    if (l2Result.isPresent()) {
      // L2 hit - populate L1 for next time
      l1Cache.put(key, l2Result.get());
      return l2Result;
    }

    // Both caches missed
    return Optional.empty();
  }

  @Override
  public void put(K key, V value) {
    // Write to both caches
    l1Cache.put(key, value);
    l2Cache.put(key, value);
  }

  @Override
  public void put(K key, V value, Duration ttl) {
    // Write to both caches with TTL
    l1Cache.put(key, value, ttl);
    l2Cache.put(key, value, ttl);
  }

  @Override
  public V computeIfAbsent(K key, Supplier<V> valueLoader) {
    // Try L1 first
    Optional<V> l1Result = l1Cache.get(key);
    if (l1Result.isPresent()) {
      return l1Result.get();
    }

    // L1 miss - try L2
    Optional<V> l2Result = l2Cache.get(key);
    if (l2Result.isPresent()) {
      // L2 hit - populate L1
      V value = l2Result.get();
      l1Cache.put(key, value);
      return value;
    }

    // Both caches missed - load value
    V value = valueLoader.get();
    if (value != null) {
      // Populate both caches
      l1Cache.put(key, value);
      l2Cache.put(key, value);
    }

    return value;
  }

  @Override
  public V computeIfAbsent(K key, Supplier<V> valueLoader, Duration ttl) {
    // Try L1 first
    Optional<V> l1Result = l1Cache.get(key);
    if (l1Result.isPresent()) {
      return l1Result.get();
    }

    // L1 miss - try L2
    Optional<V> l2Result = l2Cache.get(key);
    if (l2Result.isPresent()) {
      // L2 hit - populate L1
      V value = l2Result.get();
      l1Cache.put(key, value, ttl);
      return value;
    }

    // Both caches missed - load value
    V value = valueLoader.get();
    if (value != null) {
      // Populate both caches with TTL
      l1Cache.put(key, value, ttl);
      l2Cache.put(key, value, ttl);
    }

    return value;
  }

  @Override
  public void evict(K key) {
    // Evict from all cache levels
    l1Cache.evict(key);
    l2Cache.evict(key);
  }

  @Override
  public void clear() {
    // Clear all cache levels
    l1Cache.clear();
    l2Cache.clear();
  }

  @Override
  public boolean containsKey(K key) {
    return l1Cache.containsKey(key) || l2Cache.containsKey(key);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public CacheLevel getLevel() {
    return CacheLevel.L1;  // Multi-level is considered L1 from external perspective
  }

  @Override
  public CacheStats getStats() {
    // Aggregate stats from both caches
    CacheStats l1Stats = l1Cache.getStats();
    CacheStats l2Stats = l2Cache.getStats();

    return new CacheStats(
        l1Stats.getHitCount() + l2Stats.getHitCount(),
        l1Stats.getMissCount() + l2Stats.getMissCount(),
        l1Stats.getEvictionCount() + l2Stats.getEvictionCount(),
        l1Stats.getSize() + l2Stats.getSize()
    );
  }

  /**
   * Gets L1 cache statistics.
   *
   * @return L1 cache stats
   */
  public CacheStats getL1Stats() {
    return l1Cache.getStats();
  }

  /**
   * Gets L2 cache statistics.
   *
   * @return L2 cache stats
   */
  public CacheStats getL2Stats() {
    return l2Cache.getStats();
  }

  @Override
  public boolean isHealthy() {
    // Both caches must be healthy
    return l1Cache.isHealthy() && l2Cache.isHealthy();
  }

  @Override
  public void shutdown() {
    l1Cache.shutdown();
    l2Cache.shutdown();
  }

  /**
   * Gets the L1 cache provider.
   *
   * @return the L1 cache
   */
  public CacheProvider<K, V> getL1Cache() {
    return l1Cache;
  }

  /**
   * Gets the L2 cache provider.
   *
   * @return the L2 cache
   */
  public CacheProvider<K, V> getL2Cache() {
    return l2Cache;
  }
}
