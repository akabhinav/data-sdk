package io.dataverse.core.replica;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ReplicaRouter.
 *
 * @author DataVerse SDK Team
 */
class ReplicaRouterTest {

  private static final String PRIMARY_URL = "jdbc:postgresql://primary.example.com:5432/mydb";
  private static final String REPLICA1_URL = "jdbc:postgresql://replica1.example.com:5432/mydb";
  private static final String REPLICA2_URL = "jdbc:postgresql://replica2.example.com:5432/mydb";
  private static final String REPLICA3_URL = "jdbc:postgresql://replica3.example.com:5432/mydb";

  private ReplicaRouter router;

  @BeforeEach
  void setUp() {
    router = new ReplicaRouter(PRIMARY_URL);
  }

  @Test
  void testGetPrimary() {
    assertEquals(PRIMARY_URL, router.getPrimary());
  }

  @Test
  void testAddReplica() {
    assertEquals(0, router.getReplicaCount());

    router.addReplica(REPLICA1_URL);
    assertEquals(1, router.getReplicaCount());
    assertEquals(1, router.getHealthyReplicaCount());

    router.addReplica(REPLICA2_URL);
    assertEquals(2, router.getReplicaCount());
    assertEquals(2, router.getHealthyReplicaCount());
  }

  @Test
  void testSelectReplica_NoReplicasConfigured_ReturnsPrimary() {
    // When no replicas configured, should return primary
    String selected = router.selectReplica(ReadReplica.RoutingStrategy.ROUND_ROBIN);
    assertEquals(PRIMARY_URL, selected);
  }

  @Test
  void testSelectReplica_AllReplicasUnhealthy_ReturnsPrimary() {
    router.addReplica(REPLICA1_URL);
    router.addReplica(REPLICA2_URL);

    // Mark all replicas unhealthy
    router.markUnhealthy(REPLICA1_URL);
    router.markUnhealthy(REPLICA2_URL);

    assertEquals(0, router.getHealthyReplicaCount());

    // Should fall back to primary
    String selected = router.selectReplica(ReadReplica.RoutingStrategy.ROUND_ROBIN);
    assertEquals(PRIMARY_URL, selected);
  }

  @Test
  void testRoundRobinStrategy() {
    router.addReplica(REPLICA1_URL);
    router.addReplica(REPLICA2_URL);
    router.addReplica(REPLICA3_URL);

    // Round-robin should cycle through all replicas
    Set<String> selectedUrls = new HashSet<>();
    for (int i = 0; i < 10; i++) {
      String selected = router.selectReplica(ReadReplica.RoutingStrategy.ROUND_ROBIN);
      selectedUrls.add(selected);
    }

    // Should have used all 3 replicas
    assertTrue(selectedUrls.contains(REPLICA1_URL));
    assertTrue(selectedUrls.contains(REPLICA2_URL));
    assertTrue(selectedUrls.contains(REPLICA3_URL));
    assertFalse(selectedUrls.contains(PRIMARY_URL));
  }

  @Test
  void testRoundRobin_SkipsUnhealthyReplicas() {
    router.addReplica(REPLICA1_URL);
    router.addReplica(REPLICA2_URL);
    router.addReplica(REPLICA3_URL);

    // Mark replica2 as unhealthy
    router.markUnhealthy(REPLICA2_URL);

    assertEquals(2, router.getHealthyReplicaCount());

    // Round-robin should only use healthy replicas
    Set<String> selectedUrls = new HashSet<>();
    for (int i = 0; i < 10; i++) {
      String selected = router.selectReplica(ReadReplica.RoutingStrategy.ROUND_ROBIN);
      selectedUrls.add(selected);
    }

    // Should only use replica1 and replica3
    assertTrue(selectedUrls.contains(REPLICA1_URL));
    assertFalse(selectedUrls.contains(REPLICA2_URL));  // Unhealthy
    assertTrue(selectedUrls.contains(REPLICA3_URL));
  }

  @Test
  void testLeastConnectionsStrategy() {
    router.addReplica(REPLICA1_URL);
    router.addReplica(REPLICA2_URL);
    router.addReplica(REPLICA3_URL);

    // Set different connection counts
    router.getReplicas().get(0).setActiveConnections(10);
    router.getReplicas().get(1).setActiveConnections(5);   // Least
    router.getReplicas().get(2).setActiveConnections(15);

    // Should select replica with least connections (replica2)
    String selected = router.selectReplica(ReadReplica.RoutingStrategy.LEAST_CONNECTIONS);
    assertEquals(REPLICA2_URL, selected);
  }

  @Test
  void testRandomStrategy() {
    router.addReplica(REPLICA1_URL);
    router.addReplica(REPLICA2_URL);
    router.addReplica(REPLICA3_URL);

    // Random should eventually select all replicas
    Set<String> selectedUrls = new HashSet<>();
    for (int i = 0; i < 100; i++) {
      String selected = router.selectReplica(ReadReplica.RoutingStrategy.RANDOM);
      selectedUrls.add(selected);
    }

    // Over 100 selections, should have hit all 3 replicas
    assertEquals(3, selectedUrls.size());
    assertTrue(selectedUrls.contains(REPLICA1_URL));
    assertTrue(selectedUrls.contains(REPLICA2_URL));
    assertTrue(selectedUrls.contains(REPLICA3_URL));
  }

  @Test
  void testGeographicStrategy() {
    router.addReplica(REPLICA1_URL);
    router.addReplica(REPLICA2_URL);
    router.addReplica(REPLICA3_URL);

    // Geographic strategy (simplified) returns first healthy replica
    String selected = router.selectReplica(ReadReplica.RoutingStrategy.GEOGRAPHIC);
    assertEquals(REPLICA1_URL, selected);

    // If first is unhealthy, should use second
    router.markUnhealthy(REPLICA1_URL);
    selected = router.selectReplica(ReadReplica.RoutingStrategy.GEOGRAPHIC);
    assertEquals(REPLICA2_URL, selected);
  }

  @Test
  void testMarkHealthyUnhealthy() {
    router.addReplica(REPLICA1_URL);

    // Initially healthy
    assertEquals(1, router.getHealthyReplicaCount());

    // Mark unhealthy
    router.markUnhealthy(REPLICA1_URL);
    assertEquals(0, router.getHealthyReplicaCount());

    // Mark healthy again
    router.markHealthy(REPLICA1_URL);
    assertEquals(1, router.getHealthyReplicaCount());
  }

  @Test
  void testMarkHealthyUnhealthy_NonExistentReplica() {
    router.addReplica(REPLICA1_URL);

    // Marking non-existent replica should not throw exception
    assertDoesNotThrow(() -> router.markUnhealthy("jdbc:postgresql://nonexistent:5432/db"));
    assertDoesNotThrow(() -> router.markHealthy("jdbc:postgresql://nonexistent:5432/db"));

    // Replica count should be unchanged
    assertEquals(1, router.getHealthyReplicaCount());
  }

  @Test
  void testGetReplicas() {
    router.addReplica(REPLICA1_URL);
    router.addReplica(REPLICA2_URL);

    var replicas = router.getReplicas();
    assertEquals(2, replicas.size());

    // Should be unmodifiable
    assertThrows(UnsupportedOperationException.class, () -> replicas.add(null));
  }

  @Test
  void testReplicaInfo() {
    router.addReplica(REPLICA1_URL);
    var replica = router.getReplicas().get(0);

    // Test getters
    assertEquals(REPLICA1_URL, replica.getUrl());
    assertTrue(replica.isHealthy());
    assertEquals(0, replica.getActiveConnections());
    assertEquals(0, replica.getReplicationLagSeconds());
    assertTrue(replica.getLastHealthCheck() > 0);

    // Test setters
    replica.setHealthy(false);
    assertFalse(replica.isHealthy());

    replica.setActiveConnections(5);
    assertEquals(5, replica.getActiveConnections());

    replica.setReplicationLagSeconds(10);
    assertEquals(10, replica.getReplicationLagSeconds());

    long beforeHealthCheck = replica.getLastHealthCheck();
    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    replica.setHealthy(true);
    assertTrue(replica.getLastHealthCheck() > beforeHealthCheck);
  }

  @Test
  void testConcurrentAccess() throws InterruptedException {
    router.addReplica(REPLICA1_URL);
    router.addReplica(REPLICA2_URL);
    router.addReplica(REPLICA3_URL);

    // Start multiple threads selecting replicas concurrently
    Thread[] threads = new Thread[10];
    Set<String>[] results = new Set[10];

    for (int i = 0; i < threads.length; i++) {
      final int index = i;
      results[index] = new HashSet<>();
      threads[i] = Thread.ofVirtual().start(() -> {
        for (int j = 0; j < 100; j++) {
          String selected = router.selectReplica(ReadReplica.RoutingStrategy.ROUND_ROBIN);
          results[index].add(selected);
        }
      });
    }

    // Wait for all threads to complete
    for (Thread thread : threads) {
      thread.join();
    }

    // All threads should have seen all replicas
    for (Set<String> result : results) {
      assertEquals(3, result.size());
    }
  }
}
