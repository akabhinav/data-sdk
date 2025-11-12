package io.dataverse.example.springboot.service;

import io.dataverse.adapter.dynamodb.DynamoDBAdapter;
import io.dataverse.adapter.mongodb.MongoDBAdapter;
import io.dataverse.adapter.redis.RedisAdapter;
import io.dataverse.api.BatchOperations;
import io.dataverse.api.Repository;
import io.dataverse.example.springboot.model.Product;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Product service demonstrating multi-tier data access with DataVerse SDK.
 *
 * <p>Uses a three-tier architecture:
 * <ul>
 *   <li>Redis - Fast cache layer</li>
 *   <li>DynamoDB - Primary persistent store</li>
 *   <li>MongoDB - Analytics and reporting</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Service
public class ProductService {

  private final Repository<Product, String> primaryRepo;  // DynamoDB
  private final Repository<Product, String> cacheRepo;     // Redis
  private final Repository<Product, String> analyticsRepo; // MongoDB

  public ProductService(
      DynamoDBAdapter dynamoDBAdapter,
      RedisAdapter redisAdapter,
      MongoDBAdapter mongoDBAdapter) {
    this.primaryRepo = dynamoDBAdapter.createRepository(Product.class);
    this.cacheRepo = redisAdapter.createRepository(Product.class);
    this.analyticsRepo = mongoDBAdapter.createRepository(Product.class);
  }

  /**
   * Create a new product (write-through cache pattern).
   */
  public Product createProduct(Product product) {
    // 1. Save to primary store
    Product saved = primaryRepo.save(product);

    // 2. Write-through to cache
    cacheRepo.save(saved);

    // 3. Write to analytics store
    analyticsRepo.save(saved);

    return saved;
  }

  /**
   * Get product by ID (read-through cache pattern).
   */
  public Optional<Product> getProduct(String id) {
    // 1. Try cache first
    Optional<Product> fromCache = cacheRepo.findById(id);

    if (fromCache.isPresent()) {
      return fromCache;
    }

    // 2. Cache miss - read from primary store
    Optional<Product> fromPrimary = primaryRepo.findById(id);

    // 3. Populate cache if found
    fromPrimary.ifPresent(cacheRepo::save);

    return fromPrimary;
  }

  /**
   * Update product (invalidate cache pattern).
   */
  public Product updateProduct(String id, Product updates) {
    // 1. Update primary store
    updates.setId(id);
    Product updated = primaryRepo.save(updates);

    // 2. Invalidate cache
    cacheRepo.deleteById(id);

    // 3. Update analytics store
    analyticsRepo.save(updated);

    return updated;
  }

  /**
   * Delete product.
   */
  public void deleteProduct(String id) {
    // Delete from all stores
    primaryRepo.deleteById(id);
    cacheRepo.deleteById(id);
    analyticsRepo.deleteById(id);
  }

  /**
   * Get all products (from primary store).
   */
  public List<Product> getAllProducts() {
    return primaryRepo.findAll();
  }

  /**
   * Bulk create products (using batch operations).
   */
  public BatchOperations.BatchResult<Product> bulkCreate(List<Product> products) {
    // Batch insert to primary store
    BatchOperations<Product, String> primaryBatch = primaryRepo.batch();
    BatchOperations.BatchResult<Product> result = primaryBatch.upsertAll(products);

    // Sync successful inserts to analytics
    if (result.isAllSuccessful()) {
      BatchOperations<Product, String> analyticsBatch = analyticsRepo.batch();
      analyticsBatch.upsertAll(result.getSuccessful());

      // Cache hot items (first 20)
      List<Product> hotItems = result.getSuccessful().stream()
          .limit(20)
          .toList();

      BatchOperations<Product, String> cacheBatch = cacheRepo.batch();
      cacheBatch.upsertAll(hotItems);
    }

    return result;
  }

  /**
   * Get aggregated statistics from MongoDB.
   */
  public java.util.Map<String, Double> getCategoryTotals() {
    return analyticsRepo.aggregate()
        .groupBy("category")
        .sum("price")
        .execute();
  }
}
