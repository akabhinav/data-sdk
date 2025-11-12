package io.dataverse.adapter.postgresql;

import io.dataverse.api.BatchOperations;
import io.dataverse.api.Entity;
import io.dataverse.api.Repository;
import io.dataverse.spi.AdapterConfig;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for PostgreSQL adapter using Testcontainers.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("PostgreSQL Adapter Integration Tests")
class PostgreSQLAdapterIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
      .withDatabaseName("testdb")
      .withUsername("test")
      .withPassword("test");

  private static PostgreSQLAdapter adapter;
  private static Repository<TestProduct, Long> productRepository;

  /**
   * Test entity class.
   */
  public static class TestProduct implements Entity<Long> {
    private Long id;
    private String name;
    private String category;
    private double price;
    private int stock;
    private boolean available;

    public TestProduct() {
    }

    public TestProduct(String name, String category, double price, int stock) {
      this.name = name;
      this.category = category;
      this.price = price;
      this.stock = stock;
      this.available = stock > 0;
    }

    @Override
    public Long getId() {
      return id;
    }

    @Override
    public void setId(Long id) {
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
    public void setStock(int stock) { this.stock = stock; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
  }

  @BeforeAll
  static void setUp() throws Exception {
    // Initialize adapter
    AdapterConfig config = AdapterConfig.builder()
        .property("jdbcUrl", postgres.getJdbcUrl())
        .property("username", postgres.getUsername())
        .property("password", postgres.getPassword())
        .property("maxPoolSize", "10")
        .build();

    adapter = new PostgreSQLAdapter();
    adapter.initialize(config);

    // Create test table
    try (Connection conn = adapter.getDataSource().getConnection();
         Statement stmt = conn.createStatement()) {
      stmt.execute("""
          CREATE TABLE test_products (
              id BIGSERIAL PRIMARY KEY,
              name VARCHAR(255) NOT NULL,
              category VARCHAR(100),
              price DECIMAL(10, 2),
              stock INTEGER,
              available BOOLEAN
          )
          """);
    }

    productRepository = adapter.createRepository(TestProduct.class);
  }

  @AfterAll
  static void tearDown() {
    if (adapter != null) {
      adapter.shutdown();
    }
  }

  @BeforeEach
  void clearData() throws Exception {
    try (Connection conn = adapter.getDataSource().getConnection();
         Statement stmt = conn.createStatement()) {
      stmt.execute("TRUNCATE TABLE test_products RESTART IDENTITY CASCADE");
    }
  }

  @Test
  @Order(1)
  @DisplayName("Should save and retrieve entity")
  void shouldSaveAndRetrieveEntity() {
    // Given
    TestProduct product = new TestProduct("Laptop", "Electronics", 999.99, 10);

    // When
    TestProduct saved = productRepository.save(product);

    // Then
    assertThat(saved).isNotNull();
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getName()).isEqualTo("Laptop");

    // Verify retrieval
    Optional<TestProduct> found = productRepository.findById(saved.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getPrice()).isEqualTo(999.99);
  }

  @Test
  @Order(2)
  @DisplayName("Should update existing entity")
  void shouldUpdateExistingEntity() {
    // Given
    TestProduct product = new TestProduct("Mouse", "Accessories", 29.99, 50);
    TestProduct saved = productRepository.save(product);

    // When
    saved.setPrice(24.99);
    saved.setStock(45);
    TestProduct updated = productRepository.save(saved);

    // Then
    Optional<TestProduct> found = productRepository.findById(saved.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getPrice()).isEqualTo(24.99);
    assertThat(found.get().getStock()).isEqualTo(45);
  }

  @Test
  @Order(3)
  @DisplayName("Should delete entity by ID")
  void shouldDeleteEntityById() {
    // Given
    TestProduct product = new TestProduct("Keyboard", "Accessories", 79.99, 30);
    TestProduct saved = productRepository.save(product);

    // When
    productRepository.deleteById(saved.getId());

    // Then
    Optional<TestProduct> found = productRepository.findById(saved.getId());
    assertThat(found).isEmpty();
  }

  @Test
  @Order(4)
  @DisplayName("Should check if entity exists")
  void shouldCheckIfEntityExists() {
    // Given
    TestProduct product = new TestProduct("Monitor", "Electronics", 399.99, 15);
    TestProduct saved = productRepository.save(product);

    // When/Then
    assertThat(productRepository.existsById(saved.getId())).isTrue();
    assertThat(productRepository.existsById(999999L)).isFalse();
  }

  @Test
  @Order(5)
  @DisplayName("Should find all entities")
  void shouldFindAllEntities() {
    // Given
    for (int i = 1; i <= 10; i++) {
      productRepository.save(new TestProduct("Product " + i, "Category", 100.0 * i, i * 5));
    }

    // When
    List<TestProduct> allProducts = productRepository.findAll();

    // Then
    assertThat(allProducts).hasSize(10);
  }

  @Test
  @Order(6)
  @DisplayName("Should count entities")
  void shouldCountEntities() {
    // Given
    for (int i = 1; i <= 15; i++) {
      productRepository.save(new TestProduct("Item " + i, "Test", 50.0, 10));
    }

    // When
    long count = productRepository.count();

    // Then
    assertThat(count).isEqualTo(15);
  }

  @Test
  @Order(7)
  @DisplayName("Should perform batch upsert operations")
  void shouldPerformBatchUpsert() {
    // Given
    List<TestProduct> products = new ArrayList<>();
    for (int i = 1; i <= 100; i++) {
      products.add(new TestProduct("Batch Product " + i, "Batch", 100.0 + i, i * 2));
    }

    // When
    BatchOperations<TestProduct, Long> batchOps = productRepository.batch();
    long startTime = System.currentTimeMillis();
    BatchOperations.BatchResult<TestProduct> result = batchOps.upsertAll(products);
    long duration = System.currentTimeMillis() - startTime;

    // Then
    assertThat(result.isAllSuccessful()).isTrue();
    assertThat(result.getSuccessCount()).isEqualTo(100);
    assertThat(result.getInsertedCount()).isEqualTo(100);

    System.out.println("Batch upsert of 100 products took: " + duration + "ms");
    assertThat(duration).isLessThan(5000);
  }

  @Test
  @Order(8)
  @DisplayName("Should perform batch read operations")
  void shouldPerformBatchRead() {
    // Given - Save products first
    List<Long> ids = new ArrayList<>();
    for (int i = 1; i <= 50; i++) {
      TestProduct product = new TestProduct("Read Product " + i, "Read", 200.0, 20);
      TestProduct saved = productRepository.save(product);
      ids.add(saved.getId());
    }

    // When
    BatchOperations<TestProduct, Long> batchOps = productRepository.batch();
    BatchOperations.BatchResult<TestProduct> result = batchOps.findAllById(ids);

    // Then
    assertThat(result.getSuccessful()).hasSize(50);
    assertThat(result.getSuccessful())
        .extracting(TestProduct::getName)
        .allMatch(name -> name.startsWith("Read Product"));
  }

  @Test
  @Order(9)
  @DisplayName("Should perform batch update with transformation")
  void shouldPerformBatchUpdateWithTransformation() {
    // Given
    List<TestProduct> products = new ArrayList<>();
    for (int i = 1; i <= 25; i++) {
      TestProduct product = new TestProduct("Update Product " + i, "Update", 150.0, 30);
      products.add(productRepository.save(product));
    }

    // When
    BatchOperations<TestProduct, Long> batchOps = productRepository.batch();
    BatchOperations.BatchResult<TestProduct> result = batchOps.updateAll(products, product -> {
      product.setPrice(product.getPrice() * 0.9);  // 10% discount
      product.setStock(product.getStock() - 5);
      return product;
    });

    // Then
    assertThat(result.getUpdatedCount()).isEqualTo(25);

    // Verify transformations
    products.forEach(product -> {
      Optional<TestProduct> found = productRepository.findById(product.getId());
      assertThat(found).isPresent();
      assertThat(found.get().getPrice()).isEqualTo(135.0);
      assertThat(found.get().getStock()).isEqualTo(25);
    });
  }

  @Test
  @Order(10)
  @DisplayName("Should perform batch delete operations")
  void shouldPerformBatchDelete() {
    // Given
    List<Long> ids = new ArrayList<>();
    for (int i = 1; i <= 30; i++) {
      TestProduct product = new TestProduct("Delete Product " + i, "Delete", 100.0, 10);
      TestProduct saved = productRepository.save(product);
      ids.add(saved.getId());
    }

    // When
    BatchOperations<TestProduct, Long> batchOps = productRepository.batch();
    BatchOperations.BatchResult<TestProduct> result = batchOps.deleteAllById(ids);

    // Then
    ids.forEach(id -> {
      assertThat(productRepository.existsById(id)).isFalse();
    });
  }

  @Test
  @Order(11)
  @DisplayName("Should handle async save operations")
  void shouldHandleAsyncSave() throws Exception {
    // Given
    TestProduct product = new TestProduct("Async Product", "Async", 500.0, 20);

    // When
    var future = productRepository.saveAsync(product);

    // Then
    TestProduct saved = future.get();
    assertThat(saved).isNotNull();
    assertThat(saved.getId()).isNotNull();
  }

  @Test
  @Order(12)
  @DisplayName("Should verify adapter health")
  void shouldVerifyAdapterHealth() {
    // When/Then
    assertThat(adapter.isHealthy()).isTrue();
  }

  @Test
  @Order(13)
  @DisplayName("Should report adapter capabilities")
  void shouldReportAdapterCapabilities() {
    // When
    var capabilities = adapter.getCapabilities();

    // Then
    assertThat(capabilities.supportsTransactions()).isTrue();
    assertThat(capabilities.supportsBatchOperations()).isTrue();
    assertThat(capabilities.supportsAsyncOperations()).isTrue();
    assertThat(capabilities.getMaxBatchSize()).isEqualTo(1000);
  }

  @Test
  @Order(14)
  @DisplayName("Should handle large batch operations with auto-chunking")
  void shouldHandleLargeBatchWithAutoChunking() {
    // Given
    List<TestProduct> products = new ArrayList<>();
    for (int i = 1; i <= 500; i++) {
      products.add(new TestProduct("Large Batch " + i, "Large", 50.0 + i, i % 100));
    }

    // When
    long startTime = System.currentTimeMillis();
    BatchOperations<TestProduct, Long> batchOps = productRepository.batch();
    BatchOperations.BatchResult<TestProduct> result = batchOps.upsertAll(products);
    long duration = System.currentTimeMillis() - startTime;

    // Then
    assertThat(result.isAllSuccessful()).isTrue();
    assertThat(result.getSuccessCount()).isEqualTo(500);

    System.out.println("Batch upsert of 500 products took: " + duration + "ms");
    System.out.println("Average: " + (duration / 500.0) + "ms per product");

    // Should be significantly faster than individual operations
    assertThat(duration).isLessThan(10000);
  }

  @Test
  @Order(15)
  @DisplayName("Should demonstrate batch performance vs single operations")
  void shouldDemonstrateBatchPerformance() {
    // Given
    List<TestProduct> products = new ArrayList<>();
    for (int i = 1; i <= 200; i++) {
      products.add(new TestProduct("Perf Product " + i, "Perf", 75.0, 15));
    }

    // When - Batch operation
    BatchOperations<TestProduct, Long> batchOps = productRepository.batch();
    long batchStart = System.currentTimeMillis();
    BatchOperations.BatchResult<TestProduct> batchResult = batchOps.upsertAll(products);
    long batchDuration = System.currentTimeMillis() - batchStart;

    // Clear for next test
    try (Connection conn = adapter.getDataSource().getConnection();
         Statement stmt = conn.createStatement()) {
      stmt.execute("TRUNCATE TABLE test_products RESTART IDENTITY CASCADE");
    } catch (Exception e) {
      // Ignore
    }

    // When - Single operations
    long singleStart = System.currentTimeMillis();
    for (TestProduct product : products) {
      productRepository.save(product);
    }
    long singleDuration = System.currentTimeMillis() - singleStart;

    // Then
    System.out.println("\nPerformance Comparison (200 products):");
    System.out.println("Batch operations: " + batchDuration + "ms");
    System.out.println("Single operations: " + singleDuration + "ms");
    System.out.println("Performance improvement: " + (singleDuration / (double) batchDuration) + "x faster");

    assertThat(batchResult.isAllSuccessful()).isTrue();
    assertThat(batchDuration).isLessThan(singleDuration);
  }
}
