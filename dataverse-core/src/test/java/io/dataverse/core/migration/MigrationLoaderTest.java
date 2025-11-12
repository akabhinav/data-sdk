package io.dataverse.core.migration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for MigrationLoader.
 *
 * @author DataVerse SDK Team
 */
class MigrationLoaderTest {

  private final MigrationLoader loader = new MigrationLoader();

  @Test
  void testParseFilename_Valid() {
    String[] result = MigrationLoader.parseFilename("V001__create_users_table.sql");

    assertNotNull(result);
    assertEquals(2, result.length);
    assertEquals("001", result[0]);
    assertEquals("create users table", result[1]);
  }

  @Test
  void testParseFilename_ValidWithNumbers() {
    String[] result = MigrationLoader.parseFilename("V042__add_user_table_v2.sql");

    assertNotNull(result);
    assertEquals("042", result[0]);
    assertEquals("add user table v2", result[1]);
  }

  @Test
  void testParseFilename_Invalid_NoVersion() {
    String[] result = MigrationLoader.parseFilename("create_users_table.sql");

    assertNull(result);
  }

  @Test
  void testParseFilename_Invalid_NoDescription() {
    String[] result = MigrationLoader.parseFilename("V001.sql");

    assertNull(result);
  }

  @Test
  void testParseFilename_Invalid_WrongExtension() {
    String[] result = MigrationLoader.parseFilename("V001__create_users_table.txt");

    assertNull(result);
  }

  @Test
  void testParseFilename_Invalid_SpecialChars() {
    String[] result = MigrationLoader.parseFilename("V001__create-users-table.sql");

    assertNull(result);
  }

  @Test
  void testIsValidMigrationFilename_Valid() {
    assertTrue(MigrationLoader.isValidMigrationFilename("V001__create_users_table.sql"));
    assertTrue(MigrationLoader.isValidMigrationFilename("V1__initial.sql"));
    assertTrue(MigrationLoader.isValidMigrationFilename("V999__final_migration.sql"));
  }

  @Test
  void testIsValidMigrationFilename_Invalid() {
    assertFalse(MigrationLoader.isValidMigrationFilename("create_users.sql"));
    assertFalse(MigrationLoader.isValidMigrationFilename("V001_create_users.sql"));  // Single underscore
    assertFalse(MigrationLoader.isValidMigrationFilename("V001.sql"));
    assertFalse(MigrationLoader.isValidMigrationFilename("001__create_users.sql"));  // No V prefix
  }

  @Test
  void testIsRollbackFilename_Valid() {
    assertTrue(MigrationLoader.isRollbackFilename("V001__create_users_table.rollback.sql"));
    assertTrue(MigrationLoader.isRollbackFilename("V42__add_index.rollback.sql"));
  }

  @Test
  void testIsRollbackFilename_Invalid() {
    assertFalse(MigrationLoader.isRollbackFilename("V001__create_users_table.sql"));
    assertFalse(MigrationLoader.isRollbackFilename("V001__rollback.sql"));
  }

  @Test
  void testParseFilename_MultipleUnderscores() {
    String[] result = MigrationLoader.parseFilename("V001__create_user_profile_table.sql");

    assertNotNull(result);
    assertEquals("create user profile table", result[1]);
  }

  @Test
  void testParseFilename_LeadingZeros() {
    String[] result1 = MigrationLoader.parseFilename("V001__test.sql");
    String[] result2 = MigrationLoader.parseFilename("V01__test.sql");
    String[] result3 = MigrationLoader.parseFilename("V1__test.sql");

    assertNotNull(result1);
    assertNotNull(result2);
    assertNotNull(result3);

    assertEquals("001", result1[0]);
    assertEquals("01", result2[0]);
    assertEquals("1", result3[0]);
  }
}
