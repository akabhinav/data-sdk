package io.dataverse.api;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Central repository interface providing CRUD operations for entities.
 *
 * <p>Repository provides a high-level, type-safe API for data access operations. It abstracts
 * the underlying data source implementation, allowing seamless switching between different
 * databases without changing application code.
 *
 * <p>All operations support both synchronous and asynchronous execution patterns using Java 21
 * virtual threads for optimal performance.
 *
 * <p><strong>Example Usage:</strong>
 * <pre>{@code
 * Repository<User, String> userRepo = DataVerse.repository(User.class);
 *
 * // Save an entity
 * User user = new User("john@example.com", "John Doe");
 * User saved = userRepo.save(user);
 *
 * // Find by ID
 * Optional<User> found = userRepo.findById(saved.getId());
 *
 * // Query with builder
 * List<User> users = userRepo.query()
 *     .where("email").endsWith("@example.com")
 *     .and("active").isTrue()
 *     .orderBy("createdAt").descending()
 *     .limit(10)
 *     .execute();
 * }</pre>
 *
 * @param <T> the entity type
 * @param <ID> the identifier type
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface Repository<T extends Entity<ID>, ID extends Serializable> {

  /**
   * Saves the given entity.
   *
   * <p>If the entity does not have an ID, a new ID will be generated and assigned.
   * If the entity already has an ID, it will be updated.
   *
   * @param entity the entity to save, must not be {@code null}
   * @return the saved entity with ID populated
   * @throws IllegalArgumentException if entity is {@code null}
   */
  T save(T entity);

  /**
   * Saves all given entities in a batch operation.
   *
   * <p>This method is optimized for bulk operations and may use batching internally.
   *
   * @param entities the entities to save, must not be {@code null}
   * @return the saved entities with IDs populated
   * @throws IllegalArgumentException if entities is {@code null}
   */
  List<T> saveAll(Iterable<T> entities);

  /**
   * Retrieves an entity by its identifier.
   *
   * @param id the entity identifier, must not be {@code null}
   * @return an {@link Optional} containing the entity if found, empty otherwise
   * @throws IllegalArgumentException if id is {@code null}
   */
  Optional<T> findById(ID id);

  /**
   * Retrieves all entities with the given identifiers.
   *
   * @param ids the entity identifiers, must not be {@code null}
   * @return list of found entities, never {@code null}
   * @throws IllegalArgumentException if ids is {@code null}
   */
  List<T> findAllById(Iterable<ID> ids);

  /**
   * Returns all entities.
   *
   * <p><strong>Warning:</strong> This method may return large result sets.
   * Consider using {@link #query()} with pagination instead.
   *
   * @return all entities, never {@code null}
   */
  List<T> findAll();

  /**
   * Checks whether an entity with the given identifier exists.
   *
   * @param id the entity identifier, must not be {@code null}
   * @return {@code true} if an entity exists, {@code false} otherwise
   * @throws IllegalArgumentException if id is {@code null}
   */
  boolean existsById(ID id);

  /**
   * Returns the total count of entities.
   *
   * @return the total number of entities
   */
  long count();

  /**
   * Deletes the entity with the given identifier.
   *
   * @param id the entity identifier, must not be {@code null}
   * @throws IllegalArgumentException if id is {@code null}
   */
  void deleteById(ID id);

  /**
   * Deletes the given entity.
   *
   * @param entity the entity to delete, must not be {@code null}
   * @throws IllegalArgumentException if entity is {@code null}
   */
  void delete(T entity);

  /**
   * Deletes all given entities.
   *
   * @param entities the entities to delete, must not be {@code null}
   * @throws IllegalArgumentException if entities is {@code null}
   */
  void deleteAll(Iterable<T> entities);

  /**
   * Deletes all entities.
   *
   * <p><strong>Warning:</strong> This operation cannot be undone.
   */
  void deleteAll();

  /**
   * Creates a new query builder for complex queries.
   *
   * @return a new query builder instance, never {@code null}
   */
  QueryBuilder<T> query();

  /**
   * Creates a batch operations instance for bulk data manipulation.
   *
   * @return a new batch operations instance, never {@code null}
   */
  BatchOperations<T, ID> batch();

  /**
   * Creates an aggregation builder for advanced analytics queries.
   *
   * @return a new aggregation builder instance, never {@code null}
   */
  AggregationBuilder<T> aggregate();

  /**
   * Executes the given native query.
   *
   * <p>Native queries bypass the query builder and execute directly against the underlying
   * data source. Use this for data source-specific operations not supported by the query builder.
   *
   * @param nativeQuery the native query string in data source-specific format
   * @return query results, never {@code null}
   */
  List<T> executeNativeQuery(String nativeQuery);

  // Async Operations

  /**
   * Asynchronously saves the given entity using virtual threads.
   *
   * @param entity the entity to save, must not be {@code null}
   * @return a CompletableFuture that will complete with the saved entity
   * @throws IllegalArgumentException if entity is {@code null}
   */
  CompletableFuture<T> saveAsync(T entity);

  /**
   * Asynchronously retrieves an entity by its identifier.
   *
   * @param id the entity identifier, must not be {@code null}
   * @return a CompletableFuture that will complete with an Optional containing the entity
   * @throws IllegalArgumentException if id is {@code null}
   */
  CompletableFuture<Optional<T>> findByIdAsync(ID id);

  /**
   * Asynchronously deletes an entity by its identifier.
   *
   * @param id the entity identifier, must not be {@code null}
   * @return a CompletableFuture that will complete when deletion is finished
   * @throws IllegalArgumentException if id is {@code null}
   */
  CompletableFuture<Void> deleteByIdAsync(ID id);
}
