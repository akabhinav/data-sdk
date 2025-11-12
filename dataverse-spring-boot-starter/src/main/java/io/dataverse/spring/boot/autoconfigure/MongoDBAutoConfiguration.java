package io.dataverse.spring.boot.autoconfigure;

import io.dataverse.adapter.mongodb.MongoDBAdapter;
import io.dataverse.spi.AdapterConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for MongoDB adapter.
 *
 * <p>Automatically configures a MongoDBAdapter bean when:
 * <ul>
 *   <li>MongoDBAdapter class is on the classpath</li>
 *   <li>dataverse.adapters.mongodb.enabled is true (default)</li>
 *   <li>No MongoDBAdapter bean is already defined</li>
 * </ul>
 *
 * <p>Configuration example:
 * <pre>
 * dataverse:
 *   adapters:
 *     mongodb:
 *       enabled: true
 *       properties:
 *         connectionString: mongodb://localhost:27017
 *         database: myapp
 *         maxPoolSize: 50  # Optional
 *         minPoolSize: 10  # Optional
 * </pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
@AutoConfiguration
@ConditionalOnClass(MongoDBAdapter.class)
@ConditionalOnProperty(prefix = "dataverse.adapters.mongodb", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DataVerseProperties.class)
public class MongoDBAutoConfiguration {

  private final DataVerseProperties properties;

  public MongoDBAutoConfiguration(DataVerseProperties properties) {
    this.properties = properties;
  }

  @Bean
  @ConditionalOnMissingBean
  public MongoDBAdapter mongoDBAdapter() {
    MongoDBAdapter adapter = new MongoDBAdapter();

    // Get adapter-specific properties
    DataVerseProperties.AdapterProperties adapterProps =
        properties.getAdapters().getOrDefault("mongodb", new DataVerseProperties.AdapterProperties());

    // Build configuration
    AdapterConfig.Builder configBuilder = AdapterConfig.builder();
    adapterProps.getProperties().forEach(configBuilder::property);

    // Initialize adapter
    adapter.initialize(configBuilder.build());

    return adapter;
  }
}
