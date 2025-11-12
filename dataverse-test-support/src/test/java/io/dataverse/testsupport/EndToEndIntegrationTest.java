package io.dataverse.testsupport;

import io.dataverse.adapter.dynamodb.DynamoDBAdapter;
import io.dataverse.adapter.mongodb.MongoDBAdapter;
import io.dataverse.adapter.redis.RedisAdapter;
import io.dataverse.api.BatchOperations;
import io.dataverse.api.Entity;
import io.dataverse.api.Repository;
import io.dataverse.spi.AdapterConfig;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive end-to-end integration tests for DataVerse SDK.
 *
 * <p>Tests multiple adapters working together in realistic scenarios:
 * - Multi-tier caching (Redis + DynamoDB)
 * - Read-through cache pattern
 * - Write-through cache pattern
 * - Cache invalidation
 * - Data synchronization across stores
 *
 * <p><strong>Prerequisites:</strong>
 * - DynamoDB Local running on localhost:8000
 * - MongoDB running on localhost:27017
 * - Redis running on localhost:6379
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("End-to-End Integration Tests")
class EndToEndIntegrationTest {

  /**
   * Test entity used across all stores.
   */
  public static class Product implements Entity<String> {
    private String id;
    private String name;
    private String category;
    private double price;
    private int stock;
    private boolean available;

    public Product() {
    }

    public Product(String name, String category, double price, int stock) {
      this.name = name;
      this.category = category;
      this.price = price;
      this.stock = stock;
      this.available = stock > 0;
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
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; this.available = stock > 0; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
  }

  private static DynamoDBAdapter dynamoAdapter;
  private static MongoDBAdapter mongoAdapter;
  private static RedisAdapter redisAdapter;

  private static Repository<Product, String> dynamoRepo;
  private static Repository<Product, String> mongoRepo;
  private static Repository<Product, String> redisRepo;

  @BeforeAll
  static void setupAll() {
    System.out.println("=== Setting up End-to-End Integration Tests ===\n");

    // Initialize DynamoDB adapter (primary datastore)
    AdapterConfig dynamoConfig = AdapterConfig.builder()
        .property("region", "us-east-1")
        .property("endpoint", "http://localhost:8000")
        .build();

    dynamoAdapter = new DynamoDBAdapter();
    dynamoAdapter.initialize(dynamoConfig);
    dynamoRepo = dynamoAdapter.createRepository(Product.class);

    // Initialize MongoDB adapter (analytics datastore)
    AdapterConfig mongoConfig = AdapterConfig.builder()
        .property("connectionString", "mongodb://localhost:27017")
        .property("database", "e2e-test")
        .build();

    mongoAdapter = new MongoDBAdapter();
    mongoAdapter.initialize(mongoConfig);
    mongoRepo = mongoAdapter.createRepository(Product.class);

    // Initialize Redis adapter (cache layer)
    AdapterConfig redisConfig = AdapterConfig.builder()
        .property("host", "localhost")
        .property("port", 6379)
        .property("maxConnections", 10)
        .build();

    redisAdapter = new RedisAdapter();
    redisAdapter.initialize(redisConfig);
    redisRepo = redisAdapter.createRepository(Product.class);

    System.out.println("All adapters initialized successfully\n");
  }

  @AfterAll
  static void teardownAll() {
    if (dynamoAdapter != null) dynamoAdapter.shutdown();
    if (mongoAdapter != null) mongoAdapter.shutdown();
    if (redisAdapter != null) redisAdapter.shutdown();

    System.out.println("\n=== End-to-End Tests Complete ===");
  }

  @BeforeEach
  void clearAllStores() {
    // Clear all datastores before each test
    clearRepository(dynamoRepo);
    clearRepository(mongoRepo);
    clearRepository(redisRepo);
  }

  private void clearRepository(Repository<Product, String> repo) {
    repo.findAll().forEach(product -> repo.deleteById(product.getId()));
  }

  @Test
  @Order(1)
  @DisplayName("Scenario 1: Write-Through Cache Pattern")
  void testWriteThroughCachePattern() {
    System.out.println("--- Scenario 1: Write-Through Cache ---");

    // Given: A new product
    Product product = new Product("Laptop", "Electronics", 999.99, 10);

    // When: Write to primary store (DynamoDB)
    Product saved = dynamoRepo.save(product);
    assertNotNull(saved.getId());

    // And: Write to cache (Redis)
    saved.setId(saved.getId()); // Ensure same ID
    redisRepo.save(saved);

    // And: Write to analytics store (MongoDB)
    mongoRepo.save(saved);

    // Then: Product should be in all three stores
    Optional<Product> fromDynamo = dynamoRepo.findById(saved.getId());
    Optional<Product> fromRedis = redisRepo.findById(saved.getId());
    Optional<Product> fromMongo = mongoRepo.findById(saved.getId());

    assertTrue(fromDynamo.isPresent(), "Product should be in DynamoDB");
    assertTrue(fromRedis.isPresent(), "Product should be in Redis cache");
    assertTrue(fromMongo.isPresent(), "Product should be in MongoDB");

    assertEquals("Laptop", fromDynamo.get().getName());
    assertEquals("Laptop", fromRedis.get().getName());
    assertEquals("Laptop", fromMongo.get().getName());

    System.out.println("✓ Product successfully written to all stores");
  }

  @Test
  @Order(2)
  @DisplayName("Scenario 2: Read-Through Cache Pattern")
  void testReadThroughCachePattern() {
    System.out.println("--- Scenario 2: Read-Through Cache ---");

    // Given: Product exists in DynamoDB but not in cache
    Product product = new Product("Tablet", "Electronics", 499.99, 5);
    Product saved = dynamoRepo.save(product);

    // When: Try to read from cache first
    Optional<Product> fromCache = redisRepo.findById(saved.getId());

    // Then: Cache miss - read from primary store
    if (fromCache.isEmpty()) {
      System.out.println("Cache miss - reading from DynamoDB");
      Optional<Product> fromPrimary = dynamoRepo.findById(saved.getId());

      assertTrue(fromPrimary.isPresent(), "Product should exist in primary store");

      // Populate cache
      redisRepo.save(fromPrimary.get());
      System.out.println("✓ Cache populated from primary store");
    }

    // Verify cache now has the product
    Optional<Product> fromCacheNow = redisRepo.findById(saved.getId());
    assertTrue(fromCacheNow.isPresent(), "Product should now be in cache");
    assertEquals("Tablet", fromCacheNow.get().getName());
  }

  @Test
  @Order(3)
  @DisplayName("Scenario 3: Cache Invalidation on Update")
  void testCacheInvalidationOnUpdate() {
    System.out.println("--- Scenario 3: Cache Invalidation ---");

    // Given: Product exists in both primary store and cache
    Product product = new Product("Phone", "Electronics", 799.99, 15);
    Product saved = dynamoRepo.save(product);
    redisRepo.save(saved);

    // When: Update product in primary store
    saved.setPrice(749.99);
    saved.setStock(12);
    dynamoRepo.save(saved);

    // Then: Invalidate cache
    redisRepo.deleteById(saved.getId());

    // Verify cache is empty
    Optional<Product> fromCache = redisRepo.findById(saved.getId());
    assertTrue(fromCache.isEmpty(), "Cache should be invalidated");

    // Next read will trigger cache refresh
    Optional<Product> fromPrimary = dynamoRepo.findById(saved.getId());
    assertTrue(fromPrimary.isPresent());
    assertEquals(749.99, fromPrimary.get().getPrice());

    // Repopulate cache with updated data
    redisRepo.save(fromPrimary.get());

    System.out.println("✓ Cache invalidated and refreshed with updated data");
  }

  @Test
  @Order(4)
  @DisplayName("Scenario 4: Bulk Data Synchronization")
  void testBulkDataSynchronization() {
    System.out.println("--- Scenario 4: Bulk Sync ---");

    // Given: Multiple products in DynamoDB
    List<Product> products = new ArrayList<>();
    for (int i = 1; i <= 100; i++) {
      products.add(new Product("Product " + i, "Category " + (i % 5), 100.0 * i, i * 2));
    }

    // When: Bulk insert into DynamoDB
    BatchOperations<Product, String> dynamoBatch = dynamoRepo.batch();
    BatchOperations.BatchResult<Product> dynamoResult = dynamoBatch.upsertAll(products);

    assertEquals(100, dynamoResult.getSuccessCount(), "All products should be saved to DynamoDB");

    // Then: Synchronize to MongoDB for analytics
    BatchOperations<Product, String> mongoBatch = mongoRepo.batch();
    BatchOperations.BatchResult<Product> mongoResult = mongoBatch.upsertAll(dynamoResult.getSuccessful());

    assertEquals(100, mongoResult.getSuccessCount(), "All products should be synced to MongoDB");

    // Cache hot items (first 20 products) in Redis
    List<Product> hotItems = dynamoResult.getSuccessful().subList(0, 20);
    BatchOperations<Product, String> redisBatch = redisRepo.batch();
    BatchOperations.BatchResult<Product> redisResult = redisBatch.upsertAll(hotItems);

    assertEquals(20, redisResult.getSuccessCount(), "Hot items should be cached in Redis");

    System.out.println("✓ 100 products in DynamoDB");
    System.out.println("✓ 100 products synced to MongoDB");
    System.out.println("✓ 20 hot items cached in Redis");
  }

  @Test
  @Order(5)
  @DisplayName("Scenario 5: Multi-Store Query and Aggregation")
  void testMultiStoreQueryAndAggregation() {
    System.out.println("--- Scenario 5: Multi-Store Queries ---");

    // Given: Products distributed across stores
    List<Product> products = List.of(
        new Product("Laptop Pro", "Electronics", 1299.99, 5),
        new Product("Mouse", "Accessories", 29.99, 50),
        new Product("Keyboard", "Accessories", 79.99, 30),
        new Product("Monitor", "Electronics", 399.99, 10)
    );

    // Save to all stores
    products.forEach(p -> {
      Product saved = dynamoRepo.save(p);
      mongoRepo.save(saved);
      redisRepo.save(saved);
    });

    // When: Query from different stores
    long dynamoCount = dynamoRepo.count();
    long mongoCount = mongoRepo.count();
    long redisCount = redisRepo.count();

    // Then: Counts should match
    assertEquals(4, dynamoCount, "DynamoDB should have 4 products");
    assertEquals(4, mongoCount, "MongoDB should have 4 products");
    assertEquals(4, redisCount, "Redis should have 4 products");

    // Test aggregation on MongoDB
    var aggregationResult = mongoRepo.aggregate()
        .groupBy("category")
        .sum("stock")
        .execute();

    assertNotNull(aggregationResult);
    assertTrue(aggregationResult.containsKey("Electronics"));
    assertTrue(aggregationResult.containsKey("Accessories"));

    System.out.println("✓ Multi-store queries successful");
    System.out.println("✓ Aggregation results: " + aggregationResult);
  }

  @Test
  @Order(6)
  @DisplayName("Scenario 6: Adapter Health Checks")
  void testAdapterHealthChecks() {
    System.out.println("--- Scenario 6: Health Checks ---");

    // When: Check health of all adapters
    boolean dynamoHealthy = dynamoAdapter.isHealthy();
    boolean mongoHealthy = mongoAdapter.isHealthy();
    boolean redisHealthy = redisAdapter.isHealthy();

    // Then: All should be healthy
    assertTrue(dynamoHealthy, "DynamoDB adapter should be healthy");
    assertTrue(mongoHealthy, "MongoDB adapter should be healthy");
    assertTrue(redisHealthy, "Redis adapter should be healthy");

    System.out.println("✓ All adapters are healthy");
  }

  @Test
  @Order(7)
  @DisplayName("Scenario 7: Adapter Capabilities")
  void testAdapterCapabilities() {
    System.out.println("--- Scenario 7: Capabilities ---");

    // When: Get capabilities of each adapter
    var dynamoCaps = dynamoAdapter.getCapabilities();
    var mongoCaps = mongoAdapter.getCapabilities();
    var redisCaps = redisAdapter.getCapabilities();

    // Then: Verify expected capabilities
    assertTrue(dynamoCaps.supportsBatchOperations());
    assertTrue(mongoCaps.supportsBatchOperations());
    assertTrue(redisCaps.supportsBatchOperations());

    assertTrue(dynamoCaps.supportsAsyncOperations());
    assertTrue(mongoCaps.supportsAsyncOperations());
    assertTrue(redisCaps.supportsAsyncOperations());

    System.out.println("DynamoDB max batch size: " + dynamoCaps.getMaxBatchSize());
    System.out.println("MongoDB max batch size: " + mongoCaps.getMaxBatchSize());
    System.out.println("Redis max batch size: " + redisCaps.getMaxBatchSize());

    assertEquals(25, dynamoCaps.getMaxBatchSize());
    assertEquals(1000, mongoCaps.getMaxBatchSize());
    assertEquals(10000, redisCaps.getMaxBatchSize());

    System.out.println("✓ All adapters report correct capabilities");
  }

  @Test
  @Order(8)
  @DisplayName("Scenario 8: Concurrent Operations")
  void testConcurrentOperations() throws InterruptedException {
    System.out.println("--- Scenario 8: Concurrent Operations ---");

    // Given: Prepare test data
    Product product = new Product("Concurrent Test", "Test", 100.0, 10);
    Product saved = dynamoRepo.save(product);

    // When: Perform concurrent async operations
    var future1 = dynamoRepo.saveAsync(saved);
    var future2 = mongoRepo.saveAsync(saved);
    var future3 = redisRepo.saveAsync(saved);

    // Then: All operations should complete successfully
    assertDoesNotThrow(() -> {
      future1.get();
      future2.get();
      future3.get();
    });

    System.out.println("✓ Concurrent async operations completed successfully");
  }
}
