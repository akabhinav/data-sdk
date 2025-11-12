package io.dataverse.core.monitoring;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Configuration for query performance monitoring.
 *
 * <p>Example usage:
 * <pre>
 * QueryMonitoringConfig config = QueryMonitoringConfig.builder()
 *     .slowQueryThreshold(Duration.ofMillis(100))
 *     .onSlowQuery(metrics -> {
 *         log.warn("Slow query detected: {} took {}ms",
 *             metrics.getOperation(), metrics.getDuration().toMillis());
 *     })
 *     .trackAllQueries(true)
 *     .build();
 *
 * repository.configureMonitoring(config);
 * </pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class QueryMonitoringConfig {

  private final Duration slowQueryThreshold;
  private final Consumer<QueryMetrics> slowQueryHandler;
  private final Consumer<QueryMetrics> queryHandler;
  private final boolean trackAllQueries;
  private final boolean includeStackTrace;

  private QueryMonitoringConfig(Builder builder) {
    this.slowQueryThreshold = builder.slowQueryThreshold;
    this.slowQueryHandler = builder.slowQueryHandler;
    this.queryHandler = builder.queryHandler;
    this.trackAllQueries = builder.trackAllQueries;
    this.includeStackTrace = builder.includeStackTrace;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Duration getSlowQueryThreshold() {
    return slowQueryThreshold;
  }

  public Consumer<QueryMetrics> getSlowQueryHandler() {
    return slowQueryHandler;
  }

  public Consumer<QueryMetrics> getQueryHandler() {
    return queryHandler;
  }

  public boolean isTrackAllQueries() {
    return trackAllQueries;
  }

  public boolean isIncludeStackTrace() {
    return includeStackTrace;
  }

  public static class Builder {
    private Duration slowQueryThreshold = Duration.ofMillis(100);
    private Consumer<QueryMetrics> slowQueryHandler = metrics -> {
      // Default: print to System.err
      System.err.printf("SLOW QUERY: %s took %dms%n",
          metrics.getOperation(), metrics.getDuration().toMillis());
    };
    private Consumer<QueryMetrics> queryHandler;
    private boolean trackAllQueries = false;
    private boolean includeStackTrace = false;

    /**
     * Sets the threshold for considering a query as "slow".
     * Default is 100ms.
     *
     * @param threshold the duration threshold
     * @return this builder
     */
    public Builder slowQueryThreshold(Duration threshold) {
      this.slowQueryThreshold = threshold;
      return this;
    }

    /**
     * Sets the handler to be called when a slow query is detected.
     *
     * @param handler the slow query handler
     * @return this builder
     */
    public Builder onSlowQuery(Consumer<QueryMetrics> handler) {
      this.slowQueryHandler = handler;
      return this;
    }

    /**
     * Sets the handler to be called for all queries (regardless of speed).
     * Only invoked if trackAllQueries is true.
     *
     * @param handler the query handler
     * @return this builder
     */
    public Builder onQuery(Consumer<QueryMetrics> handler) {
      this.queryHandler = handler;
      return this;
    }

    /**
     * Enables tracking of all queries, not just slow ones.
     * Default is false.
     *
     * @param track true to track all queries
     * @return this builder
     */
    public Builder trackAllQueries(boolean track) {
      this.trackAllQueries = track;
      return this;
    }

    /**
     * Includes stack trace in query metrics for debugging.
     * Default is false (performance overhead).
     *
     * @param include true to include stack traces
     * @return this builder
     */
    public Builder includeStackTrace(boolean include) {
      this.includeStackTrace = include;
      return this;
    }

    public QueryMonitoringConfig build() {
      return new QueryMonitoringConfig(this);
    }
  }
}
