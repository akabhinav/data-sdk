package io.dataverse.spi;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Result of an adapter health check.
 *
 * <p>HealthCheckResult provides detailed information about the adapter's health status,
 * including connectivity, performance metrics, and any issues detected.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public final class HealthCheckResult {

  private final Status status;
  private final Instant timestamp;
  private final Map<String, Object> details;
  private final Optional<String> message;
  private final Optional<Throwable> error;

  private HealthCheckResult(
      Status status,
      Instant timestamp,
      Map<String, Object> details,
      Optional<String> message,
      Optional<Throwable> error) {
    this.status = status;
    this.timestamp = timestamp;
    this.details = Map.copyOf(details);
    this.message = message;
    this.error = error;
  }

  /**
   * Returns the health status.
   *
   * @return the status, never {@code null}
   */
  public Status getStatus() {
    return status;
  }

  /**
   * Returns the timestamp when the health check was performed.
   *
   * @return the timestamp, never {@code null}
   */
  public Instant getTimestamp() {
    return timestamp;
  }

  /**
   * Returns detailed health check information.
   *
   * @return the details map, never {@code null}
   */
  public Map<String, Object> getDetails() {
    return details;
  }

  /**
   * Returns an optional health check message.
   *
   * @return the message if present
   */
  public Optional<String> getMessage() {
    return message;
  }

  /**
   * Returns an optional error that occurred during health check.
   *
   * @return the error if present
   */
  public Optional<Throwable> getError() {
    return error;
  }

  /**
   * Checks if the adapter is healthy.
   *
   * @return {@code true} if status is UP, {@code false} otherwise
   */
  public boolean isHealthy() {
    return status == Status.UP;
  }

  /**
   * Creates a healthy result.
   *
   * @return a new builder for a healthy result
   */
  public static Builder healthy() {
    return new Builder(Status.UP);
  }

  /**
   * Creates an unhealthy result.
   *
   * @return a new builder for an unhealthy result
   */
  public static Builder unhealthy() {
    return new Builder(Status.DOWN);
  }

  /**
   * Creates a degraded result.
   *
   * @return a new builder for a degraded result
   */
  public static Builder degraded() {
    return new Builder(Status.DEGRADED);
  }

  /**
   * Health status enumeration.
   */
  public enum Status {
    /** Adapter is fully operational. */
    UP,

    /** Adapter is not operational. */
    DOWN,

    /** Adapter is operational but with reduced functionality. */
    DEGRADED,

    /** Health status is unknown. */
    UNKNOWN
  }

  /**
   * Builder for HealthCheckResult.
   */
  public static final class Builder {
    private final Status status;
    private final Map<String, Object> details = new HashMap<>();
    private String message;
    private Throwable error;

    private Builder(Status status) {
      this.status = status;
    }

    /**
     * Adds a detail entry.
     *
     * @param key the detail key
     * @param value the detail value
     * @return this builder
     */
    public Builder withDetail(String key, Object value) {
      details.put(key, value);
      return this;
    }

    /**
     * Adds multiple detail entries.
     *
     * @param details the details to add
     * @return this builder
     */
    public Builder withDetails(Map<String, Object> details) {
      this.details.putAll(details);
      return this;
    }

    /**
     * Sets the health check message.
     *
     * @param message the message
     * @return this builder
     */
    public Builder withMessage(String message) {
      this.message = message;
      return this;
    }

    /**
     * Sets the error that occurred during health check.
     *
     * @param error the error
     * @return this builder
     */
    public Builder withError(Throwable error) {
      this.error = error;
      return this;
    }

    /**
     * Builds the health check result.
     *
     * @return a new HealthCheckResult instance
     */
    public HealthCheckResult build() {
      return new HealthCheckResult(
          status, Instant.now(), details, Optional.ofNullable(message), Optional.ofNullable(error));
    }
  }
}
