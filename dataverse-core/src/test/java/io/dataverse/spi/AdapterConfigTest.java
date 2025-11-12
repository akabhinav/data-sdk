package io.dataverse.spi;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for AdapterConfig.
 *
 * @since 1.0.0
 */
@DisplayName("AdapterConfig Tests")
class AdapterConfigTest {

  @Test
  @DisplayName("Should build config with properties")
  void shouldBuildConfigWithProperties() {
    // When
    AdapterConfig config = AdapterConfig.builder()
        .property("endpoint", "http://localhost:8000")
        .property("region", "us-east-1")
        .property("timeout", 30)
        .build();

    // Then
    assertThat(config.getString("endpoint")).contains("http://localhost:8000");
    assertThat(config.getString("region")).contains("us-east-1");
    assertThat(config.getInt("timeout")).contains(30);
  }

  @Test
  @DisplayName("Should get string property with default")
  void shouldGetStringPropertyWithDefault() {
    // Given
    AdapterConfig config = AdapterConfig.builder()
        .property("endpoint", "http://localhost:8000")
        .build();

    // When & Then
    assertThat(config.getString("endpoint", "default")).isEqualTo("http://localhost:8000");
    assertThat(config.getString("missing", "default")).isEqualTo("default");
  }

  @Test
  @DisplayName("Should get required string property")
  void shouldGetRequiredStringProperty() {
    // Given
    AdapterConfig config = AdapterConfig.builder()
        .property("endpoint", "http://localhost:8000")
        .build();

    // When & Then
    assertThat(config.getRequiredString("endpoint")).isEqualTo("http://localhost:8000");
  }

  @Test
  @DisplayName("Should throw exception for missing required property")
  void shouldThrowExceptionForMissingRequiredProperty() {
    // Given
    AdapterConfig config = AdapterConfig.builder().build();

    // When & Then
    assertThatThrownBy(() -> config.getRequiredString("missing"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Required property not found: missing");
  }

  @Test
  @DisplayName("Should get integer property")
  void shouldGetIntegerProperty() {
    // Given
    AdapterConfig config = AdapterConfig.builder()
        .property("maxConnections", 100)
        .property("timeout", "30")
        .build();

    // When & Then
    assertThat(config.getInt("maxConnections")).contains(100);
    assertThat(config.getInt("timeout")).contains(30); // Parse from string
    assertThat(config.getInt("maxConnections", 50)).isEqualTo(100);
    assertThat(config.getInt("missing", 50)).isEqualTo(50);
  }

  @Test
  @DisplayName("Should get long property")
  void shouldGetLongProperty() {
    // Given
    AdapterConfig config = AdapterConfig.builder()
        .property("maxItems", 1000000L)
        .build();

    // When & Then
    assertThat(config.getLong("maxItems")).contains(1000000L);
    assertThat(config.getLong("maxItems", 500L)).isEqualTo(1000000L);
    assertThat(config.getLong("missing", 500L)).isEqualTo(500L);
  }

  @Test
  @DisplayName("Should get boolean property")
  void shouldGetBooleanProperty() {
    // Given
    AdapterConfig config = AdapterConfig.builder()
        .property("ssl.enabled", true)
        .property("verbose", "false")
        .build();

    // When & Then
    assertThat(config.getBoolean("ssl.enabled")).contains(true);
    assertThat(config.getBoolean("verbose")).contains(false);
    assertThat(config.getBoolean("ssl.enabled", false)).isTrue();
    assertThat(config.getBoolean("missing", true)).isTrue();
  }

  @Test
  @DisplayName("Should get duration property")
  void shouldGetDurationProperty() {
    // Given
    AdapterConfig config = AdapterConfig.builder()
        .property("timeout", Duration.ofSeconds(30))
        .property("retryDelay", "PT5S") // ISO-8601 duration string
        .build();

    // When & Then
    assertThat(config.getDuration("timeout")).contains(Duration.ofSeconds(30));
    assertThat(config.getDuration("retryDelay")).contains(Duration.ofSeconds(5));
  }

  @Test
  @DisplayName("Should check if property exists")
  void shouldCheckIfPropertyExists() {
    // Given
    AdapterConfig config = AdapterConfig.builder()
        .property("endpoint", "http://localhost:8000")
        .build();

    // When & Then
    assertThat(config.has("endpoint")).isTrue();
    assertThat(config.has("missing")).isFalse();
  }

  @Test
  @DisplayName("Should get all property keys")
  void shouldGetAllPropertyKeys() {
    // Given
    AdapterConfig config = AdapterConfig.builder()
        .property("endpoint", "http://localhost:8000")
        .property("region", "us-east-1")
        .property("timeout", 30)
        .build();

    // When
    var keys = config.keys();

    // Then
    assertThat(keys).containsExactlyInAnyOrder("endpoint", "region", "timeout");
  }

  @Test
  @DisplayName("Should be immutable after build")
  void shouldBeImmutableAfterBuild() {
    // Given
    var builder = AdapterConfig.builder()
        .property("endpoint", "http://localhost:8000");

    AdapterConfig config = builder.build();

    // When - try to modify via builder (shouldn't affect built config)
    builder.property("endpoint", "http://localhost:9000");

    // Then - original config unchanged
    assertThat(config.getString("endpoint")).contains("http://localhost:8000");
  }

  @Test
  @DisplayName("Should throw exception for null key")
  void shouldThrowExceptionForNullKey() {
    // Given
    var builder = AdapterConfig.builder();

    // When & Then
    assertThatThrownBy(() -> builder.property(null, "value"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Key must not be null");
  }

  @Test
  @DisplayName("Should throw exception for null value")
  void shouldThrowExceptionForNullValue() {
    // Given
    var builder = AdapterConfig.builder();

    // When & Then
    assertThatThrownBy(() -> builder.property("key", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Value must not be null");
  }

  @Test
  @DisplayName("Should support bulk property setting")
  void shouldSupportBulkPropertySetting() {
    // Given
    var properties = java.util.Map.of(
        "endpoint", "http://localhost:8000",
        "region", "us-east-1",
        "timeout", 30
    );

    // When
    AdapterConfig config = AdapterConfig.builder()
        .properties(properties)
        .build();

    // Then
    assertThat(config.getString("endpoint")).contains("http://localhost:8000");
    assertThat(config.getString("region")).contains("us-east-1");
    assertThat(config.getInt("timeout")).contains(30);
  }
}
