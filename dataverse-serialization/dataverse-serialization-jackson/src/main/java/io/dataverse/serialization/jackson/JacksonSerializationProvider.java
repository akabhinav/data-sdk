package io.dataverse.serialization.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.dataverse.api.Entity;
import io.dataverse.spi.SerializationException;
import io.dataverse.spi.SerializationProvider;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 * Jackson-based JSON serialization provider.
 *
 * <p>This implementation uses Jackson for high-performance JSON serialization
 * and deserialization. It includes support for:
 * <ul>
 *   <li>Java 8 date/time types (via JavaTimeModule)</li>
 *   <li>Flexible deserialization (ignores unknown properties)</li>
 *   <li>Pretty printing for debugging</li>
 *   <li>Custom configuration via builder</li>
 * </ul>
 *
 * <p><strong>Example Usage:</strong>
 * <pre>{@code
 * SerializationProvider serializer = new JacksonSerializationProvider();
 *
 * // Serialize entity to JSON
 * String json = serializer.serialize(user);
 *
 * // Deserialize JSON to entity
 * User user = serializer.deserialize(json, User.class);
 *
 * // Convert entity to map (useful for DynamoDB, MongoDB)
 * Map<String, Object> map = serializer.toMap(user);
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class JacksonSerializationProvider implements SerializationProvider {

  private final ObjectMapper objectMapper;

  /**
   * Constructs a provider with default ObjectMapper configuration.
   */
  public JacksonSerializationProvider() {
    this(createDefaultObjectMapper());
  }

  /**
   * Constructs a provider with a custom ObjectMapper.
   *
   * @param objectMapper the custom ObjectMapper
   */
  public JacksonSerializationProvider(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Creates a default ObjectMapper with recommended settings.
   *
   * @return a configured ObjectMapper
   */
  private static ObjectMapper createDefaultObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();

    // Register Java 8 date/time module
    mapper.registerModule(new JavaTimeModule());

    // Configure serialization
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    // Configure deserialization
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

    return mapper;
  }

  @Override
  public String getProviderId() {
    return "jackson-json";
  }

  @Override
  public <T extends Entity<ID>, ID extends Serializable> String serialize(T entity)
      throws SerializationException {
    if (entity == null) {
      throw new SerializationException("Cannot serialize null entity");
    }

    try {
      return objectMapper.writeValueAsString(entity);
    } catch (JsonProcessingException e) {
      throw new SerializationException(
          "Failed to serialize entity of type " + entity.getClass().getName(), e);
    }
  }

  @Override
  public <T extends Entity<ID>, ID extends Serializable> byte[] serializeToBytes(T entity)
      throws SerializationException {
    if (entity == null) {
      throw new SerializationException("Cannot serialize null entity");
    }

    try {
      return objectMapper.writeValueAsBytes(entity);
    } catch (JsonProcessingException e) {
      throw new SerializationException(
          "Failed to serialize entity of type " + entity.getClass().getName() + " to bytes", e);
    }
  }

  @Override
  public <T extends Entity<ID>, ID extends Serializable> T deserialize(
      String data, Class<T> entityClass) throws SerializationException {
    if (data == null || data.isEmpty()) {
      throw new SerializationException("Cannot deserialize null or empty string");
    }
    if (entityClass == null) {
      throw new SerializationException("Entity class must not be null");
    }

    try {
      return objectMapper.readValue(data, entityClass);
    } catch (JsonProcessingException e) {
      throw new SerializationException(
          "Failed to deserialize JSON to entity of type " + entityClass.getName(), e);
    }
  }

  @Override
  public <T extends Entity<ID>, ID extends Serializable> T deserializeFromBytes(
      byte[] data, Class<T> entityClass) throws SerializationException {
    if (data == null || data.length == 0) {
      throw new SerializationException("Cannot deserialize null or empty byte array");
    }
    if (entityClass == null) {
      throw new SerializationException("Entity class must not be null");
    }

    try {
      return objectMapper.readValue(data, entityClass);
    } catch (IOException e) {
      throw new SerializationException(
          "Failed to deserialize bytes to entity of type " + entityClass.getName(), e);
    }
  }

  @Override
  public <T extends Entity<ID>, ID extends Serializable> Map<String, Object> toMap(T entity)
      throws SerializationException {
    if (entity == null) {
      throw new SerializationException("Cannot convert null entity to map");
    }

    try {
      return objectMapper.convertValue(entity, new TypeReference<Map<String, Object>>() {});
    } catch (IllegalArgumentException e) {
      throw new SerializationException(
          "Failed to convert entity of type " + entity.getClass().getName() + " to map", e);
    }
  }

  @Override
  public <T extends Entity<ID>, ID extends Serializable> T fromMap(
      Map<String, Object> map, Class<T> entityClass) throws SerializationException {
    if (map == null) {
      throw new SerializationException("Cannot convert null map to entity");
    }
    if (entityClass == null) {
      throw new SerializationException("Entity class must not be null");
    }

    try {
      return objectMapper.convertValue(map, entityClass);
    } catch (IllegalArgumentException e) {
      throw new SerializationException(
          "Failed to convert map to entity of type " + entityClass.getName(), e);
    }
  }

  @Override
  public <T extends Entity<ID>, ID extends Serializable> T clone(T entity)
      throws SerializationException {
    if (entity == null) {
      throw new SerializationException("Cannot clone null entity");
    }

    try {
      @SuppressWarnings("unchecked")
      Class<T> entityClass = (Class<T>) entity.getClass();
      String json = objectMapper.writeValueAsString(entity);
      return objectMapper.readValue(json, entityClass);
    } catch (JsonProcessingException e) {
      throw new SerializationException(
          "Failed to clone entity of type " + entity.getClass().getName(), e);
    }
  }

  /**
   * Returns the underlying ObjectMapper for advanced configuration.
   *
   * @return the ObjectMapper
   */
  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  /**
   * Builder for creating customized JacksonSerializationProvider instances.
   */
  public static final class Builder {
    private ObjectMapper objectMapper = createDefaultObjectMapper();

    /**
     * Uses a custom ObjectMapper.
     *
     * @param objectMapper the ObjectMapper to use
     * @return this builder
     */
    public Builder objectMapper(ObjectMapper objectMapper) {
      this.objectMapper = objectMapper;
      return this;
    }

    /**
     * Enables pretty printing for JSON output.
     *
     * @return this builder
     */
    public Builder prettyPrint() {
      objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
      return this;
    }

    /**
     * Configures strict mode (fails on unknown properties).
     *
     * @return this builder
     */
    public Builder strictMode() {
      objectMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
      return this;
    }

    /**
     * Builds the serialization provider.
     *
     * @return a new JacksonSerializationProvider
     */
    public JacksonSerializationProvider build() {
      return new JacksonSerializationProvider(objectMapper);
    }
  }

  /**
   * Creates a new builder.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }
}
