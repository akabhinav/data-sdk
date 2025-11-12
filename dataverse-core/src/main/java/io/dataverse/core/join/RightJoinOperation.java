package io.dataverse.core.join;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Right join operation using hash join algorithm.
 *
 * <p>Returns all right records, with matching left records where available.
 * If no match, an empty list is passed to the combiner.
 *
 * <p><strong>Algorithm:</strong>
 * <ol>
 *   <li>Build hash map from left dataset</li>
 *   <li>For each right record, lookup matching left records</li>
 *   <li>Always add result (with empty list if no match)</li>
 * </ol>
 *
 * <p><strong>Complexity:</strong>
 * <ul>
 *   <li>Time: O(L + R)</li>
 *   <li>Space: O(L)</li>
 * </ul>
 *
 * @param <L> left type
 * @param <R> right type
 * @param <K> key type
 * @param <T> result type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
class RightJoinOperation<L, R, K, T> implements JoinBuilder.JoinOperation<L, T> {

  private final List<L> leftData;
  private final List<R> rightData;
  private final Function<L, K> leftKeyExtractor;
  private final Function<R, K> rightKeyExtractor;
  private final BiFunction<List<L>, R, T> combiner;

  RightJoinOperation(
      List<L> leftData,
      List<R> rightData,
      Function<L, K> leftKeyExtractor,
      Function<R, K> rightKeyExtractor,
      BiFunction<List<L>, R, T> combiner) {
    this.leftData = leftData;
    this.rightData = rightData;
    this.leftKeyExtractor = leftKeyExtractor;
    this.rightKeyExtractor = rightKeyExtractor;
    this.combiner = combiner;
  }

  @Override
  public List<T> execute() {
    // Build hash map from left dataset
    Map<K, List<L>> leftIndex = new HashMap<>();
    for (L left : leftData) {
      K key = leftKeyExtractor.apply(left);
      leftIndex.computeIfAbsent(key, k -> new ArrayList<>()).add(left);
    }

    // Join with right dataset
    List<T> results = new ArrayList<>();
    for (R right : rightData) {
      K key = rightKeyExtractor.apply(right);
      List<L> matchingLeft = leftIndex.getOrDefault(key, Collections.emptyList());

      // Always include right record (even if no match)
      T result = combiner.apply(matchingLeft, right);
      results.add(result);
    }

    return results;
  }
}
