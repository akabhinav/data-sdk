package io.dataverse.adapter.mongodb;

import io.dataverse.api.AggregationBuilder;
import io.dataverse.api.BatchOperations;
import io.dataverse.api.Entity;
import io.dataverse.api.Repository;
import io.dataverse.spi.AdapterConfig;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MongoDB adapter using Testcontainers.
 *
 * <p>Tests real MongoDB operations against a MongoDB container including:
 * - CRUD operations
 * - Batch operations
 * - Aggregation pipeline
 * - Query builder
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("MongoDB Adapter Integration Tests")
class MongoDBAdapterIntegrationTest {

  @Container
  static MongoDBContainer mongoDBContainer = new MongoDBContainer(
      DockerImageName.parse("mongo:7.0"))
      .withExposedPorts(27017);

  private static MongoDBAdapter adapter;
  private static Repository<TestOrder, String> orderRepository;

  /**
   * Test entity for aggregation tests.
   */
  public static class TestOrder implements Entity<String> {
    private String id;
    private String customerId;
    private String region;
    private String status;
    private double totalAmount;
    private int itemCount;

    public TestOrder() {
    }

    public TestOrder(String customerId, String region, String status, double totalAmount, int itemCount) {
      this.customerId = customerId;
      this.region = region;
      this.status = status;
      this.totalAmount = totalAmount;
      this.itemCount = itemCount;
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
  }

  @BeforeAll
  static void setUp() {
    // Initialize adapter with Testcontainers MongoDB
    AdapterConfig config = AdapterConfig.builder()
        .property("connectionString", mongoDBContainer.getReplicaSetUrl())
        .property("database", "testdb")
        .build();

    adapter = new MongoDBAdapter();
    adapter.initialize(config);

    orderRepository = adapter.createRepository(TestOrder.class);
  }

  @AfterAll
  static void tearDown() {
    if (adapter != null) {
      adapter.shutdown();
    }
  }

  @BeforeEach
  void clearData() {
    // Clear collection before each test
    orderRepository.findAll().forEach(order -> orderRepository.deleteById(order.getId()));
  }

  @Test
  @Order(1)
  @DisplayName("Should save and retrieve entity")
  void shouldSaveAndRetrieveEntity() {
    // Given
    TestOrder order = new TestOrder("CUST-001", "US-EAST", "PENDING", 150.50, 3);

    // When
    TestOrder saved = orderRepository.save(order);

    // Then
    assertThat(saved).isNotNull();
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getCustomerId()).isEqualTo("CUST-001");

    // Verify retrieval
    Optional<TestOrder> found = orderRepository.findById(saved.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getTotalAmount()).isEqualTo(150.50);
  }

  @Test
  @Order(2)
  @DisplayName("Should update existing entity")
  void shouldUpdateExistingEntity() {
    // Given
    TestOrder order = new TestOrder("CUST-002", "EU-WEST", "PENDING", 200.00, 5);
    TestOrder saved = orderRepository.save(order);

    // When
    saved.setStatus("COMPLETED");
    saved.setTotalAmount(225.00);
    TestOrder updated = orderRepository.save(saved);

    // Then
    Optional<TestOrder> found = orderRepository.findById(saved.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getStatus()).isEqualTo("COMPLETED");
    assertThat(found.get().getTotalAmount()).isEqualTo(225.00);
  }

  @Test
  @Order(3)
  @DisplayName("Should delete entity by ID")
  void shouldDeleteEntityById() {
    // Given
    TestOrder order = new TestOrder("CUST-003", "ASIA", "CANCELLED", 100.00, 2);
    TestOrder saved = orderRepository.save(order);

    // When
    orderRepository.deleteById(saved.getId());

    // Then
    Optional<TestOrder> found = orderRepository.findById(saved.getId());
    assertThat(found).isEmpty();
  }

  @Test
  @Order(4)
  @DisplayName("Should find all entities")
  void shouldFindAllEntities() {
    // Given
    for (int i = 1; i <= 10; i++) {
      TestOrder order = new TestOrder("CUST-00" + i, "US-WEST", "PENDING", 100.0 * i, i);
      orderRepository.save(order);
    }

    // When
    List<TestOrder> allOrders = orderRepository.findAll();

    // Then
    assertThat(allOrders).hasSize(10);
  }

  @Test
  @Order(5)
  @DisplayName("Should count entities")
  void shouldCountEntities() {
    // Given
    for (int i = 1; i <= 15; i++) {
      TestOrder order = new TestOrder("CUST-" + i, "EU-CENTRAL", "COMPLETED", 50.0 * i, i);
      orderRepository.save(order);
    }

    // When
    long count = orderRepository.count();

    // Then
    assertThat(count).isEqualTo(15);
  }

  @Test
  @Order(6)
  @DisplayName("Should perform batch upsert operations")
  void shouldPerformBatchUpsert() {
    // Given
    List<TestOrder> orders = new ArrayList<>();
    for (int i = 1; i <= 50; i++) {
      orders.add(new TestOrder("CUST-" + (i % 5), "REGION-" + (i % 3), "PENDING", 100.0 + i, i % 10));
    }

    // When
    BatchOperations<TestOrder, String> batchOps = orderRepository.batch();
    BatchOperations.BatchResult<TestOrder> result = batchOps.upsertAll(orders);

    // Then
    assertThat(result.isAllSuccessful()).isTrue();
    assertThat(result.getSuccessCount()).isEqualTo(50);
    assertThat(result.getInsertedCount()).isEqualTo(50);
  }

  @Test
  @Order(7)
  @DisplayName("Should perform batch read operations")
  void shouldPerformBatchRead() {
    // Given
    List<String> ids = new ArrayList<>();
    for (int i = 1; i <= 20; i++) {
      TestOrder order = new TestOrder("CUST-BATCH-" + i, "US-CENTRAL", "PENDING", 75.0, 2);
      TestOrder saved = orderRepository.save(order);
      ids.add(saved.getId());
    }

    // When
    BatchOperations<TestOrder, String> batchOps = orderRepository.batch();
    BatchOperations.BatchResult<TestOrder> result = batchOps.findAllById(ids);

    // Then
    assertThat(result.getSuccessful()).hasSize(20);
  }

  @Test
  @Order(8)
  @DisplayName("Should perform simple aggregation - group by customer")
  void shouldPerformSimpleAggregation() {
    // Given
    orderRepository.save(new TestOrder("CUST-001", "US-EAST", "COMPLETED", 100.0, 2));
    orderRepository.save(new TestOrder("CUST-001", "US-EAST", "COMPLETED", 150.0, 3));
    orderRepository.save(new TestOrder("CUST-002", "US-WEST", "COMPLETED", 200.0, 4));
    orderRepository.save(new TestOrder("CUST-002", "US-WEST", "COMPLETED", 250.0, 5));

    // When
    Map<String, Double> customerTotals = orderRepository.aggregate()
        .groupBy("customerId")
        .sum("totalAmount")
        .execute();

    // Then
    assertThat(customerTotals).hasSize(2);
    assertThat(customerTotals.get("CUST-001")).isEqualTo(250.0);
    assertThat(customerTotals.get("CUST-002")).isEqualTo(450.0);
  }

  @Test
  @Order(9)
  @DisplayName("Should perform aggregation with filtering")
  void shouldPerformAggregationWithFiltering() {
    // Given
    orderRepository.save(new TestOrder("CUST-001", "US-EAST", "COMPLETED", 100.0, 2));
    orderRepository.save(new TestOrder("CUST-001", "US-EAST", "PENDING", 150.0, 3));
    orderRepository.save(new TestOrder("CUST-002", "US-WEST", "COMPLETED", 200.0, 4));
    orderRepository.save(new TestOrder("CUST-002", "US-WEST", "CANCELLED", 250.0, 5));

    // When
    long completedCount = orderRepository.aggregate()
        .where("status").eq("COMPLETED")
        .executeCount();

    double completedSum = orderRepository.aggregate()
        .where("status").eq("COMPLETED")
        .sum("totalAmount")
        .executeSum();

    // Then
    assertThat(completedCount).isEqualTo(2);
    assertThat(completedSum).isEqualTo(300.0);
  }

  @Test
  @Order(10)
  @DisplayName("Should perform multi-field grouping aggregation")
  void shouldPerformMultiFieldGrouping() {
    // Given
    orderRepository.save(new TestOrder("CUST-001", "US-EAST", "COMPLETED", 100.0, 2));
    orderRepository.save(new TestOrder("CUST-001", "US-EAST", "COMPLETED", 150.0, 3));
    orderRepository.save(new TestOrder("CUST-001", "US-WEST", "COMPLETED", 200.0, 4));
    orderRepository.save(new TestOrder("CUST-002", "US-EAST", "COMPLETED", 250.0, 5));

    // When
    AggregationBuilder.AggregationResult result = orderRepository.aggregate()
        .groupBy("customerId", "region")
        .count()
        .sum("totalAmount")
        .avg("itemCount")
        .executeDetailed();

    // Then
    assertThat(result.getGroups()).hasSize(3);
    assertThat(result.getTotalCount()).isEqualTo(4);

    // Find CUST-001 + US-EAST group
    Map<String, Object> group = result.getGroups().stream()
        .filter(g -> "CUST-001".equals(g.get("customerId")) && "US-EAST".equals(g.get("region")))
        .findFirst()
        .orElseThrow();

    assertThat(result.getCount(group)).isEqualTo(2);
    assertThat(result.getSum(group, "totalAmount")).isEqualTo(250.0);
    assertThat(result.getAvg(group, "itemCount")).isEqualTo(2.5);
  }

  @Test
  @Order(11)
  @DisplayName("Should perform min/max aggregations")
  void shouldPerformMinMaxAggregations() {
    // Given
    orderRepository.save(new TestOrder("CUST-001", "US-EAST", "COMPLETED", 100.0, 2));
    orderRepository.save(new TestOrder("CUST-001", "US-EAST", "COMPLETED", 300.0, 8));
    orderRepository.save(new TestOrder("CUST-001", "US-EAST", "COMPLETED", 200.0, 5));

    // When
    Double minAmount = orderRepository.aggregate()
        .where("customerId").eq("CUST-001")
        .min("totalAmount")
        .executeMin();

    Double maxAmount = orderRepository.aggregate()
        .where("customerId").eq("CUST-001")
        .max("totalAmount")
        .executeMax();

    // Then
    assertThat(minAmount).isEqualTo(100.0);
    assertThat(maxAmount).isEqualTo(300.0);
  }

  @Test
  @Order(12)
  @DisplayName("Should perform average aggregation")
  void shouldPerformAverageAggregation() {
    // Given
    orderRepository.save(new TestOrder("CUST-001", "US-EAST", "COMPLETED", 100.0, 2));
    orderRepository.save(new TestOrder("CUST-001", "US-EAST", "COMPLETED", 200.0, 4));
    orderRepository.save(new TestOrder("CUST-001", "US-EAST", "COMPLETED", 300.0, 6));

    // When
    double avgAmount = orderRepository.aggregate()
        .where("customerId").eq("CUST-001")
        .avg("totalAmount")
        .executeAvg();

    // Then
    assertThat(avgAmount).isEqualTo(200.0);
  }

  @Test
  @Order(13)
  @DisplayName("Should handle async operations")
  void shouldHandleAsyncOperations() throws Exception {
    // Given
    TestOrder order = new TestOrder("CUST-ASYNC", "ASYNC-REGION", "PENDING", 500.0, 10);

    // When
    var future = orderRepository.saveAsync(order);

    // Then
    TestOrder saved = future.get();
    assertThat(saved).isNotNull();
    assertThat(saved.getId()).isNotNull();
  }

  @Test
  @Order(14)
  @DisplayName("Should verify adapter health")
  void shouldVerifyAdapterHealth() {
    // When/Then
    assertThat(adapter.isHealthy()).isTrue();
  }

  @Test
  @Order(15)
  @DisplayName("Should report adapter capabilities")
  void shouldReportAdapterCapabilities() {
    // When
    var capabilities = adapter.getCapabilities();

    // Then
    assertThat(capabilities.supportsTransactions()).isFalse();  // Not yet implemented
    assertThat(capabilities.supportsBatchOperations()).isTrue();
    assertThat(capabilities.supportsAsyncOperations()).isTrue();
    assertThat(capabilities.getMaxBatchSize()).isEqualTo(1000);  // MongoDB bulk write limit
  }

  @Test
  @Order(16)
  @DisplayName("Should handle large batch operations")
  void shouldHandleLargeBatchOperations() {
    // Given
    List<TestOrder> orders = new ArrayList<>();
    for (int i = 1; i <= 500; i++) {
      orders.add(new TestOrder(
          "CUST-" + (i % 10),
          "REGION-" + (i % 5),
          i % 2 == 0 ? "COMPLETED" : "PENDING",
          50.0 + i,
          i % 20
      ));
    }

    // When
    long startTime = System.currentTimeMillis();
    BatchOperations<TestOrder, String> batchOps = orderRepository.batch();
    BatchOperations.BatchResult<TestOrder> result = batchOps.upsertAll(orders);
    long duration = System.currentTimeMillis() - startTime;

    // Then
    assertThat(result.isAllSuccessful()).isTrue();
    assertThat(result.getSuccessCount()).isEqualTo(500);

    System.out.println("Batch upsert of 500 orders took: " + duration + "ms");
    assertThat(duration).isLessThan(5000);  // Should complete quickly
  }

  @Test
  @Order(17)
  @DisplayName("Should perform complex aggregation with multiple operations")
  void shouldPerformComplexAggregation() {
    // Given - Create diverse dataset
    String[] regions = {"US-EAST", "US-WEST", "EU-CENTRAL", "ASIA-PACIFIC"};
    String[] statuses = {"PENDING", "COMPLETED", "CANCELLED"};

    for (int i = 1; i <= 100; i++) {
      orderRepository.save(new TestOrder(
          "CUST-" + (i % 10),
          regions[i % regions.length],
          statuses[i % statuses.length],
          100.0 + (i * 5),
          i % 15
      ));
    }

    // When - Complex aggregation with filtering and grouping
    AggregationBuilder.AggregationResult result = orderRepository.aggregate()
        .where("status").eq("COMPLETED")
        .and("totalAmount").greaterThan(200.0)
        .groupBy("region")
        .count()
        .sum("totalAmount")
        .avg("itemCount")
        .min("totalAmount")
        .max("totalAmount")
        .executeDetailed();

    // Then
    assertThat(result.getGroups()).isNotEmpty();

    // Verify each group has complete aggregation data
    for (Map<String, Object> group : result.getGroups()) {
      String region = (String) group.get("region");
      assertThat(region).isIn(regions);

      long count = result.getCount(group);
      double sum = result.getSum(group, "totalAmount");
      double avg = result.getAvg(group, "itemCount");
      Double min = result.getMin(group, "totalAmount");
      Double max = result.getMax(group, "totalAmount");

      assertThat(count).isGreaterThan(0);
      assertThat(sum).isGreaterThan(0);
      assertThat(min).isGreaterThan(200.0);
      assertThat(max).isGreaterThanOrEqualTo(min);

      System.out.printf("Region: %s, Count: %d, Sum: %.2f, Avg: %.2f, Min: %.2f, Max: %.2f%n",
          region, count, sum, avg, min, max);
    }
  }
}
