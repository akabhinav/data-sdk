package io.dataverse.core.join;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Left join operation using hash join algorithm.
 *
 * <p>Returns all left records, with matching right records where available.
 * If no match, an empty list is passed to the combiner.
 *
 * <p><strong>Algorithm:</strong>
 * <ol>
 *   <li>Build hash map from right dataset</li>
 *   <li>For each left record, lookup matching right records</li>
 *   <li>Always add result (with empty list if no match)</li>
 * </ol>
 *
 * <p><strong>Complexity:</strong>
 * <ul>
 *   <li>Time: O(L + R)</li>
 *   <li>Space: O(R)</li>
 * </ul>
 *
 * @param <L> left type
 * @param <R> right type
 * @param <K> key type
 * @param <T> result type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
class LeftJoinOperation<L, R, K, T> implements JoinBuilder.JoinOperation<L, T> {

  private final List<L> leftData;
  private final List<R> rightData;
  private final Function<L, K> leftKeyExtractor;
  private final Function<R, K> rightKeyExtractor;
  private final BiFunction<L, List<R>, T> combiner;

  LeftJoinOperation(
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
      List<R> matchingRight = rightIndex.getOrDefault(key, Collections.emptyList());

      // Always include left record (even if no match)
      T result = combiner.apply(left, matchingRight);
      results.add(result);
    }

    return results;
  }
}
