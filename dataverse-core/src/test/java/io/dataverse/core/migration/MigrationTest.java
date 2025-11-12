package io.dataverse.core.migration;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Migration.
 *
 * @author DataVerse SDK Team
 */
class MigrationTest {

  @Test
  void testBuilder_Minimal() {
    Migration migration = Migration.builder()
        .version(1)
        .description("Create users table")
        .script("CREATE TABLE users (...)")
        .build();

    assertEquals(1, migration.getVersion());
    assertEquals("Create users table", migration.getDescription());
    assertEquals("CREATE TABLE users (...)", migration.getScript());
    assertNull(migration.getRollbackScript());
    assertEquals(Migration.MigrationType.SQL, migration.getType());
    assertEquals(Migration.MigrationStatus.PENDING, migration.getStatus());
    assertFalse(migration.hasRollbackScript());
  }

  @Test
  void testBuilder_WithRollback() {
    Migration migration = Migration.builder()
        .version(1)
        .description("Create users table")
        .script("CREATE TABLE users (...)")
        .rollbackScript("DROP TABLE users")
        .build();

    assertTrue(migration.hasRollbackScript());
    assertEquals("DROP TABLE users", migration.getRollbackScript());
  }

  @Test
  void testBuilder_FullConfiguration() {
    Instant now = Instant.now();

    Migration migration = Migration.builder()
        .version(5)
        .description("Add email index")
        .script("CREATE INDEX idx_email ON users(email)")
        .rollbackScript("DROP INDEX idx_email")
        .checksum("abc123")
        .type(Migration.MigrationType.SQL)
        .executedAt(now)
        .executionTimeMs(250)
        .status(Migration.MigrationStatus.SUCCESS)
        .build();

    assertEquals(5, migration.getVersion());
    assertEquals("Add email index", migration.getDescription());
    assertEquals("abc123", migration.getChecksum());
    assertEquals(Migration.MigrationType.SQL, migration.getType());
    assertEquals(now, migration.getExecutedAt());
    assertEquals(250, migration.getExecutionTimeMs());
    assertEquals(Migration.MigrationStatus.SUCCESS, migration.getStatus());
  }

  @Test
  void testBuilder_InvalidVersion() {
    assertThrows(IllegalArgumentException.class, () ->
        Migration.builder()
            .version(0)  // Invalid
            .description("Test")
            .script("CREATE TABLE test")
            .build()
    );

    assertThrows(IllegalArgumentException.class, () ->
        Migration.builder()
            .version(-1)  // Invalid
            .description("Test")
            .script("CREATE TABLE test")
            .build()
    );
  }

  @Test
  void testBuilder_MissingDescription() {
    assertThrows(IllegalArgumentException.class, () ->
        Migration.builder()
            .version(1)
            .script("CREATE TABLE test")
            .build()
    );
  }

  @Test
  void testBuilder_BlankDescription() {
    assertThrows(IllegalArgumentException.class, () ->
        Migration.builder()
            .version(1)
            .description("   ")  // Blank
            .script("CREATE TABLE test")
            .build()
    );
  }

  @Test
  void testBuilder_MissingScript() {
    assertThrows(IllegalArgumentException.class, () ->
        Migration.builder()
            .version(1)
            .description("Test migration")
            .build()
    );
  }

  @Test
  void testBuilder_BlankScript() {
    assertThrows(IllegalArgumentException.class, () ->
        Migration.builder()
            .version(1)
            .description("Test migration")
            .script("   ")  // Blank
            .build()
    );
  }

  @Test
  void testChecksum_AutomaticCalculation() {
    Migration migration1 = Migration.builder()
        .version(1)
        .description("Test")
        .script("CREATE TABLE users (id BIGINT)")
        .build();

    Migration migration2 = Migration.builder()
        .version(2)
        .description("Test")
        .script("CREATE TABLE users (id BIGINT)")  // Same script
        .build();

    Migration migration3 = Migration.builder()
        .version(3)
        .description("Test")
        .script("CREATE TABLE products (id BIGINT)")  // Different script
        .build();

    // Same script should have same checksum
    assertEquals(migration1.getChecksum(), migration2.getChecksum());

    // Different script should have different checksum
    assertNotEquals(migration1.getChecksum(), migration3.getChecksum());
  }

  @Test
  void testChecksum_ManualOverride() {
    Migration migration = Migration.builder()
        .version(1)
        .description("Test")
        .script("CREATE TABLE users")
        .checksum("custom-checksum")
        .build();

    assertEquals("custom-checksum", migration.getChecksum());
  }

  @Test
  void testHasRollbackScript_True() {
    Migration migration = Migration.builder()
        .version(1)
        .description("Test")
        .script("CREATE TABLE users")
        .rollbackScript("DROP TABLE users")
        .build();

    assertTrue(migration.hasRollbackScript());
  }

  @Test
  void testHasRollbackScript_False() {
    Migration migration = Migration.builder()
        .version(1)
        .description("Test")
        .script("CREATE TABLE users")
        .build();

    assertFalse(migration.hasRollbackScript());
  }

  @Test
  void testHasRollbackScript_Blank() {
    Migration migration = Migration.builder()
        .version(1)
        .description("Test")
        .script("CREATE TABLE users")
        .rollbackScript("   ")  // Blank
        .build();

    assertFalse(migration.hasRollbackScript());
  }

  @Test
  void testEquals_SameVersion() {
    Migration migration1 = Migration.builder()
        .version(1)
        .description("Test 1")
        .script("CREATE TABLE users")
        .build();

    Migration migration2 = Migration.builder()
        .version(1)
        .description("Test 2")  // Different description
        .script("CREATE TABLE products")  // Different script
        .build();

    // Equality based on version only
    assertEquals(migration1, migration2);
    assertEquals(migration1.hashCode(), migration2.hashCode());
  }

  @Test
  void testEquals_DifferentVersion() {
    Migration migration1 = Migration.builder()
        .version(1)
        .description("Test")
        .script("CREATE TABLE users")
        .build();

    Migration migration2 = Migration.builder()
        .version(2)
        .description("Test")
        .script("CREATE TABLE users")
        .build();

    assertNotEquals(migration1, migration2);
  }

  @Test
  void testToString() {
    Migration migration = Migration.builder()
        .version(5)
        .description("Add email index")
        .script("CREATE INDEX idx_email ON users(email)")
        .type(Migration.MigrationType.SQL)
        .status(Migration.MigrationStatus.SUCCESS)
        .build();

    String str = migration.toString();

    assertTrue(str.contains("version=5"));
    assertTrue(str.contains("Add email index"));
    assertTrue(str.contains("SQL"));
    assertTrue(str.contains("SUCCESS"));
  }

  @Test
  void testMigrationType_Values() {
    assertEquals(3, Migration.MigrationType.values().length);

    assertNotNull(Migration.MigrationType.SQL);
    assertNotNull(Migration.MigrationType.JAVA);
    assertNotNull(Migration.MigrationType.BASELINE);
  }

  @Test
  void testMigrationStatus_Values() {
    assertEquals(5, Migration.MigrationStatus.values().length);

    assertNotNull(Migration.MigrationStatus.PENDING);
    assertNotNull(Migration.MigrationStatus.EXECUTING);
    assertNotNull(Migration.MigrationStatus.SUCCESS);
    assertNotNull(Migration.MigrationStatus.FAILED);
    assertNotNull(Migration.MigrationStatus.ROLLED_BACK);
  }

  @Test
  void testJavaBasedMigration() {
    Migration migration = Migration.builder()
        .version(1)
        .description("Custom Java migration")
        .script("JavaMigration")  // Class name or identifier
        .type(Migration.MigrationType.JAVA)
        .build();

    assertEquals(Migration.MigrationType.JAVA, migration.getType());
  }

  @Test
  void testBaselineMigration() {
    Migration migration = Migration.builder()
        .version(1)
        .description("Baseline")
        .script("-- Baseline")
        .type(Migration.MigrationType.BASELINE)
        .build();

    assertEquals(Migration.MigrationType.BASELINE, migration.getType());
  }
}
