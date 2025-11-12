package io.dataverse.core.graphql;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for GraphQL schema generation.
 *
 * @author DataVerse SDK Team
 */
class GraphQLSchemaGeneratorTest {

  @Test
  void testGenerateSchema() {
    GraphQLSchema schema = GraphQLSchemaGenerator.generate(TestUser.class);

    assertNotNull(schema);
    assertFalse(schema.getTypes().isEmpty());
    assertTrue(schema.getTypes().containsKey("TestUser"));
  }

  @Test
  void testGenerateTypeDefinition() {
    GraphQLSchema schema = GraphQLSchemaGenerator.generate(TestUser.class);

    GraphQLSchema.TypeDefinition userType = schema.getTypes().get("TestUser");
    assertNotNull(userType);
    assertEquals("TestUser", userType.getName());
    assertEquals("A test user entity", userType.getDescription());
  }

  @Test
  void testGenerateFields() {
    GraphQLSchema schema = GraphQLSchemaGenerator.generate(TestUser.class);

    GraphQLSchema.TypeDefinition userType = schema.getTypes().get("TestUser");
    List<GraphQLSchema.FieldDefinition> fields = userType.getFields();

    assertFalse(fields.isEmpty());

    // Check ID field
    GraphQLSchema.FieldDefinition idField = fields.stream()
        .filter(f -> f.getName().equals("id"))
        .findFirst()
        .orElseThrow();
    assertEquals("ID", idField.getType());

    // Check name field
    GraphQLSchema.FieldDefinition nameField = fields.stream()
        .filter(f -> f.getName().equals("name"))
        .findFirst()
        .orElseThrow();
    assertEquals("String", nameField.getType());
  }

  @Test
  void testGenerateQueries() {
    GraphQLSchema schema = GraphQLSchemaGenerator.generate(TestUser.class);

    List<GraphQLSchema.QueryDefinition> queries = schema.getQueries();
    assertEquals(2, queries.size());

    // Single entity query
    GraphQLSchema.QueryDefinition singleQuery = queries.stream()
        .filter(q -> q.getName().equals("testUser"))
        .findFirst()
        .orElseThrow();
    assertEquals("TestUser", singleQuery.getReturnType());

    // List query
    GraphQLSchema.QueryDefinition listQuery = queries.stream()
        .filter(q -> q.getName().equals("testUsers"))
        .findFirst()
        .orElseThrow();
    assertEquals("[TestUser!]!", listQuery.getReturnType());
  }

  @Test
  void testGenerateMutations() {
    GraphQLSchema schema = GraphQLSchemaGenerator.generate(TestUser.class);

    List<GraphQLSchema.MutationDefinition> mutations = schema.getMutations();
    assertEquals(3, mutations.size());

    // Create mutation
    GraphQLSchema.MutationDefinition createMutation = mutations.stream()
        .filter(m -> m.getName().equals("createTestUser"))
        .findFirst()
        .orElseThrow();
    assertEquals("TestUser!", createMutation.getReturnType());

    // Update mutation
    GraphQLSchema.MutationDefinition updateMutation = mutations.stream()
        .filter(m -> m.getName().equals("updateTestUser"))
        .findFirst()
        .orElseThrow();
    assertEquals("TestUser!", updateMutation.getReturnType());

    // Delete mutation
    GraphQLSchema.MutationDefinition deleteMutation = mutations.stream()
        .filter(m -> m.getName().equals("deleteTestUser"))
        .findFirst()
        .orElseThrow();
    assertEquals("Boolean!", deleteMutation.getReturnType());
  }

  @Test
  void testGenerateSubscriptions() {
    GraphQLSchema schema = GraphQLSchemaGenerator.generate(TestProduct.class);

    List<GraphQLSchema.SubscriptionDefinition> subscriptions = schema.getSubscriptions();
    assertEquals(3, subscriptions.size());

    // Created subscription
    assertTrue(subscriptions.stream()
        .anyMatch(s -> s.getName().equals("testProductCreated")));

    // Updated subscription
    assertTrue(subscriptions.stream()
        .anyMatch(s -> s.getName().equals("testProductUpdated")));

    // Deleted subscription
    assertTrue(subscriptions.stream()
        .anyMatch(s -> s.getName().equals("testProductDeleted")));
  }

  @Test
  void testFieldWithCustomName() {
    GraphQLSchema schema = GraphQLSchemaGenerator.generate(TestUser.class);

    GraphQLSchema.TypeDefinition userType = schema.getTypes().get("TestUser");
    List<GraphQLSchema.FieldDefinition> fields = userType.getFields();

    // Email field has custom name "emailAddress"
    GraphQLSchema.FieldDefinition emailField = fields.stream()
        .filter(f -> f.getName().equals("emailAddress"))
        .findFirst()
        .orElseThrow();
    assertEquals("String", emailField.getType());
  }

  @Test
  void testFieldWithNullable() {
    GraphQLSchema schema = GraphQLSchemaGenerator.generate(TestUser.class);

    GraphQLSchema.TypeDefinition userType = schema.getTypes().get("TestUser");

    // ID field is non-nullable
    String sdl = userType.toSDL();
    assertTrue(sdl.contains("id: ID!"));
  }

  @Test
  void testIgnoredField() {
    GraphQLSchema schema = GraphQLSchemaGenerator.generate(TestUser.class);

    GraphQLSchema.TypeDefinition userType = schema.getTypes().get("TestUser");
    List<GraphQLSchema.FieldDefinition> fields = userType.getFields();

    // Password field should be ignored
    boolean hasPassword = fields.stream()
        .anyMatch(f -> f.getName().equals("password"));
    assertFalse(hasPassword);
  }

  @Test
  void testDeprecatedField() {
    GraphQLSchema schema = GraphQLSchemaGenerator.generate(TestUser.class);

    GraphQLSchema.TypeDefinition userType = schema.getTypes().get("TestUser");

    String sdl = userType.toSDL();
    assertTrue(sdl.contains("@deprecated"));
    assertTrue(sdl.contains("Use emailAddress instead"));
  }

  @Test
  void testSDLGeneration() {
    GraphQLSchema schema = GraphQLSchemaGenerator.generate(TestUser.class);

    String sdl = schema.toSDL();

    assertNotNull(sdl);
    assertTrue(sdl.contains("type TestUser"));
    assertTrue(sdl.contains("type Query"));
    assertTrue(sdl.contains("type Mutation"));
    assertTrue(sdl.contains("testUser(id: ID!): TestUser"));
    assertTrue(sdl.contains("testUsers(limit: Int, offset: Int): [TestUser!]!"));
  }

  @Test
  void testMultipleEntities() {
    GraphQLSchema schema = GraphQLSchemaGenerator.generate(
        TestUser.class,
        TestProduct.class
    );

    assertEquals(2, schema.getTypes().size());
    assertTrue(schema.getTypes().containsKey("TestUser"));
    assertTrue(schema.getTypes().containsKey("TestProduct"));

    // Should have queries for both entities
    assertEquals(4, schema.getQueries().size());

    // Should have mutations for both entities
    assertEquals(6, schema.getMutations().size());
  }

  @Test
  void testCollectionField() {
    GraphQLSchema schema = GraphQLSchemaGenerator.generate(TestUser.class);

    GraphQLSchema.TypeDefinition userType = schema.getTypes().get("TestUser");

    // Orders field should be a list
    GraphQLSchema.FieldDefinition ordersField = userType.getFields().stream()
        .filter(f -> f.getName().equals("orders"))
        .findFirst()
        .orElseThrow();

    assertTrue(ordersField.getType().startsWith("["));
    assertTrue(ordersField.getType().endsWith("]"));
  }

  @Test
  void testDisableQueryGeneration() {
    GraphQLSchema schema = GraphQLSchemaGenerator.generate(TestOrder.class);

    // TestOrder has generateQueries = false
    List<GraphQLSchema.QueryDefinition> queries = schema.getQueries();
    boolean hasOrderQueries = queries.stream()
        .anyMatch(q -> q.getName().contains("testOrder"));

    assertFalse(hasOrderQueries);
  }

  @Test
  void testDisableMutationGeneration() {
    GraphQLSchema schema = GraphQLSchemaGenerator.generate(TestOrder.class);

    // TestOrder has generateMutations = false
    List<GraphQLSchema.MutationDefinition> mutations = schema.getMutations();
    boolean hasOrderMutations = mutations.stream()
        .anyMatch(m -> m.getName().contains("TestOrder"));

    assertFalse(hasOrderMutations);
  }

  // Test entities

  @GraphQLEntity(
      name = "TestUser",
      description = "A test user entity"
  )
  static class TestUser {
    private Long id;
    private String name;
    private String email;
    private String password;
    private List<String> orders;

    @GraphQLField(description = "User's unique identifier", nullable = false)
    public Long getId() {
      return id;
    }

    @GraphQLField(description = "User's full name")
    public String getName() {
      return name;
    }

    @GraphQLField(name = "emailAddress", description = "User's email address")
    public String getEmail() {
      return email;
    }

    @GraphQLIgnore
    public String getPassword() {
      return password;
    }

    @GraphQLField(
        description = "User's orders",
        deprecated = true,
        deprecationReason = "Use emailAddress instead"
    )
    public String getLegacyEmail() {
      return email;
    }

    @GraphQLField(description = "User's orders")
    public List<String> getOrders() {
      return orders;
    }
  }

  @GraphQLEntity(
      name = "TestProduct",
      description = "A test product entity",
      generateSubscriptions = true
  )
  static class TestProduct {
    private Long id;
    private String name;
    private Double price;

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

  @GraphQLEntity(
      name = "TestOrder",
      generateQueries = false,
      generateMutations = false
  )
  static class TestOrder {
    private Long id;
    private String status;

    public Long getId() {
      return id;
    }

    public String getStatus() {
      return status;
    }
  }
}
