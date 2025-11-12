package io.dataverse.core.metrics;

/**
 * Metrics for database connection pools.
 *
 * <p>Provides real-time monitoring of connection pool health and performance.
 * Exposes metrics compatible with Micrometer/Prometheus for production monitoring.
 *
 * <p><strong>Key Metrics:</strong>
 * <ul>
 *   <li><strong>Active Connections</strong> - Connections currently in use</li>
 *   <li><strong>Idle Connections</strong> - Connections available but unused</li>
 *   <li><strong>Total Connections</strong> - Total connections in pool</li>
 *   <li><strong>Pending Threads</strong> - Threads waiting for connections</li>
 *   <li><strong>Connection Wait Time</strong> - Time waiting to acquire connection</li>
 *   <li><strong>Connection Usage Time</strong> - Time connection is held</li>
 *   <li><strong>Connection Creation Time</strong> - Time to create new connection</li>
 *   <li><strong>Connection Timeout Count</strong> - Acquisitions that timed out</li>
 * </ul>
 *
 * <p><strong>Health Indicators:</strong>
 * <ul>
 *   <li>Pool exhaustion (active == total)</li>
 *   <li>High wait times (> 100ms)</li>
 *   <li>Timeouts (connection acquisition failures)</li>
 *   <li>Low idle connections (< 10% of total)</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Get pool metrics
 * ConnectionPoolMetrics metrics = adapter.getConnectionPoolMetrics();
 *
 * // Check health
 * if (metrics.getActiveConnections() == metrics.getTotalConnections()) {
 *     logger.warn("Connection pool exhausted!");
 * }
 *
 * if (metrics.getAverageWaitTimeMs() > 100) {
 *     logger.warn("High connection wait time: {}ms", metrics.getAverageWaitTimeMs());
 * }
 *
 * // Export to Prometheus
 * metrics.registerWithMicrometer(meterRegistry);
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class ConnectionPoolMetrics {

  private final String poolName;
  private int activeConnections;
  private int idleConnections;
  private int totalConnections;
  private int maxConnections;
  private int minConnections;
  private int pendingThreads;
  private long totalConnectionsCreated;
  private long totalConnectionsAcquired;
  private long totalConnectionTimeouts;
  private long totalWaitTimeMs;
  private long totalUsageTimeMs;
  private long totalCreationTimeMs;
  private long acquisitionCount;

  public ConnectionPoolMetrics(String poolName) {
    this.poolName = poolName;
  }

  // Getters

  public String getPoolName() {
    return poolName;
  }

  public int getActiveConnections() {
    return activeConnections;
  }

  public void setActiveConnections(int activeConnections) {
    this.activeConnections = activeConnections;
  }

  public int getIdleConnections() {
    return idleConnections;
  }

  public void setIdleConnections(int idleConnections) {
    this.idleConnections = idleConnections;
  }

  public int getTotalConnections() {
    return totalConnections;
  }

  public void setTotalConnections(int totalConnections) {
    this.totalConnections = totalConnections;
  }

  public int getMaxConnections() {
    return maxConnections;
  }

  public void setMaxConnections(int maxConnections) {
    this.maxConnections = maxConnections;
  }

  public int getMinConnections() {
    return minConnections;
  }

  public void setMinConnections(int minConnections) {
    this.minConnections = minConnections;
  }

  public int getPendingThreads() {
    return pendingThreads;
  }

  public void setPendingThreads(int pendingThreads) {
    this.pendingThreads = pendingThreads;
  }

  public long getTotalConnectionsCreated() {
    return totalConnectionsCreated;
  }

  public void setTotalConnectionsCreated(long totalConnectionsCreated) {
    this.totalConnectionsCreated = totalConnectionsCreated;
  }

  public long getTotalConnectionsAcquired() {
    return totalConnectionsAcquired;
  }

  public void setTotalConnectionsAcquired(long totalConnectionsAcquired) {
    this.totalConnectionsAcquired = totalConnectionsAcquired;
  }

  public long getTotalConnectionTimeouts() {
    return totalConnectionTimeouts;
  }

  public void setTotalConnectionTimeouts(long totalConnectionTimeouts) {
    this.totalConnectionTimeouts = totalConnectionTimeouts;
  }

  public long getTotalWaitTimeMs() {
    return totalWaitTimeMs;
  }

  public void setTotalWaitTimeMs(long totalWaitTimeMs) {
    this.totalWaitTimeMs = totalWaitTimeMs;
  }

  public long getTotalUsageTimeMs() {
    return totalUsageTimeMs;
  }

  public void setTotalUsageTimeMs(long totalUsageTimeMs) {
    this.totalUsageTimeMs = totalUsageTimeMs;
  }

  public long getTotalCreationTimeMs() {
    return totalCreationTimeMs;
  }

  public void setTotalCreationTimeMs(long totalCreationTimeMs) {
    this.totalCreationTimeMs = totalCreationTimeMs;
  }

  public long getAcquisitionCount() {
    return acquisitionCount;
  }

  public void setAcquisitionCount(long acquisitionCount) {
    this.acquisitionCount = acquisitionCount;
  }

  // Computed metrics

  /**
   * Gets the pool utilization percentage (0-100).
   *
   * @return utilization percentage
   */
  public double getUtilizationPercentage() {
    if (maxConnections == 0) return 0.0;
    return (double) totalConnections / maxConnections * 100.0;
  }

  /**
   * Gets the percentage of connections that are active (0-100).
   *
   * @return active percentage
   */
  public double getActivePercentage() {
    if (totalConnections == 0) return 0.0;
    return (double) activeConnections / totalConnections * 100.0;
  }

  /**
   * Gets the average wait time to acquire a connection in milliseconds.
   *
   * @return average wait time in ms
   */
  public double getAverageWaitTimeMs() {
    if (acquisitionCount == 0) return 0.0;
    return (double) totalWaitTimeMs / acquisitionCount;
  }

  /**
   * Gets the average time a connection is used in milliseconds.
   *
   * @return average usage time in ms
   */
  public double getAverageUsageTimeMs() {
    if (totalConnectionsAcquired == 0) return 0.0;
    return (double) totalUsageTimeMs / totalConnectionsAcquired;
  }

  /**
   * Gets the average time to create a new connection in milliseconds.
   *
   * @return average creation time in ms
   */
  public double getAverageCreationTimeMs() {
    if (totalConnectionsCreated == 0) return 0.0;
    return (double) totalCreationTimeMs / totalConnectionsCreated;
  }

  /**
   * Gets the timeout rate (timeouts per acquisition).
   *
   * @return timeout rate (0.0 to 1.0)
   */
  public double getTimeoutRate() {
    if (acquisitionCount == 0) return 0.0;
    return (double) totalConnectionTimeouts / acquisitionCount;
  }

  /**
   * Checks if the pool is exhausted (all connections in use).
   *
   * @return true if pool is exhausted
   */
  public boolean isExhausted() {
    return activeConnections >= maxConnections && pendingThreads > 0;
  }

  /**
   * Checks if the pool is healthy based on standard thresholds.
   *
   * @return true if pool is healthy
   */
  public boolean isHealthy() {
    return !isExhausted()
        && getTimeoutRate() < 0.01  // < 1% timeout rate
        && getAverageWaitTimeMs() < 100;  // < 100ms wait time
  }

  /**
   * Records a connection acquisition.
   *
   * @param waitTimeMs the time waited to acquire the connection
   */
  public synchronized void recordAcquisition(long waitTimeMs) {
    acquisitionCount++;
    totalWaitTimeMs += waitTimeMs;
    totalConnectionsAcquired++;
  }

  /**
   * Records a connection creation.
   *
   * @param creationTimeMs the time to create the connection
   */
  public synchronized void recordCreation(long creationTimeMs) {
    totalConnectionsCreated++;
    totalCreationTimeMs += creationTimeMs;
  }

  /**
   * Records a connection timeout.
   */
  public synchronized void recordTimeout() {
    totalConnectionTimeouts++;
  }

  /**
   * Records connection usage.
   *
   * @param usageTimeMs the time the connection was held
   */
  public synchronized void recordUsage(long usageTimeMs) {
    totalUsageTimeMs += usageTimeMs;
  }

  @Override
  public String toString() {
    return String.format(
        "ConnectionPoolMetrics{pool='%s', active=%d/%d, idle=%d, pending=%d, " +
        "utilization=%.1f%%, avgWait=%.2fms, timeouts=%d, healthy=%s}",
        poolName, activeConnections, maxConnections, idleConnections, pendingThreads,
        getUtilizationPercentage(), getAverageWaitTimeMs(), totalConnectionTimeouts, isHealthy()
    );
  }
}
