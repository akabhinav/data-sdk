package io.dataverse.core.migration;

import java.util.Optional;

/**
 * Result of a migration operation.
 *
 * <p>Contains information about what migrations were applied, the resulting
 * database version, and any errors that occurred.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * MigrationResult result = migrationManager.migrate();
 *
 * if (result.isSuccess()) {
 *     System.out.println("Successfully applied " +
 *         result.getMigrationsApplied() + " migrations");
 *     System.out.println("Database version: " +
 *         result.getTargetVersion());
 * } else {
 *     System.err.println("Migration failed: " +
 *         result.getError().orElse("Unknown error"));
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class MigrationResult {

  private final boolean success;
  private final int initialVersion;
  private final int targetVersion;
  private final int migrationsApplied;
  private final ResultType resultType;
  private final Throwable error;

  private MigrationResult(
      boolean success,
      int initialVersion,
      int targetVersion,
      int migrationsApplied,
      ResultType resultType,
      Throwable error) {
    this.success = success;
    this.initialVersion = initialVersion;
    this.targetVersion = targetVersion;
    this.migrationsApplied = migrationsApplied;
    this.resultType = resultType;
    this.error = error;
  }

  /**
   * Whether the migration was successful.
   *
   * @return true if successful
   */
  public boolean isSuccess() {
    return success;
  }

  /**
   * Gets the initial database version.
   *
   * @return initial version
   */
  public int getInitialVersion() {
    return initialVersion;
  }

  /**
   * Gets the target database version.
   *
   * @return target version
   */
  public int getTargetVersion() {
    return targetVersion;
  }

  /**
   * Gets the number of migrations applied.
   *
   * @return migrations applied count
   */
  public int getMigrationsApplied() {
    return migrationsApplied;
  }

  /**
   * Gets the result type.
   *
   * @return result type
   */
  public ResultType getResultType() {
    return resultType;
  }

  /**
   * Gets the error if migration failed.
   *
   * @return optional containing error
   */
  public Optional<Throwable> getError() {
    return Optional.ofNullable(error);
  }

  /**
   * Gets the error message if migration failed.
   *
   * @return optional containing error message
   */
  public Optional<String> getErrorMessage() {
    return error != null ? Optional.of(error.getMessage()) : Optional.empty();
  }

  /**
   * Creates a successful migration result.
   *
   * @param initialVersion the initial version
   * @param targetVersion the target version
   * @param migrationsApplied number of migrations applied
   * @return success result
   */
  public static MigrationResult success(
      int initialVersion,
      int targetVersion,
      int migrationsApplied) {
    return new MigrationResult(
        true,
        initialVersion,
        targetVersion,
        migrationsApplied,
        ResultType.SUCCESS,
        null
    );
  }

  /**
   * Creates a failed migration result.
   *
   * @param currentVersion the current version
   * @param migrationsApplied number of migrations applied before failure
   * @param error the error that occurred
   * @return failure result
   */
  public static MigrationResult failure(
      int currentVersion,
      int migrationsApplied,
      Throwable error) {
    return new MigrationResult(
        false,
        currentVersion,
        currentVersion,
        migrationsApplied,
        ResultType.FAILURE,
        error
    );
  }

  /**
   * Creates an up-to-date result (no migrations needed).
   *
   * @param currentVersion the current version
   * @return up-to-date result
   */
  public static MigrationResult upToDate(int currentVersion) {
    return new MigrationResult(
        true,
        currentVersion,
        currentVersion,
        0,
        ResultType.UP_TO_DATE,
        null
    );
  }

  /**
   * Creates a rollback result.
   *
   * @param targetVersion the version after rollback
   * @param migrationsRolledBack number of migrations rolled back
   * @return rollback result
   */
  public static MigrationResult rollback(int targetVersion, int migrationsRolledBack) {
    return new MigrationResult(
        true,
        targetVersion + migrationsRolledBack,
        targetVersion,
        migrationsRolledBack,
        ResultType.ROLLBACK,
        null
    );
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("MigrationResult{");
    sb.append("success=").append(success);
    sb.append(", type=").append(resultType);
    sb.append(", initialVersion=").append(initialVersion);
    sb.append(", targetVersion=").append(targetVersion);
    sb.append(", migrationsApplied=").append(migrationsApplied);
    if (error != null) {
      sb.append(", error=").append(error.getMessage());
    }
    sb.append('}');
    return sb.toString();
  }

  /**
   * Result type enumeration.
   */
  public enum ResultType {
    /** Migration completed successfully */
    SUCCESS,

    /** Migration failed */
    FAILURE,

    /** Database is already up to date */
    UP_TO_DATE,

    /** Rollback completed */
    ROLLBACK
  }
}
