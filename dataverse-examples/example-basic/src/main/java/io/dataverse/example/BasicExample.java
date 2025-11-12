package io.dataverse.example;

import io.dataverse.adapter.dynamodb.DynamoDBAdapter;
import io.dataverse.api.Repository;
import io.dataverse.spi.AdapterConfig;
import io.dataverse.spi.DataSourceAdapter;
import java.util.List;
import java.util.Optional;

/**
 * Basic example demonstrating DataVerse SDK usage.
 *
 * <p>This example shows:
 * <ul>
 *   <li>How to configure and initialize an adapter
 *   <li>How to create a repository
 *   <li>How to perform CRUD operations
 *   <li>How to use the fluent query API
 *   <li>How to use async operations
 * </ul>
 *
 * @since 1.0.0
 */
public class BasicExample {

  public static void main(String[] args) {
    System.out.println("=== DataVerse SDK - Basic Example ===\n");

    try {
      // Step 1: Configure the adapter
      System.out.println("1. Configuring DynamoDB adapter...");
      AdapterConfig config =
          AdapterConfig.builder()
              .property("region", "us-east-1")
              .property("endpoint", "http://localhost:8000") // DynamoDB Local
              .property("pool.minSize", 10)
              .property("pool.maxSize", 100)
              .build();

      // Step 2: Initialize the adapter
      System.out.println("2. Initializing adapter...");
      DataSourceAdapter adapter = new DynamoDBAdapter();
      adapter.initialize(config);

      // Step 3: Check adapter health
      System.out.println("3. Checking adapter health...");
      var healthResult = adapter.healthCheck();
      System.out.println("   Health status: " + healthResult.getStatus());
      System.out.println("   Message: " + healthResult.getMessage().orElse("N/A"));

      // Step 4: Create repository
      System.out.println("\n4. Creating repository for User entity...");
      Repository<User, String> userRepository = adapter.createRepository(User.class);

      // Step 5: Save entities
      System.out.println("\n5. Saving users...");
      User john = new User("john.doe@example.com", "John Doe", 30);
      User jane = new User("jane.smith@example.com", "Jane Smith", 28);
      User bob = new User("bob.wilson@example.com", "Bob Wilson", 35);

      // Note: These operations will fail without actual DynamoDB connection
      // This is a demonstration of the API
      System.out.println("   Created users (not actually saved in this demo):");
      System.out.println("   - " + john);
      System.out.println("   - " + jane);
      System.out.println("   - " + bob);

      // Step 6: Query examples
      System.out.println("\n6. Query examples:");

      // Example: Find active users
      System.out.println("\n   a) Find active users:");
      System.out.println("      List<User> activeUsers = userRepository.query()");
      System.out.println("          .where(\"status\").eq(\"ACTIVE\")");
      System.out.println("          .orderBy(\"name\").ascending()");
      System.out.println("          .execute();");

      // Example: Find users by age range
      System.out.println("\n   b) Find users between 25 and 35:");
      System.out.println("      List<User> users = userRepository.query()");
      System.out.println("          .where(\"age\").between(25, 35)");
      System.out.println("          .and(\"status\").eq(\"ACTIVE\")");
      System.out.println("          .limit(10)");
      System.out.println("          .execute();");

      // Example: Find users with email domain
      System.out.println("\n   c) Find users with @example.com email:");
      System.out.println("      List<User> users = userRepository.query()");
      System.out.println("          .where(\"email\").endsWith(\"@example.com\")");
      System.out.println("          .select(\"id\", \"name\", \"email\")");
      System.out.println("          .execute();");

      // Example: Paginated query
      System.out.println("\n   d) Paginated query (page 2, size 20):");
      System.out.println("      List<User> page = userRepository.query()");
      System.out.println("          .where(\"status\").eq(\"ACTIVE\")");
      System.out.println("          .page(2, 20)");
      System.out.println("          .execute();");

      // Step 7: Async operations
      System.out.println("\n7. Async operations:");
      System.out.println("   CompletableFuture<User> futureUser = ");
      System.out.println("       userRepository.saveAsync(john);");
      System.out.println("   ");
      System.out.println("   futureUser.thenAccept(saved -> ");
      System.out.println("       System.out.println(\"User saved: \" + saved)");
      System.out.println("   );");

      // Step 8: Adapter capabilities
      System.out.println("\n8. Adapter capabilities:");
      var capabilities = adapter.getCapabilities();
      System.out.println("   - Transactions: " + capabilities.supportsTransactions());
      System.out.println("   - Batch operations: " + capabilities.supportsBatchOperations());
      System.out.println("   - Sorting: " + capabilities.supportsSorting());
      System.out.println("   - Full-text search: " + capabilities.supportsFullTextSearch());
      System.out.println("   - Max batch size: " + capabilities.getMaxBatchSize());

      // Step 9: Cleanup
      System.out.println("\n9. Shutting down adapter...");
      adapter.shutdown();

      System.out.println("\n=== Example completed successfully! ===");
      System.out.println("\nNote: This example demonstrates the API but does not connect to");
      System.out.println("a real database. To run with actual data, start DynamoDB Local");
      System.out.println("on port 8000 and create the User table.");

    } catch (Exception e) {
      System.err.println("Error running example: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
