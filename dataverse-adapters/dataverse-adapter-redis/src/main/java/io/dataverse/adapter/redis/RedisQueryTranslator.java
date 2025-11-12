package io.dataverse.adapter.redis;

import io.dataverse.spi.QueryTranslationException;
import io.dataverse.spi.QueryTranslator;

/**
 * Query translator for Redis.
 *
 * <p>Redis is a key-value store, so complex queries are not directly supported.
 * This translator handles simple key-based operations.
 *
 * @since 1.0.0
 */
public class RedisQueryTranslator implements QueryTranslator {

  @Override
  public NativeQuery translate(Query query) {
    // Redis is primarily key-value, complex queries not directly supported
    // For now, queries will use SCAN with pattern matching
    throw new QueryTranslationException(
        "Complex queries not supported for Redis key-value store. " +
        "Use findById() for direct key lookups or implement custom queries.");
  }

  @Override
  public boolean supports(Query query) {
    // Redis supports very limited querying
    return false;
  }

  @Override
  public String getQueryLanguage() {
    return "Redis Commands";
  }
}
