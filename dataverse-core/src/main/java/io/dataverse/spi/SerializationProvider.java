package io.dataverse.spi;

import io.dataverse.api.Entity;
import java.io.Serializable;
import java.util.Map;

/**
 * Service Provider Interface for entity serialization.
 *
 * <p>This SPI allows pluggable serialization implementations (JSON, XML, Protobuf, etc.)
 * without coupling the core module to any specific serialization library.
 *
 * <p>Implementations should be registered using Java's ServiceLoader mechanism by creating:
 * <pre>
 * META-INF/services/io.dataverse.spi.SerializationProvider
 * </pre>
 *
 * <p><strong>Example Implementation:</strong>
 * <pre>{@code
 * public class JacksonSerializationProvider implements SerializationProvider {
 *     private final ObjectMapper objectMapper = new ObjectMapper();
 *
 *     @Override
 *     public String getProviderId() {
 *         return "jackson-json";
 *     }
 *
 *     @Override
 *     public <T extends Entity<ID>, ID extends Serializable> String serialize(T entity) {
 *         return objectMapper.writeValueAsString(entity);
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface SerializationProvider {

  /**
   * Returns a unique identifier for this serialization provider.
   *
   * @return the provider ID (e.g., "jackson-json", "gson-json", "protobuf")
   */
  String getProviderId();

  /**
   * Serializes an entity to its string representation.
   *
   * @param entity the entity to serialize
   * @param <T> the entity type
   * @param <ID> the entity ID type
   * @return the serialized string
   * @throws SerializationException if serialization fails
   */
  <T extends Entity<ID>, ID extends Serializable> String serialize(T entity)
      throws SerializationException;

  /**
   * Serializes an entity to a byte array.
   *
   * @param entity the entity to serialize
   * @param <T> the entity type
   * @param <ID> the entity ID type
   * @return the serialized bytes
   * @throws SerializationException if serialization fails
   */
  <T extends Entity<ID>, ID extends Serializable> byte[] serializeToBytes(T entity)
      throws SerializationException;

  /**
   * Deserializes a string to an entity.
   *
   * @param data the serialized string
   * @param entityClass the entity class
   * @param <T> the entity type
   * @param <ID> the entity ID type
   * @return the deserialized entity
   * @throws SerializationException if deserialization fails
   */
  <T extends Entity<ID>, ID extends Serializable> T deserialize(String data, Class<T> entityClass)
      throws SerializationException;

  /**
   * Deserializes a byte array to an entity.
   *
   * @param data the serialized bytes
   * @param entityClass the entity class
   * @param <T> the entity type
   * @param <ID> the entity ID type
   * @return the deserialized entity
   * @throws SerializationException if deserialization fails
   */
  <T extends Entity<ID>, ID extends Serializable> T deserializeFromBytes(
      byte[] data, Class<T> entityClass) throws SerializationException;

  /**
   * Converts an entity to a map representation.
   * Useful for data sources that work with map-based structures (DynamoDB, MongoDB).
   *
   * @param entity the entity to convert
   * @param <T> the entity type
   * @param <ID> the entity ID type
   * @return the entity as a map
   * @throws SerializationException if conversion fails
   */
  <T extends Entity<ID>, ID extends Serializable> Map<String, Object> toMap(T entity)
      throws SerializationException;

  /**
   * Converts a map to an entity.
   * Useful for data sources that return map-based structures.
   *
   * @param map the map representation
   * @param entityClass the entity class
   * @param <T> the entity type
   * @param <ID> the entity ID type
   * @return the entity
   * @throws SerializationException if conversion fails
   */
  <T extends Entity<ID>, ID extends Serializable> T fromMap(
      Map<String, Object> map, Class<T> entityClass) throws SerializationException;

  /**
   * Creates a deep copy of an entity.
   *
   * @param entity the entity to copy
   * @param <T> the entity type
   * @param <ID> the entity ID type
   * @return a deep copy of the entity
   * @throws SerializationException if copying fails
   */
  <T extends Entity<ID>, ID extends Serializable> T clone(T entity) throws SerializationException;
}
