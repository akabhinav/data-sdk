package io.dataverse.examples;

import io.dataverse.core.replica.ReadReplica;
import io.dataverse.core.replica.ReplicaConfig;
import io.dataverse.core.replica.ReplicaHealthChecker;
import io.dataverse.core.replica.ReplicaInfo;
import io.dataverse.core.replica.ReplicaRouter;

import java.time.Duration;
import java.util.List;

/**
 * Example demonstrating Read Replicas Support (Feature #9).
 *
 * <p>Use Cases:
 * <ul>
 *   <li>Distributing read load across multiple database replicas</li>
 *   <li>Geographic routing for low-latency reads</li>
 *   <li>Automatic failover when replicas become unhealthy</li>
 *   <li>Connection pooling optimization</li>
 * </ul>
 *
 * @author DataVerse SDK Team
 */
public class ReadReplicasExample {

  public static void main(String[] args) {
    System.out.println("=== Read Replicas Support Examples ===\n");

    example1_BasicRoundRobinRouting();
    example2_GeographicRouting();
    example3_LeastConnectionsRouting();
    example4_AutomaticFailover();
    example5_HealthChecking();
    example6_CustomConfiguration();
  }

  /**
   * Example 1: Basic Round Robin Routing
   *
   * <p>Use Case: E-commerce application with read-heavy workload.
   * Distribute SELECT queries across 3 replicas for load balancing.
   */
  static void example1_BasicRoundRobinRouting() {
    System.out.println("Example 1: Basic Round Robin Routing");
    System.out.println("Use Case: E-commerce product catalog queries\n");

    // Configure replicas
    String primaryUrl = "jdbc:postgresql://primary.db.example.com:5432/ecommerce";
    List<ReplicaInfo> replicas = List.of(
        new ReplicaInfo("jdbc:postgresql://replica1.db.example.com:5432/ecommerce", "us-east-1"),
        new ReplicaInfo("jdbc:postgresql://replica2.db.example.com:5432/ecommerce", "us-east-1"),
        new ReplicaInfo("jdbc:postgresql://replica3.db.example.com:5432/ecommerce", "us-east-1")
    );

    ReplicaRouter router = new ReplicaRouter(primaryUrl, replicas);

    // Execute read queries - automatically distributed round-robin
    for (int i = 1; i <= 6; i++) {
      String replicaUrl = router.selectReplica(ReadReplica.RoutingStrategy.ROUND_ROBIN);
      System.out.println("Query " + i + " -> " + replicaUrl);
    }

    System.out.println("\nResult: Queries evenly distributed across replicas\n");
    System.out.println("---\n");
  }

  /**
   * Example 2: Geographic Routing
   *
   * <p>Use Case: Global SaaS application serving multiple regions.
   * Route reads to geographically closest replica for minimum latency.
   */
  static void example2_GeographicRouting() {
    System.out.println("Example 2: Geographic Routing");
    System.out.println("Use Case: Global user profile queries\n");

    String primaryUrl = "jdbc:postgresql://primary.db.example.com:5432/saas";
    List<ReplicaInfo> replicas = List.of(
        new ReplicaInfo("jdbc:postgresql://us-east-replica.db.example.com:5432/saas", "us-east-1"),
        new ReplicaInfo("jdbc:postgresql://eu-west-replica.db.example.com:5432/saas", "eu-west-1"),
        new ReplicaInfo("jdbc:postgresql://ap-south-replica.db.example.com:5432/saas", "ap-south-1")
    );

    ReplicaRouter router = new ReplicaRouter(primaryUrl, replicas);

    // Simulate requests from different regions
    String[] regions = {"us-east-1", "eu-west-1", "ap-south-1", "us-east-1"};
    for (String region : regions) {
      // In real application, set ThreadLocal with user's region
      String replicaUrl = router.selectReplica(ReadReplica.RoutingStrategy.GEOGRAPHIC);
      System.out.println("Request from " + region + " -> closest replica");
    }

    System.out.println("\nResult: Each request routed to geographically closest replica\n");
    System.out.println("---\n");
  }

  /**
   * Example 3: Least Connections Routing
   *
   * <p>Use Case: Analytics dashboard with varying query complexity.
   * Route to replica with fewest active connections.
   */
  static void example3_LeastConnectionsRouting() {
    System.out.println("Example 3: Least Connections Routing");
    System.out.println("Use Case: Analytics queries with mixed complexity\n");

    String primaryUrl = "jdbc:postgresql://primary.db.example.com:5432/analytics";
    List<ReplicaInfo> replicas = List.of(
        new ReplicaInfo("jdbc:postgresql://replica1.db.example.com:5432/analytics", "us-east-1"),
        new ReplicaInfo("jdbc:postgresql://replica2.db.example.com:5432/analytics", "us-east-1")
    );

    // Simulate connection counts
    replicas.get(0).incrementConnections(); // Replica 1: 1 connection
    replicas.get(0).incrementConnections(); // Replica 1: 2 connections
    replicas.get(1).incrementConnections(); // Replica 2: 1 connection

    ReplicaRouter router = new ReplicaRouter(primaryUrl, replicas);

    // New query routed to replica with least connections
    String replicaUrl = router.selectReplica(ReadReplica.RoutingStrategy.LEAST_CONNECTIONS);
    System.out.println("New query routed to: " + replicaUrl);
    System.out.println("Reason: Replica 2 has fewer active connections (1 vs 2)");

    System.out.println("\nResult: Optimal load distribution based on actual connections\n");
    System.out.println("---\n");
  }

  /**
   * Example 4: Automatic Failover
   *
   * <p>Use Case: Database maintenance or replica failure.
   * Automatically route to primary when all replicas are unhealthy.
   */
  static void example4_AutomaticFailover() {
    System.out.println("Example 4: Automatic Failover");
    System.out.println("Use Case: Replica maintenance window\n");

    String primaryUrl = "jdbc:postgresql://primary.db.example.com:5432/production";
    List<ReplicaInfo> replicas = List.of(
        new ReplicaInfo("jdbc:postgresql://replica1.db.example.com:5432/production", "us-east-1"),
        new ReplicaInfo("jdbc:postgresql://replica2.db.example.com:5432/production", "us-east-1")
    );

    ReplicaRouter router = new ReplicaRouter(primaryUrl, replicas);

    // Before maintenance: queries go to replicas
    System.out.println("Before maintenance:");
    String replicaUrl = router.selectReplica(ReadReplica.RoutingStrategy.ROUND_ROBIN);
    System.out.println("  Query -> " + replicaUrl);

    // Simulate replicas going down for maintenance
    replicas.get(0).markUnhealthy();
    replicas.get(1).markUnhealthy();

    System.out.println("\nDuring maintenance (all replicas unhealthy):");
    String failoverUrl = router.selectReplica(ReadReplica.RoutingStrategy.ROUND_ROBIN);
    System.out.println("  Query -> " + failoverUrl);
    System.out.println("  Automatically failed over to primary!");

    // Replicas come back online
    replicas.get(0).markHealthy();
    replicas.get(1).markHealthy();

    System.out.println("\nAfter maintenance:");
    replicaUrl = router.selectReplica(ReadReplica.RoutingStrategy.ROUND_ROBIN);
    System.out.println("  Query -> " + replicaUrl);

    System.out.println("\nResult: Zero downtime during replica maintenance\n");
    System.out.println("---\n");
  }

  /**
   * Example 5: Health Checking
   *
   * <p>Use Case: Proactive monitoring of replica health.
   * Periodically check replica availability and mark unhealthy ones.
   */
  static void example5_HealthChecking() {
    System.out.println("Example 5: Health Checking");
    System.out.println("Use Case: Continuous replica health monitoring\n");

    ReplicaConfig config = ReplicaConfig.builder()
        .healthCheckInterval(Duration.ofSeconds(30))
        .healthCheckTimeout(Duration.ofSeconds(5))
        .maxRetries(3)
        .retryDelay(Duration.ofSeconds(1))
        .build();

    String primaryUrl = "jdbc:postgresql://primary.db.example.com:5432/production";
    List<ReplicaInfo> replicas = List.of(
        new ReplicaInfo("jdbc:postgresql://replica1.db.example.com:5432/production", "us-east-1"),
        new ReplicaInfo("jdbc:postgresql://replica2.db.example.com:5432/production", "us-east-1")
    );

    ReplicaHealthChecker healthChecker = new ReplicaHealthChecker(
        replicas,
        config,
        replicaUrl -> {
          // Simulate health check query
          System.out.println("  Checking health: " + replicaUrl);
          return true; // In real app: execute "SELECT 1"
        }
    );

    System.out.println("Starting health checker...");
    healthChecker.start();

    System.out.println("Health checker running:");
    System.out.println("  - Checks every " + config.getHealthCheckInterval().getSeconds() + " seconds");
    System.out.println("  - Timeout: " + config.getHealthCheckTimeout().getSeconds() + " seconds");
    System.out.println("  - Max retries: " + config.getMaxRetries());

    // In real application, health checker runs continuously
    // healthChecker.stop(); // Call on shutdown

    System.out.println("\nResult: Proactive health monitoring prevents routing to dead replicas\n");
    System.out.println("---\n");
  }

  /**
   * Example 6: Custom Configuration
   *
   * <p>Use Case: Fine-tuned replica configuration for specific workload.
   * Configure health check intervals, timeouts, and retry behavior.
   */
  static void example6_CustomConfiguration() {
    System.out.println("Example 6: Custom Configuration");
    System.out.println("Use Case: High-throughput streaming application\n");

    // High-frequency health checks for critical application
    ReplicaConfig config = ReplicaConfig.builder()
        .healthCheckInterval(Duration.ofSeconds(10))  // Check every 10s
        .healthCheckTimeout(Duration.ofSeconds(2))    // Fast timeout
        .maxRetries(5)                                 // More retries
        .retryDelay(Duration.ofMillis(500))           // Quick retries
        .build();

    System.out.println("Configuration:");
    System.out.println("  Health check interval: " + config.getHealthCheckInterval().getSeconds() + "s");
    System.out.println("  Health check timeout: " + config.getHealthCheckTimeout().getSeconds() + "s");
    System.out.println("  Max retries: " + config.getMaxRetries());
    System.out.println("  Retry delay: " + config.getRetryDelay().toMillis() + "ms");

    System.out.println("\nBenefits:");
    System.out.println("  ✓ Faster detection of replica failures");
    System.out.println("  ✓ Reduced query timeouts");
    System.out.println("  ✓ Better resilience with more retries");
    System.out.println("  ✓ Minimized impact on user experience");

    System.out.println("\nResult: Optimized for low-latency, high-availability requirements\n");
    System.out.println("---\n");
  }

  /**
   * Example 7: Repository Integration
   *
   * <p>Use Case: Transparent replica routing in repository methods.
   */
  static class ProductRepository {
    private final ReplicaRouter router;

    public ProductRepository(ReplicaRouter router) {
      this.router = router;
    }

    @ReadReplica(strategy = ReadReplica.RoutingStrategy.ROUND_ROBIN)
    public List<Product> findAll() {
      String replicaUrl = router.selectReplica(ReadReplica.RoutingStrategy.ROUND_ROBIN);
      System.out.println("findAll() -> " + replicaUrl);
      // Execute SELECT * FROM products
      return List.of(new Product(1L, "Laptop"), new Product(2L, "Phone"));
    }

    @ReadReplica(strategy = ReadReplica.RoutingStrategy.GEOGRAPHIC)
    public Product findById(Long id) {
      String replicaUrl = router.selectReplica(ReadReplica.RoutingStrategy.GEOGRAPHIC);
      System.out.println("findById(" + id + ") -> " + replicaUrl);
      // Execute SELECT * FROM products WHERE id = ?
      return new Product(id, "Product " + id);
    }

    // Write operations always go to primary
    public Product save(Product product) {
      System.out.println("save() -> PRIMARY (writes always to primary)");
      // Execute INSERT/UPDATE on primary
      return product;
    }
  }

  static class Product {
    private final Long id;
    private final String name;

    Product(Long id, String name) {
      this.id = id;
      this.name = name;
    }

    @Override
    public String toString() {
      return "Product{id=" + id + ", name='" + name + "'}";
    }
  }
}
