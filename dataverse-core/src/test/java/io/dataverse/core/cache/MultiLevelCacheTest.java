package io.dataverse.core.cache;

import io.dataverse.api.Entity;
import io.dataverse.core.AbstractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for multi-level caching functionality.
 *
 * @since 1.0.0
 */
@DisplayName("Multi-Level Cache Tests")
class MultiLevelCacheTest {

  private TestProductRepository repository;
  private TestProductRepository repositoryWithoutCache;

  @BeforeEach
  void setUp() {
    CacheConfig cacheConfig = CacheConfig.builder()
        .enabled(true)
        .l1Enabled(true)
        .l1MaxSize(100)
        .l1Ttl(Duration.ofMinutes(5))
        .recordStats(true)
        .build();

    repository = new TestProductRepository(cacheConfig);
    repositoryWithoutCache = new TestProductRepository(
        CacheConfig.builder().enabled(false).build()
    );
  }

  @Test
  @DisplayName("Should cache entity on first read")
  void shouldCacheEntityOnFirstRead() {
    // Given
    TestProduct product = new TestProduct();
    product.setName("Laptop");
    product.setPrice(999.99);
    TestProduct saved = repository.save(product);

    // Clear L1 cache to test cache population
    repository.cacheProvider.clear();

    // When - First read (cache miss)
    Optional<TestProduct> found1 = repository.findById(saved.getId());

    // Then - Entity should be cached now
    assertThat(found1).isPresent();
    assertThat(repository.cacheProvider.containsKey(
        repository.cacheKeyGenerator.generateKey(saved.getId()))).isTrue();

    // When - Second read (cache hit)
    Optional<TestProduct> found2 = repository.findById(saved.getId());

    // Then - Same entity from cache
    assertThat(found2).isPresent();
    assertThat(found2.get().getName()).isEqualTo("Laptop");
  }

  @Test
  @DisplayName("Should update cache on save")
  void shouldUpdateCacheOnSave() {
    // Given
    TestProduct product = new TestProduct();
    product.setName("Mouse");
    product.setPrice(29.99);
    TestProduct saved = repository.save(product);

    // When - Update entity
    saved.setPrice(24.99);
    repository.save(saved);

    // Then - Cache should have updated value
    String cacheKey = repository.cacheKeyGenerator.generateKey(saved.getId());
    Optional<TestProduct> cached = repository.cacheProvider.get(cacheKey);
    assertThat(cached).isPresent();
    assertThat(cached.get().getPrice()).isEqualTo(24.99);
  }

  @Test
  @DisplayName("Should evict from cache on delete")
  void shouldEvictFromCacheOnDelete() {
    // Given
    TestProduct product = new TestProduct();
    product.setName("Keyboard");
    product.setPrice(79.99);
    TestProduct saved = repository.save(product);

    // Verify cached
    String cacheKey = repository.cacheKeyGenerator.generateKey(saved.getId());
    assertThat(repository.cacheProvider.containsKey(cacheKey)).isTrue();

    // When - Delete entity
    repository.deleteById(saved.getId());

    // Then - Should be evicted from cache
    assertThat(repository.cacheProvider.containsKey(cacheKey)).isFalse();
  }

  @Test
  @DisplayName("Should improve read performance with caching")
  void shouldImproveReadPerformanceWithCaching() {
    // Given - Create test data
    List<TestProduct> products = new ArrayList<>();
    for (int i = 1; i <= 100; i++) {
      TestProduct product = new TestProduct();
      product.setName("Product " + i);
      product.setPrice(100.0 * i);
      products.add(repository.save(product));
    }

    // Warm up cache
    for (TestProduct product : products) {
      repository.findById(product.getId());
    }

    // When - Read with cache
    long startCached = System.nanoTime();
    for (TestProduct product : products) {
      repository.findById(product.getId());
    }
    long cachedDuration = System.nanoTime() - startCached;

    // When - Read without cache
    long startUncached = System.nanoTime();
    for (TestProduct product : products) {
      repositoryWithoutCache.findById(product.getId());
    }
    long uncachedDuration = System.nanoTime() - startUncached;

    // Then - Cached reads should be faster
    System.out.println("Cached reads: " + cachedDuration / 1_000_000 + "ms");
    System.out.println("Uncached reads: " + uncachedDuration / 1_000_000 + "ms");
    System.out.println("Performance improvement: " +
        (uncachedDuration / (double) cachedDuration) + "x");

    assertThat(cachedDuration).isLessThan(uncachedDuration);
  }

  @Test
  @DisplayName("Should track cache statistics")
  void shouldTrackCacheStatistics() {
    // Given
    TestProduct product = new TestProduct();
    product.setName("Monitor");
    product.setPrice(399.99);
    TestProduct saved = repository.save(product);

    // Clear cache to start fresh
    repository.cacheProvider.clear();

    // When - Multiple reads (1 miss + 9 hits)
    for (int i = 0; i < 10; i++) {
      repository.findById(saved.getId());
    }

    // Then - Check statistics
    CacheProvider.CacheStats stats = repository.cacheProvider.getStats();
    assertThat(stats.getHitCount()).isGreaterThan(0);
    assertThat(stats.getHitRate()).isGreaterThan(0.5);  // Should be 90%

    System.out.println("Cache stats: " + stats);
  }

  @Test
  @DisplayName("Should respect TTL expiration")
  void shouldRespectTtlExpiration() throws Exception {
    // Given - Cache with short TTL
    CacheConfig shortTtlConfig = CacheConfig.builder()
        .enabled(true)
        .l1Enabled(true)
        .l1Ttl(Duration.ofMillis(100))
        .build();

    TestProductRepository shortTtlRepo = new TestProductRepository(shortTtlConfig);

    TestProduct product = new TestProduct();
    product.setName("Temporary");
    product.setPrice(50.0);
    TestProduct saved = shortTtlRepo.save(product);

    // When - Read immediately (should be cached)
    String cacheKey = shortTtlRepo.cacheKeyGenerator.generateKey(saved.getId());
    assertThat(shortTtlRepo.cacheProvider.containsKey(cacheKey)).isTrue();

    // Wait for TTL to expire
    Thread.sleep(150);

    // Then - Cache entry should be expired
    assertThat(shortTtlRepo.cacheProvider.containsKey(cacheKey)).isFalse();
  }

  @Test
  @DisplayName("Should handle cache size limits with eviction")
  void shouldHandleCacheSizeLimitsWithEviction() {
    // Given - Cache with small size limit
    CacheConfig smallCacheConfig = CacheConfig.builder()
        .enabled(true)
        .l1Enabled(true)
        .l1MaxSize(10)
        .build();

    TestProductRepository smallCacheRepo = new TestProductRepository(smallCacheConfig);

    // When - Add more items than cache size
    List<TestProduct> products = new ArrayList<>();
    for (int i = 1; i <= 20; i++) {
      TestProduct product = new TestProduct();
      product.setName("Product " + i);
      product.setPrice(100.0 * i);
      products.add(smallCacheRepo.save(product));
    }

    // Then - Cache size should not exceed limit
    CacheProvider.CacheStats stats = smallCacheRepo.cacheProvider.getStats();
    assertThat(stats.getSize()).isLessThanOrEqualTo(10);
    assertThat(stats.getEvictionCount()).isGreaterThan(0);
  }

  @Test
  @DisplayName("Should support cache-aside pattern")
  void shouldSupportCacheAsidePattern() {
    // Given
    TestProduct product = new TestProduct();
    product.setName("Headphones");
    product.setPrice(199.99);
    TestProduct saved = repository.save(product);

    // Clear cache
    String cacheKey = repository.cacheKeyGenerator.generateKey(saved.getId());
    repository.cacheProvider.evict(cacheKey);

    // When - Use computeIfAbsent
    TestProduct result = repository.cacheProvider.computeIfAbsent(cacheKey, () -> {
      System.out.println("Loading from data source...");
      return repository.doFindById(saved.getId()).orElse(null);
    });

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("Headphones");

    // Second call should hit cache
    TestProduct cached = repository.cacheProvider.computeIfAbsent(cacheKey, () -> {
      throw new RuntimeException("Should not be called!");
    });
    assertThat(cached.getName()).isEqualTo("Headphones");
  }

  @Test
  @DisplayName("Should work correctly when caching is disabled")
  void shouldWorkCorrectlyWhenCachingIsDisabled() {
    // Given - Repository with caching disabled
    TestProduct product = new TestProduct();
    product.setName("Test");
    product.setPrice(100.0);
    TestProduct saved = repositoryWithoutCache.save(product);

    // When
    Optional<TestProduct> found = repositoryWithoutCache.findById(saved.getId());

    // Then - Should still work, just without caching
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("Test");
  }

  // Test entity and repository

  public static class TestProduct implements Entity<Long> {
    private Long id;
    private String name;
    private double price;

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
  }

  static class TestProductRepository extends AbstractRepository<TestProduct, Long> {
    private final List<TestProduct> storage = new ArrayList<>();
    private long nextId = 1L;

    TestProductRepository(CacheConfig cacheConfig) {
      super(TestProduct.class, cacheConfig);
    }

    @Override
    public TestProduct doSave(TestProduct entity) {
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
    public Optional<TestProduct> doFindById(Long id) {
      // Simulate database access delay
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      return storage.stream()
          .filter(p -> p.getId().equals(id))
          .findFirst();
    }

    @Override
    public void doDelete(Long id) {
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
}
