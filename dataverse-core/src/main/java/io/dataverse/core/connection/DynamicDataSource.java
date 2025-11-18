package io.dataverse.core.connection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Dynamic data source routing based on context (tenant, region, business rules).
 *
 * <p>Feature #3: Dynamic Connection Switching - Runtime database switching
 * <p>Feature #8: Read/Write Split - Automatic routing to read replicas
 *
 * @since 1.0.0
 */
public class DynamicDataSource {

    private final ConnectionPoolManager poolManager;
    private final Map<String, String> tenantToDataSource = new ConcurrentHashMap<>();
    private final Map<String, String> regionToDataSource = new ConcurrentHashMap<>();
    private final Map<String, String[]> dataSourceToReplicas = new ConcurrentHashMap<>();
    private final ThreadLocal<RoutingContext> routingContext = new ThreadLocal<>();

    private String defaultDataSource = "default";
    private RoutingStrategy routingStrategy = RoutingStrategy.TENANT;
    private LoadBalancingStrategy loadBalancingStrategy = LoadBalancingStrategy.ROUND_ROBIN;

    private final Map<String, Integer> replicaIndex = new ConcurrentHashMap<>();

    public DynamicDataSource(ConnectionPoolManager poolManager) {
        this.poolManager = Objects.requireNonNull(poolManager);
    }

    /**
     * Set the routing strategy.
     */
    public void setRoutingStrategy(RoutingStrategy strategy) {
        this.routingStrategy = Objects.requireNonNull(strategy);
    }

    /**
     * Set the load balancing strategy for read replicas.
     */
    public void setLoadBalancingStrategy(LoadBalancingStrategy strategy) {
        this.loadBalancingStrategy = Objects.requireNonNull(strategy);
    }

    /**
     * Set the default data source name.
     */
    public void setDefaultDataSource(String name) {
        this.defaultDataSource = Objects.requireNonNull(name);
    }

    /**
     * Map a tenant to a specific data source.
     */
    public void mapTenant(String tenantId, String dataSourceName) {
        tenantToDataSource.put(tenantId, dataSourceName);
    }

    /**
     * Map a region to a specific data source.
     */
    public void mapRegion(String regionId, String dataSourceName) {
        regionToDataSource.put(regionId, dataSourceName);
    }

    /**
     * Configure read replicas for a data source.
     */
    public void setReadReplicas(String dataSourceName, String... replicaNames) {
        dataSourceToReplicas.put(dataSourceName, replicaNames);
    }

    /**
     * Set the current routing context for this thread.
     */
    public void setRoutingContext(RoutingContext context) {
        routingContext.set(context);
    }

    /**
     * Clear the current routing context.
     */
    public void clearRoutingContext() {
        routingContext.remove();
    }

    /**
     * Execute an operation within a specific context.
     */
    public <T> T withContext(RoutingContext context, Supplier<T> operation) {
        RoutingContext previous = routingContext.get();
        try {
            routingContext.set(context);
            return operation.get();
        } finally {
            if (previous != null) {
                routingContext.set(previous);
            } else {
                routingContext.remove();
            }
        }
    }

    /**
     * Execute a runnable within a specific context.
     */
    public void withContext(RoutingContext context, Runnable operation) {
        withContext(context, () -> {
            operation.run();
            return null;
        });
    }

    /**
     * Get a connection based on current context and operation type.
     */
    public Connection getConnection(boolean readOnly) throws SQLException {
        String dataSourceName = determineDataSource();

        if (readOnly) {
            String replicaName = selectReadReplica(dataSourceName);
            if (replicaName != null) {
                return poolManager.getConnection(replicaName);
            }
        }

        return poolManager.getConnection(dataSourceName);
    }

    /**
     * Get a write connection (always routes to primary).
     */
    public Connection getWriteConnection() throws SQLException {
        return getConnection(false);
    }

    /**
     * Get a read connection (routes to replica if available).
     */
    public Connection getReadConnection() throws SQLException {
        return getConnection(true);
    }

    private String determineDataSource() {
        RoutingContext context = routingContext.get();

        if (context != null) {
            // First check explicit data source
            if (context.dataSourceName() != null) {
                return context.dataSourceName();
            }

            // Then route based on strategy
            switch (routingStrategy) {
                case TENANT:
                    if (context.tenantId() != null) {
                        String ds = tenantToDataSource.get(context.tenantId());
                        if (ds != null) return ds;
                    }
                    break;
                case REGION:
                    if (context.regionId() != null) {
                        String ds = regionToDataSource.get(context.regionId());
                        if (ds != null) return ds;
                    }
                    break;
                case CUSTOM:
                    // Custom routing can be implemented via data source name in context
                    break;
            }
        }

        return defaultDataSource;
    }

    private String selectReadReplica(String primaryDataSource) {
        String[] replicas = dataSourceToReplicas.get(primaryDataSource);

        if (replicas == null || replicas.length == 0) {
            return null;
        }

        return switch (loadBalancingStrategy) {
            case ROUND_ROBIN -> selectRoundRobin(primaryDataSource, replicas);
            case RANDOM -> selectRandom(replicas);
            case LEAST_CONNECTIONS -> selectLeastConnections(replicas);
        };
    }

    private String selectRoundRobin(String primary, String[] replicas) {
        int index = replicaIndex.compute(primary, (k, v) -> {
            if (v == null) return 0;
            return (v + 1) % replicas.length;
        });
        return replicas[index];
    }

    private String selectRandom(String[] replicas) {
        int index = java.util.concurrent.ThreadLocalRandom.current().nextInt(replicas.length);
        return replicas[index];
    }

    private String selectLeastConnections(String[] replicas) {
        String leastLoaded = replicas[0];
        int minConnections = Integer.MAX_VALUE;

        for (String replica : replicas) {
            try {
                var stats = poolManager.getStatistics(replica);
                if (stats.activeConnections() < minConnections) {
                    minConnections = stats.activeConnections();
                    leastLoaded = replica;
                }
            } catch (Exception e) {
                // Skip this replica if we can't get stats
            }
        }

        return leastLoaded;
    }

    /**
     * Routing context for dynamic data source selection.
     */
    public record RoutingContext(
        String tenantId,
        String regionId,
        String dataSourceName,
        boolean readOnly
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String tenantId;
            private String regionId;
            private String dataSourceName;
            private boolean readOnly = false;

            public Builder tenantId(String tenantId) {
                this.tenantId = tenantId;
                return this;
            }

            public Builder regionId(String regionId) {
                this.regionId = regionId;
                return this;
            }

            public Builder dataSourceName(String dataSourceName) {
                this.dataSourceName = dataSourceName;
                return this;
            }

            public Builder readOnly(boolean readOnly) {
                this.readOnly = readOnly;
                return this;
            }

            public RoutingContext build() {
                return new RoutingContext(tenantId, regionId, dataSourceName, readOnly);
            }
        }
    }

    /**
     * Routing strategy enum.
     */
    public enum RoutingStrategy {
        /** Route based on tenant ID */
        TENANT,
        /** Route based on region ID */
        REGION,
        /** Custom routing via explicit data source name */
        CUSTOM
    }

    /**
     * Load balancing strategy for read replicas.
     */
    public enum LoadBalancingStrategy {
        /** Round-robin across replicas */
        ROUND_ROBIN,
        /** Random selection */
        RANDOM,
        /** Select replica with least active connections */
        LEAST_CONNECTIONS
    }
}
