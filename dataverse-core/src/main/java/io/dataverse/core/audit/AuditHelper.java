package io.dataverse.core.audit;

import io.dataverse.api.Entity;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for audit trail functionality.
 *
 * <p>Provides utility methods for:
 * <ul>
 *   <li>Checking if an entity is audited</li>
 *   <li>Detecting field changes between entity versions</li>
 *   <li>Building audit entries</li>
 * </ul>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class AuditHelper {

  /**
   * Checks if an entity class is annotated with @Audited.
   *
   * @param entityClass the entity class to check
   * @return true if the entity is audited, false otherwise
   */
  public static boolean isAudited(Class<?> entityClass) {
    return entityClass.isAnnotationPresent(Audited.class);
  }

  /**
   * Gets the @Audited annotation from an entity class.
   *
   * @param entityClass the entity class
   * @return the annotation, or null if not present
   */
  public static Audited getAuditedAnnotation(Class<?> entityClass) {
    return entityClass.getAnnotation(Audited.class);
  }

  /**
   * Detects field changes between two versions of an entity.
   *
   * @param beforeValue the entity before changes (may be null for INSERT)
   * @param afterValue the entity after changes
   * @param <T> the entity type
   * @return a map of field name to FieldChange for all changed fields
   */
  public static <T> Map<String, AuditEntry.FieldChange> detectChanges(T beforeValue, T afterValue) {
    Map<String, AuditEntry.FieldChange> changes = new HashMap<>();

    if (beforeValue == null) {
      // INSERT operation - all fields are "new"
      return changes;
    }

    if (afterValue == null) {
      // DELETE operation - no field changes to track
      return changes;
    }

    // Compare fields using reflection
    Class<?> entityClass = afterValue.getClass();
    Field[] fields = entityClass.getDeclaredFields();

    for (Field field : fields) {
      field.setAccessible(true);
      try {
        Object oldValue = field.get(beforeValue);
        Object newValue = field.get(afterValue);

        // Check if values are different
        if (!areEqual(oldValue, newValue)) {
          changes.put(field.getName(), new AuditEntry.FieldChange(field.getName(), oldValue, newValue));
        }
      } catch (IllegalAccessException e) {
        // Skip fields we can't access
      }
    }

    return changes;
  }

  /**
   * Checks if two values are equal, handling nulls properly.
   *
   * @param value1 the first value
   * @param value2 the second value
   * @return true if equal, false otherwise
   */
  private static boolean areEqual(Object value1, Object value2) {
    if (value1 == null && value2 == null) {
      return true;
    }
    if (value1 == null || value2 == null) {
      return false;
    }
    return value1.equals(value2);
  }

  /**
   * Creates an audit entry for an INSERT operation.
   *
   * @param entity the inserted entity
   * @param modifiedBy the user who performed the operation
   * @param <T> the entity type
   * @param <ID> the entity ID type
   * @return the audit entry
   */
  public static <T extends Entity<ID>, ID extends Serializable> AuditEntry<T> createInsertEntry(
      T entity, String modifiedBy) {
    return AuditEntry.<T>builder()
        .entityId(entity.getId())
        .entityType(entity.getClass().getSimpleName())
        .operation(AuditOperation.INSERT)
        .afterValue(entity)
        .modifiedBy(modifiedBy)
        .build();
  }

  /**
   * Creates an audit entry for an UPDATE operation.
   *
   * @param beforeValue the entity before changes
   * @param afterValue the entity after changes
   * @param modifiedBy the user who performed the operation
   * @param <T> the entity type
   * @param <ID> the entity ID type
   * @return the audit entry
   */
  public static <T extends Entity<ID>, ID extends Serializable> AuditEntry<T> createUpdateEntry(
      T beforeValue, T afterValue, String modifiedBy) {
    Map<String, AuditEntry.FieldChange> changes = detectChanges(beforeValue, afterValue);

    return AuditEntry.<T>builder()
        .entityId(afterValue.getId())
        .entityType(afterValue.getClass().getSimpleName())
        .operation(AuditOperation.UPDATE)
        .beforeValue(beforeValue)
        .afterValue(afterValue)
        .changedFields(changes)
        .modifiedBy(modifiedBy)
        .build();
  }

  /**
   * Creates an audit entry for a DELETE operation.
   *
   * @param entity the deleted entity
   * @param modifiedBy the user who performed the operation
   * @param <T> the entity type
   * @param <ID> the entity ID type
   * @return the audit entry
   */
  public static <T extends Entity<ID>, ID extends Serializable> AuditEntry<T> createDeleteEntry(
      T entity, String modifiedBy) {
    return AuditEntry.<T>builder()
        .entityId(entity.getId())
        .entityType(entity.getClass().getSimpleName())
        .operation(AuditOperation.DELETE)
        .beforeValue(entity)
        .modifiedBy(modifiedBy)
        .build();
  }

  /**
   * Creates an audit entry for a DELETE operation (by ID only).
   *
   * @param entityId the entity ID
   * @param entityType the entity type name
   * @param modifiedBy the user who performed the operation
   * @param <T> the entity type
   * @return the audit entry
   */
  public static <T> AuditEntry<T> createDeleteEntryById(
      Serializable entityId, String entityType, String modifiedBy) {
    return AuditEntry.<T>builder()
        .entityId(entityId)
        .entityType(entityType)
        .operation(AuditOperation.DELETE)
        .modifiedBy(modifiedBy)
        .build();
  }

  /**
   * Checks if a specific operation should be audited based on the @Audited annotation.
   *
   * @param annotation the @Audited annotation
   * @param operation the operation to check
   * @return true if the operation should be audited
   */
  public static boolean shouldAuditOperation(Audited annotation, AuditOperation operation) {
    if (annotation == null) {
      return false;
    }

    AuditOperation[] operations = annotation.operations();
    if (operations.length == 0) {
      // Empty means audit all operations
      return true;
    }

    for (AuditOperation op : operations) {
      if (op == operation) {
        return true;
      }
    }

    return false;
  }
}
