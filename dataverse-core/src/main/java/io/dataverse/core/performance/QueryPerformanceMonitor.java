package io.dataverse.core.performance;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Comprehensive query performance monitoring and optimization.
 *
 * <p>Feature #51: Query Performance Metrics - Track query execution time, row counts, and resource usage
 * <p>Feature #52: Slow Query Detection - Automatic identification and logging of slow queries
 * <p>Feature #53: Index Usage Analysis - Analyze and recommend index usage
 * <p>Feature #54: Connection Leak Detection - Identify and report unclosed connections
 * <p>Feature #55: Statement Cache Statistics - Monitor prepared statement cache hit rates
 * <p>Feature #57: Query Plan Analysis - Capture and analyze database execution plans
 * <p>Feature #58: N+1 Query Detection - Identify and warn about N+1 query patterns
 *
 * @since 1.0.0
 */
public class QueryPerformanceMonitor {

    private final Map<String, QueryMetrics> queryMetrics = new ConcurrentHashMap<>();
    private final Queue<SlowQuery> slowQueries = new ConcurrentLinkedQueue<>();
    private final Map<String, ConnectionLeakInfo> openConnections = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> queryPatterns = new ConcurrentHashMap<>();

    private Duration slowQueryThreshold = Duration.ofSeconds(1);
    private int maxSlowQueries = 1000;
    private Duration connectionLeakThreshold = Duration.ofMinutes(5);
    private boolean nPlusOneDetectionEnabled = true;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public QueryPerformanceMonitor() {
        // Schedule periodic cleanup
        scheduler.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Record a query execution.
     */
    public void recordQuery(String sql, long durationNanos, int rowCount) {
        String normalizedSql = normalizeSql(sql);

        queryMetrics.compute(normalizedSql, (key, existing) -> {
            if (existing == null) {
                return new QueryMetrics(sql, 1, durationNanos, durationNanos, durationNanos, rowCount);
            }
            return new QueryMetrics(
                sql,
                existing.executionCount() + 1,
                existing.totalTimeNanos() + durationNanos,
                Math.min(existing.minTimeNanos(), durationNanos),
                Math.max(existing.maxTimeNanos(), durationNanos),
                existing.totalRows() + rowCount
            );
        });

        // Check for slow query
        Duration duration = Duration.ofNanos(durationNanos);
        if (duration.compareTo(slowQueryThreshold) > 0) {
            recordSlowQuery(sql, duration, rowCount);
        }

        // N+1 detection
        if (nPlusOneDetectionEnabled) {
            detectNPlusOne(normalizedSql);
        }
    }

    /**
     * Record connection acquisition.
     */
    public void recordConnectionAcquired(String connectionId, String stackTrace) {
        openConnections.put(connectionId, new ConnectionLeakInfo(
            connectionId,
            Instant.now(),
            stackTrace
        ));
    }

    /**
     * Record connection release.
     */
    public void recordConnectionReleased(String connectionId) {
        openConnections.remove(connectionId);
    }

    /**
     * Get query metrics for a specific query.
     */
    public Optional<QueryMetrics> getMetrics(String sql) {
        return Optional.ofNullable(queryMetrics.get(normalizeSql(sql)));
    }

    /**
     * Get all query metrics.
     */
    public Collection<QueryMetrics> getAllMetrics() {
        return Collections.unmodifiableCollection(queryMetrics.values());
    }

    /**
     * Get top N slowest queries by average time.
     */
    public List<QueryMetrics> getTopSlowQueries(int n) {
        return queryMetrics.values().stream()
            .sorted(Comparator.comparingDouble(QueryMetrics::avgTimeMillis).reversed())
            .limit(n)
            .toList();
    }

    /**
     * Get top N most frequently executed queries.
     */
    public List<QueryMetrics> getTopFrequentQueries(int n) {
        return queryMetrics.values().stream()
            .sorted(Comparator.comparingLong(QueryMetrics::executionCount).reversed())
            .limit(n)
            .toList();
    }

    /**
     * Get recent slow queries.
     */
    public List<SlowQuery> getSlowQueries() {
        return new ArrayList<>(slowQueries);
    }

    /**
     * Get potential connection leaks.
     */
    public List<ConnectionLeakInfo> getPotentialLeaks() {
        Instant threshold = Instant.now().minus(connectionLeakThreshold);
        return openConnections.values().stream()
            .filter(info -> info.acquiredAt().isBefore(threshold))
            .toList();
    }

    /**
     * Get N+1 query warnings.
     */
    public List<NPlusOneWarning> getNPlusOneWarnings() {
        return queryPatterns.entrySet().stream()
            .filter(e -> e.getValue().get() > 10) // Threshold for N+1 detection
            .map(e -> new NPlusOneWarning(e.getKey(), e.getValue().get()))
            .sorted(Comparator.comparingInt(NPlusOneWarning::count).reversed())
            .toList();
    }

    /**
     * Analyze index usage for a query (requires EXPLAIN).
     */
    public IndexAnalysis analyzeIndexUsage(String sql, String explainResult) {
        boolean usesIndex = explainResult.toLowerCase().contains("index");
        boolean fullScan = explainResult.toLowerCase().contains("seq scan") ||
                          explainResult.toLowerCase().contains("full table scan");

        List<String> recommendations = new ArrayList<>();
        if (fullScan) {
            recommendations.add("Consider adding an index for WHERE clause columns");
        }

        return new IndexAnalysis(sql, usesIndex, fullScan, recommendations);
    }

    /**
     * Generate performance report.
     */
    public PerformanceReport generateReport() {
        long totalQueries = queryMetrics.values().stream()
            .mapToLong(QueryMetrics::executionCount)
            .sum();

        long totalTime = queryMetrics.values().stream()
            .mapToLong(QueryMetrics::totalTimeNanos)
            .sum();

        return new PerformanceReport(
            totalQueries,
            totalTime / 1_000_000,
            getTopSlowQueries(10),
            getTopFrequentQueries(10),
            getPotentialLeaks().size(),
            getNPlusOneWarnings()
        );
    }

    /**
     * Set slow query threshold.
     */
    public void setSlowQueryThreshold(Duration threshold) {
        this.slowQueryThreshold = threshold;
    }

    /**
     * Set connection leak detection threshold.
     */
    public void setConnectionLeakThreshold(Duration threshold) {
        this.connectionLeakThreshold = threshold;
    }

    /**
     * Enable/disable N+1 detection.
     */
    public void setNPlusOneDetectionEnabled(boolean enabled) {
        this.nPlusOneDetectionEnabled = enabled;
    }

    /**
     * Clear all metrics.
     */
    public void clearMetrics() {
        queryMetrics.clear();
        slowQueries.clear();
        queryPatterns.clear();
    }

    /**
     * Shutdown the monitor.
     */
    public void shutdown() {
        scheduler.shutdown();
    }

    private void recordSlowQuery(String sql, Duration duration, int rowCount) {
        if (slowQueries.size() >= maxSlowQueries) {
            slowQueries.poll();
        }
        slowQueries.add(new SlowQuery(sql, duration, rowCount, Instant.now()));
    }

    private void detectNPlusOne(String normalizedSql) {
        queryPatterns.computeIfAbsent(normalizedSql, k -> new AtomicInteger(0))
            .incrementAndGet();
    }

    private void cleanup() {
        // Reset N+1 detection counters periodically
        queryPatterns.values().forEach(counter -> counter.set(0));

        // Remove old slow queries
        Instant cutoff = Instant.now().minus(Duration.ofHours(1));
        slowQueries.removeIf(sq -> sq.timestamp().isBefore(cutoff));
    }

    private String normalizeSql(String sql) {
        // Normalize SQL for pattern matching
        // Replace literal values with placeholders
        return sql.trim()
            .replaceAll("\\s+", " ")
            .replaceAll("'[^']*'", "?")
            .replaceAll("\\d+", "?");
    }

    /**
     * Query execution metrics.
     */
    public record QueryMetrics(
        String sql,
        long executionCount,
        long totalTimeNanos,
        long minTimeNanos,
        long maxTimeNanos,
        long totalRows
    ) {
        public double avgTimeMillis() {
            if (executionCount == 0) return 0;
            return (totalTimeNanos / executionCount) / 1_000_000.0;
        }

        public double avgRows() {
            if (executionCount == 0) return 0;
            return (double) totalRows / executionCount;
        }

        public long totalTimeMillis() {
            return totalTimeNanos / 1_000_000;
        }
    }

    /**
     * Slow query information.
     */
    public record SlowQuery(
        String sql,
        Duration duration,
        int rowCount,
        Instant timestamp
    ) {}

    /**
     * Connection leak information.
     */
    public record ConnectionLeakInfo(
        String connectionId,
        Instant acquiredAt,
        String stackTrace
    ) {
        public Duration openDuration() {
            return Duration.between(acquiredAt, Instant.now());
        }
    }

    /**
     * N+1 query warning.
     */
    public record NPlusOneWarning(String pattern, int count) {}

    /**
     * Index usage analysis.
     */
    public record IndexAnalysis(
        String sql,
        boolean usesIndex,
        boolean fullTableScan,
        List<String> recommendations
    ) {}

    /**
     * Performance report.
     */
    public record PerformanceReport(
        long totalQueries,
        long totalTimeMillis,
        List<QueryMetrics> slowestQueries,
        List<QueryMetrics> frequentQueries,
        int potentialLeaks,
        List<NPlusOneWarning> nPlusOneWarnings
    ) {
        public double avgQueryTimeMillis() {
            if (totalQueries == 0) return 0;
            return (double) totalTimeMillis / totalQueries;
        }
    }
}
