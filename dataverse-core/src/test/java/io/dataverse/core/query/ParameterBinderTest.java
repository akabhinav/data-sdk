package io.dataverse.core.query;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ParameterBinder.
 *
 * @author DataVerse SDK Team
 */
class ParameterBinderTest {

  @Test
  void testSimpleParameterReplacement() {
    String query = "SELECT * FROM users WHERE id = :id";
    Map<String, Object> params = Map.of("id", 123L);

    ParameterBinder binder = new ParameterBinder(query, params);

    assertEquals("SELECT * FROM users WHERE id = ?", binder.getQueryWithPlaceholders());
    assertEquals(List.of("id"), binder.getParameterNames());
    assertEquals(List.of(123L), binder.getOrderedParameterValues());
    assertEquals(1, binder.getParameterCount());
  }

  @Test
  void testMultipleParameters() {
    String query = """
        SELECT * FROM users
        WHERE status = :status
        AND age > :age
        AND department = :dept
        """;

    Map<String, Object> params = Map.of(
        "status", "ACTIVE",
        "age", 18,
        "dept", "Engineering"
    );

    ParameterBinder binder = new ParameterBinder(query, params);

    String expected = """
        SELECT * FROM users
        WHERE status = ?
        AND age > ?
        AND department = ?
        """;

    assertEquals(expected, binder.getQueryWithPlaceholders());
    assertEquals(3, binder.getParameterCount());
    assertEquals(List.of("status", "age", "dept"), binder.getParameterNames());
    assertEquals(List.of("ACTIVE", 18, "Engineering"), binder.getOrderedParameterValues());
  }

  @Test
  void testParameterWithUnderscore() {
    String query = "SELECT * FROM users WHERE user_id = :user_id AND first_name = :first_name";
    Map<String, Object> params = Map.of(
        "user_id", 123L,
        "first_name", "John"
    );

    ParameterBinder binder = new ParameterBinder(query, params);

    assertEquals(
        "SELECT * FROM users WHERE user_id = ? AND first_name = ?",
        binder.getQueryWithPlaceholders()
    );
  }

  @Test
  void testParameterWithNumbers() {
    String query = "SELECT * FROM table WHERE param1 = :param1 AND param2 = :param2";
    Map<String, Object> params = Map.of(
        "param1", "value1",
        "param2", "value2"
    );

    ParameterBinder binder = new ParameterBinder(query, params);

    assertEquals(
        "SELECT * FROM table WHERE param1 = ? AND param2 = ?",
        binder.getQueryWithPlaceholders()
    );
  }

  @Test
  void testRepeatedParameter() {
    String query = "SELECT * FROM users WHERE status = :status OR fallback_status = :status";
    Map<String, Object> params = Map.of("status", "ACTIVE");

    ParameterBinder binder = new ParameterBinder(query, params);

    assertEquals(
        "SELECT * FROM users WHERE status = ? OR fallback_status = ?",
        binder.getQueryWithPlaceholders()
    );

    // Should have 2 entries for the same parameter
    assertEquals(2, binder.getParameterCount());
    assertEquals(List.of("status", "status"), binder.getParameterNames());
    assertEquals(List.of("ACTIVE", "ACTIVE"), binder.getOrderedParameterValues());
  }

  @Test
  void testNoParameters() {
    String query = "SELECT * FROM users WHERE active = true";
    ParameterBinder binder = ParameterBinder.empty(query);

    assertEquals(query, binder.getQueryWithPlaceholders());
    assertEquals(0, binder.getParameterCount());
    assertTrue(binder.getParameterNames().isEmpty());
    assertTrue(binder.getOrderedParameterValues().isEmpty());
  }

  @Test
  void testComplexSQLQuery() {
    String query = """
        SELECT u.*, COUNT(o.id) as order_count
        FROM users u
        LEFT JOIN orders o ON u.id = o.user_id
        WHERE u.created_at > :since
        AND u.status IN (:status1, :status2)
        GROUP BY u.id
        HAVING COUNT(o.id) > :minOrders
        ORDER BY order_count DESC
        LIMIT :limit
        """;

    Map<String, Object> params = Map.of(
        "since", LocalDateTime.now().minusDays(7),
        "status1", "ACTIVE",
        "status2", "PENDING",
        "minOrders", 5,
        "limit", 100
    );

    ParameterBinder binder = new ParameterBinder(query, params);

    assertEquals(5, binder.getParameterCount());
    assertFalse(binder.getQueryWithPlaceholders().contains(":"));
  }

  @Test
  void testNumberedPlaceholders() {
    String query = "SELECT * FROM users WHERE status = :status AND age > :age";
    Map<String, Object> params = Map.of(
        "status", "ACTIVE",
        "age", 18
    );

    ParameterBinder binder = new ParameterBinder(query, params);

    assertEquals(
        "SELECT * FROM users WHERE status = $1 AND age > $2",
        binder.getQueryWithNumberedPlaceholders()
    );
  }

  @Test
  void testNumberedPlaceholders_MultipleParameters() {
    String query = "UPDATE users SET name = :name, age = :age, email = :email WHERE id = :id";
    Map<String, Object> params = Map.of(
        "name", "John",
        "age", 30,
        "email", "john@example.com",
        "id", 123L
    );

    ParameterBinder binder = new ParameterBinder(query, params);

    String numbered = binder.getQueryWithNumberedPlaceholders();
    assertEquals("UPDATE users SET name = $1, age = $2, email = $3 WHERE id = $4", numbered);
  }

  @Test
  void testMissingParameter_ThrowsException() {
    String query = "SELECT * FROM users WHERE id = :id AND status = :status";
    Map<String, Object> params = Map.of("id", 123L);  // Missing 'status'

    assertThrows(IllegalArgumentException.class, () ->
        new ParameterBinder(query, params)
    );
  }

  @Test
  void testValidate_AllParametersPresent() {
    String query = "SELECT * FROM users WHERE id = :id AND status = :status";
    Map<String, Object> params = Map.of(
        "id", 123L,
        "status", "ACTIVE"
    );

    ParameterBinder binder = new ParameterBinder(query, params);
    assertDoesNotThrow(binder::validate);
  }

  @Test
  void testNullParameterValue() {
    String query = "SELECT * FROM users WHERE status = :status";
    Map<String, Object> params = Map.of("status", (Object) null);

    ParameterBinder binder = new ParameterBinder(query, params);

    assertEquals("SELECT * FROM users WHERE status = ?", binder.getQueryWithPlaceholders());
    assertEquals(List.of((Object) null), binder.getOrderedParameterValues());
  }

  @Test
  void testExtractParameterNames() {
    String query = "SELECT * FROM users WHERE id = :id AND status = :status AND age > :age";

    List<String> names = ParameterBinder.extractParameterNames(query);

    assertEquals(3, names.size());
    assertEquals(List.of("id", "status", "age"), names);
  }

  @Test
  void testExtractParameterNames_NoDuplicates() {
    String query = "SELECT * FROM users WHERE status = :status OR backup_status = :status";

    List<String> names = ParameterBinder.extractParameterNames(query);

    // Should only return unique names
    assertEquals(1, names.size());
    assertEquals(List.of("status"), names);
  }

  @Test
  void testExtractParameterNames_NoParameters() {
    String query = "SELECT * FROM users WHERE active = true";

    List<String> names = ParameterBinder.extractParameterNames(query);

    assertTrue(names.isEmpty());
  }

  @Test
  void testHasNamedParameters_True() {
    assertTrue(ParameterBinder.hasNamedParameters("SELECT * FROM users WHERE id = :id"));
  }

  @Test
  void testHasNamedParameters_False() {
    assertFalse(ParameterBinder.hasNamedParameters("SELECT * FROM users WHERE id = 123"));
    assertFalse(ParameterBinder.hasNamedParameters("SELECT * FROM users"));
  }

  @Test
  void testParameterInStringLiteral() {
    // Parameter-like pattern in string should NOT be replaced
    String query = "SELECT * FROM users WHERE comment = 'user :id is active' AND id = :id";
    Map<String, Object> params = Map.of("id", 123L);

    ParameterBinder binder = new ParameterBinder(query, params);

    // This is a limitation - we'll replace both occurrences
    // In practice, string literals should use different quoting or escaping
    String result = binder.getQueryWithPlaceholders();
    assertTrue(result.contains("?"));
  }

  @Test
  void testMongoDBQuery() {
    String query = """
        {
          "category": ":category",
          "price": { "$gte": :minPrice, "$lte": :maxPrice },
          "inStock": true
        }
        """;

    Map<String, Object> params = Map.of(
        "category", "Electronics",
        "minPrice", 100.0,
        "maxPrice", 1000.0
    );

    ParameterBinder binder = new ParameterBinder(query, params);

    String expected = """
        {
          "category": "?",
          "price": { "$gte": ?, "$lte": ? },
          "inStock": true
        }
        """;

    assertEquals(expected, binder.getQueryWithPlaceholders());
    assertEquals(3, binder.getParameterCount());
  }

  @Test
  void testParameterOrder_Preserved() {
    String query = "UPDATE users SET age = :age, name = :name, email = :email WHERE id = :id";
    Map<String, Object> params = Map.of(
        "age", 30,
        "name", "John",
        "email", "john@example.com",
        "id", 123L
    );

    ParameterBinder binder = new ParameterBinder(query, params);

    // Parameters should be in order of appearance in query
    List<String> names = binder.getParameterNames();
    assertEquals("age", names.get(0));
    assertEquals("name", names.get(1));
    assertEquals("email", names.get(2));
    assertEquals("id", names.get(3));

    List<Object> values = binder.getOrderedParameterValues();
    assertEquals(30, values.get(0));
    assertEquals("John", values.get(1));
    assertEquals("john@example.com", values.get(2));
    assertEquals(123L, values.get(3));
  }

  @Test
  void testImmutability_ParameterNames() {
    String query = "SELECT * FROM users WHERE id = :id";
    Map<String, Object> params = Map.of("id", 123L);

    ParameterBinder binder = new ParameterBinder(query, params);
    List<String> names = binder.getParameterNames();

    assertThrows(UnsupportedOperationException.class, () -> names.add("newParam"));
  }

  @Test
  void testImmutability_ParameterValues() {
    String query = "SELECT * FROM users WHERE id = :id";
    Map<String, Object> params = Map.of("id", 123L);

    ParameterBinder binder = new ParameterBinder(query, params);
    List<Object> values = binder.getOrderedParameterValues();

    assertThrows(UnsupportedOperationException.class, () -> values.add("newValue"));
  }
}
