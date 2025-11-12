package io.dataverse.core.join;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Inner join operation using hash join algorithm.
 *
 * <p>Returns only records that have matching keys in both datasets.
 *
 * <p><strong>Algorithm:</strong>
 * <ol>
 *   <li>Build hash map from right dataset (key → list of records)</li>
 *   <li>For each left record, lookup matching right records by key</li>
 *   <li>If match found, combine and add to results</li>
 * </ol>
 *
 * <p><strong>Complexity:</strong>
 * <ul>
 *   <li>Time: O(L + R) where L = left size, R = right size</li>
 *   <li>Space: O(R) for hash map</li>
 * </ul>
 *
 * @param <L> left type
 * @param <R> right type
 * @param <K> key type
 * @param <T> result type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
class InnerJoinOperation<L, R, K, T> implements JoinBuilder.JoinOperation<L, T> {

  private final List<L> leftData;
  private final List<R> rightData;
  private final Function<L, K> leftKeyExtractor;
  private final Function<R, K> rightKeyExtractor;
  private final BiFunction<L, List<R>, T> combiner;

  InnerJoinOperation(
      List<L> leftData,
      List<R> rightData,
      Function<L, K> leftKeyExtractor,
      Function<R, K> rightKeyExtractor,
      BiFunction<L, List<R>, T> combiner) {
    this.leftData = leftData;
    this.rightData = rightData;
    this.leftKeyExtractor = leftKeyExtractor;
    this.rightKeyExtractor = rightKeyExtractor;
    this.combiner = combiner;
  }

  @Override
  public List<T> execute() {
    // Build hash map from right dataset
    Map<K, List<R>> rightIndex = new HashMap<>();
    for (R right : rightData) {
      K key = rightKeyExtractor.apply(right);
      rightIndex.computeIfAbsent(key, k -> new ArrayList<>()).add(right);
    }

    // Join with left dataset
    List<T> results = new ArrayList<>();
    for (L left : leftData) {
      K key = leftKeyExtractor.apply(left);
      List<R> matchingRight = rightIndex.get(key);

      // Only include if match found (inner join)
      if (matchingRight != null && !matchingRight.isEmpty()) {
        T result = combiner.apply(left, matchingRight);
        results.add(result);
      }
    }

    return results;
  }
}
