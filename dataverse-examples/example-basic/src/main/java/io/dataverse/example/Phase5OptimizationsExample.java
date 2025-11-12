package io.dataverse.example;

import io.dataverse.adapter.dynamodb.DynamoDBAdapter;
import io.dataverse.adapter.mongodb.MongoDBAdapter;
import io.dataverse.adapter.redis.RedisAdapter;
import io.dataverse.api.AggregationBuilder;
import io.dataverse.api.BatchOperations;
import io.dataverse.api.Repository;
import io.dataverse.spi.AdapterConfig;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Example demonstrating Phase 5 production optimizations:
 * - DynamoDB Batch Operations (BatchWriteItem, BatchGetItem)
 * - MongoDB Aggregation Pipeline
 * - Redis Pipelining
 * - Performance Comparisons
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class Phase5OptimizationsExample {

  /**
   * Order entity for aggregation examples.
   */
  public static class Order implements io.dataverse.api.Entity<String> {
    private String id;
    private String customerId;
    private String region;
    private String status;
    private double totalAmount;
    private int itemCount;
    private Instant orderDate;
    private Instant createdAt;

    public Order() {
    }

    public Order(String id, String customerId, String region, String status,
                 double totalAmount, int itemCount) {
      this.id = id;
      this.customerId = customerId;
      this.region = region;
      this.status = status;
      this.totalAmount = totalAmount;
      this.itemCount = itemCount;
      this.orderDate = Instant.now();
      this.createdAt = Instant.now();
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public void setId(String id) {
      this.id = id;
    }

    // Getters and setters
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }
    public Instant getOrderDate() { return orderDate; }
    public void setOrderDate(Instant orderDate) { this.orderDate = orderDate; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  }

  /**
   * Session entity for Redis examples.
   */
  public static class Session implements io.dataverse.api.Entity<String> {
    private String id;
    private String userId;
    private Instant createdAt;
    private Instant expiresAt;
    private Map<String, String> attributes;

    public Session() {
    }

    public Session(String id, String userId) {
      this.id = id;
      this.userId = userId;
      this.createdAt = Instant.now();
      this.expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public void setId(String id) {
      this.id = id;
    }

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Map<String, String> getAttributes() { return attributes; }
    public void setAttributes(Map<String, String> attributes) { this.attributes = attributes; }
  }

  public static void main(String[] args) {
    System.out.println("=== DataVerse SDK - Phase 5 Optimizations Example ===\n");

    // ===================================================================
    // 1. DynamoDB Batch Optimizations
    // ===================================================================
    demonstrateDynamoDBBatchOperations();

    // ===================================================================
    // 2. MongoDB Aggregation Pipeline
    // ===================================================================
    demonstrateMongoDBAggregations();

    // ===================================================================
    // 3. Redis Pipelining
    // ===================================================================
    demonstrateRedisPipelining();

    System.out.println("\n=== Example Complete ===");
  }

  /**
   * Demonstrates DynamoDB batch operations with performance comparison.
   */
  private static void demonstrateDynamoDBBatchOperations() {
    System.out.println("--- 1. DynamoDB Batch Operations ---\n");

    // Initialize DynamoDB adapter
    AdapterConfig config = AdapterConfig.builder()
        .property("region", "us-east-1")
        .property("endpoint", "http://localhost:8000")
        .build();

    DynamoDBAdapter adapter = new DynamoDBAdapter();
    adapter.initialize(config);

    Repository<User, String> userRepo = adapter.createRepository(User.class);
    BatchOperations<User, String> batchOps = userRepo.batch();

    // Generate test data
    int itemCount = 1000;
    System.out.println("Generating " + itemCount + " test users...");
    List<User> users = generateUsers(itemCount);

    // ===== Batch Write Performance =====
    System.out.println("\n1a. Batch Write Performance");
    System.out.println("-----------------------------");

    // Single-item operations (simulated)
    long singleItemStart = System.currentTimeMillis();
    System.out.println("Simulated single-item writes: ~" + (itemCount * 10) + "ms");

    // Batch operations
    long batchStart = System.currentTimeMillis();
    BatchOperations.BatchResult<User> writeResult = batchOps.upsertAll(users);
    long batchDuration = System.currentTimeMillis() - batchStart;

    System.out.println("\nBatch write completed in: " + batchDuration + "ms");
    System.out.println("  Total items: " + writeResult.getTotalCount());
    System.out.println("  Successful: " + writeResult.getSuccessCount());
    System.out.println("  Inserted: " + writeResult.getInsertedCount());
    System.out.println("  Failed: " + writeResult.getFailureCount());
    System.out.println("  Performance gain: ~" + ((itemCount * 10) / Math.max(1, batchDuration)) + "x faster");

    // ===== Batch Read Performance =====
    System.out.println("\n1b. Batch Read Performance");
    System.out.println("---------------------------");

    // Extract IDs
    List<String> userIds = users.stream()
        .map(User::getId)
        .limit(500)
        .toList();

    // Single-item operations (simulated)
    System.out.println("Simulated single-item reads: ~" + (userIds.size() * 8) + "ms");

    // Batch read
    long readStart = System.currentTimeMillis();
    BatchOperations.BatchResult<User> readResult = batchOps.findAllById(userIds);
    long readDuration = System.currentTimeMillis() - readStart;

    System.out.println("\nBatch read completed in: " + readDuration + "ms");
    System.out.println("  Items requested: " + userIds.size());
    System.out.println("  Items found: " + readResult.getSuccessful().size());
    System.out.println("  Performance gain: ~" + ((userIds.size() * 8) / Math.max(1, readDuration)) + "x faster");

    // ===== Batch Delete =====
    System.out.println("\n1c. Batch Delete");
    System.out.println("-----------------");

    List<String> idsToDelete = userIds.subList(0, Math.min(100, userIds.size()));
    BatchOperations.BatchResult<User> deleteResult = batchOps.deleteAllById(idsToDelete);

    System.out.println("Deleted " + idsToDelete.size() + " items");
    System.out.println("  Success: " + (idsToDelete.size() - deleteResult.getFailureCount()));
    System.out.println("  Failures: " + deleteResult.getFailureCount());

    adapter.shutdown();
    System.out.println();
  }

  /**
   * Demonstrates MongoDB aggregation pipeline.
   */
  private static void demonstrateMongoDBAggregations() {
    System.out.println("--- 2. MongoDB Aggregation Pipeline ---\n");

    // Initialize MongoDB adapter
    AdapterConfig config = AdapterConfig.builder()
        .property("connectionString", "mongodb://localhost:27017")
        .property("database", "dataverse-test")
        .build();

    MongoDBAdapter adapter = new MongoDBAdapter();
    adapter.initialize(config);

    Repository<Order, String> orderRepo = adapter.createRepository(Order.class);

    // Generate sample orders
    System.out.println("Generating sample orders...");
    List<Order> orders = generateOrders(1000);

    // Save orders
    BatchOperations<Order, String> batchOps = orderRepo.batch();
    BatchOperations.BatchResult<Order> saveResult = batchOps.upsertAll(orders);
    System.out.println("Saved " + saveResult.getSuccessCount() + " orders\n");

    // ===== Simple Aggregation =====
    System.out.println("2a. Simple Aggregation - Total by Customer");
    System.out.println("--------------------------------------------");

    Map<String, Double> customerTotals = orderRepo.aggregate()
        .groupBy("customerId")
        .sum("totalAmount")
        .execute();

    System.out.println("Customer totals:");
    customerTotals.forEach((customerId, total) ->
        System.out.printf("  %s: $%.2f%n", customerId, total)
    );

    // ===== Advanced Aggregation with Filtering =====
    System.out.println("\n2b. Advanced Aggregation - Multi-field Grouping");
    System.out.println("------------------------------------------------");

    Instant oneWeekAgo = Instant.now().minus(7, ChronoUnit.DAYS);

    AggregationBuilder.AggregationResult result = orderRepo.aggregate()
        .where("status").eq("COMPLETED")
        .and("createdAt").greaterThan(oneWeekAgo)
        .groupBy("customerId", "region")
        .count()
        .sum("totalAmount")
        .avg("itemCount")
        .min("orderDate")
        .max("orderDate")
        .executeDetailed();

    System.out.println("Detailed aggregation results:");
    for (Map<String, Object> group : result.getGroups()) {
      String customerId = (String) group.get("customerId");
      String region = (String) group.get("region");

      long orderCount = result.getCount(group);
      double totalAmount = result.getSum(group, "totalAmount");
      double avgItems = result.getAvg(group, "itemCount");
      Instant minDate = result.getMin(group, "orderDate");
      Instant maxDate = result.getMax(group, "orderDate");

      System.out.printf("  Customer: %s, Region: %s%n", customerId, region);
      System.out.printf("    Orders: %d%n", orderCount);
      System.out.printf("    Total: $%.2f%n", totalAmount);
      System.out.printf("    Avg Items: %.1f%n", avgItems);
      System.out.printf("    Date Range: %s to %s%n", minDate, maxDate);
    }

    // ===== Count Aggregation =====
    System.out.println("\n2c. Count Aggregation");
    System.out.println("---------------------");

    long completedCount = orderRepo.aggregate()
        .where("status").eq("COMPLETED")
        .executeCount();

    long pendingCount = orderRepo.aggregate()
        .where("status").eq("PENDING")
        .executeCount();

    System.out.println("Order counts by status:");
    System.out.println("  Completed: " + completedCount);
    System.out.println("  Pending: " + pendingCount);

    // ===== Sum/Avg Aggregation =====
    System.out.println("\n2d. Sum and Average Aggregations");
    System.out.println("---------------------------------");

    double totalRevenue = orderRepo.aggregate()
        .where("status").eq("COMPLETED")
        .sum("totalAmount")
        .executeSum();

    double avgOrderValue = orderRepo.aggregate()
        .where("status").eq("COMPLETED")
        .avg("totalAmount")
        .executeAvg();

    System.out.printf("Total revenue: $%.2f%n", totalRevenue);
    System.out.printf("Average order value: $%.2f%n", avgOrderValue);

    adapter.shutdown();
    System.out.println();
  }

  /**
   * Demonstrates Redis pipelining for bulk operations.
   */
  private static void demonstrateRedisPipelining() {
    System.out.println("--- 3. Redis Pipelining ---\n");

    // Initialize Redis adapter
    AdapterConfig config = AdapterConfig.builder()
        .property("host", "localhost")
        .property("port", 6379)
        .property("maxConnections", 20)
        .build();

    RedisAdapter adapter = new RedisAdapter();
    adapter.initialize(config);

    Repository<Session, String> sessionRepo = adapter.createRepository(Session.class);
    BatchOperations<Session, String> batchOps = sessionRepo.batch();

    // Generate test sessions
    int sessionCount = 10000;
    System.out.println("Generating " + sessionCount + " test sessions...");
    List<Session> sessions = generateSessions(sessionCount);

    // ===== Pipeline Write Performance =====
    System.out.println("\n3a. Pipeline Write Performance");
    System.out.println("-------------------------------");

    // Single-item operations (simulated)
    System.out.println("Simulated single-item writes: ~" + (sessionCount * 0.5) + "ms");

    // Pipeline write
    long writeStart = System.currentTimeMillis();
    BatchOperations.BatchResult<Session> writeResult = batchOps.upsertAll(sessions);
    long writeDuration = System.currentTimeMillis() - writeStart;

    System.out.println("\nPipeline write completed in: " + writeDuration + "ms");
    System.out.println("  Total items: " + writeResult.getTotalCount());
    System.out.println("  Successful: " + writeResult.getSuccessCount());
    System.out.println("  Performance gain: ~" + ((sessionCount * 0.5) / Math.max(1, writeDuration)) + "x faster");

    // ===== MGET Read Performance =====
    System.out.println("\n3b. MGET Read Performance (Atomic Multi-Get)");
    System.out.println("---------------------------------------------");

    // Extract session IDs
    List<String> sessionIds = sessions.stream()
        .map(Session::getId)
        .limit(5000)
        .toList();

    // Single-item operations (simulated)
    System.out.println("Simulated single-item reads: ~" + (sessionIds.size() * 0.4) + "ms");

    // MGET batch read
    long readStart = System.currentTimeMillis();
    BatchOperations.BatchResult<Session> readResult = batchOps.findAllById(sessionIds);
    long readDuration = System.currentTimeMillis() - readStart;

    System.out.println("\nMGET read completed in: " + readDuration + "ms");
    System.out.println("  Items requested: " + sessionIds.size());
    System.out.println("  Items found: " + readResult.getSuccessful().size());
    System.out.println("  Performance gain: ~" + ((sessionIds.size() * 0.4) / Math.max(1, readDuration)) + "x faster");

    // ===== Pipeline Update =====
    System.out.println("\n3c. Pipeline Bulk Update");
    System.out.println("-------------------------");

    List<Session> sessionsToUpdate = sessions.subList(0, Math.min(1000, sessions.size()));

    long updateStart = System.currentTimeMillis();
    BatchOperations.BatchResult<Session> updateResult = batchOps.updateAll(
        sessionsToUpdate,
        session -> {
          session.setExpiresAt(Instant.now().plus(2, ChronoUnit.HOURS));
          return session;
        }
    );
    long updateDuration = System.currentTimeMillis() - updateStart;

    System.out.println("Pipeline update completed in: " + updateDuration + "ms");
    System.out.println("  Updated: " + updateResult.getUpdatedCount());
    System.out.println("  Failed: " + updateResult.getFailureCount());

    adapter.shutdown();
    System.out.println();
  }

  /**
   * Generates test users.
   */
  private static List<User> generateUsers(int count) {
    List<User> users = new ArrayList<>();
    for (int i = 1; i <= count; i++) {
      User user = new User();
      user.setId("user-" + i);
      user.setName("User " + i);
      user.setEmail("user" + i + "@example.com");
      user.setAge(20 + (i % 50));
      user.setActive(i % 3 != 0);
      users.add(user);
    }
    return users;
  }

  /**
   * Generates test orders.
   */
  private static List<Order> generateOrders(int count) {
    List<Order> orders = new ArrayList<>();
    String[] customers = {"CUST-001", "CUST-002", "CUST-003", "CUST-004", "CUST-005"};
    String[] regions = {"US-EAST", "US-WEST", "EU-CENTRAL", "ASIA-PACIFIC"};
    String[] statuses = {"PENDING", "COMPLETED", "CANCELLED"};

    for (int i = 1; i <= count; i++) {
      Order order = new Order(
          "order-" + i,
          customers[i % customers.length],
          regions[i % regions.length],
          statuses[i % statuses.length],
          50.0 + (i % 500),
          1 + (i % 10)
      );
      orders.add(order);
    }
    return orders;
  }

  /**
   * Generates test sessions.
   */
  private static List<Session> generateSessions(int count) {
    List<Session> sessions = new ArrayList<>();
    for (int i = 1; i <= count; i++) {
      Session session = new Session(
          "session-" + i,
          "user-" + (i % 100)
      );
      sessions.add(session);
    }
    return sessions;
  }
}
