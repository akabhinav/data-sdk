package io.dataverse.core.sync;

/**
 * Synchronization mode enumeration.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public enum SyncMode {

  /**
   * One-way sync: Primary → Secondary.
   *
   * <p>Changes from primary are pushed to secondary.
   * Secondary changes are ignored.
   */
  ONE_WAY,

  /**
   * Two-way sync: Primary ↔ Secondary.
   *
   * <p>Changes from both sides are synchronized.
   * Conflicts are resolved based on configuration.
   */
  TWO_WAY,

  /**
   * Master-slave sync: Primary controls Secondary.
   *
   * <p>Secondary is kept as exact replica of primary.
   * Records deleted from primary are deleted from secondary.
   */
  MASTER_SLAVE
}
