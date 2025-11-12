package io.dataverse.examples;

import io.dataverse.core.hotreload.HotReloadManager;
import io.dataverse.core.hotreload.Reloadable;
import io.dataverse.core.hotreload.ReloadListener;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Properties;

/**
 * Example demonstrating Hot Reload (Feature #20).
 *
 * <p>Use Cases:
 * <ul>
 *   <li>Configuration changes without restart</li>
 *   <li>Query mapping updates</li>
 *   <li>Feature flag toggling</li>
 *   <li>Connection pool reconfiguration</li>
 * </ul>
 *
 * @author DataVerse SDK Team
 */
public class HotReloadExample {

  public static void main(String[] args) throws Exception {
    System.out.println("=== Hot Reload Examples ===\n");

    example1_BasicFileWatching();
    example2_DatabaseConfigReload();
    example3_QueryMappingsReload();
    example4_ListenerNotifications();
    example5_DirectoryWatching();
    example6_ProductionScenario();
  }

  /**
   * Example 1: Basic File Watching
   *
   * <p>Use Case: Monitor single configuration file.
   */
  static void example1_BasicFileWatching() {
    System.out.println("Example 1: Basic File Watching");
    System.out.println("Use Case: Application properties reload\n");

    HotReloadManager manager = new HotReloadManager();

    System.out.println("Setup:");
    System.out.println("  manager.watch(");
    System.out.println("      Path.of(\"config/application.properties\"),");
    System.out.println("      () -> reloadConfig()");
    System.out.println("  );");
    System.out.println("  manager.start();");

    // Register file
    manager.watch(
        Path.of("config/application.properties"),
        () -> {
          System.out.println("  ✓ Configuration reloaded!");
        }
    );

    System.out.println("\nMonitoring started...");
    System.out.println("  Checking: config/application.properties");
    System.out.println("  Frequency: Every 1 second");

    System.out.println("\nUser edits file:");
    System.out.println("  $ vim config/application.properties");
    System.out.println("  # Change: db.pool.size=10 → db.pool.size=20");
    System.out.println("  # Save file");

    System.out.println("\nHot Reload detects change:");
    System.out.println("  [10:05:23] File modified: config/application.properties");
    System.out.println("  [10:05:23] Executing reload action...");
    System.out.println("  [10:05:23] ✓ Configuration reloaded!");
    System.out.println("  [10:05:23] New pool size: 20");

    System.out.println("\nBenefits:");
    System.out.println("  ✓ Zero downtime");
    System.out.println("  ✓ No application restart");
    System.out.println("  ✓ Instant configuration updates");

    System.out.println("\nResult: Live configuration updates\n");
    System.out.println("---\n");
  }

  /**
   * Example 2: Database Configuration Reload
   *
   * <p>Use Case: Update database connection settings on-the-fly.
   */
  static void example2_DatabaseConfigReload() {
    System.out.println("Example 2: Database Configuration Reload");
    System.out.println("Use Case: Connection pool tuning\n");

    System.out.println("Initial configuration (database.properties):");
    System.out.println("  db.pool.size=10");
    System.out.println("  db.pool.timeout=30s");
    System.out.println("  db.pool.maxLifetime=30m");

    // Simulated database config
    DatabaseConfig config = new DatabaseConfig();

    HotReloadManager manager = new HotReloadManager();
    manager.watch(
        Path.of("config/database.properties"),
        () -> {
          System.out.println("  Reloading database configuration...");
          config.reload();
          System.out.println("  ✓ Pool reconfigured");
        }
    );

    System.out.println("\nProduction issue detected:");
    System.out.println("  [ERROR] Connection timeout - pool exhausted");
    System.out.println("  Current connections: 10/10");
    System.out.println("  Waiting requests: 45");

    System.out.println("\nAdmin action:");
    System.out.println("  $ echo 'db.pool.size=25' >> database.properties");

    System.out.println("\nHot reload triggered:");
    System.out.println("  [10:15:30] Change detected");
    System.out.println("  [10:15:30] Reloading database configuration...");
    System.out.println("  [10:15:30] Closing idle connections");
    System.out.println("  [10:15:30] Creating new pool with size: 25");
    System.out.println("  [10:15:30] ✓ Pool reconfigured");
    System.out.println("  [10:15:31] Waiting requests now processing");

    System.out.println("\nResult:");
    System.out.println("  Problem resolved in 1 second");
    System.out.println("  No deployment needed");
    System.out.println("  Zero downtime");

    System.out.println("\nResult: Dynamic capacity scaling\n");
    System.out.println("---\n");
  }

  /**
   * Example 3: Query Mappings Reload
   *
   * <p>Use Case: Update SQL queries without redeployment.
   */
  static void example3_QueryMappingsReload() {
    System.out.println("Example 3: Query Mappings Reload");
    System.out.println("Use Case: Optimize slow query in production\n");

    System.out.println("Query mappings (queries.xml):");
    System.out.println("  <query id=\"findActiveUsers\">");
    System.out.println("    SELECT * FROM users WHERE status = 'ACTIVE'");
    System.out.println("  </query>");

    System.out.println("\nPerformance problem:");
    System.out.println("  Query execution: 5000ms (SLOW!)");
    System.out.println("  Missing index on status column");

    System.out.println("\nDeveloper investigates:");
    System.out.println("  $ EXPLAIN SELECT * FROM users WHERE status = 'ACTIVE'");
    System.out.println("  → Seq Scan on users (cost=0..1000)");
    System.out.println("  → Need to add index!");

    System.out.println("\nDBA creates index:");
    System.out.println("  $ CREATE INDEX idx_users_status ON users(status);");

    System.out.println("\nOptimize query (queries.xml):");
    System.out.println("  <query id=\"findActiveUsers\">");
    System.out.println("    SELECT id, name, email FROM users");
    System.out.println("    WHERE status = 'ACTIVE'");
    System.out.println("    ORDER BY created_at DESC");
    System.out.println("  </query>");

    QueryMapper mapper = new QueryMapper();
    HotReloadManager manager = new HotReloadManager();
    manager.watch(
        Path.of("config/queries.xml"),
        () -> {
          System.out.println("  ✓ Query mappings reloaded");
          mapper.reload();
        }
    );

    System.out.println("\nHot reload activates:");
    System.out.println("  [11:20:15] queries.xml modified");
    System.out.println("  [11:20:15] ✓ Query mappings reloaded");
    System.out.println("  [11:20:15] New query registered: findActiveUsers");

    System.out.println("\nNext request uses optimized query:");
    System.out.println("  Query execution: 45ms (111x faster!)");
    System.out.println("  Using index: idx_users_status");

    System.out.println("\nResult: Instant query optimization\n");
    System.out.println("---\n");
  }

  /**
   * Example 4: Listener Notifications
   *
   * <p>Use Case: Audit and monitoring of configuration changes.
   */
  static void example4_ListenerNotifications() {
    System.out.println("Example 4: Listener Notifications");
    System.out.println("Use Case: Audit configuration changes\n");

    HotReloadManager manager = new HotReloadManager();

    // Add audit listener
    manager.addListener(new ReloadListener() {
      @Override
      public void beforeReload(Path path) {
        System.out.println("  [AUDIT] Starting reload: " + path);
        System.out.println("  [AUDIT] Timestamp: " + Instant.now());
        System.out.println("  [AUDIT] User: admin");
      }

      @Override
      public void afterReload(Path path) {
        System.out.println("  [AUDIT] ✓ Reload successful: " + path);
        System.out.println("  [AUDIT] Notifying monitoring system...");
      }

      @Override
      public void onReloadError(Path path, Exception error) {
        System.out.println("  [AUDIT] ✗ Reload failed: " + path);
        System.out.println("  [AUDIT] Error: " + error.getMessage());
        System.out.println("  [AUDIT] Alerting on-call team...");
      }
    });

    System.out.println("Listener registered: AuditListener");

    System.out.println("\nConfiguration change:");
    System.out.println("  $ vim config/database.properties");

    System.out.println("\nEvent log:");
    System.out.println("  [AUDIT] Starting reload: config/database.properties");
    System.out.println("  [AUDIT] Timestamp: 2025-01-12T10:30:00Z");
    System.out.println("  [AUDIT] User: admin");
    System.out.println("  [AUDIT] ✓ Reload successful: config/database.properties");
    System.out.println("  [AUDIT] Notifying monitoring system...");

    System.out.println("\nMonitoring dashboard:");
    System.out.println("  Recent configuration changes:");
    System.out.println("  • 10:30:00 - database.properties - SUCCESS");
    System.out.println("  • 10:15:00 - application.properties - SUCCESS");
    System.out.println("  • 09:45:00 - queries.xml - FAILED (syntax error)");

    System.out.println("\nBenefits:");
    System.out.println("  ✓ Complete audit trail");
    System.out.println("  ✓ Change notifications");
    System.out.println("  ✓ Error alerting");
    System.out.println("  ✓ Compliance tracking");

    System.out.println("\nResult: Monitored configuration management\n");
    System.out.println("---\n");
  }

  /**
   * Example 5: Directory Watching
   *
   * <p>Use Case: Monitor entire directory for changes.
   */
  static void example5_DirectoryWatching() {
    System.out.println("Example 5: Directory Watching");
    System.out.println("Use Case: Dynamic repository registration\n");

    System.out.println("Directory structure:");
    System.out.println("  repositories/");
    System.out.println("    ├─ UserRepository.class");
    System.out.println("    ├─ OrderRepository.class");
    System.out.println("    └─ ProductRepository.class");

    HotReloadManager manager = new HotReloadManager();
    manager.watchDirectory(
        Path.of("repositories/"),
        () -> {
          System.out.println("  Scanning for new repositories...");
          System.out.println("  ✓ Repository registry updated");
        }
    );

    System.out.println("\nMonitoring: repositories/");

    System.out.println("\nDeveloper adds new repository:");
    System.out.println("  $ cp CustomerRepository.class repositories/");

    System.out.println("\nHot reload triggered:");
    System.out.println("  [15:45:20] New file: repositories/CustomerRepository.class");
    System.out.println("  [15:45:20] Scanning for new repositories...");
    System.out.println("  [15:45:20] Found: CustomerRepository");
    System.out.println("  [15:45:20] Registering with DataVerse...");
    System.out.println("  [15:45:20] ✓ Repository registry updated");

    System.out.println("\nNew repository immediately available:");
    System.out.println("  CustomerRepository repo = dataverse.getRepository(");
    System.out.println("      CustomerRepository.class");
    System.out.println("  );");
    System.out.println("  List<Customer> customers = repo.findAll();");

    System.out.println("\nUse cases:");
    System.out.println("  ✓ Plugin system");
    System.out.println("  ✓ Dynamic feature loading");
    System.out.println("  ✓ Development workflow");

    System.out.println("\nResult: Dynamic component loading\n");
    System.out.println("---\n");
  }

  /**
   * Example 6: Production Scenario
   *
   * <p>Use Case: Complete production setup with hot reload.
   */
  static void example6_ProductionScenario() {
    System.out.println("Example 6: Production Scenario");
    System.out.println("Use Case: E-commerce platform configuration\n");

    System.out.println("Production setup:");

    HotReloadManager manager = new HotReloadManager();

    // Database config
    manager.watch(
        Path.of("config/database.properties"),
        () -> System.out.println("  ✓ Database config reloaded")
    );

    // Cache config
    manager.watch(
        Path.of("config/cache.properties"),
        () -> System.out.println("  ✓ Cache config reloaded")
    );

    // Feature flags
    manager.watch(
        Path.of("config/features.yaml"),
        () -> System.out.println("  ✓ Feature flags reloaded")
    );

    // Query mappings
    manager.watchDirectory(
        Path.of("config/queries"),
        () -> System.out.println("  ✓ Query mappings reloaded")
    );

    System.out.println("\n  Watching 4 configuration sources:");
    System.out.println("    1. database.properties");
    System.out.println("    2. cache.properties");
    System.out.println("    3. features.yaml");
    System.out.println("    4. queries/ directory");

    manager.start();
    System.out.println("\n  Hot reload manager started!");

    System.out.println("\nProduction scenarios:\n");

    System.out.println("Scenario 1: Black Friday - Scale up");
    System.out.println("  $ vim database.properties");
    System.out.println("  db.pool.size=50  # was 20");
    System.out.println("  → Reload in 1 second, capacity increased");

    System.out.println("\nScenario 2: Enable new feature");
    System.out.println("  $ vim features.yaml");
    System.out.println("  new_checkout: true  # was false");
    System.out.println("  → Feature live immediately");

    System.out.println("\nScenario 3: Fix slow query");
    System.out.println("  $ vim queries/orders.sql");
    System.out.println("  # Add WHERE clause optimization");
    System.out.println("  → Next request uses new query");

    System.out.println("\nScenario 4: Adjust cache");
    System.out.println("  $ vim cache.properties");
    System.out.println("  cache.ttl=300  # was 600");
    System.out.println("  → Cache policy updated");

    System.out.println("\nBusiness impact:");
    System.out.println("  • Zero-downtime configuration");
    System.out.println("  • Instant feature rollout");
    System.out.println("  • Fast incident response");
    System.out.println("  • No deployment overhead");

    System.out.println("\nUptime:");
    System.out.println("  Traditional: Deploy = 5 min downtime");
    System.out.println("  Hot Reload: <1 second disruption");
    System.out.println("  → 300x faster changes");

    System.out.println("\nResult: Agile production operations\n");
    System.out.println("---\n");
  }

  // Example classes
  static class DatabaseConfig implements Reloadable {
    private Properties props = new Properties();
    private Instant lastModified = Instant.now();

    @Override
    public void reload() {
      // Load properties from file
      System.out.println("    Loading database.properties...");
      System.out.println("    Reconfiguring connection pool...");
    }

    @Override
    public Instant getLastModified() {
      return lastModified;
    }

    @Override
    public void updateLastModified() {
      lastModified = Instant.now();
    }
  }

  static class QueryMapper {
    public void reload() {
      System.out.println("    Parsing queries.xml...");
      System.out.println("    Registering query definitions...");
    }
  }
}
