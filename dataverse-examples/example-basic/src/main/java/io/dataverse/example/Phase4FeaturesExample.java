package io.dataverse.example;

import io.dataverse.adapter.dynamodb.DynamoDBAdapter;
import io.dataverse.api.BatchOperations;
import io.dataverse.api.EntityMapper;
import io.dataverse.api.Repository;
import io.dataverse.serialization.jackson.JacksonSerializationProvider;
import io.dataverse.spi.AdapterConfig;
import io.dataverse.spi.SerializationProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Example demonstrating Phase 4 enterprise features:
 * - Batch Operations (Upsert, Bulk Updates)
 * - JSON Serialization
 * - Entity Mapping
 * - Advanced Query Features
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class Phase4FeaturesExample {

  /**
   * DTO for transferring user data.
   */
  public record UserDTO(String id, String fullName, String contact, boolean active) {
  }

  public static void main(String[] args) {
    System.out.println("=== DataVerse SDK - Phase 4 Features Example ===\n");

    // Initialize adapter
    AdapterConfig config = AdapterConfig.builder()
        .property("region", "us-east-1")
        .property("endpoint", "http://localhost:8000")
        .build();

    DynamoDBAdapter adapter = new DynamoDBAdapter();
    adapter.initialize(config);

    Repository<User, String> userRepository = adapter.createRepository(User.class);

    // ===================================================================
    // 1. JSON Serialization
    // ===================================================================
    demonstrateSerialization();

    // ===================================================================
    // 2. Batch Operations - Upsert
    // ===================================================================
    demonstrateBatchUpsert(userRepository);

    // ===================================================================
    // 3. Batch Operations - Bulk Updates
    // ===================================================================
    demonstrateBulkUpdates(userRepository);

    // ===================================================================
    // 4. Entity Mapping
    // ===================================================================
    demonstrateEntityMapping(userRepository);

    // ===================================================================
    // 5. Advanced Query Features
    // ===================================================================
    demonstrateAdvancedQueries(userRepository);

    // Cleanup
    adapter.shutdown();
    System.out.println("\n=== Example Complete ===");
  }

  /**
   * Demonstrates JSON serialization features.
   */
  private static void demonstrateSerialization() {
    System.out.println("--- 1. JSON Serialization ---");

    SerializationProvider serializer = new JacksonSerializationProvider();

    User user = new User();
    user.setId("user-001");
    user.setName("Alice Johnson");
    user.setEmail("alice@example.com");
    user.setAge(28);
    user.setActive(true);

    // Serialize to JSON
    String json = serializer.serialize(user);
    System.out.println("Serialized to JSON:");
    System.out.println(json);

    // Deserialize from JSON
    User deserialized = serializer.deserialize(json, User.class);
    System.out.println("\nDeserialized: " + deserialized.getName());

    // Convert to Map (useful for DynamoDB/MongoDB)
    Map<String, Object> map = serializer.toMap(user);
    System.out.println("\nConverted to Map:");
    map.forEach((key, value) -> System.out.println("  " + key + ": " + value));

    // Clone entity
    User cloned = serializer.clone(user);
    System.out.println("\nCloned entity: " + cloned.getName());
    System.out.println();
  }

  /**
   * Demonstrates batch upsert operations.
   */
  private static void demonstrateBatchUpsert(Repository<User, String> repository) {
    System.out.println("--- 2. Batch Upsert Operations ---");

    // Create multiple users
    List<User> users = new ArrayList<>();
    for (int i = 1; i <= 10; i++) {
      User user = new User();
      user.setId("batch-user-" + i);
      user.setName("User " + i);
      user.setEmail("user" + i + "@example.com");
      user.setAge(20 + i);
      user.setActive(i % 2 == 0); // Even IDs are active
      users.add(user);
    }

    // Perform batch upsert
    BatchOperations<User, String> batchOps = repository.batch();
    BatchOperations.BatchResult<User> result = batchOps.upsertAll(users);

    // Display results
    System.out.println("Batch Upsert Results:");
    System.out.println("  Total: " + result.getTotalCount());
    System.out.println("  Successful: " + result.getSuccessCount());
    System.out.println("  Failed: " + result.getFailureCount());
    System.out.println("  Inserted: " + result.getInsertedCount());
    System.out.println("  Updated: " + result.getUpdatedCount());
    System.out.println("  All Successful: " + result.isAllSuccessful());

    if (result.hasFailures()) {
      System.out.println("\nFailures:");
      result.getFailures().forEach((entity, error) ->
          System.out.println("  " + entity.getId() + ": " + error)
      );
    }

    System.out.println();
  }

  /**
   * Demonstrates bulk update operations.
   */
  private static void demonstrateBulkUpdates(Repository<User, String> repository) {
    System.out.println("--- 3. Bulk Update Operations ---");

    // Find all users (in real scenario, use query)
    List<User> users = List.of(); // Assume we have users from previous operation

    // Update all users using transformation function
    BatchOperations<User, String> batchOps = repository.batch();

    // For demonstration, create sample users to update
    List<User> usersToUpdate = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      User user = new User();
      user.setId("batch-user-" + i);
      user.setName("Updated User " + i);
      user.setEmail("updated" + i + "@example.com");
      user.setAge(30 + i);
      user.setActive(true);
      usersToUpdate.add(user);
    }

    BatchOperations.BatchResult<User> result = batchOps.updateAll(
        usersToUpdate,
        user -> {
          // Apply transformation to each user
          user.setActive(true);
          user.setName(user.getName() + " (Verified)");
          return user;
        }
    );

    System.out.println("Bulk Update Results:");
    System.out.println("  Total: " + result.getTotalCount());
    System.out.println("  Successful: " + result.getSuccessCount());
    System.out.println("  Failed: " + result.getFailureCount());
    System.out.println("  Updated: " + result.getUpdatedCount());
    System.out.println();
  }

  /**
   * Demonstrates entity mapping between User and UserDTO.
   */
  private static void demonstrateEntityMapping(Repository<User, String> repository) {
    System.out.println("--- 4. Entity Mapping ---");

    // Define mapper
    EntityMapper<User, UserDTO> mapper = EntityMapper.of(
        // Entity to DTO
        user -> new UserDTO(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.isActive()
        ),
        // DTO to Entity
        dto -> {
          User user = new User();
          user.setId(dto.id());
          user.setName(dto.fullName());
          user.setEmail(dto.contact());
          user.setActive(dto.active());
          return user;
        }
    );

    // Create a user
    User user = new User();
    user.setId("mapped-001");
    user.setName("Bob Smith");
    user.setEmail("bob@example.com");
    user.setActive(true);

    // Convert to DTO
    UserDTO dto = mapper.toDto(user);
    System.out.println("Mapped to DTO:");
    System.out.println("  ID: " + dto.id());
    System.out.println("  Full Name: " + dto.fullName());
    System.out.println("  Contact: " + dto.contact());
    System.out.println("  Active: " + dto.active());

    // Convert back to entity
    User convertedUser = mapper.toEntity(dto);
    System.out.println("\nMapped back to Entity:");
    System.out.println("  Name: " + convertedUser.getName());
    System.out.println("  Email: " + convertedUser.getEmail());

    // Batch mapping
    List<User> userList = List.of(user);
    List<UserDTO> dtoList = mapper.toDtoList(userList);
    System.out.println("\nBatch mapped " + dtoList.size() + " entities to DTOs");
    System.out.println();
  }

  /**
   * Demonstrates advanced query features (pagination, projection, distinct).
   */
  private static void demonstrateAdvancedQueries(Repository<User, String> repository) {
    System.out.println("--- 5. Advanced Query Features ---");

    // Pagination
    System.out.println("Pagination Example:");
    List<User> page1 = repository.query()
        .where("active").isTrue()
        .page(0, 5) // First page, 5 items
        .execute();
    System.out.println("  Page 1: " + page1.size() + " users");

    List<User> page2 = repository.query()
        .where("active").isTrue()
        .page(1, 5) // Second page, 5 items
        .execute();
    System.out.println("  Page 2: " + page2.size() + " users");

    // Projection (select specific fields)
    System.out.println("\nProjection Example:");
    List<User> projected = repository.query()
        .where("age").greaterThan(25)
        .select("id", "name", "email") // Only fetch these fields
        .execute();
    System.out.println("  Projected query returned " + projected.size() + " users");

    // Distinct results
    System.out.println("\nDistinct Example:");
    List<User> distinct = repository.query()
        .distinct()
        .execute();
    System.out.println("  Distinct query returned " + distinct.size() + " unique users");

    // Count aggregation
    System.out.println("\nAggregation Example:");
    long activeCount = repository.query()
        .where("active").isTrue()
        .count();
    System.out.println("  Active users: " + activeCount);

    long inactiveCount = repository.query()
        .where("active").isFalse()
        .count();
    System.out.println("  Inactive users: " + inactiveCount);

    // Check existence
    boolean hasUsers = repository.query()
        .where("age").greaterThan(30)
        .exists();
    System.out.println("  Has users over 30: " + hasUsers);

    System.out.println();
  }
}
