package io.dataverse.core.migration;

/**
 * Exception thrown when a migration operation fails.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class MigrationException extends RuntimeException {

  private final int version;

  /**
   * Creates a migration exception.
   *
   * @param message the error message
   */
  public MigrationException(String message) {
    this(message, -1);
  }

  /**
   * Creates a migration exception with version.
   *
   * @param message the error message
   * @param version the migration version that failed
   */
  public MigrationException(String message, int version) {
    super(message);
    this.version = version;
  }

  /**
   * Creates a migration exception with cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public MigrationException(String message, Throwable cause) {
    this(message, -1, cause);
  }

  /**
   * Creates a migration exception with version and cause.
   *
   * @param message the error message
   * @param version the migration version that failed
   * @param cause the underlying cause
   */
  public MigrationException(String message, int version, Throwable cause) {
    super(message, cause);
    this.version = version;
  }

  /**
   * Gets the migration version that failed.
   *
   * @return the version, or -1 if not applicable
   */
  public int getVersion() {
    return version;
  }
}
