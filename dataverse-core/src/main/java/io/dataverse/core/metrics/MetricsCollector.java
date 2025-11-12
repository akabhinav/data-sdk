package io.dataverse.core.metrics;

import java.util.Map;

/**
 * Interface for collecting metrics from data source adapters.
 *
 * <p>Adapters implement this interface to expose their internal metrics
 * for monitoring and observability.
 *
 * <p><strong>Supported Metrics:</strong>
 * <ul>
 *   <li>Connection pool metrics (active, idle, wait times)</li>
 *   <li>Query execution metrics (count, duration, errors)</li>
 *   <li>Cache hit rates and statistics</li>
 *   <li>Transaction counts and durations</li>
 *   <li>Error rates and types</li>
 * </ul>
 *
 * <p><strong>Integration with Monitoring Systems:</strong>
 * <ul>
 *   <li><strong>Micrometer</strong> - Export to Prometheus, Grafana, etc.</li>
 *   <li><strong>Spring Boot Actuator</strong> - /actuator/metrics endpoint</li>
 *   <li><strong>Custom Dashboards</strong> - Poll metrics periodically</li>
 * </ul>
 *
 * <p><strong>Example Implementation:</strong>
 * <pre>{@code
 * public class PostgreSQLAdapter implements MetricsCollector {
 *     private final ConnectionPoolMetrics poolMetrics;
 *
 *     @Override
 *     public ConnectionPoolMetrics getConnectionPoolMetrics() {
 *         // Update metrics from HikariCP
 *         HikariPoolMXBean pool = hikariDataSource.getHikariPoolMXBean();
 *         poolMetrics.setActiveConnections(pool.getActiveConnections());
 *         poolMetrics.setIdleConnections(pool.getIdleConnections());
 *         poolMetrics.setTotalConnections(pool.getTotalConnections());
 *         return poolMetrics;
 *     }
 *
 *     @Override
 *     public Map<String, Object> getCustomMetrics() {
 *         return Map.of(
 *             "query.count", queryCount,
 *             "query.errors", errorCount,
 *             "transaction.active", activeTransactions
 *         );
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface MetricsCollector {

  /**
   * Gets connection pool metrics.
   *
   * <p>Returns null if the adapter doesn't use a connection pool
   * (e.g., MongoDB, Redis use connection per request).
   *
   * @return connection pool metrics, or null if not applicable
   */
  ConnectionPoolMetrics getConnectionPoolMetrics();

  /**
   * Gets custom adapter-specific metrics.
   *
   * <p>Metrics are returned as a map of metric names to values.
   * Values can be numbers (long, double) or strings.
   *
   * <p>Common metric names:
   * <ul>
   *   <li>query.count - Total queries executed</li>
   *   <li>query.errors - Total query errors</li>
   *   <li>query.duration.avg - Average query duration in ms</li>
   *   <li>transaction.active - Active transactions</li>
   *   <li>transaction.committed - Total committed transactions</li>
   *   <li>transaction.rolled_back - Total rolled back transactions</li>
   *   <li>cache.hit_rate - Cache hit rate percentage</li>
   * </ul>
   *
   * @return map of metric names to values
   */
  Map<String, Object> getCustomMetrics();

  /**
   * Checks if the adapter is healthy and available.
   *
   * <p>Performs a health check (e.g., SELECT 1, ping) to verify
   * the connection to the data source is working.
   *
   * @return true if healthy, false otherwise
   */
  boolean isHealthy();

  /**
   * Gets the data source type name.
   *
   * @return data source type (e.g., "PostgreSQL", "MongoDB", "Redis")
   */
  String getDataSourceType();
}
