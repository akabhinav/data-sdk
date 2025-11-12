package io.dataverse.core.replica;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Routes database queries to primary or read replicas.
 *
 * <p>Implements routing strategies for distributing read load across replicas
 * while directing writes to the primary database.
 *
 * <p><strong>Automatic Failover:</strong>
 * If a replica is unhealthy or unreachable, automatically fails over to:
 * <ol>
 *   <li>Next available replica</li>
 *   <li>Primary database (if no replicas available)</li>
 * </ol>
 *
 * <p><strong>Health Checking:</strong>
 * Periodically checks replica health and removes unhealthy replicas from rotation.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Configure replicas
 * ReplicaRouter router = new ReplicaRouter();
 * router.addReplica("replica1.example.com:5432");
 * router.addReplica("replica2.example.com:5432");
 * router.addReplica("replica3.example.com:5432");
 *
 * // Route read query
 * String replicaUrl = router.selectReplica(ReadReplica.RoutingStrategy.ROUND_ROBIN);
 * Connection conn = DriverManager.getConnection(replicaUrl);
 *
 * // Write always goes to primary
 * Connection primaryConn = router.getPrimaryConnection();
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class ReplicaRouter {

  private final String primaryUrl;
  private final List<ReplicaInfo> replicas = new ArrayList<>();
  private final AtomicInteger roundRobinIndex = new AtomicInteger(0);

  public ReplicaRouter(String primaryUrl) {
    this.primaryUrl = primaryUrl;
  }

  /**
   * Adds a read replica to the router.
   *
   * @param replicaUrl the replica connection URL
   */
  public void addReplica(String replicaUrl) {
    replicas.add(new ReplicaInfo(replicaUrl));
  }

  /**
   * Selects a replica based on the routing strategy.
   *
   * <p>Falls back to primary if no healthy replicas are available.
   *
   * @param strategy the routing strategy
   * @return the selected replica URL, or primary if no replicas available
   */
  public String selectReplica(ReadReplica.RoutingStrategy strategy) {
    // Filter healthy replicas
    List<ReplicaInfo> healthyReplicas = replicas.stream()
        .filter(ReplicaInfo::isHealthy)
        .toList();

    if (healthyReplicas.isEmpty()) {
      // No healthy replicas - use primary
      return primaryUrl;
    }

    return switch (strategy) {
      case ROUND_ROBIN -> selectRoundRobin(healthyReplicas);
      case LEAST_CONNECTIONS -> selectLeastConnections(healthyReplicas);
      case RANDOM -> selectRandom(healthyReplicas);
      case GEOGRAPHIC -> selectGeographic(healthyReplicas);
    };
  }

  /**
   * Gets the primary database URL.
   *
   * @return the primary URL
   */
  public String getPrimary() {
    return primaryUrl;
  }

  /**
   * Gets the number of configured replicas.
   *
   * @return replica count
   */
  public int getReplicaCount() {
    return replicas.size();
  }

  /**
   * Gets the number of healthy replicas.
   *
   * @return healthy replica count
   */
  public long getHealthyReplicaCount() {
    return replicas.stream().filter(ReplicaInfo::isHealthy).count();
  }

  /**
   * Gets all replicas (for health checking).
   *
   * @return unmodifiable list of replicas
   */
  public List<ReplicaInfo> getReplicas() {
    return List.copyOf(replicas);
  }

  /**
   * Marks a replica as unhealthy.
   *
   * @param replicaUrl the replica URL to mark unhealthy
   */
  public void markUnhealthy(String replicaUrl) {
    replicas.stream()
        .filter(r -> r.getUrl().equals(replicaUrl))
        .findFirst()
        .ifPresent(r -> r.setHealthy(false));
  }

  /**
   * Marks a replica as healthy.
   *
   * @param replicaUrl the replica URL to mark healthy
   */
  public void markHealthy(String replicaUrl) {
    replicas.stream()
        .filter(r -> r.getUrl().equals(replicaUrl))
        .findFirst()
        .ifPresent(r -> r.setHealthy(true));
  }

  // Routing strategy implementations

  private String selectRoundRobin(List<ReplicaInfo> healthyReplicas) {
    int index = roundRobinIndex.getAndIncrement() % healthyReplicas.size();
    return healthyReplicas.get(index).getUrl();
  }

  private String selectLeastConnections(List<ReplicaInfo> healthyReplicas) {
    return healthyReplicas.stream()
        .min((r1, r2) -> Integer.compare(r1.getActiveConnections(), r2.getActiveConnections()))
        .map(ReplicaInfo::getUrl)
        .orElse(primaryUrl);
  }

  private String selectRandom(List<ReplicaInfo> healthyReplicas) {
    int randomIndex = (int) (Math.random() * healthyReplicas.size());
    return healthyReplicas.get(randomIndex).getUrl();
  }

  private String selectGeographic(List<ReplicaInfo> healthyReplicas) {
    // Simplified: returns first healthy replica
    // In production, would consider geographic proximity
    return healthyReplicas.get(0).getUrl();
  }

  /**
   * Replica information holder.
   */
  static class ReplicaInfo {
    private final String url;
    private boolean healthy = true;
    private int activeConnections = 0;
    private long lastHealthCheck = System.currentTimeMillis();
    private int replicationLagSeconds = 0;

    ReplicaInfo(String url) {
      this.url = url;
    }

    public String getUrl() {
      return url;
    }

    public boolean isHealthy() {
      return healthy;
    }

    public void setHealthy(boolean healthy) {
      this.healthy = healthy;
      this.lastHealthCheck = System.currentTimeMillis();
    }

    public int getActiveConnections() {
      return activeConnections;
    }

    public void setActiveConnections(int activeConnections) {
      this.activeConnections = activeConnections;
    }

    public int getReplicationLagSeconds() {
      return replicationLagSeconds;
    }

    public void setReplicationLagSeconds(int replicationLagSeconds) {
      this.replicationLagSeconds = replicationLagSeconds;
    }

    public long getLastHealthCheck() {
      return lastHealthCheck;
    }
  }
}
