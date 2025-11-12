package io.dataverse.core.cache;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Cache manager for query results.
 *
 * <p>Caches entire query results (lists, counts, aggregates) with automatic invalidation
 * when the underlying data changes.
 *
 * <p><strong>Key Features:</strong>
 * <ul>
 *   <li>Automatic cache key generation from query parameters</li>
 *   <li>TTL-based expiration</li>
 *   <li>Entity-type based invalidation</li>
 *   <li>Thread-safe operations</li>
 *   <li>Statistics tracking</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Create cache
 * QueryCache<User> cache = new QueryCache<>("User", Duration.ofMinutes(5));
 *
 * // Cache a query result
 * List<User> activeUsers = cache.computeIfAbsent(
 *     "findByActiveTrue",
 *     () -> repository.query().where("active").isTrue().execute()
 * );
 *
 * // Invalidate on update
 * repository.save(user);
 * cache.invalidateAll();  // Clear all cached queries for User
 * }</pre>
 *
 * @param <T> the entity type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class QueryCache<T> {

  private final String entityType;
  private final Duration defaultTtl;
  private final Map<String, QueryCacheEntry<Object>> cache = new ConcurrentHashMap<>();

  // Statistics
  private long hitCount = 0;
  private long missCount = 0;
  private long evictionCount = 0;

  /**
   * Creates a query cache for the given entity type.
   *
   * @param entityType the entity type name
   * @param defaultTtl the default TTL for cached entries
   */
  public QueryCache(String entityType, Duration defaultTtl) {
    this.entityType = entityType;
    this.defaultTtl = defaultTtl;
  }

  /**
   * Gets a cached query result or computes it if absent.
   *
   * @param queryKey the query key (should be unique for the query and parameters)
   * @param loader the supplier to compute the result if not cached
   * @param <R> the result type
   * @return the cached or computed result
   */
  @SuppressWarnings("unchecked")
  public <R> R computeIfAbsent(String queryKey, Supplier<R> loader) {
    return computeIfAbsent(queryKey, loader, defaultTtl);
  }

  /**
   * Gets a cached query result or computes it if absent with custom TTL.
   *
   * @param queryKey the query key
   * @param loader the supplier to compute the result if not cached
   * @param ttl the TTL for this entry
   * @param <R> the result type
   * @return the cached or computed result
   */
  @SuppressWarnings("unchecked")
  public <R> R computeIfAbsent(String queryKey, Supplier<R> loader, Duration ttl) {
    String fullKey = entityType + ":" + queryKey;

    // Check cache first
    QueryCacheEntry<Object> entry = cache.get(fullKey);
    if (entry != null && !entry.isExpired()) {
      hitCount++;
      return (R) entry.getResult();
    }

    // Cache miss - compute result
    missCount++;
    R result = loader.get();

    // Store in cache
    QueryCacheEntry<Object> newEntry = new QueryCacheEntry<>(result, fullKey, ttl);
    cache.put(fullKey, newEntry);

    return result;
  }

  /**
   * Gets a cached result without computing if absent.
   *
   * @param queryKey the query key
   * @param <R> the result type
   * @return an Optional containing the cached result, or empty if not found/expired
   */
  @SuppressWarnings("unchecked")
  public <R> Optional<R> get(String queryKey) {
    String fullKey = entityType + ":" + queryKey;
    QueryCacheEntry<Object> entry = cache.get(fullKey);

    if (entry == null || entry.isExpired()) {
      if (entry != null) {
        cache.remove(fullKey);
        evictionCount++;
      }
      missCount++;
      return Optional.empty();
    }

    hitCount++;
    return Optional.of((R) entry.getResult());
  }

  /**
   * Puts a result in the cache.
   *
   * @param queryKey the query key
   * @param result the result to cache
   * @param <R> the result type
   */
  public <R> void put(String queryKey, R result) {
    put(queryKey, result, defaultTtl);
  }

  /**
   * Puts a result in the cache with custom TTL.
   *
   * @param queryKey the query key
   * @param result the result to cache
   * @param ttl the TTL
   * @param <R> the result type
   */
  public <R> void put(String queryKey, R result, Duration ttl) {
    String fullKey = entityType + ":" + queryKey;
    QueryCacheEntry<Object> entry = new QueryCacheEntry<>(result, fullKey, ttl);
    cache.put(fullKey, entry);
  }

  /**
   * Invalidates a specific query.
   *
   * @param queryKey the query key to invalidate
   */
  public void invalidate(String queryKey) {
    String fullKey = entityType + ":" + queryKey;
    if (cache.remove(fullKey) != null) {
      evictionCount++;
    }
  }

  /**
   * Invalidates all cached queries for this entity type.
   *
   * <p>Called automatically when any entity of this type is created, updated, or deleted.
   */
  public void invalidateAll() {
    long size = cache.size();
    cache.clear();
    evictionCount += size;
  }

  /**
   * Gets the entity type this cache is for.
   *
   * @return the entity type name
   */
  public String getEntityType() {
    return entityType;
  }

  /**
   * Gets cache statistics.
   *
   * @return the cache statistics
   */
  public QueryCacheStats getStats() {
    return new QueryCacheStats(hitCount, missCount, evictionCount, cache.size());
  }

  /**
   * Clears statistics counters.
   */
  public void resetStats() {
    hitCount = 0;
    missCount = 0;
    evictionCount = 0;
  }

  /**
   * Query cache statistics.
   */
  public static class QueryCacheStats {
    private final long hitCount;
    private final long missCount;
    private final long evictionCount;
    private final long size;

    public QueryCacheStats(long hitCount, long missCount, long evictionCount, long size) {
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
      long total = hitCount + missCount;
      return total == 0 ? 0.0 : (double) hitCount / total;
    }

    @Override
    public String toString() {
      return String.format("QueryCacheStats{hits=%d, misses=%d, hitRate=%.2f%%, evictions=%d, size=%d}",
          hitCount, missCount, getHitRate() * 100, evictionCount, size);
    }
  }
}
