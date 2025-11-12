package io.dataverse.spring.boot.autoconfigure;

import io.dataverse.adapter.postgresql.PostgreSQLAdapter;
import io.dataverse.spi.AdapterConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for PostgreSQL adapter.
 *
 * <p>Automatically configures a PostgreSQLAdapter bean when:
 * <ul>
 *   <li>PostgreSQLAdapter class is on the classpath</li>
 *   <li>dataverse.adapters.postgresql.enabled is true (default)</li>
 *   <li>No PostgreSQLAdapter bean is already defined</li>
 * </ul>
 *
 * <p>Configuration example:
 * <pre>
 * dataverse:
 *   adapters:
 *     postgresql:
 *       enabled: true
 *       properties:
 *         jdbcUrl: jdbc:postgresql://localhost:5432/mydb
 *         username: postgres
 *         password: ${DB_PASSWORD}
 *         maxPoolSize: 20  # Optional
 *         minPoolSize: 2   # Optional
 *         connectionTimeout: 30000  # Optional
 * </pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
@AutoConfiguration
@ConditionalOnClass(PostgreSQLAdapter.class)
@ConditionalOnProperty(prefix = "dataverse.adapters.postgresql", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DataVerseProperties.class)
public class PostgreSQLAutoConfiguration {

  private final DataVerseProperties properties;

  public PostgreSQLAutoConfiguration(DataVerseProperties properties) {
    this.properties = properties;
  }

  @Bean
  @ConditionalOnMissingBean
  public PostgreSQLAdapter postgreSQLAdapter() {
    PostgreSQLAdapter adapter = new PostgreSQLAdapter();

    // Get adapter-specific properties
    DataVerseProperties.AdapterProperties adapterProps =
        properties.getAdapters().getOrDefault("postgresql", new DataVerseProperties.AdapterProperties());

    // Build configuration
    AdapterConfig.Builder configBuilder = AdapterConfig.builder();
    adapterProps.getProperties().forEach(configBuilder::property);

    // Initialize adapter
    adapter.initialize(configBuilder.build());

    return adapter;
  }
}
