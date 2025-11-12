package io.dataverse.api;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Abstraction for mapping entities to DTOs and vice versa.
 *
 * <p>This interface provides type-safe mapping capabilities between entity types
 * and data transfer objects, supporting both single entity and collection mappings.
 *
 * <p><strong>Example Usage:</strong>
 * <pre>{@code
 * // Define a mapper
 * EntityMapper<User, UserDTO> mapper = new EntityMapper<>() {
 *     @Override
 *     public UserDTO toDto(User entity) {
 *         return new UserDTO(entity.getId(), entity.getName(), entity.getEmail());
 *     }
 *
 *     @Override
 *     public User toEntity(UserDTO dto) {
 *         User user = new User();
 *         user.setId(dto.id());
 *         user.setName(dto.name());
 *         user.setEmail(dto.email());
 *         return user;
 *     }
 * };
 *
 * // Use the mapper
 * UserDTO dto = mapper.toDto(user);
 * List<UserDTO> dtos = mapper.toDtoList(users);
 * }</pre>
 *
 * @param <E> the entity type
 * @param <D> the DTO type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface EntityMapper<E extends Entity<?>, D> {

  /**
   * Converts an entity to a DTO.
   *
   * @param entity the entity to convert
   * @return the DTO
   */
  D toDto(E entity);

  /**
   * Converts a DTO to an entity.
   *
   * @param dto the DTO to convert
   * @return the entity
   */
  E toEntity(D dto);

  /**
   * Converts a list of entities to DTOs.
   *
   * @param entities the entities to convert
   * @return the list of DTOs
   */
  default List<D> toDtoList(List<E> entities) {
    return entities.stream().map(this::toDto).collect(Collectors.toList());
  }

  /**
   * Converts a list of DTOs to entities.
   *
   * @param dtos the DTOs to convert
   * @return the list of entities
   */
  default List<E> toEntityList(List<D> dtos) {
    return dtos.stream().map(this::toEntity).collect(Collectors.toList());
  }

  /**
   * Updates an existing entity with data from a DTO.
   *
   * @param dto the DTO containing new data
   * @param entity the entity to update
   */
  default void updateEntity(D dto, E entity) {
    // Default implementation: convert and copy
    E newEntity = toEntity(dto);
    // Subclasses should override this for partial updates
  }

  /**
   * Creates a mapper that chains this mapper with another.
   *
   * @param after the mapper to apply after this one
   * @param <T> the result type
   * @return a composed mapper
   */
  default <T> EntityMapper<E, T> andThen(Function<D, T> after, Function<T, D> before) {
    return new EntityMapper<>() {
      @Override
      public T toDto(E entity) {
        return after.apply(EntityMapper.this.toDto(entity));
      }

      @Override
      public E toEntity(T dto) {
        return EntityMapper.this.toEntity(before.apply(dto));
      }
    };
  }

  /**
   * Creates a simple mapper using conversion functions.
   *
   * @param toDto the entity-to-DTO function
   * @param toEntity the DTO-to-entity function
   * @param <E> the entity type
   * @param <D> the DTO type
   * @return a new mapper
   */
  static <E extends Entity<?>, D> EntityMapper<E, D> of(
      Function<E, D> toDto, Function<D, E> toEntity) {
    return new EntityMapper<>() {
      @Override
      public D toDto(E entity) {
        return toDto.apply(entity);
      }

      @Override
      public E toEntity(D dto) {
        return toEntity.apply(dto);
      }
    };
  }
}
