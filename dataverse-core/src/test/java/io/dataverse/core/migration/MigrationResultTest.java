package io.dataverse.core.migration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for MigrationResult.
 *
 * @author DataVerse SDK Team
 */
class MigrationResultTest {

  @Test
  void testSuccess() {
    MigrationResult result = MigrationResult.success(1, 5, 4);

    assertTrue(result.isSuccess());
    assertEquals(1, result.getInitialVersion());
    assertEquals(5, result.getTargetVersion());
    assertEquals(4, result.getMigrationsApplied());
    assertEquals(MigrationResult.ResultType.SUCCESS, result.getResultType());
    assertTrue(result.getError().isEmpty());
    assertTrue(result.getErrorMessage().isEmpty());
  }

  @Test
  void testFailure() {
    Exception error = new RuntimeException("Database connection failed");
    MigrationResult result = MigrationResult.failure(2, 1, error);

    assertFalse(result.isSuccess());
    assertEquals(2, result.getInitialVersion());
    assertEquals(2, result.getTargetVersion());  // Same as initial on failure
    assertEquals(1, result.getMigrationsApplied());
    assertEquals(MigrationResult.ResultType.FAILURE, result.getResultType());
    assertTrue(result.getError().isPresent());
    assertEquals(error, result.getError().get());
    assertTrue(result.getErrorMessage().isPresent());
    assertEquals("Database connection failed", result.getErrorMessage().get());
  }

  @Test
  void testUpToDate() {
    MigrationResult result = MigrationResult.upToDate(10);

    assertTrue(result.isSuccess());
    assertEquals(10, result.getInitialVersion());
    assertEquals(10, result.getTargetVersion());
    assertEquals(0, result.getMigrationsApplied());
    assertEquals(MigrationResult.ResultType.UP_TO_DATE, result.getResultType());
    assertTrue(result.getError().isEmpty());
  }

  @Test
  void testRollback() {
    MigrationResult result = MigrationResult.rollback(3, 2);

    assertTrue(result.isSuccess());
    assertEquals(5, result.getInitialVersion());  // 3 + 2
    assertEquals(3, result.getTargetVersion());
    assertEquals(2, result.getMigrationsApplied());
    assertEquals(MigrationResult.ResultType.ROLLBACK, result.getResultType());
    assertTrue(result.getError().isEmpty());
  }

  @Test
  void testToString_Success() {
    MigrationResult result = MigrationResult.success(1, 5, 4);
    String str = result.toString();

    assertTrue(str.contains("success=true"));
    assertTrue(str.contains("type=SUCCESS"));
    assertTrue(str.contains("initialVersion=1"));
    assertTrue(str.contains("targetVersion=5"));
    assertTrue(str.contains("migrationsApplied=4"));
    assertFalse(str.contains("error="));
  }

  @Test
  void testToString_Failure() {
    Exception error = new RuntimeException("Test error");
    MigrationResult result = MigrationResult.failure(2, 1, error);
    String str = result.toString();

    assertTrue(str.contains("success=false"));
    assertTrue(str.contains("type=FAILURE"));
    assertTrue(str.contains("error=Test error"));
  }

  @Test
  void testToString_UpToDate() {
    MigrationResult result = MigrationResult.upToDate(10);
    String str = result.toString();

    assertTrue(str.contains("success=true"));
    assertTrue(str.contains("type=UP_TO_DATE"));
    assertTrue(str.contains("migrationsApplied=0"));
  }

  @Test
  void testToString_Rollback() {
    MigrationResult result = MigrationResult.rollback(3, 2);
    String str = result.toString();

    assertTrue(str.contains("success=true"));
    assertTrue(str.contains("type=ROLLBACK"));
    assertTrue(str.contains("migrationsApplied=2"));
  }

  @Test
  void testSuccess_NoMigrations() {
    MigrationResult result = MigrationResult.success(5, 5, 0);

    assertTrue(result.isSuccess());
    assertEquals(0, result.getMigrationsApplied());
  }

  @Test
  void testFailure_NoMigrationsApplied() {
    Exception error = new RuntimeException("First migration failed");
    MigrationResult result = MigrationResult.failure(1, 0, error);

    assertFalse(result.isSuccess());
    assertEquals(0, result.getMigrationsApplied());
  }

  @Test
  void testResultType_AllValues() {
    assertEquals(4, MigrationResult.ResultType.values().length);

    assertNotNull(MigrationResult.ResultType.SUCCESS);
    assertNotNull(MigrationResult.ResultType.FAILURE);
    assertNotNull(MigrationResult.ResultType.UP_TO_DATE);
    assertNotNull(MigrationResult.ResultType.ROLLBACK);
  }

  @Test
  void testError_NullSafe() {
    MigrationResult result = MigrationResult.success(1, 2, 1);

    assertTrue(result.getError().isEmpty());
    assertTrue(result.getErrorMessage().isEmpty());
  }

  @Test
  void testError_WithCause() {
    Exception cause = new IllegalStateException("Root cause");
    Exception error = new RuntimeException("Wrapper exception", cause);
    MigrationResult result = MigrationResult.failure(1, 0, error);

    assertTrue(result.getError().isPresent());
    assertEquals(error, result.getError().get());
    assertEquals(cause, result.getError().get().getCause());
  }
}
