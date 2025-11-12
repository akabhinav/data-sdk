package io.dataverse.api;

import io.dataverse.core.audit.AuditRepository;
import io.dataverse.core.specification.Specification;

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
   * Returns all entities matching the given specification.
   *
   * <p>Uses the Specification pattern for type-safe, composable query building.
   *
   * <p>Example usage:
   * <pre>{@code
   * Specification<User> spec = Specification.<User>isTrue("active")
   *     .and(Specification.greaterThan("age", 18));
   * List<User> activeAdults = repository.findAll(spec);
   * }</pre>
   *
   * @param spec the specification to apply, must not be {@code null}
   * @return all entities matching the specification, never {@code null}
   * @throws IllegalArgumentException if spec is {@code null}
   */
  List<T> findAll(Specification<T> spec);

  /**
   * Returns a single entity matching the given specification.
   *
   * @param spec the specification to apply, must not be {@code null}
   * @return an Optional containing the entity if found, empty otherwise
   * @throws IllegalArgumentException if spec is {@code null}
   */
  Optional<T> findOne(Specification<T> spec);

  /**
   * Returns the count of entities matching the given specification.
   *
   * @param spec the specification to apply, must not be {@code null}
   * @return the count of matching entities
   * @throws IllegalArgumentException if spec is {@code null}
   */
  long count(Specification<T> spec);

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
   * Returns the audit repository for querying audit trail entries.
   *
   * <p>Audit trail is only available for entities annotated with {@code @Audited}.
   * If the entity is not audited, this method may return null or throw an exception.
   *
   * <p>Example usage:
   * <pre>{@code
   * // Query all changes to a specific product
   * List<AuditEntry<Product>> history = repository.audit()
   *     .forEntityId(productId)
   *     .execute();
   *
   * // Query updates in the last 30 days
   * List<AuditEntry<Product>> recentUpdates = repository.audit()
   *     .forEntityType(Product.class)
   *     .operation(AuditOperation.UPDATE)
   *     .after(Instant.now().minus(30, ChronoUnit.DAYS))
   *     .execute();
   * }</pre>
   *
   * @return the audit repository, never {@code null}
   */
  AuditRepository<T> audit();

  /**
   * Creates a native query executor for type-safe native queries.
   *
   * <p>Native queries allow executing raw database queries while maintaining type safety
   * for results. Supports named parameters, result mapping, and pagination.
   *
   * <p>Example usage:
   * <pre>{@code
   * // Execute native SQL with parameters
   * List<User> users = repository.nativeQuery(User.class)
   *     .sql("SELECT * FROM users WHERE status = :status AND age > :age")
   *     .parameter("status", "ACTIVE")
   *     .parameter("age", 18)
   *     .execute();
   *
   * // Map to DTO
   * List<UserSummary> summaries = repository.nativeQuery(UserSummary.class)
   *     .sql("SELECT id, username, email FROM users WHERE dept = :dept")
   *     .parameter("dept", "Engineering")
   *     .execute();
   * }</pre>
   *
   * @param <R> the result type
   * @param resultClass the class to map results to
   * @return a native query executor, never {@code null}
   */
  <R> io.dataverse.core.query.NativeQueryExecutor<R> nativeQuery(Class<R> resultClass);

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
