package io.dataverse.adapter.dynamodb;

import io.dataverse.api.BatchOperations;
import io.dataverse.api.Entity;
import io.dataverse.api.Repository;
import io.dataverse.spi.AdapterConfig;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;

/**
 * Integration tests for DynamoDB adapter using Testcontainers.
 *
 * <p>Tests real DynamoDB operations against a LocalStack container running
 * a local DynamoDB instance. This ensures the adapter works correctly with
 * actual DynamoDB APIs.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("DynamoDB Adapter Integration Tests")
class DynamoDBAdapterIntegrationTest {

  @Container
  static LocalStackContainer localstack = new LocalStackContainer(
      DockerImageName.parse("localstack/localstack:3.0"))
      .withServices(DYNAMODB);

  private static DynamoDBAdapter adapter;
  private static Repository<TestUser, String> userRepository;
  private static DynamoDbClient dynamoDbClient;

  /**
   * Test entity class.
   */
  public static class TestUser implements Entity<String> {
    private String id;
    private String name;
    private String email;
    private int age;
    private boolean active;

    public TestUser() {
    }

    public TestUser(String name, String email, int age) {
      this.name = name;
      this.email = email;
      this.age = age;
      this.active = true;
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
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
  }

  @BeforeAll
  static void setUp() {
    // Configure DynamoDB client for LocalStack
    dynamoDbClient = DynamoDbClient.builder()
        .endpointOverride(localstack.getEndpointOverride(DYNAMODB))
        .region(Region.of(localstack.getRegion()))
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(
                localstack.getAccessKey(),
                localstack.getSecretKey()
            )
        ))
        .build();

    // Create test table
    createTestTable();

    // Initialize adapter
    AdapterConfig config = AdapterConfig.builder()
        .property("region", localstack.getRegion())
        .property("endpoint", localstack.getEndpointOverride(DYNAMODB).toString())
        .property("accessKeyId", localstack.getAccessKey())
        .property("secretAccessKey", localstack.getSecretKey())
        .build();

    adapter = new DynamoDBAdapter();
    adapter.initialize(config);

    userRepository = adapter.createRepository(TestUser.class);
  }

  @AfterAll
  static void tearDown() {
    if (adapter != null) {
      adapter.shutdown();
    }
    if (dynamoDbClient != null) {
      dynamoDbClient.close();
    }
  }

  private static void createTestTable() {
    try {
      dynamoDbClient.createTable(CreateTableRequest.builder()
          .tableName("testusers")
          .keySchema(KeySchemaElement.builder()
              .attributeName("id")
              .keyType(KeyType.HASH)
              .build())
          .attributeDefinitions(AttributeDefinition.builder()
              .attributeName("id")
              .attributeType(ScalarAttributeType.S)
              .build())
          .billingMode(BillingMode.PAY_PER_REQUEST)
          .build());

      // Wait for table to be active
      dynamoDbClient.waiter().waitUntilTableExists(DescribeTableRequest.builder()
          .tableName("testusers")
          .build());
    } catch (Exception e) {
      // Table might already exist
    }
  }

  @Test
  @Order(1)
  @DisplayName("Should save and retrieve entity")
  void shouldSaveAndRetrieveEntity() {
    // Given
    TestUser user = new TestUser("John Doe", "john@example.com", 30);

    // When
    TestUser saved = userRepository.save(user);

    // Then
    assertThat(saved).isNotNull();
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getName()).isEqualTo("John Doe");

    // Verify retrieval
    Optional<TestUser> found = userRepository.findById(saved.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getEmail()).isEqualTo("john@example.com");
  }

  @Test
  @Order(2)
  @DisplayName("Should update existing entity")
  void shouldUpdateExistingEntity() {
    // Given
    TestUser user = new TestUser("Jane Smith", "jane@example.com", 25);
    TestUser saved = userRepository.save(user);

    // When
    saved.setEmail("jane.smith@example.com");
    saved.setAge(26);
    TestUser updated = userRepository.save(saved);

    // Then
    Optional<TestUser> found = userRepository.findById(saved.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getEmail()).isEqualTo("jane.smith@example.com");
    assertThat(found.get().getAge()).isEqualTo(26);
  }

  @Test
  @Order(3)
  @DisplayName("Should delete entity by ID")
  void shouldDeleteEntityById() {
    // Given
    TestUser user = new TestUser("Bob Johnson", "bob@example.com", 35);
    TestUser saved = userRepository.save(user);

    // When
    userRepository.deleteById(saved.getId());

    // Then
    Optional<TestUser> found = userRepository.findById(saved.getId());
    assertThat(found).isEmpty();
  }

  @Test
  @Order(4)
  @DisplayName("Should check if entity exists")
  void shouldCheckIfEntityExists() {
    // Given
    TestUser user = new TestUser("Alice Brown", "alice@example.com", 28);
    TestUser saved = userRepository.save(user);

    // When/Then
    assertThat(userRepository.existsById(saved.getId())).isTrue();
    assertThat(userRepository.existsById("non-existent-id")).isFalse();
  }

  @Test
  @Order(5)
  @DisplayName("Should perform batch upsert operations")
  void shouldPerformBatchUpsert() {
    // Given
    List<TestUser> users = new ArrayList<>();
    for (int i = 1; i <= 50; i++) {
      users.add(new TestUser("User " + i, "user" + i + "@example.com", 20 + i));
    }

    // When
    BatchOperations<TestUser, String> batchOps = userRepository.batch();
    BatchOperations.BatchResult<TestUser> result = batchOps.upsertAll(users);

    // Then
    assertThat(result.isAllSuccessful()).isTrue();
    assertThat(result.getSuccessCount()).isEqualTo(50);
    assertThat(result.getInsertedCount()).isEqualTo(50);
    assertThat(result.getFailureCount()).isZero();
  }

  @Test
  @Order(6)
  @DisplayName("Should perform batch read operations")
  void shouldPerformBatchRead() {
    // Given - Save some users first
    List<TestUser> users = new ArrayList<>();
    List<String> ids = new ArrayList<>();
    for (int i = 1; i <= 100; i++) {
      TestUser user = new TestUser("Batch User " + i, "batch" + i + "@example.com", 25);
      TestUser saved = userRepository.save(user);
      ids.add(saved.getId());
    }

    // When
    BatchOperations<TestUser, String> batchOps = userRepository.batch();
    BatchOperations.BatchResult<TestUser> result = batchOps.findAllById(ids);

    // Then
    assertThat(result.getSuccessful()).hasSize(100);
    assertThat(result.getSuccessful())
        .extracting(TestUser::getName)
        .allMatch(name -> name.startsWith("Batch User"));
  }

  @Test
  @Order(7)
  @DisplayName("Should perform batch update with transformation")
  void shouldPerformBatchUpdateWithTransformation() {
    // Given
    List<TestUser> users = new ArrayList<>();
    for (int i = 1; i <= 25; i++) {
      TestUser user = new TestUser("Transform User " + i, "transform" + i + "@example.com", 30);
      users.add(userRepository.save(user));
    }

    // When
    BatchOperations<TestUser, String> batchOps = userRepository.batch();
    BatchOperations.BatchResult<TestUser> result = batchOps.updateAll(users, user -> {
      user.setActive(false);
      user.setAge(user.getAge() + 1);
      return user;
    });

    // Then
    assertThat(result.getUpdatedCount()).isEqualTo(25);

    // Verify transformations were applied
    users.forEach(user -> {
      Optional<TestUser> found = userRepository.findById(user.getId());
      assertThat(found).isPresent();
      assertThat(found.get().isActive()).isFalse();
      assertThat(found.get().getAge()).isEqualTo(31);
    });
  }

  @Test
  @Order(8)
  @DisplayName("Should perform batch delete operations")
  void shouldPerformBatchDelete() {
    // Given
    List<String> ids = new ArrayList<>();
    for (int i = 1; i <= 30; i++) {
      TestUser user = new TestUser("Delete User " + i, "delete" + i + "@example.com", 40);
      TestUser saved = userRepository.save(user);
      ids.add(saved.getId());
    }

    // When
    BatchOperations<TestUser, String> batchOps = userRepository.batch();
    BatchOperations.BatchResult<TestUser> result = batchOps.deleteAllById(ids);

    // Then
    // Verify deletion
    ids.forEach(id -> {
      assertThat(userRepository.existsById(id)).isFalse();
    });
  }

  @Test
  @Order(9)
  @DisplayName("Should handle async save operations")
  void shouldHandleAsyncSave() throws Exception {
    // Given
    TestUser user = new TestUser("Async User", "async@example.com", 27);

    // When
    var future = userRepository.saveAsync(user);

    // Then
    TestUser saved = future.get();
    assertThat(saved).isNotNull();
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getName()).isEqualTo("Async User");
  }

  @Test
  @Order(10)
  @DisplayName("Should handle async batch operations")
  void shouldHandleAsyncBatchOperations() throws Exception {
    // Given
    List<TestUser> users = new ArrayList<>();
    for (int i = 1; i <= 20; i++) {
      users.add(new TestUser("Async Batch " + i, "asyncbatch" + i + "@example.com", 22));
    }

    // When
    BatchOperations<TestUser, String> batchOps = userRepository.batch();
    var future = batchOps.upsertAllAsync(users);

    // Then
    BatchOperations.BatchResult<TestUser> result = future.get();
    assertThat(result.isAllSuccessful()).isTrue();
    assertThat(result.getSuccessCount()).isEqualTo(20);
  }

  @Test
  @Order(11)
  @DisplayName("Should verify adapter health")
  void shouldVerifyAdapterHealth() {
    // When/Then
    assertThat(adapter.isHealthy()).isTrue();
  }

  @Test
  @Order(12)
  @DisplayName("Should report adapter capabilities")
  void shouldReportAdapterCapabilities() {
    // When
    var capabilities = adapter.getCapabilities();

    // Then
    assertThat(capabilities.supportsTransactions()).isFalse();  // DynamoDB transactions not yet implemented
    assertThat(capabilities.supportsBatchOperations()).isTrue();
    assertThat(capabilities.supportsAsyncOperations()).isTrue();
    assertThat(capabilities.getMaxBatchSize()).isEqualTo(25);  // DynamoDB batch write limit
  }

  @Test
  @Order(13)
  @DisplayName("Should handle large batch operations with auto-chunking")
  void shouldHandleLargeBatchWithAutoChunking() {
    // Given - Create more than DynamoDB's batch limit (25 items)
    List<TestUser> users = new ArrayList<>();
    for (int i = 1; i <= 150; i++) {
      users.add(new TestUser("Large Batch " + i, "large" + i + "@example.com", 25 + (i % 30)));
    }

    // When
    BatchOperations<TestUser, String> batchOps = userRepository.batch();
    long startTime = System.currentTimeMillis();
    BatchOperations.BatchResult<TestUser> result = batchOps.upsertAll(users);
    long duration = System.currentTimeMillis() - startTime;

    // Then
    assertThat(result.isAllSuccessful()).isTrue();
    assertThat(result.getSuccessCount()).isEqualTo(150);
    assertThat(result.getInsertedCount()).isEqualTo(150);

    // Verify performance - should be much faster than 150 individual operations
    System.out.println("Batch upsert of 150 items took: " + duration + "ms");
    assertThat(duration).isLessThan(5000);  // Should complete in under 5 seconds
  }
}
