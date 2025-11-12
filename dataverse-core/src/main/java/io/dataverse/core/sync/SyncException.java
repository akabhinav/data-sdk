package io.dataverse.core.sync;

/**
 * Exception thrown when synchronization fails.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class SyncException extends RuntimeException {

  public SyncException(String message) {
    super(message);
  }

  public SyncException(String message, Throwable cause) {
    super(message, cause);
  }
}
