package io.dataverse.core.join;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Builder for cross-datasource join operations.
 *
 * <p>Enables joining data from different databases (e.g., PostgreSQL + MongoDB)
 * using in-memory processing with optimized algorithms.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Join PostgreSQL users with MongoDB orders
 * List<UserWithOrders> results = JoinBuilder
 *     .from(userRepository.findAll())  // PostgreSQL
 *     .innerJoin(
 *         orderRepository.findAll(),    // MongoDB
 *         User::getId,                  // Left key
 *         Order::getUserId,             // Right key
 *         (user, orders) -> new UserWithOrders(user, orders)
 *     )
 *     .execute();
 *
 * // Left join with filtering
 * List<UserOrderSummary> summaries = JoinBuilder
 *     .from(activeUsers)
 *     .leftJoin(
 *         recentOrders,
 *         User::getId,
 *         Order::getUserId,
 *         (user, orders) -> new UserOrderSummary(
 *             user.getUsername(),
 *             orders.size(),
 *             orders.stream().mapToDouble(Order::getTotal).sum()
 *         )
 *     )
 *     .where(summary -> summary.getTotalAmount() > 1000)
 *     .execute();
 * }</pre>
 *
 * <p><strong>Join Types:</strong>
 * <ul>
 *   <li><strong>INNER JOIN</strong> - Only matching records from both sides</li>
 *   <li><strong>LEFT JOIN</strong> - All left records + matching right records</li>
 *   <li><strong>RIGHT JOIN</strong> - All right records + matching left records</li>
 *   <li><strong>FULL OUTER JOIN</strong> - All records from both sides</li>
 * </ul>
 *
 * <p><strong>Performance Considerations:</strong>
 * <ul>
 *   <li>Use indexed lookups when possible (hash join algorithm)</li>
 *   <li>Filter data before joining to reduce memory</li>
 *   <li>Consider pagination for large result sets</li>
 *   <li>Use streaming for processing large datasets</li>
 * </ul>
 *
 * @param <T> the result type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class JoinBuilder<T> {

  private final List<JoinOperation<?, ?>> operations = new ArrayList<>();
  private final List<Function<T, Boolean>> filters = new ArrayList<>();
  private Integer limit;
  private Integer offset;

  private JoinBuilder() {
  }

  /**
   * Starts a join from a left dataset.
   *
   * @param <L> the left type
   * @param leftData the left dataset
   * @return join builder
   */
  public static <L> LeftJoinBuilder<L> from(List<L> leftData) {
    return new LeftJoinBuilder<>(leftData);
  }

  /**
   * Adds a filter to the results.
   *
   * @param filter the filter predicate
   * @return this builder
   */
  public JoinBuilder<T> where(Function<T, Boolean> filter) {
    filters.add(filter);
    return this;
  }

  /**
   * Sets the maximum number of results.
   *
   * @param limit the limit
   * @return this builder
   */
  public JoinBuilder<T> limit(int limit) {
    this.limit = limit;
    return this;
  }

  /**
   * Sets the offset for results.
   *
   * @param offset the offset
   * @return this builder
   */
  public JoinBuilder<T> offset(int offset) {
    this.offset = offset;
    return this;
  }

  /**
   * Builder for left-side of join.
   *
   * @param <L> the left type
   */
  public static class LeftJoinBuilder<L> {
    private final List<L> leftData;

    private LeftJoinBuilder(List<L> leftData) {
      this.leftData = leftData;
    }

    /**
     * Performs an inner join with the right dataset.
     *
     * @param <R> the right type
     * @param <K> the key type
     * @param <T> the result type
     * @param rightData the right dataset
     * @param leftKeyExtractor extracts join key from left
     * @param rightKeyExtractor extracts join key from right
     * @param combiner combines matching left and right records
     * @return join executor
     */
    public <R, K, T> JoinExecutor<T> innerJoin(
        List<R> rightData,
        Function<L, K> leftKeyExtractor,
        Function<R, K> rightKeyExtractor,
        BiFunction<L, List<R>, T> combiner) {

      return new JoinExecutor<>(
          new InnerJoinOperation<>(
              leftData,
              rightData,
              leftKeyExtractor,
              rightKeyExtractor,
              combiner
          )
      );
    }

    /**
     * Performs a left join with the right dataset.
     *
     * <p>Returns all left records, with matching right records where available.
     * If no match, right parameter to combiner will be empty list.
     *
     * @param <R> the right type
     * @param <K> the key type
     * @param <T> the result type
     * @param rightData the right dataset
     * @param leftKeyExtractor extracts join key from left
     * @param rightKeyExtractor extracts join key from right
     * @param combiner combines left and matching right records
     * @return join executor
     */
    public <R, K, T> JoinExecutor<T> leftJoin(
        List<R> rightData,
        Function<L, K> leftKeyExtractor,
        Function<R, K> rightKeyExtractor,
        BiFunction<L, List<R>, T> combiner) {

      return new JoinExecutor<>(
          new LeftJoinOperation<>(
              leftData,
              rightData,
              leftKeyExtractor,
              rightKeyExtractor,
              combiner
          )
      );
    }

    /**
     * Performs a right join with the right dataset.
     *
     * @param <R> the right type
     * @param <K> the key type
     * @param <T> the result type
     * @param rightData the right dataset
     * @param leftKeyExtractor extracts join key from left
     * @param rightKeyExtractor extracts join key from right
     * @param combiner combines matching left and right records
     * @return join executor
     */
    public <R, K, T> JoinExecutor<T> rightJoin(
        List<R> rightData,
        Function<L, K> leftKeyExtractor,
        Function<R, K> rightKeyExtractor,
        BiFunction<List<L>, R, T> combiner) {

      return new JoinExecutor<>(
          new RightJoinOperation<>(
              leftData,
              rightData,
              leftKeyExtractor,
              rightKeyExtractor,
              combiner
          )
      );
    }

    /**
     * Performs a full outer join with the right dataset.
     *
     * @param <R> the right type
     * @param <K> the key type
     * @param <T> the result type
     * @param rightData the right dataset
     * @param leftKeyExtractor extracts join key from left
     * @param rightKeyExtractor extracts join key from right
     * @param leftCombiner combines left with matching right
     * @param rightCombiner combines right with matching left
     * @return join executor
     */
    public <R, K, T> JoinExecutor<T> fullOuterJoin(
        List<R> rightData,
        Function<L, K> leftKeyExtractor,
        Function<R, K> rightKeyExtractor,
        BiFunction<L, List<R>, T> leftCombiner,
        BiFunction<List<L>, R, T> rightCombiner) {

      return new JoinExecutor<>(
          new FullOuterJoinOperation<>(
              leftData,
              rightData,
              leftKeyExtractor,
              rightKeyExtractor,
              leftCombiner,
              rightCombiner
          )
      );
    }
  }

  /**
   * Interface for join operations.
   */
  interface JoinOperation<L, T> {
    List<T> execute();
  }

  /**
   * Executor for join with filtering and pagination.
   *
   * @param <T> the result type
   */
  public static class JoinExecutor<T> {
    private final JoinOperation<?, T> operation;
    private final List<Function<T, Boolean>> filters = new ArrayList<>();
    private Integer limit;
    private Integer offset;

    private JoinExecutor(JoinOperation<?, T> operation) {
      this.operation = operation;
    }

    /**
     * Adds a filter to the results.
     *
     * @param filter the filter predicate
     * @return this executor
     */
    public JoinExecutor<T> where(Function<T, Boolean> filter) {
      filters.add(filter);
      return this;
    }

    /**
     * Sets the maximum number of results.
     *
     * @param limit the limit
     * @return this executor
     */
    public JoinExecutor<T> limit(int limit) {
      this.limit = limit;
      return this;
    }

    /**
     * Sets the offset for results.
     *
     * @param offset the offset
     * @return this executor
     */
    public JoinExecutor<T> offset(int offset) {
      this.offset = offset;
      return this;
    }

    /**
     * Executes the join and returns results.
     *
     * @return join results
     */
    public List<T> execute() {
      List<T> results = operation.execute();

      // Apply filters
      for (Function<T, Boolean> filter : filters) {
        results = results.stream()
            .filter(filter::apply)
            .toList();
      }

      // Apply offset
      if (offset != null && offset > 0) {
        results = results.stream()
            .skip(offset)
            .toList();
      }

      // Apply limit
      if (limit != null && limit > 0) {
        results = results.stream()
            .limit(limit)
            .toList();
      }

      return results;
    }
  }
}
