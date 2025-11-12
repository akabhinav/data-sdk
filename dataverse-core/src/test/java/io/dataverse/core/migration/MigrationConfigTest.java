package io.dataverse.core.migration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for MigrationConfig.
 *
 * @author DataVerse SDK Team
 */
class MigrationConfigTest {

  @Test
  void testDefaults() {
    MigrationConfig config = MigrationConfig.defaults();

    assertTrue(config.isValidateChecksums());
    assertTrue(config.isRollbackOnFailure());
    assertEquals("schema_migrations", config.getMigrationsTable());
    assertEquals("classpath:db/migrations", config.getMigrationsLocation());
    assertFalse(config.isBaselineOnMigrate());
    assertEquals(1, config.getBaselineVersion());
    assertFalse(config.isOutOfOrder());
    assertFalse(config.isCleanOnValidationError());
  }

  @Test
  void testBuilder_AllOptions() {
    MigrationConfig config = MigrationConfig.builder()
        .validateChecksums(false)
        .rollbackOnFailure(false)
        .migrationsTable("custom_migrations")
        .migrationsLocation("filesystem:/migrations")
        .baselineOnMigrate(true)
        .baselineVersion(5)
        .outOfOrder(true)
        .cleanOnValidationError(true)
        .build();

    assertFalse(config.isValidateChecksums());
    assertFalse(config.isRollbackOnFailure());
    assertEquals("custom_migrations", config.getMigrationsTable());
    assertEquals("filesystem:/migrations", config.getMigrationsLocation());
    assertTrue(config.isBaselineOnMigrate());
    assertEquals(5, config.getBaselineVersion());
    assertTrue(config.isOutOfOrder());
    assertTrue(config.isCleanOnValidationError());
  }

  @Test
  void testBuilder_ValidateChecksumsOnly() {
    MigrationConfig config = MigrationConfig.builder()
        .validateChecksums(false)
        .build();

    assertFalse(config.isValidateChecksums());
    // Other values should be defaults
    assertTrue(config.isRollbackOnFailure());
    assertEquals("schema_migrations", config.getMigrationsTable());
  }

  @Test
  void testBuilder_RollbackOnFailureOnly() {
    MigrationConfig config = MigrationConfig.builder()
        .rollbackOnFailure(false)
        .build();

    assertFalse(config.isRollbackOnFailure());
    assertTrue(config.isValidateChecksums());
  }

  @Test
  void testBuilder_CustomMigrationsTable() {
    MigrationConfig config = MigrationConfig.builder()
        .migrationsTable("my_custom_table")
        .build();

    assertEquals("my_custom_table", config.getMigrationsTable());
  }

  @Test
  void testBuilder_CustomMigrationsLocation() {
    MigrationConfig config = MigrationConfig.builder()
        .migrationsLocation("classpath:db/custom/migrations")
        .build();

    assertEquals("classpath:db/custom/migrations", config.getMigrationsLocation());
  }

  @Test
  void testBuilder_BaselineOnMigrate() {
    MigrationConfig config = MigrationConfig.builder()
        .baselineOnMigrate(true)
        .baselineVersion(10)
        .build();

    assertTrue(config.isBaselineOnMigrate());
    assertEquals(10, config.getBaselineVersion());
  }

  @Test
  void testBuilder_OutOfOrder() {
    MigrationConfig config = MigrationConfig.builder()
        .outOfOrder(true)
        .build();

    assertTrue(config.isOutOfOrder());
  }

  @Test
  void testBuilder_CleanOnValidationError() {
    MigrationConfig config = MigrationConfig.builder()
        .cleanOnValidationError(true)
        .build();

    assertTrue(config.isCleanOnValidationError());
  }

  @Test
  void testBuilder_MultipleBuilds() {
    MigrationConfig.Builder builder = MigrationConfig.builder()
        .validateChecksums(false)
        .rollbackOnFailure(false);

    MigrationConfig config1 = builder.build();
    MigrationConfig config2 = builder.build();

    // Both should have same configuration
    assertFalse(config1.isValidateChecksums());
    assertFalse(config2.isValidateChecksums());
    assertFalse(config1.isRollbackOnFailure());
    assertFalse(config2.isRollbackOnFailure());
  }

  @Test
  void testStrictMode() {
    MigrationConfig config = MigrationConfig.builder()
        .validateChecksums(true)
        .rollbackOnFailure(true)
        .outOfOrder(false)
        .cleanOnValidationError(false)
        .build();

    assertTrue(config.isValidateChecksums());
    assertTrue(config.isRollbackOnFailure());
    assertFalse(config.isOutOfOrder());
    assertFalse(config.isCleanOnValidationError());
  }

  @Test
  void testLenientMode() {
    MigrationConfig config = MigrationConfig.builder()
        .validateChecksums(false)
        .rollbackOnFailure(false)
        .outOfOrder(true)
        .cleanOnValidationError(true)
        .build();

    assertFalse(config.isValidateChecksums());
    assertFalse(config.isRollbackOnFailure());
    assertTrue(config.isOutOfOrder());
    assertTrue(config.isCleanOnValidationError());
  }
}
