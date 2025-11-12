package io.dataverse.core.monitoring;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Metrics for a single query execution.
 *
 * <p>Captures timing, operation type, parameters, and result information.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class QueryMetrics {

  private final String operation;
  private final String entityClass;
  private final Instant startTime;
  private final Instant endTime;
  private final Duration duration;
  private final boolean success;
  private final Throwable error;
  private final int resultCount;
  private final Map<String, Object> parameters;
  private final String stackTrace;
  private final String adapterId;

  private QueryMetrics(Builder builder) {
    this.operation = builder.operation;
    this.entityClass = builder.entityClass;
    this.startTime = builder.startTime;
    this.endTime = builder.endTime;
    this.duration = Duration.between(startTime, endTime);
    this.success = builder.success;
    this.error = builder.error;
    this.resultCount = builder.resultCount;
    this.parameters = builder.parameters;
    this.stackTrace = builder.stackTrace;
    this.adapterId = builder.adapterId;
  }

  public static Builder builder() {
    return new Builder();
  }

  // Getters
  public String getOperation() { return operation; }
  public String getEntityClass() { return entityClass; }
  public Instant getStartTime() { return startTime; }
  public Instant getEndTime() { return endTime; }
  public Duration getDuration() { return duration; }
  public long getDurationMillis() { return duration.toMillis(); }
  public boolean isSuccess() { return success; }
  public Throwable getError() { return error; }
  public int getResultCount() { return resultCount; }
  public Map<String, Object> getParameters() { return parameters; }
  public String getStackTrace() { return stackTrace; }
  public String getAdapterId() { return adapterId; }

  /**
   * Returns true if this query is considered slow based on the given threshold.
   *
   * @param threshold the slow query threshold
   * @return true if duration exceeds threshold
   */
  public boolean isSlowQuery(Duration threshold) {
    return duration.compareTo(threshold) > 0;
  }

  @Override
  public String toString() {
    return String.format("QueryMetrics{operation='%s', entityClass='%s', duration=%dms, success=%s, resultCount=%d}",
        operation, entityClass, getDurationMillis(), success, resultCount);
  }

  public static class Builder {
    private String operation;
    private String entityClass;
    private Instant startTime;
    private Instant endTime;
    private boolean success = true;
    private Throwable error;
    private int resultCount = 0;
    private Map<String, Object> parameters = new ConcurrentHashMap<>();
    private String stackTrace;
    private String adapterId;

    public Builder operation(String operation) {
      this.operation = operation;
      return this;
    }

    public Builder entityClass(String entityClass) {
      this.entityClass = entityClass;
      return this;
    }

    public Builder startTime(Instant startTime) {
      this.startTime = startTime;
      return this;
    }

    public Builder endTime(Instant endTime) {
      this.endTime = endTime;
      return this;
    }

    public Builder success(boolean success) {
      this.success = success;
      return this;
    }

    public Builder error(Throwable error) {
      this.error = error;
      this.success = false;
      return this;
    }

    public Builder resultCount(int count) {
      this.resultCount = count;
      return this;
    }

    public Builder parameter(String key, Object value) {
      this.parameters.put(key, value);
      return this;
    }

    public Builder parameters(Map<String, Object> params) {
      this.parameters.putAll(params);
      return this;
    }

    public Builder stackTrace(String stackTrace) {
      this.stackTrace = stackTrace;
      return this;
    }

    public Builder adapterId(String adapterId) {
      this.adapterId = adapterId;
      return this;
    }

    public QueryMetrics build() {
      if (startTime == null) {
        throw new IllegalStateException("startTime is required");
      }
      if (endTime == null) {
        throw new IllegalStateException("endTime is required");
      }
      return new QueryMetrics(this);
    }
  }
}
