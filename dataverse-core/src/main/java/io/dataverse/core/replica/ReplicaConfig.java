package io.dataverse.core.replica;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for read replica support.
 *
 * <p>Configures read replicas for horizontal read scaling and high availability.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * ReplicaConfig replicaConfig = ReplicaConfig.builder()
 *     .addReplica("jdbc:postgresql://replica1.example.com:5432/mydb")
 *     .addReplica("jdbc:postgresql://replica2.example.com:5432/mydb")
 *     .addReplica("jdbc:postgresql://replica3.example.com:5432/mydb")
 *     .routingStrategy(ReadReplica.RoutingStrategy.ROUND_ROBIN)
 *     .healthCheckIntervalSeconds(30)
 *     .maxReplicationLagSeconds(10)
 *     .build();
 *
 * AdapterConfig config = AdapterConfig.builder()
 *     .property("jdbcUrl", "jdbc:postgresql://primary.example.com:5432/mydb")
 *     .replicaConfig(replicaConfig)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class ReplicaConfig {

  private final List<String> replicaUrls;
  private final ReadReplica.RoutingStrategy routingStrategy;
  private final int healthCheckIntervalSeconds;
  private final int maxReplicationLagSeconds;
  private final boolean enabled;

  private ReplicaConfig(Builder builder) {
    this.replicaUrls = new ArrayList<>(builder.replicaUrls);
    this.routingStrategy = builder.routingStrategy;
    this.healthCheckIntervalSeconds = builder.healthCheckIntervalSeconds;
    this.maxReplicationLagSeconds = builder.maxReplicationLagSeconds;
    this.enabled = builder.enabled;
  }

  public List<String> getReplicaUrls() {
    return replicaUrls;
  }

  public ReadReplica.RoutingStrategy getRoutingStrategy() {
    return routingStrategy;
  }

  public int getHealthCheckIntervalSeconds() {
    return healthCheckIntervalSeconds;
  }

  public int getMaxReplicationLagSeconds() {
    return maxReplicationLagSeconds;
  }

  public boolean isEnabled() {
    return enabled && !replicaUrls.isEmpty();
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for ReplicaConfig.
   */
  public static class Builder {
    private List<String> replicaUrls = new ArrayList<>();
    private ReadReplica.RoutingStrategy routingStrategy = ReadReplica.RoutingStrategy.ROUND_ROBIN;
    private int healthCheckIntervalSeconds = 30;
    private int maxReplicationLagSeconds = 10;
    private boolean enabled = true;

    /**
     * Adds a replica URL.
     *
     * @param replicaUrl the replica connection URL
     * @return this builder
     */
    public Builder addReplica(String replicaUrl) {
      this.replicaUrls.add(replicaUrl);
      return this;
    }

    /**
     * Sets all replica URLs.
     *
     * @param replicaUrls list of replica connection URLs
     * @return this builder
     */
    public Builder replicas(List<String> replicaUrls) {
      this.replicaUrls = new ArrayList<>(replicaUrls);
      return this;
    }

    /**
     * Sets the routing strategy.
     *
     * @param routingStrategy the routing strategy
     * @return this builder
     */
    public Builder routingStrategy(ReadReplica.RoutingStrategy routingStrategy) {
      this.routingStrategy = routingStrategy;
      return this;
    }

    /**
     * Sets the health check interval.
     *
     * @param healthCheckIntervalSeconds health check interval in seconds
     * @return this builder
     */
    public Builder healthCheckIntervalSeconds(int healthCheckIntervalSeconds) {
      this.healthCheckIntervalSeconds = healthCheckIntervalSeconds;
      return this;
    }

    /**
     * Sets the maximum acceptable replication lag.
     *
     * @param maxReplicationLagSeconds maximum lag in seconds
     * @return this builder
     */
    public Builder maxReplicationLagSeconds(int maxReplicationLagSeconds) {
      this.maxReplicationLagSeconds = maxReplicationLagSeconds;
      return this;
    }

    /**
     * Enables or disables replica support.
     *
     * @param enabled true to enable replicas
     * @return this builder
     */
    public Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    /**
     * Builds the ReplicaConfig.
     *
     * @return the replica configuration
     */
    public ReplicaConfig build() {
      return new ReplicaConfig(this);
    }
  }
}
