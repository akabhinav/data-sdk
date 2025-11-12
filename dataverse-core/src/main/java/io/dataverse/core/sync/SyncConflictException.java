package io.dataverse.core.sync;

/**
 * Exception thrown when a sync conflict requires manual resolution.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class SyncConflictException extends SyncException {

  private final Object primaryRecord;
  private final Object secondaryRecord;

  public SyncConflictException(
      String message,
      Object primaryRecord,
      Object secondaryRecord) {
    super(message);
    this.primaryRecord = primaryRecord;
    this.secondaryRecord = secondaryRecord;
  }

  public Object getPrimaryRecord() {
    return primaryRecord;
  }

  public Object getSecondaryRecord() {
    return secondaryRecord;
  }
}
