package io.dataverse.core.cache;

import java.io.Serializable;

/**
 * Generates cache keys for entities.
 *
 * <p>Default implementation creates keys in the format: {@code entityType:id}
 * <p>Example: {@code Product:12345}
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class CacheKeyGenerator {

  private final String prefix;

  /**
   * Creates a cache key generator with a prefix.
   *
   * @param prefix the key prefix (typically entity type name)
   */
  public CacheKeyGenerator(String prefix) {
    this.prefix = prefix;
  }

  /**
   * Generates a cache key for an entity ID.
   *
   * @param id the entity ID
   * @return the cache key
   */
  public String generateKey(Serializable id) {
    return prefix + ":" + id;
  }

  /**
   * Generates a cache key with a custom suffix.
   *
   * @param suffix the key suffix
   * @return the cache key
   */
  public String generateKey(String suffix) {
    return prefix + ":" + suffix;
  }

  /**
   * Generates a cache key for a query.
   *
   * @param queryName the query name
   * @param params the query parameters
   * @return the cache key
   */
  public String generateQueryKey(String queryName, Object... params) {
    StringBuilder key = new StringBuilder(prefix)
        .append(":query:")
        .append(queryName);

    if (params != null && params.length > 0) {
      key.append(":");
      for (int i = 0; i < params.length; i++) {
        if (i > 0) key.append(",");
        key.append(params[i]);
      }
    }

    return key.toString();
  }

  /**
   * Gets the prefix for this generator.
   *
   * @return the prefix
   */
  public String getPrefix() {
    return prefix;
  }
}
