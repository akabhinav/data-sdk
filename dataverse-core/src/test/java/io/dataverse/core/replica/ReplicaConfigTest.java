package io.dataverse.core.replica;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ReplicaConfig.
 *
 * @author DataVerse SDK Team
 */
class ReplicaConfigTest {

  @Test
  void testBuilder_Defaults() {
    ReplicaConfig config = ReplicaConfig.builder().build();

    assertTrue(config.getReplicaUrls().isEmpty());
    assertEquals(ReadReplica.RoutingStrategy.ROUND_ROBIN, config.getRoutingStrategy());
    assertEquals(30, config.getHealthCheckIntervalSeconds());
    assertEquals(10, config.getMaxReplicationLagSeconds());
    assertFalse(config.isEnabled());  // Not enabled if no replicas
  }

  @Test
  void testBuilder_AddSingleReplica() {
    ReplicaConfig config = ReplicaConfig.builder()
        .addReplica("jdbc:postgresql://replica1.example.com:5432/mydb")
        .build();

    assertEquals(1, config.getReplicaUrls().size());
    assertEquals("jdbc:postgresql://replica1.example.com:5432/mydb", config.getReplicaUrls().get(0));
    assertTrue(config.isEnabled());
  }

  @Test
  void testBuilder_AddMultipleReplicas() {
    ReplicaConfig config = ReplicaConfig.builder()
        .addReplica("jdbc:postgresql://replica1.example.com:5432/mydb")
        .addReplica("jdbc:postgresql://replica2.example.com:5432/mydb")
        .addReplica("jdbc:postgresql://replica3.example.com:5432/mydb")
        .build();

    assertEquals(3, config.getReplicaUrls().size());
    assertTrue(config.isEnabled());
  }

  @Test
  void testBuilder_ReplicasList() {
    List<String> replicas = List.of(
        "jdbc:postgresql://replica1.example.com:5432/mydb",
        "jdbc:postgresql://replica2.example.com:5432/mydb"
    );

    ReplicaConfig config = ReplicaConfig.builder()
        .replicas(replicas)
        .build();

    assertEquals(2, config.getReplicaUrls().size());
    assertEquals(replicas, config.getReplicaUrls());
  }

  @Test
  void testBuilder_CustomRoutingStrategy() {
    ReplicaConfig config = ReplicaConfig.builder()
        .addReplica("jdbc:postgresql://replica1.example.com:5432/mydb")
        .routingStrategy(ReadReplica.RoutingStrategy.LEAST_CONNECTIONS)
        .build();

    assertEquals(ReadReplica.RoutingStrategy.LEAST_CONNECTIONS, config.getRoutingStrategy());
  }

  @Test
  void testBuilder_CustomHealthCheckInterval() {
    ReplicaConfig config = ReplicaConfig.builder()
        .addReplica("jdbc:postgresql://replica1.example.com:5432/mydb")
        .healthCheckIntervalSeconds(60)
        .build();

    assertEquals(60, config.getHealthCheckIntervalSeconds());
  }

  @Test
  void testBuilder_CustomMaxReplicationLag() {
    ReplicaConfig config = ReplicaConfig.builder()
        .addReplica("jdbc:postgresql://replica1.example.com:5432/mydb")
        .maxReplicationLagSeconds(5)
        .build();

    assertEquals(5, config.getMaxReplicationLagSeconds());
  }

  @Test
  void testBuilder_Disabled() {
    ReplicaConfig config = ReplicaConfig.builder()
        .addReplica("jdbc:postgresql://replica1.example.com:5432/mydb")
        .enabled(false)
        .build();

    assertFalse(config.isEnabled());
  }

  @Test
  void testBuilder_FullConfiguration() {
    ReplicaConfig config = ReplicaConfig.builder()
        .addReplica("jdbc:postgresql://replica1.example.com:5432/mydb")
        .addReplica("jdbc:postgresql://replica2.example.com:5432/mydb")
        .routingStrategy(ReadReplica.RoutingStrategy.GEOGRAPHIC)
        .healthCheckIntervalSeconds(45)
        .maxReplicationLagSeconds(15)
        .enabled(true)
        .build();

    assertEquals(2, config.getReplicaUrls().size());
    assertEquals(ReadReplica.RoutingStrategy.GEOGRAPHIC, config.getRoutingStrategy());
    assertEquals(45, config.getHealthCheckIntervalSeconds());
    assertEquals(15, config.getMaxReplicationLagSeconds());
    assertTrue(config.isEnabled());
  }

  @Test
  void testReplicaUrls_Immutability() {
    ReplicaConfig config = ReplicaConfig.builder()
        .addReplica("jdbc:postgresql://replica1.example.com:5432/mydb")
        .build();

    List<String> urls = config.getReplicaUrls();

    // Should not allow modification
    assertThrows(UnsupportedOperationException.class, () ->
        urls.add("jdbc:postgresql://replica2.example.com:5432/mydb")
    );
  }

  @Test
  void testReplicas_OverwritesPreviousReplicas() {
    List<String> replicas1 = List.of("jdbc:postgresql://replica1.example.com:5432/mydb");
    List<String> replicas2 = List.of(
        "jdbc:postgresql://replica2.example.com:5432/mydb",
        "jdbc:postgresql://replica3.example.com:5432/mydb"
    );

    ReplicaConfig config = ReplicaConfig.builder()
        .replicas(replicas1)
        .replicas(replicas2)  // Should overwrite
        .build();

    assertEquals(2, config.getReplicaUrls().size());
    assertEquals(replicas2, config.getReplicaUrls());
  }

  @Test
  void testIsEnabled_NoReplicasButEnabled() {
    ReplicaConfig config = ReplicaConfig.builder()
        .enabled(true)
        .build();

    // Should be false because no replicas configured
    assertFalse(config.isEnabled());
  }
}
