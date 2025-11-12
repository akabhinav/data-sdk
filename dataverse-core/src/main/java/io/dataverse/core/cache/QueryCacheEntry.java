package io.dataverse.core.cache;

import java.time.Duration;
import java.util.Objects;

/**
 * Represents a cached query result with metadata.
 *
 * <p>Query result caching stores the results of entire queries, not just individual entities.
 * This is beneficial for:
 * <ul>
 *   <li>Repeated complex queries with the same parameters</li>
 *   <li>Aggregate queries (count, sum, etc.)</li>
 *   <li>List/pagination queries</li>
 *   <li>Read-heavy workloads with infrequent updates</li>
 * </ul>
 *
 * <p><strong>Automatic Invalidation:</strong>
 * Query cache is automatically invalidated when:
 * <ul>
 *   <li>Any entity of the queried type is created</li>
 *   <li>Any entity of the queried type is updated</li>
 *   <li>Any entity of the queried type is deleted</li>
 *   <li>TTL expires</li>
 * </ul>
 *
 * @param <T> the result type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class QueryCacheEntry<T> {

  private final T result;
  private final String queryKey;
  private final long cacheTime;
  private final Duration ttl;

  public QueryCacheEntry(T result, String queryKey, Duration ttl) {
    this.result = result;
    this.queryKey = queryKey;
    this.cacheTime = System.currentTimeMillis();
    this.ttl = ttl;
  }

  public T getResult() {
    return result;
  }

  public String getQueryKey() {
    return queryKey;
  }

  public long getCacheTime() {
    return cacheTime;
  }

  public Duration getTtl() {
    return ttl;
  }

  public boolean isExpired() {
    if (ttl == null) {
      return false;
    }
    long age = System.currentTimeMillis() - cacheTime;
    return age > ttl.toMillis();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    QueryCacheEntry<?> that = (QueryCacheEntry<?>) o;
    return Objects.equals(queryKey, that.queryKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(queryKey);
  }
}
