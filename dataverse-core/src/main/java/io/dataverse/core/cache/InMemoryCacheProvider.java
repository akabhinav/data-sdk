package io.dataverse.core.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * In-memory cache provider implementation (L1 cache).
 *
 * <p>This implementation uses ConcurrentHashMap for thread-safe caching with:
 * <ul>
 *   <li>Time-based expiration (TTL)</li>
 *   <li>Size-based eviction (LRU-like)</li>
 *   <li>Cache statistics</li>
 * </ul>
 *
 * <p><strong>Note:</strong> This is a simple implementation. For production use,
 * consider using Caffeine cache library for better performance and features.
 *
 * @param <K> the cache key type
 * @param <V> the cached value type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class InMemoryCacheProvider<K, V> implements CacheProvider<K, V> {

  private final Map<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
  private final String name;
  private final Duration defaultTtl;
  private final long maxSize;
  private final boolean recordStats;

  // Statistics
  private final AtomicLong hitCount = new AtomicLong(0);
  private final AtomicLong missCount = new AtomicLong(0);
  private final AtomicLong evictionCount = new AtomicLong(0);

  public InMemoryCacheProvider(String name, CacheConfig config) {
    this.name = name;
    this.defaultTtl = config.getL1Ttl();
    this.maxSize = config.getL1MaxSize();
    this.recordStats = config.isRecordStats();
  }

  @Override
  public Optional<V> get(K key) {
    CacheEntry<V> entry = cache.get(key);

    if (entry == null) {
      if (recordStats) missCount.incrementAndGet();
      return Optional.empty();
    }

    if (entry.isExpired()) {
      cache.remove(key);
      if (recordStats) {
        missCount.incrementAndGet();
        evictionCount.incrementAndGet();
      }
      return Optional.empty();
    }

    if (recordStats) hitCount.incrementAndGet();
    entry.updateAccessTime();
    return Optional.of(entry.getValue());
  }

  @Override
  public void put(K key, V value) {
    put(key, value, defaultTtl);
  }

  @Override
  public void put(K key, V value, Duration ttl) {
    // Check if we need to evict entries
    if (cache.size() >= maxSize) {
      evictOldestEntry();
    }

    CacheEntry<V> entry = new CacheEntry<>(value, ttl);
    cache.put(key, entry);
  }

  @Override
  public V computeIfAbsent(K key, Supplier<V> valueLoader) {
    return computeIfAbsent(key, valueLoader, defaultTtl);
  }

  @Override
  public V computeIfAbsent(K key, Supplier<V> valueLoader, Duration ttl) {
    // Check cache first
    Optional<V> cached = get(key);
    if (cached.isPresent()) {
      return cached.get();
    }

    // Compute value
    V value = valueLoader.get();
    if (value != null) {
      put(key, value, ttl);
    }

    return value;
  }

  @Override
  public void evict(K key) {
    if (cache.remove(key) != null) {
      if (recordStats) evictionCount.incrementAndGet();
    }
  }

  @Override
  public void clear() {
    long size = cache.size();
    cache.clear();
    if (recordStats) evictionCount.addAndGet(size);
  }

  @Override
  public boolean containsKey(K key) {
    CacheEntry<V> entry = cache.get(key);
    if (entry == null) {
      return false;
    }
    if (entry.isExpired()) {
      cache.remove(key);
      return false;
    }
    return true;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public CacheLevel getLevel() {
    return CacheLevel.L1;
  }

  @Override
  public CacheStats getStats() {
    return new CacheStats(
        hitCount.get(),
        missCount.get(),
        evictionCount.get(),
        cache.size()
    );
  }

  @Override
  public boolean isHealthy() {
    return true;
  }

  @Override
  public void shutdown() {
    cache.clear();
  }

  /**
   * Evicts the oldest entry based on last access time.
   */
  private void evictOldestEntry() {
    K oldestKey = null;
    Instant oldestAccessTime = Instant.now();

    for (Map.Entry<K, CacheEntry<V>> entry : cache.entrySet()) {
      if (entry.getValue().getLastAccessTime().isBefore(oldestAccessTime)) {
        oldestAccessTime = entry.getValue().getLastAccessTime();
        oldestKey = entry.getKey();
      }
    }

    if (oldestKey != null) {
      cache.remove(oldestKey);
      if (recordStats) evictionCount.incrementAndGet();
    }
  }

  /**
   * Cache entry wrapper with expiration and access time tracking.
   */
  private static class CacheEntry<V> {
    private final V value;
    private final Instant expiresAt;
    private volatile Instant lastAccessTime;

    CacheEntry(V value, Duration ttl) {
      this.value = value;
      this.lastAccessTime = Instant.now();
      this.expiresAt = ttl != null ? lastAccessTime.plus(ttl) : Instant.MAX;
    }

    V getValue() {
      return value;
    }

    boolean isExpired() {
      return Instant.now().isAfter(expiresAt);
    }

    Instant getLastAccessTime() {
      return lastAccessTime;
    }

    void updateAccessTime() {
      this.lastAccessTime = Instant.now();
    }
  }
}
