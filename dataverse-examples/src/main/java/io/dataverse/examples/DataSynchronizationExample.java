package io.dataverse.examples;

import io.dataverse.core.sync.ConflictResolution;
import io.dataverse.core.sync.SyncConfig;
import io.dataverse.core.sync.SyncManager;
import io.dataverse.core.sync.SyncMode;
import io.dataverse.core.sync.SyncResult;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Example demonstrating Data Synchronization (Feature #17).
 *
 * <p>Use Cases:
 * <ul>
 *   <li>PostgreSQL ↔ MongoDB synchronization</li>
 *   <li>Primary-Secondary replication</li>
 *   <li>Multi-datacenter sync</li>
 *   <li>Conflict resolution strategies</li>
 * </ul>
 *
 * @author DataVerse SDK Team
 */
public class DataSynchronizationExample {

  public static void main(String[] args) {
    System.out.println("=== Data Synchronization Examples ===\n");

    example1_OneWaySync();
    example2_TwoWaySync();
    example3_ConflictResolution();
    example4_BatchSync();
    example5_ContinuousSync();
    example6_MultiDatacenterSync();
  }

  /**
   * Example 1: One-Way Synchronization
   *
   * <p>Use Case: Replicate PostgreSQL data to MongoDB for analytics.
   */
  static void example1_OneWaySync() {
    System.out.println("Example 1: One-Way Synchronization");
    System.out.println("Use Case: PostgreSQL (OLTP) → MongoDB (Analytics)\n");

    System.out.println("Architecture:");
    System.out.println("  PostgreSQL (Primary)   →   MongoDB (Analytics)");
    System.out.println("     [Users Table]             [Users Collection]");
    System.out.println("     ↓ changes only           ← read-only");

    SyncConfig config = SyncConfig.builder()
        .mode(SyncMode.ONE_WAY)
        .syncInterval(Duration.ofSeconds(30))
        .batchSize(100)
        .conflictResolution(ConflictResolution.PRIMARY_WINS)
        .build();

    System.out.println("\nConfiguration:");
    System.out.println("  Mode: ONE_WAY (PostgreSQL → MongoDB)");
    System.out.println("  Interval: 30 seconds");
    System.out.println("  Batch size: 100 records");

    System.out.println("\nSync process:");
    System.out.println("  1. Query PostgreSQL for changes since last sync");
    System.out.println("     SELECT * FROM users WHERE updated_at > ?");
    System.out.println("     → Found 25 changed records");

    System.out.println("\n  2. Transform to MongoDB format");
    System.out.println("     PostgreSQL → MongoDB document mapping");

    System.out.println("\n  3. Upsert to MongoDB");
    System.out.println("     db.users.bulkWrite([");
    System.out.println("       { updateOne: { filter: {id: 1}, update: {...}, upsert: true } },");
    System.out.println("       ...");
    System.out.println("     ])");

    System.out.println("\nSync result:");
    System.out.println("  ✓ 25 records synchronized");
    System.out.println("  ✓ Duration: 450ms");
    System.out.println("  ✓ No conflicts");

    System.out.println("\nUse cases:");
    System.out.println("  ✓ OLTP → Analytics warehouse");
    System.out.println("  ✓ Primary → Read replicas");
    System.out.println("  ✓ Database migration");

    System.out.println("\nResult: Automatic one-way replication\n");
    System.out.println("---\n");
  }

  /**
   * Example 2: Two-Way Synchronization
   *
   * <p>Use Case: Keep two databases in sync bidirectionally.
   */
  static void example2_TwoWaySync() {
    System.out.println("Example 2: Two-Way Synchronization");
    System.out.println("Use Case: Multi-region user management\n");

    System.out.println("Architecture:");
    System.out.println("  US Datacenter (PostgreSQL) ↔ EU Datacenter (PostgreSQL)");
    System.out.println("       [Users Table]                [Users Table]");
    System.out.println("       changes ←→                   ←→ changes");

    SyncConfig config = SyncConfig.builder()
        .mode(SyncMode.TWO_WAY)
        .syncInterval(Duration.ofSeconds(10))
        .conflictResolution(ConflictResolution.LAST_WRITE_WINS)
        .build();

    System.out.println("\nConfiguration:");
    System.out.println("  Mode: TWO_WAY (bidirectional)");
    System.out.println("  Interval: 10 seconds");
    System.out.println("  Conflict resolution: LAST_WRITE_WINS");

    System.out.println("\nScenario:");
    System.out.println("  Time 10:00:00 - User 123 updated in US");
    System.out.println("    {id: 123, name: 'Alice', updated: 10:00:00}");

    System.out.println("\n  Time 10:00:05 - User 123 updated in EU");
    System.out.println("    {id: 123, name: 'Alicia', updated: 10:00:05}");

    System.out.println("\n  Time 10:00:10 - Sync runs");
    System.out.println("    Conflict detected! Both modified user 123");
    System.out.println("    Resolution: LAST_WRITE_WINS");
    System.out.println("    → EU version (10:00:05) wins");
    System.out.println("    → US database updated to 'Alicia'");

    System.out.println("\nBoth databases now consistent:");
    System.out.println("  US: {id: 123, name: 'Alicia', updated: 10:00:05}");
    System.out.println("  EU: {id: 123, name: 'Alicia', updated: 10:00:05}");

    System.out.println("\nBenefits:");
    System.out.println("  ✓ Low latency for local users");
    System.out.println("  ✓ High availability");
    System.out.println("  ✓ Automatic conflict resolution");

    System.out.println("\nResult: Seamless bidirectional sync\n");
    System.out.println("---\n");
  }

  /**
   * Example 3: Conflict Resolution Strategies
   *
   * <p>Use Case: Different strategies for different scenarios.
   */
  static void example3_ConflictResolution() {
    System.out.println("Example 3: Conflict Resolution Strategies");
    System.out.println("Use Case: Handling concurrent updates\n");

    System.out.println("Conflict scenario:");
    System.out.println("  Database A: User 123 balance = $100");
    System.out.println("  Database B: User 123 balance = $100");
    System.out.println("");
    System.out.println("  Concurrent updates:");
    System.out.println("    A: balance = $80  (spent $20)");
    System.out.println("    B: balance = $150 (deposited $50)");

    System.out.println("\n1️⃣ LAST_WRITE_WINS:");
    System.out.println("  Result: $150 (B's update was later)");
    System.out.println("  Issue: Lost A's $20 transaction!");
    System.out.println("  Use when: Updates are idempotent");

    System.out.println("\n2️⃣ PRIMARY_WINS:");
    System.out.println("  Result: $80 (A is primary)");
    System.out.println("  Issue: Lost B's $50 deposit!");
    System.out.println("  Use when: Clear primary authority");

    System.out.println("\n3️⃣ SECONDARY_WINS:");
    System.out.println("  Result: $150 (B is secondary)");
    System.out.println("  Use when: Secondary has fresher data");

    System.out.println("\n4️⃣ MANUAL:");
    System.out.println("  Action: Log conflict, alert admin");
    System.out.println("  Admin reviews and resolves manually");
    System.out.println("  Use when: Data is critical");

    System.out.println("\n5️⃣ CUSTOM:");
    System.out.println("  Custom logic: Apply both changes!");
    System.out.println("  Start: $100");
    System.out.println("  A: -$20 = $80");
    System.out.println("  B: +$50 = $150");
    System.out.println("  Custom: $100 - $20 + $50 = $130");
    System.out.println("  ✓ Both transactions preserved!");

    System.out.println("\nImplementation:");
    System.out.println("  ConflictResolver customResolver = (primary, secondary) -> {");
    System.out.println("    // Apply business logic");
    System.out.println("    return mergedValue;");
    System.out.println("  };");

    System.out.println("\nResult: Flexible conflict handling\n");
    System.out.println("---\n");
  }

  /**
   * Example 4: Batch Synchronization
   *
   * <p>Use Case: Initial data migration or catch-up sync.
   */
  static void example4_BatchSync() {
    System.out.println("Example 4: Batch Synchronization");
    System.out.println("Use Case: Initial data migration\n");

    System.out.println("Scenario: Migrate 1 million users to new database");

    System.out.println("\nWithout batching:");
    System.out.println("  Load 1,000,000 records into memory");
    System.out.println("  → OutOfMemoryError!");

    System.out.println("\nWith batching:");
    SyncConfig config = SyncConfig.builder()
        .batchSize(1000)
        .syncInterval(Duration.ZERO)  // Manual trigger
        .build();

    System.out.println("  Batch size: 1,000 records");
    System.out.println("  Total batches: 1,000");

    System.out.println("\nExecution:");
    System.out.println("  [Batch   1/1000] 1,000 records - 2.5s");
    System.out.println("  [Batch   2/1000] 1,000 records - 2.4s");
    System.out.println("  [Batch   3/1000] 1,000 records - 2.6s");
    System.out.println("  ...");
    System.out.println("  [Batch 1000/1000] 1,000 records - 2.5s");

    System.out.println("\nProgress tracking:");
    System.out.println("  Synced: 500,000 / 1,000,000 (50%)");
    System.out.println("  Elapsed: 20 minutes");
    System.out.println("  Estimated remaining: 20 minutes");

    System.out.println("\nResumable:");
    System.out.println("  If process crashes at batch 500:");
    System.out.println("  → Resume from batch 501");
    System.out.println("  → No duplicate work");

    System.out.println("\nMemory usage:");
    System.out.println("  Without batching: ~8GB");
    System.out.println("  With batching: ~50MB (constant)");

    System.out.println("\nResult: Scalable large dataset migration\n");
    System.out.println("---\n");
  }

  /**
   * Example 5: Continuous Synchronization
   *
   * <p>Use Case: Real-time data replication.
   */
  static void example5_ContinuousSync() {
    System.out.println("Example 5: Continuous Synchronization");
    System.out.println("Use Case: Real-time inventory sync\n");

    System.out.println("Architecture:");
    System.out.println("  E-commerce DB (PostgreSQL) → Warehouse DB (MongoDB)");
    System.out.println("     [Inventory Table]            [Inventory Collection]");

    SyncConfig config = SyncConfig.builder()
        .mode(SyncMode.ONE_WAY)
        .syncInterval(Duration.ofSeconds(5))  // Every 5 seconds
        .enabled(true)
        .build();

    System.out.println("\nConfiguration:");
    System.out.println("  Sync interval: 5 seconds");
    System.out.println("  Mode: Continuous (daemon thread)");

    System.out.println("\nOperation log:");
    System.out.println("  [10:00:00] Sync started");
    System.out.println("  [10:00:00] Synced 0 changes (0ms)");
    System.out.println("  [10:00:05] Synced 3 changes (125ms)");
    System.out.println("  [10:00:10] Synced 0 changes (0ms)");
    System.out.println("  [10:00:15] Synced 15 changes (450ms)");
    System.out.println("  [10:00:20] Synced 8 changes (280ms)");

    System.out.println("\nChange detection:");
    System.out.println("  SELECT * FROM inventory");
    System.out.println("  WHERE updated_at > '2025-01-12 10:00:15'");
    System.out.println("  ORDER BY updated_at ASC");

    System.out.println("\nLifecycle:");
    System.out.println("  manager.start();   // Begin continuous sync");
    System.out.println("  // Runs in background...");
    System.out.println("  manager.stop();    // Graceful shutdown");

    System.out.println("\nMonitoring:");
    System.out.println("  SyncResult result = manager.getLastSyncResult();");
    System.out.println("  System.out.println(\"Synced: \" + result.getSyncedCount());");
    System.out.println("  System.out.println(\"Failed: \" + result.getFailedCount());");
    System.out.println("  System.out.println(\"Duration: \" + result.getDuration());");

    System.out.println("\nBenefits:");
    System.out.println("  ✓ Near real-time replication");
    System.out.println("  ✓ Low latency (5s max)");
    System.out.println("  ✓ Automatic error handling");
    System.out.println("  ✓ Zero manual intervention");

    System.out.println("\nResult: Hands-off continuous sync\n");
    System.out.println("---\n");
  }

  /**
   * Example 6: Multi-Datacenter Synchronization
   *
   * <p>Use Case: Global application with regional databases.
   */
  static void example6_MultiDatacenterSync() {
    System.out.println("Example 6: Multi-Datacenter Synchronization");
    System.out.println("Use Case: Global SaaS with regional compliance\n");

    System.out.println("Global architecture:");
    System.out.println("        ┌─────────────┐");
    System.out.println("        │   US-EAST   │");
    System.out.println("        │ PostgreSQL  │");
    System.out.println("        └──────┬──────┘");
    System.out.println("               │");
    System.out.println("       ┌───────┴───────┐");
    System.out.println("       │               │");
    System.out.println("  ┌────▼────┐    ┌────▼────┐");
    System.out.println("  │ EU-WEST │    │ AP-SOUTH│");
    System.out.println("  │PostgreSQL│    │PostgreSQL│");
    System.out.println("  └─────────┘    └─────────┘");

    System.out.println("\nRequirements:");
    System.out.println("  • GDPR: EU user data stays in EU");
    System.out.println("  • Performance: Low latency per region");
    System.out.println("  • Consistency: Global user directory");

    System.out.println("\nSync strategy:");
    System.out.println("  1. US users → US database only");
    System.out.println("  2. EU users → EU database only");
    System.out.println("  3. AP users → AP database only");
    System.out.println("  4. Global metadata → All regions");

    System.out.println("\nConfiguration:");
    System.out.println("  // US → EU sync (EU users only)");
    System.out.println("  SyncConfig usToEu = SyncConfig.builder()");
    System.out.println("    .mode(SyncMode.ONE_WAY)");
    System.out.println("    .filter(user -> user.region.equals(\"EU\"))");
    System.out.println("    .build();");

    System.out.println("\nLatency comparison:");
    System.out.println("  Without regional DBs:");
    System.out.println("    US user → US DB: 20ms ✓");
    System.out.println("    EU user → US DB: 150ms ✗ (transatlantic)");
    System.out.println("    AP user → US DB: 250ms ✗ (transpacific)");

    System.out.println("\n  With regional DBs + sync:");
    System.out.println("    US user → US DB: 20ms ✓");
    System.out.println("    EU user → EU DB: 25ms ✓ (local)");
    System.out.println("    AP user → AP DB: 30ms ✓ (local)");

    System.out.println("\nCompliance benefits:");
    System.out.println("  ✓ GDPR: EU data stored in EU");
    System.out.println("  ✓ Data residency: Per-region storage");
    System.out.println("  ✓ Performance: Local access");
    System.out.println("  ✓ Disaster recovery: Multi-region backups");

    System.out.println("\nResult: Compliant global synchronization\n");
    System.out.println("---\n");
  }

  /**
   * Example 7: Monitoring and Metrics
   */
  static class MonitoringExample {
    public static void demonstrateMetrics() {
      System.out.println("Sync Metrics Dashboard:\n");

      SyncResult result = SyncResult.builder()
          .syncedCount(1250)
          .failedCount(3)
          .duration(Duration.ofMillis(4500))
          .startTime(LocalDateTime.now().minusSeconds(5))
          .endTime(LocalDateTime.now())
          .build();

      System.out.println("Last Sync:");
      System.out.println("  Synced: " + result.getSyncedCount() + " records");
      System.out.println("  Failed: " + result.getFailedCount() + " records");
      System.out.println("  Success rate: " +
          (100.0 * result.getSyncedCount() /
          (result.getSyncedCount() + result.getFailedCount())) + "%");
      System.out.println("  Duration: " + result.getDuration().toMillis() + "ms");
      System.out.println("  Throughput: " +
          (result.getSyncedCount() * 1000 / result.getDuration().toMillis()) +
          " records/second");
    }
  }
}
