package io.dataverse.spi;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration for data source adapters.
 *
 * <p>AdapterConfig provides a flexible key-value based configuration mechanism
 * for adapter initialization. Each adapter defines its own configuration keys.
 *
 * <p><strong>Example Usage:</strong>
 * <pre>{@code
 * AdapterConfig config = AdapterConfig.builder()
 *     .property("endpoint", "https://dynamodb.us-east-1.amazonaws.com")
 *     .property("region", "us-east-1")
 *     .property("pool.minSize", 10)
 *     .property("pool.maxSize", 100)
 *     .property("connection.timeout", Duration.ofSeconds(30))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public final class AdapterConfig {

  private final Map<String, Object> properties;

  private AdapterConfig(Map<String, Object> properties) {
    this.properties = Map.copyOf(properties); // Immutable
  }

  /**
   * Gets a configuration property as a String.
   *
   * @param key the property key, must not be {@code null}
   * @return an Optional containing the value if present
   * @throws IllegalArgumentException if key is {@code null}
   */
  public Optional<String> getString(String key) {
    if (key == null) {
      throw new IllegalArgumentException("Key must not be null");
    }
    return Optional.ofNullable(properties.get(key)).map(Object::toString);
  }

  /**
   * Gets a configuration property as a String with a default value.
   *
   * @param key the property key, must not be {@code null}
   * @param defaultValue the default value if key not found
   * @return the property value or default
   * @throws IllegalArgumentException if key is {@code null}
   */
  public String getString(String key, String defaultValue) {
    return getString(key).orElse(defaultValue);
  }

  /**
   * Gets a required configuration property as a String.
   *
   * @param key the property key, must not be {@code null}
   * @return the property value, never {@code null}
   * @throws IllegalArgumentException if key is {@code null}
   * @throws IllegalStateException if key is not found
   */
  public String getRequiredString(String key) {
    return getString(key)
        .orElseThrow(() -> new IllegalStateException("Required property not found: " + key));
  }

  /**
   * Gets a configuration property as an Integer.
   *
   * @param key the property key, must not be {@code null}
   * @return an Optional containing the value if present
   * @throws IllegalArgumentException if key is {@code null}
   * @throws NumberFormatException if value cannot be parsed as integer
   */
  public Optional<Integer> getInt(String key) {
    return getString(key).map(Integer::parseInt);
  }

  /**
   * Gets a configuration property as an Integer with a default value.
   *
   * @param key the property key, must not be {@code null}
   * @param defaultValue the default value if key not found
   * @return the property value or default
   */
  public int getInt(String key, int defaultValue) {
    return getInt(key).orElse(defaultValue);
  }

  /**
   * Gets a configuration property as a Long.
   *
   * @param key the property key, must not be {@code null}
   * @return an Optional containing the value if present
   */
  public Optional<Long> getLong(String key) {
    return getString(key).map(Long::parseLong);
  }

  /**
   * Gets a configuration property as a Long with a default value.
   *
   * @param key the property key, must not be {@code null}
   * @param defaultValue the default value if key not found
   * @return the property value or default
   */
  public long getLong(String key, long defaultValue) {
    return getLong(key).orElse(defaultValue);
  }

  /**
   * Gets a configuration property as a Boolean.
   *
   * @param key the property key, must not be {@code null}
   * @return an Optional containing the value if present
   */
  public Optional<Boolean> getBoolean(String key) {
    return getString(key).map(Boolean::parseBoolean);
  }

  /**
   * Gets a configuration property as a Boolean with a default value.
   *
   * @param key the property key, must not be {@code null}
   * @param defaultValue the default value if key not found
   * @return the property value or default
   */
  public boolean getBoolean(String key, boolean defaultValue) {
    return getBoolean(key).orElse(defaultValue);
  }

  /**
   * Gets a configuration property as a Duration.
   *
   * @param key the property key, must not be {@code null}
   * @return an Optional containing the value if present
   */
  @SuppressWarnings("unchecked")
  public Optional<Duration> getDuration(String key) {
    Object value = properties.get(key);
    if (value instanceof Duration) {
      return Optional.of((Duration) value);
    } else if (value instanceof String) {
      return Optional.of(Duration.parse((String) value));
    }
    return Optional.empty();
  }

  /**
   * Gets a raw property value.
   *
   * @param key the property key, must not be {@code null}
   * @return an Optional containing the raw value if present
   */
  public Optional<Object> get(String key) {
    return Optional.ofNullable(properties.get(key));
  }

  /**
   * Checks if a property exists.
   *
   * @param key the property key, must not be {@code null}
   * @return {@code true} if the property exists, {@code false} otherwise
   */
  public boolean has(String key) {
    return properties.containsKey(key);
  }

  /**
   * Returns all property keys.
   *
   * @return all property keys, never {@code null}
   */
  public Iterable<String> keys() {
    return properties.keySet();
  }

  /**
   * Creates a new builder.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for AdapterConfig.
   */
  public static final class Builder {
    private final Map<String, Object> properties = new HashMap<>();

    private Builder() {}

    /**
     * Sets a configuration property.
     *
     * @param key the property key, must not be {@code null}
     * @param value the property value, must not be {@code null}
     * @return this builder
     */
    public Builder property(String key, Object value) {
      if (key == null) {
        throw new IllegalArgumentException("Key must not be null");
      }
      if (value == null) {
        throw new IllegalArgumentException("Value must not be null");
      }
      properties.put(key, value);
      return this;
    }

    /**
     * Sets multiple configuration properties.
     *
     * @param properties the properties to set, must not be {@code null}
     * @return this builder
     */
    public Builder properties(Map<String, Object> properties) {
      if (properties == null) {
        throw new IllegalArgumentException("Properties must not be null");
      }
      this.properties.putAll(properties);
      return this;
    }

    /**
     * Builds the configuration.
     *
     * @return a new AdapterConfig instance
     */
    public AdapterConfig build() {
      return new AdapterConfig(properties);
    }
  }
}
