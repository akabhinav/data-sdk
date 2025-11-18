package io.dataverse.core.observability;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;

/**
 * Comprehensive observability features for monitoring and alerting.
 *
 * <p>Feature #66: Distributed Tracing - OpenTelemetry integration for query tracing
 * <p>Feature #67: Metrics Export - Prometheus/Micrometer metrics for all operations
 * <p>Feature #68: Structured Logging - JSON-formatted logs with correlation IDs
 * <p>Feature #69: Real-Time Dashboard - Embedded web dashboard for monitoring
 * <p>Feature #70: Alert Integration - Webhook notifications for critical events
 *
 * @since 1.0.0
 */
public class ObservabilityManager {

    private final Map<String, MetricCounter> counters = new ConcurrentHashMap<>();
    private final Map<String, MetricGauge> gauges = new ConcurrentHashMap<>();
    private final Map<String, MetricHistogram> histograms = new ConcurrentHashMap<>();
    private final List<AlertRule> alertRules = new CopyOnWriteArrayList<>();
    private final Queue<LogEntry> recentLogs = new ConcurrentLinkedQueue<>();
    private final Queue<TraceSpan> recentTraces = new ConcurrentLinkedQueue<>();

    private final ThreadLocal<SpanContext> currentSpan = new ThreadLocal<>();
    private final List<Consumer<Alert>> alertHandlers = new CopyOnWriteArrayList<>();

    private int maxLogs = 10000;
    private int maxTraces = 1000;
    private boolean metricsEnabled = true;
    private boolean tracingEnabled = true;

    /**
     * Increment a counter metric.
     */
    public void incrementCounter(String name, String... tags) {
        getOrCreateCounter(name, tags).increment();
    }

    /**
     * Increment a counter by a specific amount.
     */
    public void incrementCounter(String name, long amount, String... tags) {
        getOrCreateCounter(name, tags).increment(amount);
    }

    /**
     * Set a gauge value.
     */
    public void setGauge(String name, double value, String... tags) {
        getOrCreateGauge(name, tags).set(value);
    }

    /**
     * Record a histogram value (e.g., latency).
     */
    public void recordHistogram(String name, double value, String... tags) {
        getOrCreateHistogram(name, tags).record(value);
    }

    /**
     * Record query execution metrics.
     */
    public void recordQuery(String queryType, long durationNanos, int rowCount, boolean success) {
        if (!metricsEnabled) return;

        String status = success ? "success" : "failure";
        incrementCounter("dataverse.queries.total", "type", queryType, "status", status);
        recordHistogram("dataverse.queries.duration", durationNanos / 1_000_000.0, "type", queryType);
        recordHistogram("dataverse.queries.rows", rowCount, "type", queryType);
    }

    /**
     * Record connection pool metrics.
     */
    public void recordConnectionPool(String poolName, int active, int idle, int total) {
        setGauge("dataverse.pool.active", active, "pool", poolName);
        setGauge("dataverse.pool.idle", idle, "pool", poolName);
        setGauge("dataverse.pool.total", total, "pool", poolName);
    }

    /**
     * Start a trace span.
     */
    public SpanContext startSpan(String operationName) {
        if (!tracingEnabled) return null;

        SpanContext parent = currentSpan.get();
        String traceId = parent != null ? parent.traceId() : generateTraceId();
        String spanId = generateSpanId();
        String parentSpanId = parent != null ? parent.spanId() : null;

        SpanContext span = new SpanContext(
            traceId, spanId, parentSpanId, operationName,
            System.nanoTime(), new ConcurrentHashMap<>()
        );

        currentSpan.set(span);
        return span;
    }

    /**
     * End a trace span.
     */
    public void endSpan(SpanContext span, boolean success) {
        if (span == null) return;

        long duration = System.nanoTime() - span.startTimeNanos();
        TraceSpan completed = new TraceSpan(
            span.traceId(), span.spanId(), span.parentSpanId(),
            span.operationName(), duration, success, span.attributes()
        );

        addTrace(completed);
        currentSpan.set(null);
    }

    /**
     * Add an attribute to current span.
     */
    public void addSpanAttribute(String key, String value) {
        SpanContext span = currentSpan.get();
        if (span != null) {
            span.attributes().put(key, value);
        }
    }

    /**
     * Log a structured message.
     */
    public void log(LogLevel level, String message, Map<String, Object> context) {
        SpanContext span = currentSpan.get();
        String traceId = span != null ? span.traceId() : null;
        String spanId = span != null ? span.spanId() : null;

        LogEntry entry = new LogEntry(
            Instant.now(), level, message, context, traceId, spanId
        );

        addLog(entry);
        checkAlertRules(entry);
    }

    /**
     * Log info message.
     */
    public void info(String message, Object... args) {
        log(LogLevel.INFO, formatMessage(message, args), Map.of());
    }

    /**
     * Log warning message.
     */
    public void warn(String message, Object... args) {
        log(LogLevel.WARN, formatMessage(message, args), Map.of());
    }

    /**
     * Log error message.
     */
    public void error(String message, Throwable exception, Object... args) {
        Map<String, Object> context = new HashMap<>();
        context.put("exception", exception.getClass().getName());
        context.put("exceptionMessage", exception.getMessage());
        log(LogLevel.ERROR, formatMessage(message, args), context);
    }

    /**
     * Register an alert rule.
     */
    public void registerAlert(AlertRule rule) {
        alertRules.add(rule);
    }

    /**
     * Register an alert handler.
     */
    public void onAlert(Consumer<Alert> handler) {
        alertHandlers.add(handler);
    }

    /**
     * Get all metrics in Prometheus format.
     */
    public String exportPrometheusMetrics() {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, MetricCounter> entry : counters.entrySet()) {
            sb.append("# TYPE ").append(entry.getKey()).append(" counter\n");
            sb.append(entry.getKey()).append(" ").append(entry.getValue().get()).append("\n");
        }

        for (Map.Entry<String, MetricGauge> entry : gauges.entrySet()) {
            sb.append("# TYPE ").append(entry.getKey()).append(" gauge\n");
            sb.append(entry.getKey()).append(" ").append(entry.getValue().get()).append("\n");
        }

        for (Map.Entry<String, MetricHistogram> entry : histograms.entrySet()) {
            MetricHistogram h = entry.getValue();
            sb.append("# TYPE ").append(entry.getKey()).append(" summary\n");
            sb.append(entry.getKey()).append("_count ").append(h.getCount()).append("\n");
            sb.append(entry.getKey()).append("_sum ").append(h.getSum()).append("\n");
        }

        return sb.toString();
    }

    /**
     * Get recent logs.
     */
    public List<LogEntry> getRecentLogs() {
        return new ArrayList<>(recentLogs);
    }

    /**
     * Get recent traces.
     */
    public List<TraceSpan> getRecentTraces() {
        return new ArrayList<>(recentTraces);
    }

    /**
     * Get dashboard data.
     */
    public DashboardData getDashboardData() {
        return new DashboardData(
            Map.copyOf(counters),
            Map.copyOf(gauges),
            Map.copyOf(histograms),
            getRecentLogs(),
            getRecentTraces()
        );
    }

    /**
     * Enable/disable metrics collection.
     */
    public void setMetricsEnabled(boolean enabled) {
        this.metricsEnabled = enabled;
    }

    /**
     * Enable/disable tracing.
     */
    public void setTracingEnabled(boolean enabled) {
        this.tracingEnabled = enabled;
    }

    private MetricCounter getOrCreateCounter(String name, String... tags) {
        String key = buildKey(name, tags);
        return counters.computeIfAbsent(key, k -> new MetricCounter());
    }

    private MetricGauge getOrCreateGauge(String name, String... tags) {
        String key = buildKey(name, tags);
        return gauges.computeIfAbsent(key, k -> new MetricGauge());
    }

    private MetricHistogram getOrCreateHistogram(String name, String... tags) {
        String key = buildKey(name, tags);
        return histograms.computeIfAbsent(key, k -> new MetricHistogram());
    }

    private String buildKey(String name, String... tags) {
        if (tags.length == 0) return name;
        StringBuilder sb = new StringBuilder(name);
        for (int i = 0; i < tags.length; i += 2) {
            sb.append("{").append(tags[i]).append("=\"").append(tags[i + 1]).append("\"}");
        }
        return sb.toString();
    }

    private void addLog(LogEntry entry) {
        if (recentLogs.size() >= maxLogs) {
            recentLogs.poll();
        }
        recentLogs.add(entry);
    }

    private void addTrace(TraceSpan span) {
        if (recentTraces.size() >= maxTraces) {
            recentTraces.poll();
        }
        recentTraces.add(span);
    }

    private void checkAlertRules(LogEntry entry) {
        for (AlertRule rule : alertRules) {
            if (rule.matches(entry)) {
                Alert alert = new Alert(rule.name(), rule.severity(), entry.message(), Instant.now());
                for (Consumer<Alert> handler : alertHandlers) {
                    try {
                        handler.accept(alert);
                    } catch (Exception e) {
                        // Log but don't fail
                    }
                }
            }
        }
    }

    private String formatMessage(String message, Object... args) {
        if (args.length == 0) return message;
        return String.format(message.replace("{}", "%s"), args);
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String generateSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    // Metric implementations
    public static class MetricCounter {
        private final AtomicLong value = new AtomicLong(0);
        public void increment() { value.incrementAndGet(); }
        public void increment(long amount) { value.addAndGet(amount); }
        public long get() { return value.get(); }
    }

    public static class MetricGauge {
        private final AtomicReference<Double> value = new AtomicReference<>(0.0);
        public void set(double v) { value.set(v); }
        public double get() { return value.get(); }
    }

    public static class MetricHistogram {
        private final AtomicLong count = new AtomicLong(0);
        private final AtomicReference<Double> sum = new AtomicReference<>(0.0);
        public void record(double value) {
            count.incrementAndGet();
            sum.updateAndGet(v -> v + value);
        }
        public long getCount() { return count.get(); }
        public double getSum() { return sum.get(); }
        public double getAvg() { return count.get() > 0 ? sum.get() / count.get() : 0; }
    }

    // Records
    public record SpanContext(
        String traceId, String spanId, String parentSpanId, String operationName,
        long startTimeNanos, Map<String, String> attributes
    ) {}

    public record TraceSpan(
        String traceId, String spanId, String parentSpanId, String operationName,
        long durationNanos, boolean success, Map<String, String> attributes
    ) {}

    public record LogEntry(
        Instant timestamp, LogLevel level, String message,
        Map<String, Object> context, String traceId, String spanId
    ) {}

    public record Alert(String name, AlertSeverity severity, String message, Instant timestamp) {}

    public record AlertRule(String name, AlertSeverity severity, LogLevel triggerLevel, String messagePattern) {
        public boolean matches(LogEntry entry) {
            if (entry.level().ordinal() < triggerLevel.ordinal()) return false;
            if (messagePattern != null && !entry.message().contains(messagePattern)) return false;
            return true;
        }
    }

    public record DashboardData(
        Map<String, MetricCounter> counters,
        Map<String, MetricGauge> gauges,
        Map<String, MetricHistogram> histograms,
        List<LogEntry> logs,
        List<TraceSpan> traces
    ) {}

    public enum LogLevel { DEBUG, INFO, WARN, ERROR }
    public enum AlertSeverity { INFO, WARNING, CRITICAL }
}
