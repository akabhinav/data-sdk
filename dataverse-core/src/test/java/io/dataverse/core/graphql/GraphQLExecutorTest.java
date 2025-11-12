package io.dataverse.core.graphql;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for GraphQLExecutor.
 *
 * @author DataVerse SDK Team
 */
class GraphQLExecutorTest {

  private GraphQLSchema schema;
  private GraphQLExecutor executor;

  @BeforeEach
  void setUp() {
    schema = GraphQLSchemaGenerator.generate(User.class);
    executor = new GraphQLExecutor(schema);
  }

  @Test
  void testExecuteQuery() {
    // Register resolver
    executor.registerResolver("User", (fieldName, arguments) -> {
      if (fieldName.equals("user")) {
        Long id = (Long) arguments.get("id");
        return new User(id, "John Doe", "john@example.com");
      }
      return null;
    });

    // Execute query
    GraphQLResult result = executor.execute(
        "query { user(id: 1) }",
        Collections.emptyMap()
    );

    assertTrue(result.isSuccessful());
    assertFalse(result.hasErrors());

    Map<String, Object> data = result.getData();
    assertNotNull(data);
    assertTrue(data.containsKey("user"));

    User user = (User) data.get("user");
    assertEquals(1L, user.getId());
    assertEquals("John Doe", user.getName());
  }

  @Test
  void testExecuteListQuery() {
    // Register resolver
    executor.registerResolver("User", (fieldName, arguments) -> {
      if (fieldName.equals("users")) {
        return List.of(
            new User(1L, "Alice", "alice@example.com"),
            new User(2L, "Bob", "bob@example.com")
        );
      }
      return null;
    });

    // Execute query
    GraphQLResult result = executor.execute(
        "query { users }",
        Collections.emptyMap()
    );

    assertTrue(result.isSuccessful());

    @SuppressWarnings("unchecked")
    List<User> users = (List<User>) result.getData().get("users");
    assertEquals(2, users.size());
    assertEquals("Alice", users.get(0).getName());
    assertEquals("Bob", users.get(1).getName());
  }

  @Test
  void testExecuteQueryWithArguments() {
    // Register resolver
    executor.registerResolver("User", (fieldName, arguments) -> {
      if (fieldName.equals("users")) {
        Integer limit = (Integer) arguments.get("limit");
        Integer offset = (Integer) arguments.get("offset");

        assertNotNull(limit);
        assertNotNull(offset);
        assertEquals(10, limit);
        assertEquals(0, offset);

        return List.of(new User(1L, "Test", "test@example.com"));
      }
      return null;
    });

    // Execute query with arguments
    GraphQLResult result = executor.execute(
        "query { users(limit: 10, offset: 0) }",
        Collections.emptyMap()
    );

    assertTrue(result.isSuccessful());
  }

  @Test
  void testExecuteQueryWithVariables() {
    // Register resolver
    executor.registerResolver("User", (fieldName, arguments) -> {
      if (fieldName.equals("user")) {
        Long id = (Long) arguments.get("id");
        return new User(id, "Variable User", "var@example.com");
      }
      return null;
    });

    // Execute query with variable
    Map<String, Object> variables = Map.of("userId", 123L);
    GraphQLResult result = executor.execute(
        "query { user(id: $userId) }",
        variables
    );

    assertTrue(result.isSuccessful());

    User user = (User) result.getData().get("user");
    assertEquals(123L, user.getId());
  }

  @Test
  void testExecuteQueryWithoutResolver() {
    // Execute query without registering resolver
    GraphQLResult result = executor.execute(
        "query { user(id: 1) }",
        Collections.emptyMap()
    );

    assertTrue(result.hasErrors());
    assertFalse(result.isSuccessful());
    assertNull(result.getData());
  }

  @Test
  void testExecuteInvalidQuery() {
    GraphQLResult result = executor.execute(
        "invalid query",
        Collections.emptyMap()
    );

    assertTrue(result.hasErrors());
    assertFalse(result.isSuccessful());
  }

  @Test
  void testRegisterResolver() {
    GraphQLResolver<User> resolver = (fieldName, arguments) -> null;

    executor.registerResolver("User", resolver);

    Map<String, GraphQLResolver<?>> resolvers = executor.getResolvers();
    assertTrue(resolvers.containsKey("User"));
    assertEquals(resolver, resolvers.get("User"));
  }

  @Test
  void testUnregisterResolver() {
    GraphQLResolver<User> resolver = (fieldName, arguments) -> null;

    executor.registerResolver("User", resolver);
    assertTrue(executor.getResolvers().containsKey("User"));

    executor.unregisterResolver("User");
    assertFalse(executor.getResolvers().containsKey("User"));
  }

  @Test
  void testResolverException() {
    // Register resolver that throws exception
    executor.registerResolver("User", (fieldName, arguments) -> {
      throw new RuntimeException("Test exception");
    });

    GraphQLResult result = executor.execute(
        "query { user(id: 1) }",
        Collections.emptyMap()
    );

    assertTrue(result.hasErrors());
    assertFalse(result.isSuccessful());
  }

  @Test
  void testGetSchema() {
    assertEquals(schema, executor.getSchema());
  }

  @Test
  void testMultipleResolvers() {
    // Register multiple resolvers
    executor.registerResolver("User", (fieldName, arguments) ->
        new User(1L, "User1", "user1@example.com"));

    executor.registerResolver("Product", (fieldName, arguments) ->
        new Product(1L, "Product1", 99.99));

    Map<String, GraphQLResolver<?>> resolvers = executor.getResolvers();
    assertEquals(2, resolvers.size());
    assertTrue(resolvers.containsKey("User"));
    assertTrue(resolvers.containsKey("Product"));
  }

  // Test entities

  @GraphQLEntity
  static class User {
    private final Long id;
    private final String name;
    private final String email;

    User(Long id, String name, String email) {
      this.id = id;
      this.name = name;
      this.email = email;
    }

    public Long getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public String getEmail() {
      return email;
    }
  }

  @GraphQLEntity
  static class Product {
    private final Long id;
    private final String name;
    private final Double price;

    Product(Long id, String name, Double price) {
      this.id = id;
      this.name = name;
      this.price = price;
    }

    public Long getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public Double getPrice() {
      return price;
    }
  }
}
