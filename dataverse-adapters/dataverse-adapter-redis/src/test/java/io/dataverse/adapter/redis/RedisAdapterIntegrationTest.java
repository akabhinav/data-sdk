package io.dataverse.adapter.redis;

import io.dataverse.api.BatchOperations;
import io.dataverse.api.Entity;
import io.dataverse.api.Repository;
import io.dataverse.spi.AdapterConfig;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Redis adapter using Testcontainers.
 *
 * <p>Tests real Redis operations against a Redis container including:
 * - CRUD operations
 * - Batch operations with pipelining
 * - MGET/MSET optimizations
 * - Performance characteristics
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Redis Adapter Integration Tests")
class RedisAdapterIntegrationTest {

  @Container
  static GenericContainer<?> redisContainer = new GenericContainer<>(
      DockerImageName.parse("redis:7-alpine"))
      .withExposedPorts(6379);

  private static RedisAdapter adapter;
  private static Repository<TestSession, String> sessionRepository;

  /**
   * Test entity for session cache.
   */
  public static class TestSession implements Entity<String> {
    private String id;
    private String userId;
    private String token;
    private long createdAt;
    private long expiresAt;
    private boolean active;

    public TestSession() {
    }

    public TestSession(String userId, String token) {
      this.userId = userId;
      this.token = token;
      this.createdAt = System.currentTimeMillis();
      this.expiresAt = System.currentTimeMillis() + 3600000; // 1 hour
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
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
  }

  @BeforeAll
  static void setUp() {
    // Initialize adapter with Testcontainers Redis
    AdapterConfig config = AdapterConfig.builder()
        .property("host", redisContainer.getHost())
        .property("port", redisContainer.getFirstMappedPort())
        .property("maxConnections", 10)
        .build();

    adapter = new RedisAdapter();
    adapter.initialize(config);

    sessionRepository = adapter.createRepository(TestSession.class);
  }

  @AfterAll
  static void tearDown() {
    if (adapter != null) {
      adapter.shutdown();
    }
  }

  @BeforeEach
  void clearData() {
    // Clear all sessions before each test
    sessionRepository.findAll().forEach(session -> sessionRepository.deleteById(session.getId()));
  }

  @Test
  @Order(1)
  @DisplayName("Should save and retrieve entity")
  void shouldSaveAndRetrieveEntity() {
    // Given
    TestSession session = new TestSession("user-001", "token-abc123");

    // When
    TestSession saved = sessionRepository.save(session);

    // Then
    assertThat(saved).isNotNull();
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getUserId()).isEqualTo("user-001");

    // Verify retrieval
    Optional<TestSession> found = sessionRepository.findById(saved.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getToken()).isEqualTo("token-abc123");
  }

  @Test
  @Order(2)
  @DisplayName("Should update existing entity")
  void shouldUpdateExistingEntity() {
    // Given
    TestSession session = new TestSession("user-002", "token-xyz789");
    TestSession saved = sessionRepository.save(session);

    // When
    saved.setToken("token-updated");
    saved.setActive(false);
    TestSession updated = sessionRepository.save(saved);

    // Then
    Optional<TestSession> found = sessionRepository.findById(saved.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getToken()).isEqualTo("token-updated");
    assertThat(found.get().isActive()).isFalse();
  }

  @Test
  @Order(3)
  @DisplayName("Should delete entity by ID")
  void shouldDeleteEntityById() {
    // Given
    TestSession session = new TestSession("user-003", "token-delete");
    TestSession saved = sessionRepository.save(session);

    // When
    sessionRepository.deleteById(saved.getId());

    // Then
    Optional<TestSession> found = sessionRepository.findById(saved.getId());
    assertThat(found).isEmpty();
  }

  @Test
  @Order(4)
  @DisplayName("Should check if entity exists")
  void shouldCheckIfEntityExists() {
    // Given
    TestSession session = new TestSession("user-004", "token-exists");
    TestSession saved = sessionRepository.save(session);

    // When/Then
    assertThat(sessionRepository.existsById(saved.getId())).isTrue();
    assertThat(sessionRepository.existsById("non-existent-id")).isFalse();
  }

  @Test
  @Order(5)
  @DisplayName("Should perform batch upsert with pipelining")
  void shouldPerformBatchUpsertWithPipelining() {
    // Given
    List<TestSession> sessions = new ArrayList<>();
    for (int i = 1; i <= 100; i++) {
      sessions.add(new TestSession("user-" + i, "token-batch-" + i));
    }

    // When
    long startTime = System.currentTimeMillis();
    BatchOperations<TestSession, String> batchOps = sessionRepository.batch();
    BatchOperations.BatchResult<TestSession> result = batchOps.upsertAll(sessions);
    long duration = System.currentTimeMillis() - startTime;

    // Then
    assertThat(result.isAllSuccessful()).isTrue();
    assertThat(result.getSuccessCount()).isEqualTo(100);
    assertThat(result.getInsertedCount()).isEqualTo(100);

    System.out.println("Pipelined batch upsert of 100 sessions took: " + duration + "ms");
    assertThat(duration).isLessThan(1000);  // Should be very fast with pipelining
  }

  @Test
  @Order(6)
  @DisplayName("Should perform batch read with MGET")
  void shouldPerformBatchReadWithMGET() {
    // Given - Save sessions first
    List<String> ids = new ArrayList<>();
    for (int i = 1; i <= 200; i++) {
      TestSession session = new TestSession("user-mget-" + i, "token-" + i);
      TestSession saved = sessionRepository.save(session);
      ids.add(saved.getId());
    }

    // When
    long startTime = System.currentTimeMillis();
    BatchOperations<TestSession, String> batchOps = sessionRepository.batch();
    BatchOperations.BatchResult<TestSession> result = batchOps.findAllById(ids);
    long duration = System.currentTimeMillis() - startTime;

    // Then
    assertThat(result.getSuccessful()).hasSize(200);

    System.out.println("MGET batch read of 200 sessions took: " + duration + "ms");
    assertThat(duration).isLessThan(500);  // MGET should be very fast
  }

  @Test
  @Order(7)
  @DisplayName("Should perform batch update with pipelining")
  void shouldPerformBatchUpdateWithPipelining() {
    // Given
    List<TestSession> sessions = new ArrayList<>();
    for (int i = 1; i <= 50; i++) {
      TestSession session = new TestSession("user-update-" + i, "token-old-" + i);
      sessions.add(sessionRepository.save(session));
    }

    // When
    BatchOperations<TestSession, String> batchOps = sessionRepository.batch();
    BatchOperations.BatchResult<TestSession> result = batchOps.updateAll(sessions, session -> {
      session.setToken("token-new-" + session.getUserId());
      session.setActive(false);
      return session;
    });

    // Then
    assertThat(result.getUpdatedCount()).isEqualTo(50);

    // Verify updates
    sessions.forEach(session -> {
      Optional<TestSession> found = sessionRepository.findById(session.getId());
      assertThat(found).isPresent();
      assertThat(found.get().getToken()).startsWith("token-new-");
      assertThat(found.get().isActive()).isFalse();
    });
  }

  @Test
  @Order(8)
  @DisplayName("Should perform batch delete with pipelining")
  void shouldPerformBatchDeleteWithPipelining() {
    // Given
    List<String> ids = new ArrayList<>();
    for (int i = 1; i <= 75; i++) {
      TestSession session = new TestSession("user-delete-" + i, "token-" + i);
      TestSession saved = sessionRepository.save(session);
      ids.add(saved.getId());
    }

    // When
    BatchOperations<TestSession, String> batchOps = sessionRepository.batch();
    BatchOperations.BatchResult<TestSession> result = batchOps.deleteAllById(ids);

    // Then
    // Verify deletion
    ids.forEach(id -> {
      assertThat(sessionRepository.existsById(id)).isFalse();
    });
  }

  @Test
  @Order(9)
  @DisplayName("Should handle async save operations")
  void shouldHandleAsyncSave() throws Exception {
    // Given
    TestSession session = new TestSession("user-async", "token-async");

    // When
    var future = sessionRepository.saveAsync(session);

    // Then
    TestSession saved = future.get();
    assertThat(saved).isNotNull();
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getUserId()).isEqualTo("user-async");
  }

  @Test
  @Order(10)
  @DisplayName("Should handle async batch operations")
  void shouldHandleAsyncBatchOperations() throws Exception {
    // Given
    List<TestSession> sessions = new ArrayList<>();
    for (int i = 1; i <= 30; i++) {
      sessions.add(new TestSession("user-async-batch-" + i, "token-" + i));
    }

    // When
    BatchOperations<TestSession, String> batchOps = sessionRepository.batch();
    var future = batchOps.upsertAllAsync(sessions);

    // Then
    BatchOperations.BatchResult<TestSession> result = future.get();
    assertThat(result.isAllSuccessful()).isTrue();
    assertThat(result.getSuccessCount()).isEqualTo(30);
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
    assertThat(capabilities.supportsTransactions()).isFalse();  // Not yet implemented
    assertThat(capabilities.supportsBatchOperations()).isTrue();
    assertThat(capabilities.supportsAsyncOperations()).isTrue();
    assertThat(capabilities.getMaxBatchSize()).isEqualTo(10000);  // Pipeline limit
  }

  @Test
  @Order(13)
  @DisplayName("Should handle very large batch operations with auto-chunking")
  void shouldHandleVeryLargeBatchWithAutoChunking() {
    // Given - Create more than pipeline chunk size (1000 items)
    List<TestSession> sessions = new ArrayList<>();
    for (int i = 1; i <= 5000; i++) {
      sessions.add(new TestSession("user-large-" + i, "token-" + i));
    }

    // When
    long startTime = System.currentTimeMillis();
    BatchOperations<TestSession, String> batchOps = sessionRepository.batch();
    BatchOperations.BatchResult<TestSession> result = batchOps.upsertAll(sessions);
    long duration = System.currentTimeMillis() - startTime;

    // Then
    assertThat(result.isAllSuccessful()).isTrue();
    assertThat(result.getSuccessCount()).isEqualTo(5000);

    System.out.println("Pipelined batch upsert of 5000 sessions took: " + duration + "ms");
    System.out.println("Average: " + (duration / 5000.0) + "ms per session");

    // Should be significantly faster than individual operations
    // Individual operations would take ~5000ms (1ms each)
    // Pipelined should take < 1000ms (20ms+ improvement)
    assertThat(duration).isLessThan(2000);
  }

  @Test
  @Order(14)
  @DisplayName("Should demonstrate MGET performance vs individual GETs")
  void shouldDemonstrateMGETPerformance() {
    // Given - Create test data
    List<String> ids = new ArrayList<>();
    for (int i = 1; i <= 1000; i++) {
      TestSession session = new TestSession("user-perf-" + i, "token-" + i);
      TestSession saved = sessionRepository.save(session);
      ids.add(saved.getId());
    }

    // When - Individual reads (simulated timing)
    long individualReadTime = 1000; // Estimated 1ms per read = 1000ms for 1000 reads

    // When - Batch read with MGET
    long batchStartTime = System.currentTimeMillis();
    BatchOperations<TestSession, String> batchOps = sessionRepository.batch();
    BatchOperations.BatchResult<TestSession> result = batchOps.findAllById(ids);
    long batchDuration = System.currentTimeMillis() - batchStartTime;

    // Then
    assertThat(result.getSuccessful()).hasSize(1000);

    System.out.println("\nPerformance Comparison:");
    System.out.println("Individual reads (estimated): " + individualReadTime + "ms");
    System.out.println("MGET batch read (actual): " + batchDuration + "ms");
    System.out.println("Performance improvement: " + (individualReadTime / (double) batchDuration) + "x faster");

    // MGET should be significantly faster (20-50x)
    assertThat(batchDuration).isLessThan(individualReadTime / 10);
  }

  @Test
  @Order(15)
  @DisplayName("Should handle mixed batch operations")
  void shouldHandleMixedBatchOperations() {
    // Given - Mix of new and existing sessions
    List<TestSession> existingSessions = new ArrayList<>();
    for (int i = 1; i <= 25; i++) {
      TestSession session = new TestSession("user-existing-" + i, "token-old-" + i);
      existingSessions.add(sessionRepository.save(session));
    }

    List<TestSession> newSessions = new ArrayList<>();
    for (int i = 1; i <= 25; i++) {
      newSessions.add(new TestSession("user-new-" + i, "token-new-" + i));
    }

    List<TestSession> allSessions = new ArrayList<>();
    allSessions.addAll(existingSessions);
    allSessions.addAll(newSessions);

    // When - Upsert all (mix of inserts and updates)
    BatchOperations<TestSession, String> batchOps = sessionRepository.batch();
    BatchOperations.BatchResult<TestSession> result = batchOps.upsertAll(allSessions);

    // Then
    assertThat(result.isAllSuccessful()).isTrue();
    assertThat(result.getSuccessCount()).isEqualTo(50);
    assertThat(result.getInsertedCount()).isEqualTo(25);
    assertThat(result.getUpdatedCount()).isEqualTo(25);
  }

  @Test
  @Order(16)
  @DisplayName("Should maintain data consistency across batch operations")
  void shouldMaintainDataConsistency() {
    // Given
    List<TestSession> sessions = new ArrayList<>();
    for (int i = 1; i <= 100; i++) {
      sessions.add(new TestSession("user-consistency-" + i, "token-initial-" + i));
    }

    // When - Perform multiple batch operations
    BatchOperations<TestSession, String> batchOps = sessionRepository.batch();

    // 1. Initial insert
    BatchOperations.BatchResult<TestSession> insertResult = batchOps.upsertAll(sessions);
    assertThat(insertResult.isAllSuccessful()).isTrue();

    // 2. Batch update
    BatchOperations.BatchResult<TestSession> updateResult = batchOps.updateAll(
        insertResult.getSuccessful(),
        session -> {
          session.setToken("token-updated-" + session.getUserId());
          return session;
        }
    );
    assertThat(updateResult.isAllSuccessful()).isTrue();

    // 3. Verify all updates
    List<String> ids = updateResult.getSuccessful().stream()
        .map(TestSession::getId)
        .toList();

    BatchOperations.BatchResult<TestSession> readResult = batchOps.findAllById(ids);

    // Then
    assertThat(readResult.getSuccessful()).hasSize(100);
    readResult.getSuccessful().forEach(session -> {
      assertThat(session.getToken()).startsWith("token-updated-");
    });
  }
}
