package io.dataverse.core;

import io.dataverse.api.BatchOperations;
import io.dataverse.api.Entity;
import io.dataverse.api.Repository;
import io.dataverse.core.audit.*;
import io.dataverse.core.cache.*;
import io.dataverse.core.encryption.*;
import io.dataverse.core.specification.Specification;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Abstract base implementation of Repository providing common functionality.
 *
 * <p>Adapter implementations should extend this class and implement the abstract methods
 * for data source-specific operations. This class provides:
 *
 * <ul>
 *   <li>Virtual thread executor for async operations
 *   <li>Default implementations for batch operations
 *   <li>Validation logic
 *   <li>Exception handling
 * </ul>
 *
 * @param <T> the entity type
 * @param <ID> the identifier type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public abstract class AbstractRepository<T extends Entity<ID>, ID extends Serializable>
    implements Repository<T, ID> {

  protected final Class<T> entityClass;
  protected final ExecutorService virtualThreadExecutor;
  protected final AuditRepository<T> auditRepository;
  protected final boolean isAudited;
  protected final CacheProvider<String, T> cacheProvider;
  protected final CacheKeyGenerator cacheKeyGenerator;
  protected final CacheConfig cacheConfig;
  protected final EncryptionProvider encryptionProvider;
  protected final boolean hasEncryptedFields;

  // ThreadLocal to store entity state before operations (for audit trail)
  private final ThreadLocal<T> beforeState = new ThreadLocal<>();

  /**
   * Constructs a new abstract repository with default caching disabled.
   *
   * @param entityClass the entity class, must not be {@code null}
   */
  protected AbstractRepository(Class<T> entityClass) {
    this(entityClass, CacheConfig.builder().enabled(false).build(), new NoEncryptionProvider());
  }

  /**
   * Constructs a new abstract repository with caching configuration.
   *
   * @param entityClass the entity class, must not be {@code null}
   * @param cacheConfig the cache configuration
   */
  protected AbstractRepository(Class<T> entityClass, CacheConfig cacheConfig) {
    this(entityClass, cacheConfig, new NoEncryptionProvider());
  }

  /**
   * Constructs a new abstract repository with caching and encryption configuration.
   *
   * @param entityClass the entity class, must not be {@code null}
   * @param cacheConfig the cache configuration
   * @param encryptionProvider the encryption provider
   */
  protected AbstractRepository(Class<T> entityClass, CacheConfig cacheConfig, EncryptionProvider encryptionProvider) {
    if (entityClass == null) {
      throw new IllegalArgumentException("Entity class must not be null");
    }
    this.entityClass = entityClass;
    this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    this.isAudited = AuditHelper.isAudited(entityClass);
    this.auditRepository = isAudited ? new InMemoryAuditRepository<>(entityClass) : null;
    this.cacheConfig = cacheConfig;
    this.cacheKeyGenerator = new CacheKeyGenerator(entityClass.getSimpleName());
    this.encryptionProvider = encryptionProvider != null ? encryptionProvider : new NoEncryptionProvider();
    this.hasEncryptedFields = EncryptionHelper.hasEncryptedFields(entityClass);

    // Initialize cache provider based on configuration
    if (cacheConfig.isEnabled()) {
      CacheProvider<String, T> l1 = cacheConfig.isL1Enabled()
          ? new InMemoryCacheProvider<>(entityClass.getSimpleName() + "-L1", cacheConfig)
          : new NoCacheProvider<>();

      CacheProvider<String, T> l2 = cacheConfig.isL2Enabled()
          ? createL2CacheProvider(cacheConfig)
          : new NoCacheProvider<>();

      this.cacheProvider = new MultiLevelCacheProvider<>(
          entityClass.getSimpleName() + "-Cache",
          l1,
          l2
      );
    } else {
      this.cacheProvider = new NoCacheProvider<>();
    }
  }

  /**
   * Creates an L2 (distributed) cache provider.
   * Subclasses can override to provide custom L2 cache implementations (e.g., Redis).
   *
   * @param config the cache configuration
   * @return the L2 cache provider
   */
  protected CacheProvider<String, T> createL2CacheProvider(CacheConfig config) {
    // Default: no L2 cache
    // Subclasses can override to integrate with Redis, Memcached, etc.
    return new NoCacheProvider<>();
  }

  /**
   * Gets the entity class managed by this repository.
   *
   * @return the entity class, never {@code null}
   */
  public Class<T> getEntityClass() {
    return entityClass;
  }

  // Template Methods - Subclasses must implement these

  /**
   * Performs the actual save operation.
   *
   * @param entity the entity to save
   * @return the saved entity
   */
  protected abstract T doSave(T entity);

  /**
   * Performs the actual find by ID operation.
   *
   * @param id the entity identifier
   * @return an Optional containing the entity if found
   */
  protected abstract Optional<T> doFindById(ID id);

  /**
   * Performs the actual delete operation.
   *
   * @param id the entity identifier
   */
  protected abstract void doDelete(ID id);

  /**
   * Performs the actual find all operation.
   *
   * @return all entities
   */
  protected abstract List<T> doFindAll();

  /**
   * Performs the actual count operation.
   *
   * @return the total count
   */
  protected abstract long doCount();

  // Public API Implementation with validation and common logic

  @Override
  public T save(T entity) {
    validateEntity(entity, "Entity to save");
    beforeSave(entity);

    // Encrypt fields before saving if entity has encrypted fields
    if (hasEncryptedFields) {
      EncryptionHelper.encryptFields(entity, encryptionProvider);
    }

    T saved = doSave(entity);

    // Decrypt fields after saving for return value
    if (hasEncryptedFields) {
      EncryptionHelper.decryptFields(saved, encryptionProvider);
    }

    afterSave(saved);

    // Update cache after successful save (with decrypted version)
    if (cacheConfig.isEnabled() && saved.getId() != null) {
      String cacheKey = cacheKeyGenerator.generateKey(saved.getId());
      cacheProvider.put(cacheKey, saved);
    }

    return saved;
  }

  @Override
  public List<T> saveAll(Iterable<T> entities) {
    if (entities == null) {
      throw new IllegalArgumentException("Entities must not be null");
    }

    List<T> result = new ArrayList<>();
    for (T entity : entities) {
      result.add(save(entity));
    }
    return result;
  }

  @Override
  public Optional<T> findById(ID id) {
    validateId(id);

    // Use cache-aside pattern if caching is enabled
    if (cacheConfig.isEnabled()) {
      String cacheKey = cacheKeyGenerator.generateKey(id);
      T cached = cacheProvider.computeIfAbsent(cacheKey, () -> {
        Optional<T> result = doFindById(id);
        if (result.isPresent() && hasEncryptedFields) {
          // Decrypt fields after fetching from database
          EncryptionHelper.decryptFields(result.get(), encryptionProvider);
        }
        return result.orElse(null);
      });
      return Optional.ofNullable(cached);
    }

    // Without caching
    Optional<T> result = doFindById(id);
    if (result.isPresent() && hasEncryptedFields) {
      // Decrypt fields after fetching from database
      EncryptionHelper.decryptFields(result.get(), encryptionProvider);
    }
    return result;
  }

  @Override
  public List<T> findAllById(Iterable<ID> ids) {
    if (ids == null) {
      throw new IllegalArgumentException("IDs must not be null");
    }

    List<T> result = new ArrayList<>();
    for (ID id : ids) {
      findById(id).ifPresent(result::add);
    }
    return result;
  }

  @Override
  public List<T> findAll() {
    return doFindAll();
  }

  @Override
  public List<T> findAll(Specification<T> spec) {
    if (spec == null) {
      throw new IllegalArgumentException("Specification must not be null");
    }
    // Apply specification to query builder and execute
    return spec.apply(query()).execute();
  }

  @Override
  public Optional<T> findOne(Specification<T> spec) {
    if (spec == null) {
      throw new IllegalArgumentException("Specification must not be null");
    }
    List<T> results = spec.apply(query()).limit(1).execute();
    return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
  }

  @Override
  public long count(Specification<T> spec) {
    if (spec == null) {
      throw new IllegalArgumentException("Specification must not be null");
    }
    // Apply specification and count results
    return spec.apply(query()).execute().size();
  }

  @Override
  public boolean existsById(ID id) {
    return findById(id).isPresent();
  }

  @Override
  public long count() {
    return doCount();
  }

  @Override
  public void deleteById(ID id) {
    validateId(id);
    beforeDelete(id);
    doDelete(id);
    afterDelete(id);

    // Evict from cache after successful delete
    if (cacheConfig.isEnabled()) {
      String cacheKey = cacheKeyGenerator.generateKey(id);
      cacheProvider.evict(cacheKey);
    }
  }

  @Override
  public void delete(T entity) {
    validateEntity(entity, "Entity to delete");
    deleteById(entity.getId());
  }

  @Override
  public void deleteAll(Iterable<T> entities) {
    if (entities == null) {
      throw new IllegalArgumentException("Entities must not be null");
    }

    for (T entity : entities) {
      delete(entity);
    }
  }

  @Override
  public BatchOperations<T, ID> batch() {
    return new DefaultBatchOperations<>(this, virtualThreadExecutor);
  }

  @Override
  public AuditRepository<T> audit() {
    if (!isAudited) {
      throw new IllegalStateException(
          "Entity " + entityClass.getSimpleName() + " is not annotated with @Audited. " +
          "Add @Audited annotation to enable audit trail.");
    }
    return auditRepository;
  }

  // Async Operations using Virtual Threads

  @Override
  public CompletableFuture<T> saveAsync(T entity) {
    return CompletableFuture.supplyAsync(() -> save(entity), virtualThreadExecutor);
  }

  @Override
  public CompletableFuture<Optional<T>> findByIdAsync(ID id) {
    return CompletableFuture.supplyAsync(() -> findById(id), virtualThreadExecutor);
  }

  @Override
  public CompletableFuture<Void> deleteByIdAsync(ID id) {
    return CompletableFuture.runAsync(() -> deleteById(id), virtualThreadExecutor);
  }

  // Validation Methods

  /**
   * Validates that an entity is not null.
   *
   * @param entity the entity to validate
   * @param message the error message
   * @throws IllegalArgumentException if entity is null
   */
  protected void validateEntity(T entity, String message) {
    if (entity == null) {
      throw new IllegalArgumentException(message + " must not be null");
    }
  }

  /**
   * Validates that an ID is not null.
   *
   * @param id the ID to validate
   * @throws IllegalArgumentException if ID is null
   */
  protected void validateId(ID id) {
    if (id == null) {
      throw new IllegalArgumentException("Entity ID must not be null");
    }
  }

  // Lifecycle Hooks (subclasses can override)

  /**
   * Called before an entity is saved. Subclasses can override to add pre-save logic.
   *
   * <p>This method handles audit trail by capturing the "before" state for UPDATE operations.
   *
   * @param entity the entity being saved
   */
  protected void beforeSave(T entity) {
    if (isAudited && entity.getId() != null) {
      // Entity has an ID - this is an UPDATE operation
      // Capture the "before" state for audit trail
      Optional<T> existing = doFindById(entity.getId());
      existing.ifPresent(beforeState::set);
    }
    // Hook for subclasses
  }

  /**
   * Called after an entity is saved. Subclasses can override to add post-save logic.
   *
   * <p>This method handles audit trail by creating audit entries for INSERT and UPDATE operations.
   *
   * @param entity the saved entity
   */
  protected void afterSave(T entity) {
    if (isAudited) {
      Audited annotation = AuditHelper.getAuditedAnnotation(entityClass);
      T before = beforeState.get();

      if (before == null) {
        // INSERT operation
        if (AuditHelper.shouldAuditOperation(annotation, AuditOperation.INSERT)) {
          AuditEntry<T> entry = AuditHelper.createInsertEntry(entity, getCurrentUser());
          auditRepository.save(entry);
        }
      } else {
        // UPDATE operation
        if (AuditHelper.shouldAuditOperation(annotation, AuditOperation.UPDATE)) {
          AuditEntry<T> entry = AuditHelper.createUpdateEntry(before, entity, getCurrentUser());
          auditRepository.save(entry);
        }
        // Clean up ThreadLocal
        beforeState.remove();
      }
    }
    // Hook for subclasses
  }

  /**
   * Called before an entity is deleted. Subclasses can override to add pre-delete logic.
   *
   * <p>This method handles audit trail by capturing the entity state before deletion.
   *
   * @param id the ID of the entity being deleted
   */
  protected void beforeDelete(ID id) {
    if (isAudited) {
      // Capture entity state before deletion for audit trail
      Optional<T> existing = doFindById(id);
      existing.ifPresent(beforeState::set);
    }
    // Hook for subclasses
  }

  /**
   * Called after an entity is deleted. Subclasses can override to add post-delete logic.
   *
   * <p>This method handles audit trail by creating audit entries for DELETE operations.
   *
   * @param id the ID of the deleted entity
   */
  protected void afterDelete(ID id) {
    if (isAudited) {
      Audited annotation = AuditHelper.getAuditedAnnotation(entityClass);
      if (AuditHelper.shouldAuditOperation(annotation, AuditOperation.DELETE)) {
        T before = beforeState.get();
        if (before != null) {
          AuditEntry<T> entry = AuditHelper.createDeleteEntry(before, getCurrentUser());
          auditRepository.save(entry);
          beforeState.remove();
        } else {
          // Fallback if we couldn't capture the entity before deletion
          AuditEntry<T> entry = AuditHelper.createDeleteEntryById(id, entityClass.getSimpleName(), getCurrentUser());
          auditRepository.save(entry);
        }
      }
    }
    // Hook for subclasses
  }

  /**
   * Gets the current user/principal for audit trail.
   *
   * <p>Default implementation returns "system". Subclasses can override to integrate
   * with security frameworks (Spring Security, etc.).
   *
   * @return the current user identifier
   */
  protected String getCurrentUser() {
    // Default to "system" - subclasses can override to get from SecurityContext
    return "system";
  }

  /**
   * Shuts down the virtual thread executor. Should be called during cleanup.
   */
  protected void shutdown() {
    if (virtualThreadExecutor != null && !virtualThreadExecutor.isShutdown()) {
      virtualThreadExecutor.shutdown();
    }
  }
}
