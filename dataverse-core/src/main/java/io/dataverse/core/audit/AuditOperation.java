package io.dataverse.core.audit;

/**
 * Types of operations that can be audited.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public enum AuditOperation {
  /**
   * Entity was inserted/created.
   */
  INSERT,

  /**
   * Entity was updated/modified.
   */
  UPDATE,

  /**
   * Entity was deleted/removed.
   */
  DELETE
}
