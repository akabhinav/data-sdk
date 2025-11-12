package io.dataverse.core.loading;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Defines a plan for fetching related entities.
 *
 * <p>FetchPlan allows you to specify which relationships to load and how,
 * providing fine-grained control over query performance and data loading.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Build a fetch plan
 * FetchPlan plan = FetchPlan.builder()
 *     .eager("orders")
 *     .eager("orders.items")
 *     .lazy("profile")
 *     .batch("activities", 100)
 *     .build();
 *
 * // Apply to query
 * List<User> users = repository.query()
 *     .where("active").isTrue()
 *     .fetchPlan(plan)
 *     .execute();
 * }</pre>
 *
 * <p><strong>Predefined Plans:</strong>
 * <pre>{@code
 * // Load everything eagerly
 * FetchPlan allEager = FetchPlan.eagerAll();
 *
 * // Load everything lazily
 * FetchPlan allLazy = FetchPlan.lazyAll();
 *
 * // Default adapter behavior
 * FetchPlan defaults = FetchPlan.defaults();
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class FetchPlan {

  private final Map<String, FetchStrategy> strategies;
  private final Map<String, Integer> batchSizes;
  private final FetchStrategy defaultStrategy;

  private FetchPlan(Builder builder) {
    this.strategies = Map.copyOf(builder.strategies);
    this.batchSizes = Map.copyOf(builder.batchSizes);
    this.defaultStrategy = builder.defaultStrategy;
  }

  /**
   * Gets the fetch strategy for a field.
   *
   * @param fieldPath the field path
   * @return the fetch strategy, or default if not specified
   */
  public FetchStrategy getStrategy(String fieldPath) {
    return strategies.getOrDefault(fieldPath, defaultStrategy);
  }

  /**
   * Gets the batch size for a field.
   *
   * @param fieldPath the field path
   * @return the batch size, or 0 if not applicable
   */
  public int getBatchSize(String fieldPath) {
    return batchSizes.getOrDefault(fieldPath, 0);
  }

  /**
   * Gets all field paths with explicit strategies.
   *
   * @return set of field paths
   */
  public Set<String> getFieldPaths() {
    return strategies.keySet();
  }

  /**
   * Gets the default strategy for unspecified fields.
   *
   * @return the default strategy
   */
  public FetchStrategy getDefaultStrategy() {
    return defaultStrategy;
  }

  /**
   * Creates a builder for FetchPlan.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a plan that loads everything eagerly.
   *
   * @return eager fetch plan
   */
  public static FetchPlan eagerAll() {
    return builder().defaultStrategy(FetchStrategy.EAGER).build();
  }

  /**
   * Creates a plan that loads everything lazily.
   *
   * @return lazy fetch plan
   */
  public static FetchPlan lazyAll() {
    return builder().defaultStrategy(FetchStrategy.LAZY).build();
  }

  /**
   * Creates a plan using adapter defaults.
   *
   * @return default fetch plan
   */
  public static FetchPlan defaults() {
    return builder().defaultStrategy(FetchStrategy.DEFAULT).build();
  }

  /**
   * Builder for FetchPlan.
   */
  public static class Builder {
    private final Map<String, FetchStrategy> strategies = new HashMap<>();
    private final Map<String, Integer> batchSizes = new HashMap<>();
    private FetchStrategy defaultStrategy = FetchStrategy.DEFAULT;

    /**
     * Sets a field to be loaded eagerly.
     *
     * @param fieldPath the field path
     * @return this builder
     */
    public Builder eager(String fieldPath) {
      strategies.put(fieldPath, FetchStrategy.EAGER);
      return this;
    }

    /**
     * Sets multiple fields to be loaded eagerly.
     *
     * @param fieldPaths the field paths
     * @return this builder
     */
    public Builder eager(String... fieldPaths) {
      for (String fieldPath : fieldPaths) {
        eager(fieldPath);
      }
      return this;
    }

    /**
     * Sets a field to be loaded lazily.
     *
     * @param fieldPath the field path
     * @return this builder
     */
    public Builder lazy(String fieldPath) {
      strategies.put(fieldPath, FetchStrategy.LAZY);
      return this;
    }

    /**
     * Sets multiple fields to be loaded lazily.
     *
     * @param fieldPaths the field paths
     * @return this builder
     */
    public Builder lazy(String... fieldPaths) {
      for (String fieldPath : fieldPaths) {
        lazy(fieldPath);
      }
      return this;
    }

    /**
     * Sets a field to be loaded in batches.
     *
     * @param fieldPath the field path
     * @param batchSize the batch size
     * @return this builder
     */
    public Builder batch(String fieldPath, int batchSize) {
      strategies.put(fieldPath, FetchStrategy.BATCH);
      batchSizes.put(fieldPath, batchSize);
      return this;
    }

    /**
     * Sets a custom strategy for a field.
     *
     * @param fieldPath the field path
     * @param strategy the fetch strategy
     * @return this builder
     */
    public Builder fetch(String fieldPath, FetchStrategy strategy) {
      strategies.put(fieldPath, strategy);
      return this;
    }

    /**
     * Sets the default strategy for unspecified fields.
     *
     * @param defaultStrategy the default strategy
     * @return this builder
     */
    public Builder defaultStrategy(FetchStrategy defaultStrategy) {
      this.defaultStrategy = defaultStrategy;
      return this;
    }

    /**
     * Builds the fetch plan.
     *
     * @return the fetch plan
     */
    public FetchPlan build() {
      return new FetchPlan(this);
    }
  }
}
