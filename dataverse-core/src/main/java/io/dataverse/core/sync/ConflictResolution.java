package io.dataverse.core.sync;

/**
 * Conflict resolution strategy enumeration.
 *
 * <p>Defines how to resolve conflicts when the same record
 * has been modified in both data sources.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public enum ConflictResolution {

  /**
   * Last write wins - most recent timestamp wins.
   *
   * <p>Compares last modified timestamps and keeps the newer version.
   */
  LAST_WRITE_WINS,

  /**
   * Primary always wins.
   *
   * <p>Primary data source version is always kept.
   */
  PRIMARY_WINS,

  /**
   * Secondary always wins.
   *
   * <p>Secondary data source version is always kept.
   */
  SECONDARY_WINS,

  /**
   * Manual resolution required.
   *
   * <p>Throws SyncConflictException for manual handling.
   */
  MANUAL,

  /**
   * Custom resolver function.
   *
   * <p>Uses user-provided BiFunction to resolve conflicts.
   */
  CUSTOM
}
