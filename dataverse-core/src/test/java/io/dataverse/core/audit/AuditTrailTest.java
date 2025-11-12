package io.dataverse.core.audit;

import io.dataverse.api.Entity;
import io.dataverse.core.AbstractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for audit trail functionality.
 *
 * @since 1.0.0
 */
@DisplayName("Audit Trail Tests")
class AuditTrailTest {

  private TestProductRepository productRepository;
  private TestUserRepository userRepository;  // Not audited

  @BeforeEach
  void setUp() {
    productRepository = new TestProductRepository();
    userRepository = new TestUserRepository();
  }

  @Test
  @DisplayName("Should create audit entry on INSERT")
  void shouldCreateAuditEntryOnInsert() {
    // Given
    TestProduct product = new TestProduct();
    product.setName("Laptop");
    product.setPrice(999.99);
    product.setStock(10);

    // When
    TestProduct saved = productRepository.save(product);

    // Then
    List<AuditEntry<TestProduct>> history = productRepository.audit()
        .forEntityId(saved.getId())
        .execute();

    assertThat(history).hasSize(1);
    AuditEntry<TestProduct> entry = history.get(0);
    assertThat(entry.getOperation()).isEqualTo(AuditOperation.INSERT);
    assertThat(entry.getAfterValue().getName()).isEqualTo("Laptop");
    assertThat(entry.getBeforeValue()).isNull();
    assertThat(entry.getModifiedBy()).isEqualTo("system");
  }

  @Test
  @DisplayName("Should create audit entry on UPDATE with field changes")
  void shouldCreateAuditEntryOnUpdate() {
    // Given - Save initial entity
    TestProduct product = new TestProduct();
    product.setName("Mouse");
    product.setPrice(29.99);
    product.setStock(50);
    TestProduct saved = productRepository.save(product);

    // When - Update entity
    saved.setPrice(24.99);
    saved.setStock(45);
    productRepository.save(saved);

    // Then
    List<AuditEntry<TestProduct>> history = productRepository.audit()
        .forEntityId(saved.getId())
        .orderByTimestamp(true)
        .execute();

    assertThat(history).hasSize(2);  // INSERT + UPDATE

    AuditEntry<TestProduct> updateEntry = history.get(1);
    assertThat(updateEntry.getOperation()).isEqualTo(AuditOperation.UPDATE);
    assertThat(updateEntry.getBeforeValue().getPrice()).isEqualTo(29.99);
    assertThat(updateEntry.getAfterValue().getPrice()).isEqualTo(24.99);

    // Check field changes
    assertThat(updateEntry.getChangedFields()).hasSize(2);
    assertThat(updateEntry.getChangedFields()).containsKeys("price", "stock");
    assertThat(updateEntry.getChangedFields().get("price").getOldValue()).isEqualTo(29.99);
    assertThat(updateEntry.getChangedFields().get("price").getNewValue()).isEqualTo(24.99);
  }

  @Test
  @DisplayName("Should create audit entry on DELETE")
  void shouldCreateAuditEntryOnDelete() {
    // Given
    TestProduct product = new TestProduct();
    product.setName("Keyboard");
    product.setPrice(79.99);
    TestProduct saved = productRepository.save(product);
    Long productId = saved.getId();

    // When
    productRepository.deleteById(productId);

    // Then
    List<AuditEntry<TestProduct>> history = productRepository.audit()
        .forEntityId(productId)
        .execute();

    assertThat(history).hasSize(2);  // INSERT + DELETE

    AuditEntry<TestProduct> deleteEntry = history.get(1);
    assertThat(deleteEntry.getOperation()).isEqualTo(AuditOperation.DELETE);
    assertThat(deleteEntry.getBeforeValue().getName()).isEqualTo("Keyboard");
    assertThat(deleteEntry.getAfterValue()).isNull();
  }

  @Test
  @DisplayName("Should not audit non-audited entities")
  void shouldNotAuditNonAuditedEntities() {
    // Given
    TestUser user = new TestUser();
    user.setUsername("john@example.com");
    user.setFullName("John Doe");

    // When
    TestUser saved = userRepository.save(user);

    // Then - Trying to access audit should throw exception
    assertThrows(IllegalStateException.class, () -> userRepository.audit());
  }

  @Test
  @DisplayName("Should query audit history by time range")
  void shouldQueryAuditHistoryByTimeRange() throws Exception {
    // Given
    TestProduct product = new TestProduct();
    product.setName("Monitor");
    product.setPrice(399.99);
    TestProduct saved = productRepository.save(product);

    Instant afterInsert = Instant.now();
    Thread.sleep(100);

    // Update 1
    saved.setPrice(379.99);
    productRepository.save(saved);

    Thread.sleep(100);

    // Update 2
    saved.setPrice(359.99);
    productRepository.save(saved);

    Instant afterUpdates = Instant.now();

    // Then - Query only updates
    List<AuditEntry<TestProduct>> updates = productRepository.audit()
        .forEntityId(saved.getId())
        .between(afterInsert, afterUpdates)
        .execute();

    assertThat(updates).hasSize(2);
    assertThat(updates).allMatch(entry -> entry.getOperation() == AuditOperation.UPDATE);
  }

  @Test
  @DisplayName("Should query audit history by operation type")
  void shouldQueryAuditHistoryByOperationType() {
    // Given - Create and update multiple products
    for (int i = 1; i <= 5; i++) {
      TestProduct product = new TestProduct();
      product.setName("Product " + i);
      product.setPrice(100.0 * i);
      TestProduct saved = productRepository.save(product);

      saved.setPrice(saved.getPrice() * 0.9);
      productRepository.save(saved);
    }

    // Then - Query only INSERT operations
    List<AuditEntry<TestProduct>> inserts = productRepository.audit()
        .forEntityType(TestProduct.class)
        .operation(AuditOperation.INSERT)
        .execute();

    assertThat(inserts).hasSize(5);
    assertThat(inserts).allMatch(entry -> entry.getOperation() == AuditOperation.INSERT);

    // Query only UPDATE operations
    List<AuditEntry<TestProduct>> updates = productRepository.audit()
        .forEntityType(TestProduct.class)
        .operation(AuditOperation.UPDATE)
        .execute();

    assertThat(updates).hasSize(5);
    assertThat(updates).allMatch(entry -> entry.getOperation() == AuditOperation.UPDATE);
  }

  @Test
  @DisplayName("Should support pagination of audit history")
  void shouldSupportPaginationOfAuditHistory() {
    // Given - Create multiple audit entries
    TestProduct product = new TestProduct();
    product.setName("Test Product");
    product.setPrice(100.0);
    TestProduct saved = productRepository.save(product);

    for (int i = 0; i < 10; i++) {
      saved.setPrice(saved.getPrice() + 10);
      productRepository.save(saved);
    }

    // Then - Query with pagination
    List<AuditEntry<TestProduct>> page1 = productRepository.audit()
        .forEntityId(saved.getId())
        .orderByTimestamp(true)
        .limit(5)
        .execute();

    assertThat(page1).hasSize(5);

    List<AuditEntry<TestProduct>> page2 = productRepository.audit()
        .forEntityId(saved.getId())
        .orderByTimestamp(true)
        .offset(5)
        .limit(5)
        .execute();

    assertThat(page2).hasSize(5);
  }

  @Test
  @DisplayName("Should get first and last audit entries")
  void shouldGetFirstAndLastAuditEntries() {
    // Given
    TestProduct product = new TestProduct();
    product.setName("Test Product");
    product.setPrice(100.0);
    TestProduct saved = productRepository.save(product);

    for (int i = 0; i < 5; i++) {
      saved.setPrice(saved.getPrice() + 10);
      productRepository.save(saved);
    }

    // Then
    AuditEntry<TestProduct> first = productRepository.audit()
        .forEntityId(saved.getId())
        .first();

    assertThat(first).isNotNull();
    assertThat(first.getOperation()).isEqualTo(AuditOperation.INSERT);

    AuditEntry<TestProduct> last = productRepository.audit()
        .forEntityId(saved.getId())
        .last();

    assertThat(last).isNotNull();
    assertThat(last.getOperation()).isEqualTo(AuditOperation.UPDATE);
  }

  @Test
  @DisplayName("Should count audit entries")
  void shouldCountAuditEntries() {
    // Given
    TestProduct product = new TestProduct();
    product.setName("Test Product");
    TestProduct saved = productRepository.save(product);

    for (int i = 0; i < 3; i++) {
      saved.setPrice(100.0 + i);
      productRepository.save(saved);
    }

    // Then
    long count = productRepository.audit()
        .forEntityId(saved.getId())
        .count();

    assertThat(count).isEqualTo(4);  // 1 INSERT + 3 UPDATEs
  }

  @Test
  @DisplayName("Should support audit retention policies")
  void shouldSupportAuditRetentionPolicies() {
    // Given - Create old and new audit entries
    TestProduct product = new TestProduct();
    product.setName("Test Product");
    TestProduct saved = productRepository.save(product);

    Instant cutoffTime = Instant.now().plus(1, ChronoUnit.SECONDS);

    // When - Delete old entries
    long deletedCount = productRepository.audit()
        .deleteEntriesOlderThan(cutoffTime);

    // Then
    assertThat(deletedCount).isGreaterThan(0);
    assertThat(productRepository.audit().count()).isEqualTo(0);
  }

  // Test entity classes

  @Audited
  public static class TestProduct implements Entity<Long> {
    private Long id;
    private String name;
    private double price;
    private int stock;

    @Override
    public Long getId() {
      return id;
    }

    @Override
    public void setId(Long id) {
      this.id = id;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
  }

  // Not audited
  public static class TestUser implements Entity<Long> {
    private Long id;
    private String username;
    private String fullName;

    @Override
    public Long getId() {
      return id;
    }

    @Override
    public void setId(Long id) {
      this.id = id;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
  }

  // Test repository implementations

  static class TestProductRepository extends AbstractRepository<TestProduct, Long> {
    private final List<TestProduct> storage = new ArrayList<>();
    private long nextId = 1L;

    TestProductRepository() {
      super(TestProduct.class);
    }

    @Override
    protected TestProduct doSave(TestProduct entity) {
      if (entity.getId() == null) {
        entity.setId(nextId++);
        storage.add(entity);
      } else {
        storage.removeIf(p -> p.getId().equals(entity.getId()));
        storage.add(entity);
      }
      return entity;
    }

    @Override
    protected Optional<TestProduct> doFindById(Long id) {
      return storage.stream()
          .filter(p -> p.getId().equals(id))
          .findFirst();
    }

    @Override
    protected void doDelete(Long id) {
      storage.removeIf(p -> p.getId().equals(id));
    }

    @Override
    protected List<TestProduct> doFindAll() {
      return new ArrayList<>(storage);
    }

    @Override
    protected long doCount() {
      return storage.size();
    }

    @Override
    public io.dataverse.api.QueryBuilder<TestProduct> query() {
      return null;
    }

    @Override
    public io.dataverse.api.AggregationBuilder<TestProduct> aggregate() {
      return null;
    }

    @Override
    public List<TestProduct> executeNativeQuery(String nativeQuery) {
      return null;
    }
  }

  static class TestUserRepository extends AbstractRepository<TestUser, Long> {
    private final List<TestUser> storage = new ArrayList<>();
    private long nextId = 1L;

    TestUserRepository() {
      super(TestUser.class);
    }

    @Override
    protected TestUser doSave(TestUser entity) {
      if (entity.getId() == null) {
        entity.setId(nextId++);
        storage.add(entity);
      }
      return entity;
    }

    @Override
    protected Optional<TestUser> doFindById(Long id) {
      return storage.stream()
          .filter(u -> u.getId().equals(id))
          .findFirst();
    }

    @Override
    protected void doDelete(Long id) {
      storage.removeIf(u -> u.getId().equals(id));
    }

    @Override
    protected List<TestUser> doFindAll() {
      return new ArrayList<>(storage);
    }

    @Override
    protected long doCount() {
      return storage.size();
    }

    @Override
    public io.dataverse.api.QueryBuilder<TestUser> query() {
      return null;
    }

    @Override
    public io.dataverse.api.AggregationBuilder<TestUser> aggregate() {
      return null;
    }

    @Override
    public List<TestUser> executeNativeQuery(String nativeQuery) {
      return null;
    }
  }
}
