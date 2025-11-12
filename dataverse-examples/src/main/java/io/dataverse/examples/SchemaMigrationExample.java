package io.dataverse.examples;

import io.dataverse.core.migration.Migration;
import io.dataverse.core.migration.MigrationConfig;
import io.dataverse.core.migration.MigrationLoader;
import io.dataverse.core.migration.MigrationManager;

import java.nio.file.Path;
import java.util.List;

/**
 * Example demonstrating Schema Migration & Versioning (Feature #16).
 *
 * <p>Use Cases:
 * <ul>
 *   <li>Database schema evolution</li>
 *   <li>Version control for database changes</li>
 *   <li>Team collaboration on schema changes</li>
 *   <li>Production deployment automation</li>
 * </ul>
 *
 * @author DataVerse SDK Team
 */
public class SchemaMigrationExample {

  public static void main(String[] args) {
    System.out.println("=== Schema Migration & Versioning Examples ===\n");

    example1_BasicMigration();
    example2_MigrationFiles();
    example3_RollbackSupport();
    example4_ValidationAndChecksums();
    example5_ProductionDeployment();
    example6_TeamCollaboration();
  }

  /**
   * Example 1: Basic Migration
   *
   * <p>Use Case: Initial database schema creation.
   */
  static void example1_BasicMigration() {
    System.out.println("Example 1: Basic Migration");
    System.out.println("Use Case: Create initial schema\n");

    Migration migration = Migration.builder()
        .version(1)
        .description("Create users table")
        .script("""
            CREATE TABLE users (
                id BIGSERIAL PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                email VARCHAR(255) NOT NULL UNIQUE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );

            CREATE INDEX idx_users_email ON users(email);
            """)
        .rollbackScript("""
            DROP INDEX IF EXISTS idx_users_email;
            DROP TABLE IF EXISTS users;
            """)
        .build();

    System.out.println("Migration Details:");
    System.out.println("  Version: " + migration.getVersion());
    System.out.println("  Description: " + migration.getDescription());
    System.out.println("  Has rollback: " + (migration.getRollbackScript() != null));

    System.out.println("\nExecution:");
    System.out.println("  ✓ Create users table with indexes");
    System.out.println("  ✓ Record in schema_migrations table");
    System.out.println("  ✓ Calculate checksum for validation");

    System.out.println("\nResult: Schema version 1 applied successfully\n");
    System.out.println("---\n");
  }

  /**
   * Example 2: Migration Files
   *
   * <p>Use Case: File-based migrations for version control.
   */
  static void example2_MigrationFiles() {
    System.out.println("Example 2: Migration Files");
    System.out.println("Use Case: Version control database changes\n");

    System.out.println("File structure:");
    System.out.println("  migrations/");
    System.out.println("    ├─ V1__Create_users_table.sql");
    System.out.println("    ├─ V2__Add_orders_table.sql");
    System.out.println("    ├─ V3__Add_user_status_column.sql");
    System.out.println("    ├─ V4__Create_products_table.sql");
    System.out.println("    └─ V5__Add_foreign_keys.sql");

    System.out.println("\nV1__Create_users_table.sql:");
    System.out.println("  CREATE TABLE users (...);");

    System.out.println("\nV2__Add_orders_table.sql:");
    System.out.println("  CREATE TABLE orders (");
    System.out.println("    id BIGSERIAL PRIMARY KEY,");
    System.out.println("    user_id BIGINT REFERENCES users(id),");
    System.out.println("    ...");
    System.out.println("  );");

    System.out.println("\nLoading migrations:");
    System.out.println("  MigrationLoader loader = new MigrationLoader();");
    System.out.println("  List<Migration> migrations = loader.loadFromDirectory(");
    System.out.println("      Path.of(\"migrations\")");
    System.out.println("  );");

    System.out.println("\nBenefits:");
    System.out.println("  ✓ Version controlled with Git");
    System.out.println("  ✓ Code review for schema changes");
    System.out.println("  ✓ Sequential execution guaranteed");
    System.out.println("  ✓ Team collaboration friendly");

    System.out.println("\nResult: Professional database change management\n");
    System.out.println("---\n");
  }

  /**
   * Example 3: Rollback Support
   *
   * <p>Use Case: Safely rollback problematic migrations.
   */
  static void example3_RollbackSupport() {
    System.out.println("Example 3: Rollback Support");
    System.out.println("Use Case: Rollback after production issue\n");

    System.out.println("Scenario:");
    System.out.println("  Current version: 10");
    System.out.println("  Applied V11: Added NOT NULL constraint");
    System.out.println("  Problem: Existing data has nulls!");
    System.out.println("  Solution: Rollback to version 10");

    Migration migration = Migration.builder()
        .version(11)
        .description("Add NOT NULL to user email")
        .script("ALTER TABLE users ALTER COLUMN email SET NOT NULL;")
        .rollbackScript("ALTER TABLE users ALTER COLUMN email DROP NOT NULL;")
        .build();

    System.out.println("\nRollback process:");
    System.out.println("  1. Execute rollback script:");
    System.out.println("     ALTER TABLE users ALTER COLUMN email DROP NOT NULL;");
    System.out.println("  2. Delete from schema_migrations WHERE version = 11");
    System.out.println("  3. Current version now: 10");

    System.out.println("\nCode:");
    System.out.println("  MigrationManager manager = new MigrationManager(...);");
    System.out.println("  manager.rollbackTo(10);");

    System.out.println("\nSafety features:");
    System.out.println("  ✓ Validates rollback script exists");
    System.out.println("  ✓ Runs in transaction");
    System.out.println("  ✓ Auto-rollback on error");
    System.out.println("  ✓ Audit trail maintained");

    System.out.println("\nResult: Safe recovery from schema issues\n");
    System.out.println("---\n");
  }

  /**
   * Example 4: Validation and Checksums
   *
   * <p>Use Case: Detect unauthorized schema changes.
   */
  static void example4_ValidationAndChecksums() {
    System.out.println("Example 4: Validation and Checksums");
    System.out.println("Use Case: Ensure migration integrity\n");

    MigrationConfig config = MigrationConfig.builder()
        .validateChecksums(true)
        .failOnMissingMigrations(true)
        .build();

    System.out.println("Configuration:");
    System.out.println("  validateChecksums: true");
    System.out.println("  failOnMissingMigrations: true");

    System.out.println("\nScenario 1: Modified migration detected");
    System.out.println("  Database has V1 with checksum: ABC123");
    System.out.println("  Current V1 file has checksum: XYZ789");
    System.out.println("  → ERROR: Migration V1 was modified!");
    System.out.println("  → Prevents corrupted schema");

    System.out.println("\nScenario 2: Missing migration");
    System.out.println("  Database has: V1, V2, V3, V5");
    System.out.println("  Files have: V1, V2, V3, V4, V5");
    System.out.println("  → ERROR: V4 missing from database!");
    System.out.println("  → Prevents inconsistent state");

    System.out.println("\nScenario 3: All valid");
    System.out.println("  All checksums match");
    System.out.println("  All migrations present");
    System.out.println("  → ✓ Schema validated successfully");

    System.out.println("\nBenefits:");
    System.out.println("  ✓ Detect manual schema changes");
    System.out.println("  ✓ Ensure migration order");
    System.out.println("  ✓ Prevent production issues");
    System.out.println("  ✓ Audit compliance");

    System.out.println("\nResult: Guaranteed schema integrity\n");
    System.out.println("---\n");
  }

  /**
   * Example 5: Production Deployment
   *
   * <p>Use Case: Automated production database updates.
   */
  static void example5_ProductionDeployment() {
    System.out.println("Example 5: Production Deployment");
    System.out.println("Use Case: CI/CD pipeline database updates\n");

    System.out.println("Deployment workflow:");
    System.out.println("  1. Application starts");
    System.out.println("  2. MigrationManager checks current version");
    System.out.println("  3. Finds pending migrations");
    System.out.println("  4. Executes migrations in transaction");
    System.out.println("  5. Application continues startup");

    System.out.println("\nCode (application startup):");
    System.out.println("  @PostConstruct");
    System.out.println("  public void runMigrations() {");
    System.out.println("      MigrationManager manager = new MigrationManager(");
    System.out.println("          dataSource,");
    System.out.println("          migrations,");
    System.out.println("          MigrationConfig.production()");
    System.out.println("      );");
    System.out.println("      manager.migrate();");
    System.out.println("  }");

    System.out.println("\nProduction config:");
    MigrationConfig prodConfig = MigrationConfig.builder()
        .validateChecksums(true)
        .failOnMissingMigrations(true)
        .transactional(true)
        .baselineVersion(1)
        .build();

    System.out.println("  ✓ Checksum validation enabled");
    System.out.println("  ✓ Missing migration detection");
    System.out.println("  ✓ Transactional execution");
    System.out.println("  ✓ Baseline version tracking");

    System.out.println("\nExecution log:");
    System.out.println("  [INFO] Current database version: 8");
    System.out.println("  [INFO] Found 2 pending migrations (9, 10)");
    System.out.println("  [INFO] Executing V9__Add_customer_index.sql");
    System.out.println("  [INFO] ✓ Migration V9 applied (125ms)");
    System.out.println("  [INFO] Executing V10__Add_audit_table.sql");
    System.out.println("  [INFO] ✓ Migration V10 applied (89ms)");
    System.out.println("  [INFO] Database is up to date (version 10)");

    System.out.println("\nResult: Zero-downtime automated deployments\n");
    System.out.println("---\n");
  }

  /**
   * Example 6: Team Collaboration
   *
   * <p>Use Case: Multiple developers working on schema changes.
   */
  static void example6_TeamCollaboration() {
    System.out.println("Example 6: Team Collaboration");
    System.out.println("Use Case: Parallel feature development\n");

    System.out.println("Scenario:");
    System.out.println("  Developer A (feature-payments):");
    System.out.println("    Creates V11__Add_payments_table.sql");
    System.out.println("    Commits to Git");
    System.out.println("");
    System.out.println("  Developer B (feature-notifications):");
    System.out.println("    Creates V12__Add_notifications_table.sql");
    System.out.println("    Commits to Git");
    System.out.println("");
    System.out.println("  Developer C (main branch):");
    System.out.println("    Merges both features");
    System.out.println("    Migrations run in order: V11, V12");

    System.out.println("\nConflict resolution:");
    System.out.println("  Problem: Both create V11");
    System.out.println("  Solution: Developer B renames to V12");
    System.out.println("  Tool: migration-version-check.sh in CI");

    System.out.println("\nCI/CD validation:");
    System.out.println("  ✓ Check for duplicate version numbers");
    System.out.println("  ✓ Validate SQL syntax");
    System.out.println("  ✓ Run on test database");
    System.out.println("  ✓ Generate rollback scripts");

    System.out.println("\nBest practices:");
    System.out.println("  1. One migration per feature");
    System.out.println("  2. Descriptive migration names");
    System.out.println("  3. Always include rollback");
    System.out.println("  4. Test on staging first");
    System.out.println("  5. Code review all migrations");

    System.out.println("\nResult: Smooth team collaboration on schema changes\n");
    System.out.println("---\n");
  }

  /**
   * Example 7: Complex Migration Scenario
   */
  static class CompleteExample {
    public static void run() {
      System.out.println("Complete Migration Example:");
      System.out.println("Scenario: Startup company evolving database\n");

      // Load migrations from files
      MigrationLoader loader = new MigrationLoader();
      List<Migration> migrations = List.of(
          Migration.builder()
              .version(1)
              .description("Create initial schema")
              .script("CREATE TABLE users (...)")
              .build(),
          Migration.builder()
              .version(2)
              .description("Add orders table")
              .script("CREATE TABLE orders (...)")
              .build(),
          Migration.builder()
              .version(3)
              .description("Add products table")
              .script("CREATE TABLE products (...)")
              .build()
      );

      // Configure migration manager
      MigrationConfig config = MigrationConfig.builder()
          .validateChecksums(true)
          .failOnMissingMigrations(true)
          .transactional(true)
          .build();

      // Create manager (would use real datasource)
      System.out.println("Loaded 3 migrations");
      System.out.println("Configuration: production mode");
      System.out.println("\nReady to migrate!");
    }
  }
}
