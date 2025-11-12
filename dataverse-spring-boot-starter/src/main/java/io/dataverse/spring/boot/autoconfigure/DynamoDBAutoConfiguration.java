package io.dataverse.spring.boot.autoconfigure;

import io.dataverse.adapter.dynamodb.DynamoDBAdapter;
import io.dataverse.spi.AdapterConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for DynamoDB adapter.
 *
 * <p>Automatically configures a DynamoDBAdapter bean when:
 * <ul>
 *   <li>DynamoDBAdapter class is on the classpath</li>
 *   <li>dataverse.adapters.dynamodb.enabled is true (default)</li>
 *   <li>No DynamoDBAdapter bean is already defined</li>
 * </ul>
 *
 * <p>Configuration example:
 * <pre>
 * dataverse:
 *   adapters:
 *     dynamodb:
 *       enabled: true
 *       properties:
 *         region: us-east-1
 *         endpoint: http://localhost:8000  # Optional - for DynamoDB Local
 *         accessKeyId: ${AWS_ACCESS_KEY_ID}  # Optional
 *         secretAccessKey: ${AWS_SECRET_ACCESS_KEY}  # Optional
 * </pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
@AutoConfiguration
@ConditionalOnClass(DynamoDBAdapter.class)
@ConditionalOnProperty(prefix = "dataverse.adapters.dynamodb", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DataVerseProperties.class)
public class DynamoDBAutoConfiguration {

  private final DataVerseProperties properties;

  public DynamoDBAutoConfiguration(DataVerseProperties properties) {
    this.properties = properties;
  }

  @Bean
  @ConditionalOnMissingBean
  public DynamoDBAdapter dynamoDBAdapter() {
    DynamoDBAdapter adapter = new DynamoDBAdapter();

    // Get adapter-specific properties
    DataVerseProperties.AdapterProperties adapterProps =
        properties.getAdapters().getOrDefault("dynamodb", new DataVerseProperties.AdapterProperties());

    // Build configuration
    AdapterConfig.Builder configBuilder = AdapterConfig.builder();
    adapterProps.getProperties().forEach(configBuilder::property);

    // Initialize adapter
    adapter.initialize(configBuilder.build());

    return adapter;
  }
}
