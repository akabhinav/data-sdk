package io.dataverse.api;

import java.util.List;
import java.util.Map;

/**
 * Fluent API for building aggregation queries.
 *
 * <p>AggregationBuilder provides a type-safe way to construct complex aggregations
 * including grouping, counting, summing, averaging, and finding min/max values.
 *
 * <p><strong>Example Usage:</strong>
 * <pre>{@code
 * // Count by status
 * Map<String, Long> countsByStatus = repository.aggregate()
 *     .groupBy("status")
 *     .count()
 *     .execute();
 *
 * // Average salary by department
 * Map<String, Double> avgSalaryByDept = repository.aggregate()
 *     .where("active").isTrue()
 *     .groupBy("department")
 *     .avg("salary")
 *     .execute();
 *
 * // Multiple aggregations
 * AggregationResult result = repository.aggregate()
 *     .groupBy("category")
 *     .count()
 *     .sum("price")
 *     .avg("rating")
 *     .min("price")
 *     .max("price")
 *     .executeDetailed();
 * }</pre>
 *
 * @param <T> the entity type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface AggregationBuilder<T> {

  /**
   * Groups results by the specified field.
   *
   * @param fieldName the field to group by
   * @return this aggregation builder
   */
  AggregationBuilder<T> groupBy(String... fieldName);

  /**
   * Adds a filter condition using the query builder.
   *
   * @param fieldName the field to filter on
   * @return a condition builder
   */
  QueryBuilder.ConditionBuilder<T> where(String fieldName);

  /**
   * Counts the number of items in each group.
   *
   * @return this aggregation builder
   */
  AggregationBuilder<T> count();

  /**
   * Sums the values of the specified field.
   *
   * @param fieldName the field to sum
   * @return this aggregation builder
   */
  AggregationBuilder<T> sum(String fieldName);

  /**
   * Calculates the average of the specified field.
   *
   * @param fieldName the field to average
   * @return this aggregation builder
   */
  AggregationBuilder<T> avg(String fieldName);

  /**
   * Finds the minimum value of the specified field.
   *
   * @param fieldName the field to find minimum
   * @return this aggregation builder
   */
  AggregationBuilder<T> min(String fieldName);

  /**
   * Finds the maximum value of the specified field.
   *
   * @param fieldName the field to find maximum
   * @return this aggregation builder
   */
  AggregationBuilder<T> max(String fieldName);

  /**
   * Executes the aggregation and returns results as a map.
   *
   * <p>For simple aggregations (single operation, single group), returns:
   * <pre>{@code Map<GroupValue, AggregationValue>}</pre>
   *
   * @return aggregation results
   */
  <K, V> Map<K, V> execute();

  /**
   * Executes the aggregation and returns detailed results.
   *
   * <p>For complex aggregations with multiple operations, returns a structured result.
   *
   * @return detailed aggregation result
   */
  AggregationResult executeDetailed();

  /**
   * Executes a simple count aggregation without grouping.
   *
   * @return the total count
   */
  long executeCount();

  /**
   * Executes a simple sum aggregation without grouping.
   *
   * @return the sum
   */
  double executeSum();

  /**
   * Executes a simple average aggregation without grouping.
   *
   * @return the average
   */
  double executeAvg();

  /**
   * Executes a simple min aggregation without grouping.
   *
   * @return the minimum value
   */
  <V> V executeMin();

  /**
   * Executes a simple max aggregation without grouping.
   *
   * @return the maximum value
   */
  <V> V executeMax();

  /**
   * Result of a complex aggregation operation.
   */
  interface AggregationResult {

    /**
     * Returns the grouping values.
     *
     * @return list of group values
     */
    List<Map<String, Object>> getGroups();

    /**
     * Returns the count for a specific group.
     *
     * @param groupValue the group value
     * @return the count, or 0 if not found
     */
    long getCount(Map<String, Object> groupValue);

    /**
     * Returns the sum for a specific group and field.
     *
     * @param groupValue the group value
     * @param fieldName the field name
     * @return the sum, or 0.0 if not found
     */
    double getSum(Map<String, Object> groupValue, String fieldName);

    /**
     * Returns the average for a specific group and field.
     *
     * @param groupValue the group value
     * @param fieldName the field name
     * @return the average, or 0.0 if not found
     */
    double getAvg(Map<String, Object> groupValue, String fieldName);

    /**
     * Returns the minimum for a specific group and field.
     *
     * @param groupValue the group value
     * @param fieldName the field name
     * @return the minimum value, or null if not found
     */
    <V> V getMin(Map<String, Object> groupValue, String fieldName);

    /**
     * Returns the maximum for a specific group and field.
     *
     * @param groupValue the group value
     * @param fieldName the field name
     * @return the maximum value, or null if not found
     */
    <V> V getMax(Map<String, Object> groupValue, String fieldName);

    /**
     * Returns all aggregation data for a specific group.
     *
     * @param groupValue the group value
     * @return map of aggregation type to values
     */
    Map<String, Object> getGroupData(Map<String, Object> groupValue);

    /**
     * Returns the total count across all groups.
     *
     * @return total count
     */
    long getTotalCount();
  }
}
