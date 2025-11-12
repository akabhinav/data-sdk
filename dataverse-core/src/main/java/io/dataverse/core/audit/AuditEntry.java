package io.dataverse.core.audit;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * Represents a single audit trail entry.
 *
 * <p>Contains information about:
 * <ul>
 *   <li>What changed (operation type)</li>
 *   <li>When it changed (timestamp)</li>
 *   <li>Who changed it (user/principal)</li>
 *   <li>Before and after values</li>
 *   <li>Changed fields</li>
 * </ul>
 *
 * @param <T> the entity type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class AuditEntry<T> {

  private final Serializable entityId;
  private final String entityType;
  private final AuditOperation operation;
  private final Instant timestamp;
  private final String modifiedBy;
  private final T beforeValue;
  private final T afterValue;
  private final Map<String, FieldChange> changedFields;
  private final String comment;

  private AuditEntry(Builder<T> builder) {
    this.entityId = builder.entityId;
    this.entityType = builder.entityType;
    this.operation = builder.operation;
    this.timestamp = builder.timestamp;
    this.modifiedBy = builder.modifiedBy;
    this.beforeValue = builder.beforeValue;
    this.afterValue = builder.afterValue;
    this.changedFields = builder.changedFields;
    this.comment = builder.comment;
  }

  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  // Getters
  public Serializable getEntityId() { return entityId; }
  public String getEntityType() { return entityType; }
  public AuditOperation getOperation() { return operation; }
  public Instant getTimestamp() { return timestamp; }
  public String getModifiedBy() { return modifiedBy; }
  public T getBeforeValue() { return beforeValue; }
  public T getAfterValue() { return afterValue; }
  public Map<String, FieldChange> getChangedFields() { return changedFields; }
  public String getComment() { return comment; }

  /**
   * Returns true if this was an insert operation.
   */
  public boolean isInsert() {
    return operation == AuditOperation.INSERT;
  }

  /**
   * Returns true if this was an update operation.
   */
  public boolean isUpdate() {
    return operation == AuditOperation.UPDATE;
  }

  /**
   * Returns true if this was a delete operation.
   */
  public boolean isDelete() {
    return operation == AuditOperation.DELETE;
  }

  @Override
  public String toString() {
    return String.format("AuditEntry{entityType='%s', entityId=%s, operation=%s, timestamp=%s, modifiedBy='%s'}",
        entityType, entityId, operation, timestamp, modifiedBy);
  }

  /**
   * Represents a change to a single field.
   */
  public static class FieldChange {
    private final String fieldName;
    private final Object oldValue;
    private final Object newValue;

    public FieldChange(String fieldName, Object oldValue, Object newValue) {
      this.fieldName = fieldName;
      this.oldValue = oldValue;
      this.newValue = newValue;
    }

    public String getFieldName() { return fieldName; }
    public Object getOldValue() { return oldValue; }
    public Object getNewValue() { return newValue; }

    @Override
    public String toString() {
      return String.format("%s: %s → %s", fieldName, oldValue, newValue);
    }
  }

  public static class Builder<T> {
    private Serializable entityId;
    private String entityType;
    private AuditOperation operation;
    private Instant timestamp = Instant.now();
    private String modifiedBy;
    private T beforeValue;
    private T afterValue;
    private Map<String, FieldChange> changedFields;
    private String comment;

    public Builder<T> entityId(Serializable entityId) {
      this.entityId = entityId;
      return this;
    }

    public Builder<T> entityType(String entityType) {
      this.entityType = entityType;
      return this;
    }

    public Builder<T> operation(AuditOperation operation) {
      this.operation = operation;
      return this;
    }

    public Builder<T> timestamp(Instant timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder<T> modifiedBy(String modifiedBy) {
      this.modifiedBy = modifiedBy;
      return this;
    }

    public Builder<T> beforeValue(T beforeValue) {
      this.beforeValue = beforeValue;
      return this;
    }

    public Builder<T> afterValue(T afterValue) {
      this.afterValue = afterValue;
      return this;
    }

    public Builder<T> changedFields(Map<String, FieldChange> changedFields) {
      this.changedFields = changedFields;
      return this;
    }

    public Builder<T> comment(String comment) {
      this.comment = comment;
      return this;
    }

    public AuditEntry<T> build() {
      if (entityId == null) throw new IllegalStateException("entityId is required");
      if (entityType == null) throw new IllegalStateException("entityType is required");
      if (operation == null) throw new IllegalStateException("operation is required");
      return new AuditEntry<>(this);
    }
  }
}
