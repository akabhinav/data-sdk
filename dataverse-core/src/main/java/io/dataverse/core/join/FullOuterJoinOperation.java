package io.dataverse.core.join;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Full outer join operation using hash join algorithm.
 *
 * <p>Returns all records from both datasets. When records match, they're combined.
 * Unmatched left records get empty right list, unmatched right records get empty left list.
 *
 * <p><strong>Algorithm:</strong>
 * <ol>
 *   <li>Build hash maps from both datasets</li>
 *   <li>Process all left records (with or without matches)</li>
 *   <li>Process unmatched right records</li>
 * </ol>
 *
 * <p><strong>Complexity:</strong>
 * <ul>
 *   <li>Time: O(L + R)</li>
 *   <li>Space: O(L + R)</li>
 * </ul>
 *
 * @param <L> left type
 * @param <R> right type
 * @param <K> key type
 * @param <T> result type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
class FullOuterJoinOperation<L, R, K, T> implements JoinBuilder.JoinOperation<L, T> {

  private final List<L> leftData;
  private final List<R> rightData;
  private final Function<L, K> leftKeyExtractor;
  private final Function<R, K> rightKeyExtractor;
  private final BiFunction<L, List<R>, T> leftCombiner;
  private final BiFunction<List<L>, R, T> rightCombiner;

  FullOuterJoinOperation(
      List<L> leftData,
      List<R> rightData,
      Function<L, K> leftKeyExtractor,
      Function<R, K> rightKeyExtractor,
      BiFunction<L, List<R>, T> leftCombiner,
      BiFunction<List<L>, R, T> rightCombiner) {
    this.leftData = leftData;
    this.rightData = rightData;
    this.leftKeyExtractor = leftKeyExtractor;
    this.rightKeyExtractor = rightKeyExtractor;
    this.leftCombiner = leftCombiner;
    this.rightCombiner = rightCombiner;
  }

  @Override
  public List<T> execute() {
    // Build hash map from right dataset
    Map<K, List<R>> rightIndex = new HashMap<>();
    for (R right : rightData) {
      K key = rightKeyExtractor.apply(right);
      rightIndex.computeIfAbsent(key, k -> new ArrayList<>()).add(right);
    }

    List<T> results = new ArrayList<>();
    Set<K> matchedKeys = new HashSet<>();

    // Process all left records
    for (L left : leftData) {
      K key = leftKeyExtractor.apply(left);
      List<R> matchingRight = rightIndex.getOrDefault(key, Collections.emptyList());

      if (!matchingRight.isEmpty()) {
        matchedKeys.add(key);
      }

      T result = leftCombiner.apply(left, matchingRight);
      results.add(result);
    }

    // Process unmatched right records
    for (Map.Entry<K, List<R>> entry : rightIndex.entrySet()) {
      K key = entry.getKey();

      // Skip if already matched with left
      if (matchedKeys.contains(key)) {
        continue;
      }

      // Add unmatched right records
      for (R right : entry.getValue()) {
        T result = rightCombiner.apply(Collections.emptyList(), right);
        results.add(result);
      }
    }

    return results;
  }
}
