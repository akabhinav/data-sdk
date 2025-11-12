package io.dataverse.spring.boot.autoconfigure;

import io.dataverse.spi.DataSourceAdapter;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Auto-configuration for DataVerse health indicators.
 *
 * <p>Provides health check endpoints for all configured adapters via Spring Boot Actuator.
 *
 * <p>Access health endpoint at: {@code /actuator/health/dataverse}
 *
 * <p>Example response:
 * <pre>
 * {
 *   "status": "UP",
 *   "details": {
 *     "dynamodb": {
 *       "status": "UP",
 *       "details": {
 *         "adapterId": "dynamodb",
 *         "healthy": true
 *       }
 *     },
 *     "mongodb": {
 *       "status": "UP"
 *     },
 *     "redis": {
 *       "status": "UP"
 *     }
 *   }
 * }
 * </pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
@AutoConfiguration
@ConditionalOnClass(HealthIndicator.class)
@ConditionalOnEnabledHealthIndicator("dataverse")
public class DataVerseHealthIndicatorAutoConfiguration {

  /**
   * Health indicator for all DataVerse adapters.
   */
  @Bean
  public HealthIndicator dataverseHealthIndicator(List<DataSourceAdapter> adapters) {
    return () -> {
      Health.Builder builder = Health.up();

      for (DataSourceAdapter adapter : adapters) {
        String adapterId = adapter.getAdapterId();
        boolean healthy = adapter.isHealthy();

        if (healthy) {
          builder.withDetail(adapterId, Health.up()
              .withDetail("adapterId", adapterId)
              .withDetail("healthy", true)
              .build());
        } else {
          builder.down();
          builder.withDetail(adapterId, Health.down()
              .withDetail("adapterId", adapterId)
              .withDetail("healthy", false)
              .build());
        }
      }

      return builder.build();
    };
  }
}
