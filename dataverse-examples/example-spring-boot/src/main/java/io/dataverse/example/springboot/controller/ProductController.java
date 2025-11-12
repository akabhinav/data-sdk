package io.dataverse.example.springboot.controller;

import io.dataverse.api.BatchOperations;
import io.dataverse.example.springboot.model.Product;
import io.dataverse.example.springboot.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for product management using DataVerse SDK.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/products - Create product</li>
 *   <li>GET /api/products/{id} - Get product by ID</li>
 *   <li>GET /api/products - Get all products</li>
 *   <li>PUT /api/products/{id} - Update product</li>
 *   <li>DELETE /api/products/{id} - Delete product</li>
 *   <li>POST /api/products/bulk - Bulk create products</li>
 *   <li>GET /api/products/stats/categories - Get category statistics</li>
 * </ul>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

  private final ProductService productService;

  public ProductController(ProductService productService) {
    this.productService = productService;
  }

  @PostMapping
  public ResponseEntity<Product> createProduct(@RequestBody Product product) {
    Product created = productService.createProduct(product);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @GetMapping("/{id}")
  public ResponseEntity<Product> getProduct(@PathVariable String id) {
    return productService.getProduct(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping
  public ResponseEntity<List<Product>> getAllProducts() {
    List<Product> products = productService.getAllProducts();
    return ResponseEntity.ok(products);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Product> updateProduct(
      @PathVariable String id,
      @RequestBody Product product) {
    Product updated = productService.updateProduct(id, product);
    return ResponseEntity.ok(updated);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
    productService.deleteProduct(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/bulk")
  public ResponseEntity<BulkCreateResponse> bulkCreate(@RequestBody List<Product> products) {
    BatchOperations.BatchResult<Product> result = productService.bulkCreate(products);

    BulkCreateResponse response = new BulkCreateResponse(
        result.getSuccessCount(),
        result.getFailureCount(),
        result.getInsertedCount(),
        result.isAllSuccessful()
    );

    return ResponseEntity.ok(response);
  }

  @GetMapping("/stats/categories")
  public ResponseEntity<Map<String, Double>> getCategoryStats() {
    Map<String, Double> stats = productService.getCategoryTotals();
    return ResponseEntity.ok(stats);
  }

  /**
   * Bulk create response DTO.
   */
  public record BulkCreateResponse(
      int successCount,
      int failureCount,
      int insertedCount,
      boolean allSuccessful
  ) {
  }
}
