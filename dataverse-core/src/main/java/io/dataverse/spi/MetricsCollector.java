package io.dataverse.spi;

import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Service Provider Interface for metrics collection.
 *
 * <p>MetricsCollector enables pluggable metrics and monitoring solutions.
 * Implementations can integrate with various monitoring systems.
 *
 * <p><strong>Example Implementations:</strong>
 * <ul>
 *   <li>Micrometer</li>
 *   <li>Prometheus</li>
 *   <li>CloudWatch</li>
 *   <li>Custom metrics systems</li>
 * </ul>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface MetricsCollector {

  /**
   * Increments a counter metric by 1.
   *
   * @param name the metric name, must not be {@code null}
   * @param tags optional tags for the metric
   */
  void incrementCounter(String name, String... tags);

  /**
   * Increments a counter metric by the specified amount.
   *
   * @param name the metric name, must not be {@code null}
   * @param amount the amount to increment by
   * @param tags optional tags for the metric
   */
  void incrementCounter(String name, long amount, String... tags);

  /**
   * Records a gauge metric value.
   *
   * @param name the metric name, must not be {@code null}
   * @param value the gauge value
   * @param tags optional tags for the metric
   */
  void recordGauge(String name, double value, String... tags);

  /**
   * Registers a gauge metric with a value supplier.
   *
   * @param name the metric name, must not be {@code null}
   * @param valueSupplier the supplier that provides the gauge value
   * @param tags optional tags for the metric
   */
  void registerGauge(String name, Supplier<Number> valueSupplier, String... tags);

  /**
   * Records a timer metric value.
   *
   * @param name the metric name, must not be {@code null}
   * @param duration the duration to record
   * @param tags optional tags for the metric
   */
  void recordTimer(String name, Duration duration, String... tags);

  /**
   * Records a distribution summary value.
   *
   * @param name the metric name, must not be {@code null}
   * @param value the value to record
   * @param tags optional tags for the metric
   */
  void recordDistribution(String name, double value, String... tags);

  /**
   * Starts timing an operation.
   *
   * @return a Timer instance for recording the elapsed time
   */
  Timer startTimer();

  /**
   * Times a code block execution.
   *
   * @param name the metric name, must not be {@code null}
   * @param operation the operation to time
   * @param tags optional tags for the metric
   * @param <T> the return type of the operation
   * @return the result of the operation
   */
  <T> T time(String name, Supplier<T> operation, String... tags);

  /**
   * Executes a code block and records timing.
   *
   * @param name the metric name, must not be {@code null}
   * @param operation the operation to time
   * @param tags optional tags for the metric
   */
  void time(String name, Runnable operation, String... tags);

  /**
   * Creates tags from key-value pairs.
   *
   * @param keyValues alternating key-value pairs
   * @return an array of formatted tags
   */
  static String[] tags(String... keyValues) {
    if (keyValues.length % 2 != 0) {
      throw new IllegalArgumentException("Key-value pairs must be even");
    }
    return keyValues;
  }

  /**
   * Creates tags from a map.
   *
   * @param tagMap the tag map
   * @return an array of formatted tags
   */
  static String[] tags(Map<String, String> tagMap) {
    String[] tags = new String[tagMap.size() * 2];
    int i = 0;
    for (Map.Entry<String, String> entry : tagMap.entrySet()) {
      tags[i++] = entry.getKey();
      tags[i++] = entry.getValue();
    }
    return tags;
  }

  /**
   * Timer interface for recording elapsed time.
   */
  interface Timer {
    /**
     * Stops the timer and records the duration.
     *
     * @param name the metric name, must not be {@code null}
     * @param tags optional tags for the metric
     */
    void stop(String name, String... tags);

    /**
     * Gets the elapsed time since the timer started.
     *
     * @return the elapsed duration
     */
    Duration elapsed();
  }
}
