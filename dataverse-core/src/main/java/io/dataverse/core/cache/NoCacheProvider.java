package io.dataverse.core.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * No-op cache provider implementation.
 *
 * <p>This provider is used when caching is disabled. All operations are no-ops,
 * and computeIfAbsent always calls the value loader without caching the result.
 *
 * @param <K> the cache key type
 * @param <V> the cached value type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class NoCacheProvider<K, V> implements CacheProvider<K, V> {

  @Override
  public Optional<V> get(K key) {
    return Optional.empty();
  }

  @Override
  public void put(K key, V value) {
    // No-op
  }

  @Override
  public void put(K key, V value, Duration ttl) {
    // No-op
  }

  @Override
  public V computeIfAbsent(K key, Supplier<V> valueLoader) {
    return valueLoader.get();
  }

  @Override
  public V computeIfAbsent(K key, Supplier<V> valueLoader, Duration ttl) {
    return valueLoader.get();
  }

  @Override
  public void evict(K key) {
    // No-op
  }

  @Override
  public void clear() {
    // No-op
  }

  @Override
  public boolean containsKey(K key) {
    return false;
  }

  @Override
  public String getName() {
    return "NoCache";
  }

  @Override
  public CacheLevel getLevel() {
    return CacheLevel.L1;
  }

  @Override
  public CacheStats getStats() {
    return new CacheStats(0, 0, 0, 0);
  }

  @Override
  public boolean isHealthy() {
    return true;
  }

  @Override
  public void shutdown() {
    // No-op
  }
}
