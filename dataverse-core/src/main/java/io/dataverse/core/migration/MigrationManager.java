package io.dataverse.core.migration;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Manages database schema migrations.
 *
 * <p>MigrationManager coordinates the execution of migrations, tracks their
 * history, and provides rollback capabilities.
 *
 * <p><strong>Features:</strong>
 * <ul>
 *   <li>Version-based migration ordering</li>
 *   <li>Checksum validation for script changes</li>
 *   <li>Automatic rollback on failure</li>
 *   <li>Migration history tracking</li>
 *   <li>Baseline support for existing databases</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Create migration manager
 * MigrationManager manager = new MigrationManager(executor);
 *
 * // Add migrations
 * manager.addMigration(
 *     Migration.builder()
 *         .version(1)
 *         .description("Create users table")
 *         .script("CREATE TABLE users (...)")
 *         .rollbackScript("DROP TABLE users")
 *         .build()
 * );
 *
 * // Execute all pending migrations
 * MigrationResult result = manager.migrate();
 *
 * // Check result
 * if (result.isSuccess()) {
 *     System.out.println("Applied " + result.getMigrationsApplied() + " migrations");
 * }
 *
 * // Rollback to specific version
 * manager.rollbackTo(1);
 * }</pre>
 *
 * <p><strong>Migration Table Schema:</strong>
 * <pre>{@code
 * CREATE TABLE schema_migrations (
 *     version INT PRIMARY KEY,
 *     description VARCHAR(255) NOT NULL,
 *     script_checksum VARCHAR(64) NOT NULL,
 *     executed_at TIMESTAMP NOT NULL,
 *     execution_time_ms BIGINT NOT NULL,
 *     status VARCHAR(32) NOT NULL
 * );
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class MigrationManager {

  private static final Logger logger = System.getLogger(MigrationManager.class.getName());

  private final MigrationExecutor executor;
  private final List<Migration> migrations = new ArrayList<>();
  private final MigrationConfig config;

  /**
   * Creates a migration manager.
   *
   * @param executor the migration executor
   */
  public MigrationManager(MigrationExecutor executor) {
    this(executor, MigrationConfig.defaults());
  }

  /**
   * Creates a migration manager with custom configuration.
   *
   * @param executor the migration executor
   * @param config the migration configuration
   */
  public MigrationManager(MigrationExecutor executor, MigrationConfig config) {
    this.executor = executor;
    this.config = config;
  }

  /**
   * Adds a migration to be managed.
   *
   * @param migration the migration to add
   */
  public void addMigration(Migration migration) {
    if (migrations.stream().anyMatch(m -> m.getVersion() == migration.getVersion())) {
      throw new IllegalArgumentException(
          "Migration with version " + migration.getVersion() + " already exists");
    }
    migrations.add(migration);
    Collections.sort(migrations, (m1, m2) -> Integer.compare(m1.getVersion(), m2.getVersion()));
  }

  /**
   * Adds multiple migrations.
   *
   * @param migrations the migrations to add
   */
  public void addMigrations(List<Migration> migrations) {
    for (Migration migration : migrations) {
      addMigration(migration);
    }
  }

  /**
   * Executes all pending migrations.
   *
   * @return migration result
   */
  public MigrationResult migrate() {
    logger.log(Level.INFO, "Starting database migration");

    // Ensure migration table exists
    executor.ensureMigrationTableExists();

    // Get current database version
    int currentVersion = executor.getCurrentVersion();
    logger.log(Level.INFO, "Current database version: " + currentVersion);

    // Find pending migrations
    List<Migration> pendingMigrations = migrations.stream()
        .filter(m -> m.getVersion() > currentVersion)
        .toList();

    if (pendingMigrations.isEmpty()) {
      logger.log(Level.INFO, "Database is up to date");
      return MigrationResult.upToDate(currentVersion);
    }

    logger.log(Level.INFO, "Found " + pendingMigrations.size() + " pending migrations");

    // Validate checksums for already applied migrations
    if (config.isValidateChecksums()) {
      validateAppliedMigrations();
    }

    // Execute pending migrations
    int migrationsApplied = 0;
    List<Migration> appliedMigrations = new ArrayList<>();

    for (Migration migration : pendingMigrations) {
      logger.log(Level.INFO, "Applying migration V" + migration.getVersion() + ": " + migration.getDescription());

      try {
        long startTime = System.currentTimeMillis();
        executor.execute(migration);
        long executionTime = System.currentTimeMillis() - startTime;

        // Record successful migration
        Migration executed = Migration.builder()
            .version(migration.getVersion())
            .description(migration.getDescription())
            .script(migration.getScript())
            .rollbackScript(migration.getRollbackScript())
            .checksum(migration.getChecksum())
            .type(migration.getType())
            .executedAt(Instant.now())
            .executionTimeMs(executionTime)
            .status(Migration.MigrationStatus.SUCCESS)
            .build();

        executor.recordMigration(executed);
        appliedMigrations.add(executed);
        migrationsApplied++;

        logger.log(Level.INFO, "Migration V" + migration.getVersion() + " completed in " + executionTime + "ms");

      } catch (Exception e) {
        logger.log(Level.ERROR, "Migration V" + migration.getVersion() + " failed: " + e.getMessage(), e);

        // Record failed migration
        Migration failed = Migration.builder()
            .version(migration.getVersion())
            .description(migration.getDescription())
            .script(migration.getScript())
            .checksum(migration.getChecksum())
            .type(migration.getType())
            .executedAt(Instant.now())
            .executionTimeMs(0)
            .status(Migration.MigrationStatus.FAILED)
            .build();

        executor.recordMigration(failed);

        // Rollback if configured
        if (config.isRollbackOnFailure() && !appliedMigrations.isEmpty()) {
          logger.log(Level.WARNING, "Rolling back applied migrations");
          rollbackMigrations(appliedMigrations);
        }

        return MigrationResult.failure(currentVersion, migrationsApplied, e);
      }
    }

    int newVersion = executor.getCurrentVersion();
    logger.log(Level.INFO, "Migration completed successfully. Database version: " + newVersion);

    return MigrationResult.success(currentVersion, newVersion, migrationsApplied);
  }

  /**
   * Rolls back to a specific version.
   *
   * @param targetVersion the version to roll back to
   * @return migration result
   */
  public MigrationResult rollbackTo(int targetVersion) {
    logger.log(Level.INFO, "Rolling back to version " + targetVersion);

    int currentVersion = executor.getCurrentVersion();

    if (targetVersion >= currentVersion) {
      logger.log(Level.WARNING, "Target version is not lower than current version");
      return MigrationResult.upToDate(currentVersion);
    }

    // Get migrations to roll back (in reverse order)
    List<Migration> toRollback = executor.getAppliedMigrations().stream()
        .filter(m -> m.getVersion() > targetVersion)
        .sorted((m1, m2) -> Integer.compare(m2.getVersion(), m1.getVersion()))  // Descending
        .toList();

    return rollbackMigrations(toRollback);
  }

  /**
   * Gets the current database version.
   *
   * @return current version
   */
  public int getCurrentVersion() {
    return executor.getCurrentVersion();
  }

  /**
   * Gets all applied migrations.
   *
   * @return list of applied migrations
   */
  public List<Migration> getAppliedMigrations() {
    return executor.getAppliedMigrations();
  }

  /**
   * Gets all pending migrations.
   *
   * @return list of pending migrations
   */
  public List<Migration> getPendingMigrations() {
    int currentVersion = getCurrentVersion();
    return migrations.stream()
        .filter(m -> m.getVersion() > currentVersion)
        .toList();
  }

  /**
   * Validates that applied migrations haven't been modified.
   */
  private void validateAppliedMigrations() {
    logger.log(Level.DEBUG, "Validating applied migrations");

    List<Migration> appliedMigrations = executor.getAppliedMigrations();

    for (Migration applied : appliedMigrations) {
      Optional<Migration> current = migrations.stream()
          .filter(m -> m.getVersion() == applied.getVersion())
          .findFirst();

      if (current.isPresent()) {
        Migration currentMigration = current.get();
        if (!currentMigration.getChecksum().equals(applied.getChecksum())) {
          throw new IllegalStateException(
              "Migration V" + applied.getVersion() + " has been modified. " +
              "Expected checksum: " + applied.getChecksum() + ", " +
              "actual: " + currentMigration.getChecksum());
        }
      }
    }
  }

  /**
   * Rolls back a list of migrations.
   *
   * @param migrationsToRollback migrations to roll back
   * @return migration result
   */
  private MigrationResult rollbackMigrations(List<Migration> migrationsToRollback) {
    int rollbackCount = 0;

    for (Migration migration : migrationsToRollback) {
      if (!migration.hasRollbackScript()) {
        logger.log(Level.WARNING, "Migration V" + migration.getVersion() + " has no rollback script, skipping");
        continue;
      }

      try {
        logger.log(Level.INFO, "Rolling back migration V" + migration.getVersion());
        executor.rollback(migration);
        rollbackCount++;
      } catch (Exception e) {
        logger.log(Level.ERROR, "Rollback of V" + migration.getVersion() + " failed", e);
        return MigrationResult.failure(
            executor.getCurrentVersion(),
            rollbackCount,
            e
        );
      }
    }

    int newVersion = executor.getCurrentVersion();
    return MigrationResult.rollback(newVersion, rollbackCount);
  }

  /**
   * Creates a baseline at the current database state.
   *
   * @param version the baseline version
   * @param description the baseline description
   */
  public void baseline(int version, String description) {
    logger.log(Level.INFO, "Creating baseline at version " + version);

    Migration baseline = Migration.builder()
        .version(version)
        .description(description)
        .script("-- Baseline")
        .type(Migration.MigrationType.BASELINE)
        .executedAt(Instant.now())
        .executionTimeMs(0)
        .status(Migration.MigrationStatus.SUCCESS)
        .build();

    executor.recordMigration(baseline);
  }
}
