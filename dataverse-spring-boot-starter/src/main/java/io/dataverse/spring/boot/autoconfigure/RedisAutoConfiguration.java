package io.dataverse.spring.boot.autoconfigure;

import io.dataverse.adapter.redis.RedisAdapter;
import io.dataverse.spi.AdapterConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Redis adapter.
 *
 * <p>Automatically configures a RedisAdapter bean when:
 * <ul>
 *   <li>RedisAdapter class is on the classpath</li>
 *   <li>dataverse.adapters.redis.enabled is true (default)</li>
 *   <li>No RedisAdapter bean is already defined</li>
 * </ul>
 *
 * <p>Configuration example:
 * <pre>
 * dataverse:
 *   adapters:
 *     redis:
 *       enabled: true
 *       properties:
 *         host: localhost
 *         port: 6379
 *         maxConnections: 20  # Optional
 *         password: ${REDIS_PASSWORD}  # Optional
 * </pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
@AutoConfiguration
@ConditionalOnClass(RedisAdapter.class)
@ConditionalOnProperty(prefix = "dataverse.adapters.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DataVerseProperties.class)
public class RedisAutoConfiguration {

  private final DataVerseProperties properties;

  public RedisAutoConfiguration(DataVerseProperties properties) {
    this.properties = properties;
  }

  @Bean
  @ConditionalOnMissingBean
  public RedisAdapter redisAdapter() {
    RedisAdapter adapter = new RedisAdapter();

    // Get adapter-specific properties
    DataVerseProperties.AdapterProperties adapterProps =
        properties.getAdapters().getOrDefault("redis", new DataVerseProperties.AdapterProperties());

    // Build configuration
    AdapterConfig.Builder configBuilder = AdapterConfig.builder();
    adapterProps.getProperties().forEach(configBuilder::property);

    // Initialize adapter
    adapter.initialize(configBuilder.build());

    return adapter;
  }
}
