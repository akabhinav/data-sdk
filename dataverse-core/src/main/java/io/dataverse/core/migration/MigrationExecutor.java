package io.dataverse.core.migration;

import java.util.List;

/**
 * Interface for executing database migrations.
 *
 * <p>Implementations of this interface are database-specific and handle
 * the actual execution of migration scripts against the database.
 *
 * <p><strong>Responsibilities:</strong>
 * <ul>
 *   <li>Ensure migration tracking table exists</li>
 *   <li>Execute migration scripts</li>
 *   <li>Execute rollback scripts</li>
 *   <li>Record migration history</li>
 *   <li>Query migration status</li>
 * </ul>
 *
 * <p><strong>Implementation Example (SQL):</strong>
 * <pre>{@code
 * public class JdbcMigrationExecutor implements MigrationExecutor {
 *     private final DataSource dataSource;
 *
 *     @Override
 *     public void execute(Migration migration) {
 *         try (Connection conn = dataSource.getConnection();
 *              Statement stmt = conn.createStatement()) {
 *             stmt.execute(migration.getScript());
 *         }
 *     }
 *
 *     @Override
 *     public void ensureMigrationTableExists() {
 *         try (Connection conn = dataSource.getConnection();
 *              Statement stmt = conn.createStatement()) {
 *             stmt.execute("""
 *                 CREATE TABLE IF NOT EXISTS schema_migrations (
 *                     version INT PRIMARY KEY,
 *                     description VARCHAR(255) NOT NULL,
 *                     script_checksum VARCHAR(64) NOT NULL,
 *                     executed_at TIMESTAMP NOT NULL,
 *                     execution_time_ms BIGINT NOT NULL,
 *                     status VARCHAR(32) NOT NULL
 *                 )
 *                 """);
 *         }
 *     }
 *     // ... other methods
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface MigrationExecutor {

  /**
   * Ensures the migration tracking table exists.
   *
   * <p>Creates the table if it doesn't exist. The table should store:
   * <ul>
   *   <li>version - migration version number</li>
   *   <li>description - human-readable description</li>
   *   <li>script_checksum - checksum of the migration script</li>
   *   <li>executed_at - timestamp when migration was executed</li>
   *   <li>execution_time_ms - how long the migration took</li>
   *   <li>status - SUCCESS, FAILED, ROLLED_BACK</li>
   * </ul>
   */
  void ensureMigrationTableExists();

  /**
   * Executes a migration script.
   *
   * @param migration the migration to execute
   * @throws MigrationException if execution fails
   */
  void execute(Migration migration);

  /**
   * Executes a rollback script.
   *
   * @param migration the migration to roll back
   * @throws MigrationException if rollback fails
   */
  void rollback(Migration migration);

  /**
   * Records a migration in the tracking table.
   *
   * @param migration the migration to record
   */
  void recordMigration(Migration migration);

  /**
   * Gets the current database schema version.
   *
   * <p>Returns the highest successfully applied migration version,
   * or 0 if no migrations have been applied.
   *
   * @return current version
   */
  int getCurrentVersion();

  /**
   * Gets all applied migrations.
   *
   * @return list of applied migrations, ordered by version
   */
  List<Migration> getAppliedMigrations();

  /**
   * Checks if a migration has been applied.
   *
   * @param version the migration version
   * @return true if applied
   */
  boolean isMigrationApplied(int version);

  /**
   * Validates database connection and permissions.
   *
   * @throws MigrationException if validation fails
   */
  default void validateConnection() {
    // Default implementation does nothing
  }
}
