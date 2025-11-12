package io.dataverse.spring.boot.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for DataVerse SDK Spring Boot integration.
 *
 * <p>Example configuration in application.yml:
 * <pre>
 * dataverse:
 *   adapters:
 *     dynamodb:
 *       enabled: true
 *       properties:
 *         region: us-east-1
 *         endpoint: http://localhost:8000
 *     mongodb:
 *       enabled: true
 *       properties:
 *         connectionString: mongodb://localhost:27017
 *         database: mydb
 *     redis:
 *       enabled: true
 *       properties:
 *         host: localhost
 *         port: 6379
 * </pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
@ConfigurationProperties(prefix = "dataverse")
public class DataVerseProperties {

  /**
   * Adapter configurations keyed by adapter ID.
   */
  private Map<String, AdapterProperties> adapters = new HashMap<>();

  public Map<String, AdapterProperties> getAdapters() {
    return adapters;
  }

  public void setAdapters(Map<String, AdapterProperties> adapters) {
    this.adapters = adapters;
  }

  /**
   * Configuration for a single adapter.
   */
  public static class AdapterProperties {
    /**
     * Whether this adapter is enabled.
     */
    private boolean enabled = true;

    /**
     * Adapter-specific configuration properties.
     */
    private Map<String, String> properties = new HashMap<>();

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public Map<String, String> getProperties() {
      return properties;
    }

    public void setProperties(Map<String, String> properties) {
      this.properties = properties;
    }
  }
}
