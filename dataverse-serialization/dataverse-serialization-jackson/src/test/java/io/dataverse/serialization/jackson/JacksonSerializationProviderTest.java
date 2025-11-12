package io.dataverse.serialization.jackson;

import io.dataverse.api.Entity;
import io.dataverse.spi.SerializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JacksonSerializationProvider.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
class JacksonSerializationProviderTest {

  private JacksonSerializationProvider provider;

  static class TestUser implements Entity<String> {
    private String id;
    private String name;
    private String email;
    private Integer age;
    private LocalDateTime createdAt;

    public TestUser() {}

    public TestUser(String id, String name, String email, Integer age) {
      this.id = id;
      this.name = name;
      this.email = email;
      this.age = age;
      this.createdAt = LocalDateTime.now();
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public void setId(String id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public Integer getAge() {
      return age;
    }

    public void setAge(Integer age) {
      this.age = age;
    }

    public LocalDateTime getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
    }
  }

  @BeforeEach
  void setUp() {
    provider = new JacksonSerializationProvider();
  }

  @Test
  @DisplayName("Should return correct provider ID")
  void shouldReturnCorrectProviderId() {
    assertThat(provider.getProviderId()).isEqualTo("jackson-json");
  }

  @Test
  @DisplayName("Should serialize entity to JSON string")
  void shouldSerializeEntityToJsonString() {
    TestUser user = new TestUser("123", "John Doe", "john@example.com", 30);

    String json = provider.serialize(user);

    assertThat(json).isNotNull();
    assertThat(json).contains("\"id\":\"123\"");
    assertThat(json).contains("\"name\":\"John Doe\"");
    assertThat(json).contains("\"email\":\"john@example.com\"");
    assertThat(json).contains("\"age\":30");
  }

  @Test
  @DisplayName("Should deserialize JSON string to entity")
  void shouldDeserializeJsonStringToEntity() {
    String json = "{\"id\":\"123\",\"name\":\"John Doe\",\"email\":\"john@example.com\",\"age\":30}";

    TestUser user = provider.deserialize(json, TestUser.class);

    assertThat(user).isNotNull();
    assertThat(user.getId()).isEqualTo("123");
    assertThat(user.getName()).isEqualTo("John Doe");
    assertThat(user.getEmail()).isEqualTo("john@example.com");
    assertThat(user.getAge()).isEqualTo(30);
  }

  @Test
  @DisplayName("Should serialize entity to bytes")
  void shouldSerializeEntityToBytes() {
    TestUser user = new TestUser("123", "John Doe", "john@example.com", 30);

    byte[] bytes = provider.serializeToBytes(user);

    assertThat(bytes).isNotNull();
    assertThat(bytes).isNotEmpty();
  }

  @Test
  @DisplayName("Should deserialize bytes to entity")
  void shouldDeserializeBytesToEntity() {
    TestUser original = new TestUser("123", "John Doe", "john@example.com", 30);
    byte[] bytes = provider.serializeToBytes(original);

    TestUser user = provider.deserializeFromBytes(bytes, TestUser.class);

    assertThat(user).isNotNull();
    assertThat(user.getId()).isEqualTo("123");
    assertThat(user.getName()).isEqualTo("John Doe");
    assertThat(user.getEmail()).isEqualTo("john@example.com");
    assertThat(user.getAge()).isEqualTo(30);
  }

  @Test
  @DisplayName("Should convert entity to map")
  void shouldConvertEntityToMap() {
    TestUser user = new TestUser("123", "John Doe", "john@example.com", 30);

    Map<String, Object> map = provider.toMap(user);

    assertThat(map).isNotNull();
    assertThat(map).containsEntry("id", "123");
    assertThat(map).containsEntry("name", "John Doe");
    assertThat(map).containsEntry("email", "john@example.com");
    assertThat(map).containsEntry("age", 30);
    assertThat(map).containsKey("createdAt");
  }

  @Test
  @DisplayName("Should convert map to entity")
  void shouldConvertMapToEntity() {
    Map<String, Object> map = Map.of(
        "id", "123",
        "name", "John Doe",
        "email", "john@example.com",
        "age", 30
    );

    TestUser user = provider.fromMap(map, TestUser.class);

    assertThat(user).isNotNull();
    assertThat(user.getId()).isEqualTo("123");
    assertThat(user.getName()).isEqualTo("John Doe");
    assertThat(user.getEmail()).isEqualTo("john@example.com");
    assertThat(user.getAge()).isEqualTo(30);
  }

  @Test
  @DisplayName("Should clone entity")
  void shouldCloneEntity() {
    TestUser original = new TestUser("123", "John Doe", "john@example.com", 30);

    TestUser clone = provider.clone(original);

    assertThat(clone).isNotNull();
    assertThat(clone).isNotSameAs(original);
    assertThat(clone.getId()).isEqualTo(original.getId());
    assertThat(clone.getName()).isEqualTo(original.getName());
    assertThat(clone.getEmail()).isEqualTo(original.getEmail());
    assertThat(clone.getAge()).isEqualTo(original.getAge());
  }

  @Test
  @DisplayName("Should handle Java 8 date/time types")
  void shouldHandleJava8DateTimeTypes() {
    TestUser user = new TestUser("123", "John Doe", "john@example.com", 30);
    LocalDateTime now = LocalDateTime.now();
    user.setCreatedAt(now);

    String json = provider.serialize(user);
    TestUser deserialized = provider.deserialize(json, TestUser.class);

    assertThat(deserialized.getCreatedAt()).isNotNull();
    // Verify it's in ISO format
    assertThat(json).contains("createdAt");
  }

  @Test
  @DisplayName("Should throw exception when serializing null entity")
  void shouldThrowExceptionWhenSerializingNullEntity() {
    assertThatThrownBy(() -> provider.serialize(null))
        .isInstanceOf(SerializationException.class)
        .hasMessageContaining("Cannot serialize null entity");
  }

  @Test
  @DisplayName("Should throw exception when deserializing null string")
  void shouldThrowExceptionWhenDeserializingNullString() {
    assertThatThrownBy(() -> provider.deserialize(null, TestUser.class))
        .isInstanceOf(SerializationException.class)
        .hasMessageContaining("Cannot deserialize null or empty string");
  }

  @Test
  @DisplayName("Should throw exception when deserializing empty string")
  void shouldThrowExceptionWhenDeserializingEmptyString() {
    assertThatThrownBy(() -> provider.deserialize("", TestUser.class))
        .isInstanceOf(SerializationException.class)
        .hasMessageContaining("Cannot deserialize null or empty string");
  }

  @Test
  @DisplayName("Should throw exception when deserializing with null class")
  void shouldThrowExceptionWhenDeserializingWithNullClass() {
    assertThatThrownBy(() -> provider.deserialize("{}", null))
        .isInstanceOf(SerializationException.class)
        .hasMessageContaining("Entity class must not be null");
  }

  @Test
  @DisplayName("Should throw exception when converting null entity to map")
  void shouldThrowExceptionWhenConvertingNullEntityToMap() {
    assertThatThrownBy(() -> provider.toMap(null))
        .isInstanceOf(SerializationException.class)
        .hasMessageContaining("Cannot convert null entity to map");
  }

  @Test
  @DisplayName("Should throw exception when converting null map to entity")
  void shouldThrowExceptionWhenConvertingNullMapToEntity() {
    assertThatThrownBy(() -> provider.fromMap(null, TestUser.class))
        .isInstanceOf(SerializationException.class)
        .hasMessageContaining("Cannot convert null map to entity");
  }

  @Test
  @DisplayName("Should throw exception when cloning null entity")
  void shouldThrowExceptionWhenCloningNullEntity() {
    assertThatThrownBy(() -> provider.clone(null))
        .isInstanceOf(SerializationException.class)
        .hasMessageContaining("Cannot clone null entity");
  }

  @Test
  @DisplayName("Should create provider with builder")
  void shouldCreateProviderWithBuilder() {
    JacksonSerializationProvider customProvider = JacksonSerializationProvider.builder()
        .prettyPrint()
        .build();

    assertThat(customProvider).isNotNull();
    assertThat(customProvider.getProviderId()).isEqualTo("jackson-json");

    TestUser user = new TestUser("123", "John Doe", "john@example.com", 30);
    String json = customProvider.serialize(user);

    // Pretty printed JSON should contain newlines
    assertThat(json).contains("\n");
  }

  @Test
  @DisplayName("Should handle round-trip serialization")
  void shouldHandleRoundTripSerialization() {
    TestUser original = new TestUser("123", "John Doe", "john@example.com", 30);
    original.setCreatedAt(LocalDateTime.now());

    // JSON round-trip
    String json = provider.serialize(original);
    TestUser fromJson = provider.deserialize(json, TestUser.class);

    // Bytes round-trip
    byte[] bytes = provider.serializeToBytes(original);
    TestUser fromBytes = provider.deserializeFromBytes(bytes, TestUser.class);

    // Map round-trip
    Map<String, Object> map = provider.toMap(original);
    TestUser fromMap = provider.fromMap(map, TestUser.class);

    // All should have same values
    assertThat(fromJson.getId()).isEqualTo(original.getId());
    assertThat(fromBytes.getId()).isEqualTo(original.getId());
    assertThat(fromMap.getId()).isEqualTo(original.getId());

    assertThat(fromJson.getName()).isEqualTo(original.getName());
    assertThat(fromBytes.getName()).isEqualTo(original.getName());
    assertThat(fromMap.getName()).isEqualTo(original.getName());
  }
}
