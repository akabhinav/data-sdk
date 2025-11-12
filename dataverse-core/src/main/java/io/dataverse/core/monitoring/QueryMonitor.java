package io.dataverse.core.monitoring;

import java.time.Instant;
import java.util.concurrent.Callable;

/**
 * Query monitor for tracking query execution and performance.
 *
 * <p>Example usage in repository:
 * <pre>
 * public T save(T entity) {
 *     return queryMonitor.execute("save", () -> {
 *         return doSave(entity);
 *     });
 * }
 * </pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class QueryMonitor {

  private final QueryMonitoringConfig config;
  private final String entityClass;
  private final String adapterId;

  public QueryMonitor(QueryMonitoringConfig config, String entityClass, String adapterId) {
    this.config = config;
    this.entityClass = entityClass;
    this.adapterId = adapterId;
  }

  /**
   * Executes an operation and tracks its performance.
   *
   * @param operation the operation name (e.g., "save", "findById", "query")
   * @param callable the operation to execute
   * @param <T> the return type
   * @return the result of the operation
   * @throws Exception if the operation fails
   */
  public <T> T execute(String operation, Callable<T> callable) throws Exception {
    if (config == null) {
      // Monitoring not configured, execute directly
      return callable.call();
    }

    Instant startTime = Instant.now();
    QueryMetrics.Builder metricsBuilder = QueryMetrics.builder()
        .operation(operation)
        .entityClass(entityClass)
        .adapterId(adapterId)
        .startTime(startTime);

    // Capture stack trace if configured
    if (config.isIncludeStackTrace()) {
      metricsBuilder.stackTrace(captureStackTrace());
    }

    T result = null;
    try {
      result = callable.call();
      metricsBuilder.success(true);

      // Count results
      if (result instanceof Iterable<?>) {
        int count = 0;
        for (Object ignored : (Iterable<?>) result) {
          count++;
        }
        metricsBuilder.resultCount(count);
      } else if (result != null) {
        metricsBuilder.resultCount(1);
      }

      return result;

    } catch (Exception e) {
      metricsBuilder.error(e);
      throw e;

    } finally {
      Instant endTime = Instant.now();
      metricsBuilder.endTime(endTime);

      QueryMetrics metrics = metricsBuilder.build();

      // Check if slow query
      if (metrics.isSlowQuery(config.getSlowQueryThreshold()) && config.getSlowQueryHandler() != null) {
        config.getSlowQueryHandler().accept(metrics);
      }

      // Track all queries if configured
      if (config.isTrackAllQueries() && config.getQueryHandler() != null) {
        config.getQueryHandler().accept(metrics);
      }
    }
  }

  /**
   * Executes an operation without return value and tracks its performance.
   *
   * @param operation the operation name
   * @param runnable the operation to execute
   */
  public void execute(String operation, Runnable runnable) {
    try {
      execute(operation, () -> {
        runnable.run();
        return null;
      });
    } catch (Exception e) {
      // Rethrow as RuntimeException
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException("Query execution failed", e);
    }
  }

  /**
   * Captures current stack trace for debugging slow queries.
   */
  private String captureStackTrace() {
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    StringBuilder sb = new StringBuilder();

    // Skip first 3 elements (getStackTrace, captureStackTrace, execute)
    for (int i = 3; i < Math.min(stackTrace.length, 10); i++) {
      sb.append("  at ").append(stackTrace[i].toString()).append("\n");
    }

    return sb.toString();
  }

  /**
   * Creates a no-op monitor when monitoring is disabled.
   */
  public static QueryMonitor noOp() {
    return new QueryMonitor(null, null, null);
  }
}
