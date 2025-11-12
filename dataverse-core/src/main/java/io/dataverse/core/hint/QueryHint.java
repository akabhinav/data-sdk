package io.dataverse.core.hint;

/**
 * Query hints for database query optimization.
 *
 * <p>Query hints provide database-specific optimization instructions that can
 * significantly improve query performance. Different databases support different
 * hints, so use them carefully and test thoroughly.
 *
 * <p><strong>Common Hint Types:</strong>
 * <ul>
 *   <li><strong>Index Hints</strong> - Force use of specific index</li>
 *   <li><strong>Join Hints</strong> - Control join algorithm (nested loop, hash, merge)</li>
 *   <li><strong>Scan Hints</strong> - Force index scan vs table scan</li>
 *   <li><strong>Parallel Hints</strong> - Enable/disable parallel execution</li>
 *   <li><strong>Timeout Hints</strong> - Set query timeout</li>
 * </ul>
 *
 * <p><strong>Database-Specific Examples:</strong>
 *
 * <p><strong>PostgreSQL:</strong>
 * <pre>{@code
 * // Force index usage
 * repository.query()
 *     .hint(QueryHint.USE_INDEX, "idx_user_email")
 *     .where("email").equals("john@example.com")
 *     .execute();
 *
 * // Disable sequential scan
 * repository.query()
 *     .hint(QueryHint.NO_SEQ_SCAN)
 *     .execute();
 * }</pre>
 *
 * <p><strong>MongoDB:</strong>
 * <pre>{@code
 * // Specify index
 * repository.query()
 *     .hint(QueryHint.INDEX, "{ email: 1 }")
 *     .where("email").equals("john@example.com")
 *     .execute();
 *
 * // Max time for query
 * repository.query()
 *     .hint(QueryHint.MAX_TIME_MS, "5000")
 *     .execute();
 * }</pre>
 *
 * <p><strong>MySQL:</strong>
 * <pre>{@code
 * // Force index
 * repository.query()
 *     .hint(QueryHint.FORCE_INDEX, "idx_user_email")
 *     .where("email").equals("john@example.com")
 *     .execute();
 *
 * // Use index
 * repository.query()
 *     .hint(QueryHint.USE_INDEX, "idx_created_at")
 *     .orderBy("createdAt").descending()
 *     .execute();
 * }</pre>
 *
 * <p><strong>When to Use Hints:</strong>
 * <ul>
 *   <li>Query planner chooses suboptimal execution plan</li>
 *   <li>Need consistent query performance (prevent plan changes)</li>
 *   <li>Complex queries where automatic optimization fails</li>
 *   <li>Specific performance requirements (e.g., <100ms)</li>
 * </ul>
 *
 * <p><strong>Best Practices:</strong>
 * <ul>
 *   <li>Use hints sparingly - let optimizer do its job most of the time</li>
 *   <li>Always measure performance before and after</li>
 *   <li>Document why each hint is needed</li>
 *   <li>Review hints periodically as data grows</li>
 *   <li>Test hints in production-like environments</li>
 * </ul>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class QueryHint {

  private final String name;
  private final String value;

  public QueryHint(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public QueryHint(String name) {
    this(name, null);
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  // Common hints across databases

  /**
   * Force use of specific index.
   * <p>PostgreSQL: USING INDEX, MySQL: FORCE INDEX, MongoDB: hint()
   */
  public static final String USE_INDEX = "USE_INDEX";

  /**
   * Force use of specific index (stronger than USE_INDEX).
   * <p>MySQL: FORCE INDEX
   */
  public static final String FORCE_INDEX = "FORCE_INDEX";

  /**
   * Suggest index to use (weaker than USE_INDEX).
   * <p>PostgreSQL: may use index, MySQL: USE INDEX
   */
  public static final String INDEX_HINT = "INDEX_HINT";

  /**
   * Disable sequential scan (force index scan).
   * <p>PostgreSQL: enable_seqscan = off
   */
  public static final String NO_SEQ_SCAN = "NO_SEQ_SCAN";

  /**
   * Enable parallel query execution.
   * <p>PostgreSQL: max_parallel_workers_per_gather
   */
  public static final String PARALLEL = "PARALLEL";

  /**
   * Disable parallel query execution.
   * <p>PostgreSQL: max_parallel_workers_per_gather = 0
   */
  public static final String NO_PARALLEL = "NO_PARALLEL";

  /**
   * Set maximum execution time in milliseconds.
   * <p>MongoDB: maxTimeMS, PostgreSQL: statement_timeout
   */
  public static final String MAX_TIME_MS = "MAX_TIME_MS";

  /**
   * Force nested loop join.
   * <p>PostgreSQL: enable_nestloop = on
   */
  public static final String NESTED_LOOP = "NESTED_LOOP";

  /**
   * Force hash join.
   * <p>PostgreSQL: enable_hashjoin = on
   */
  public static final String HASH_JOIN = "HASH_JOIN";

  /**
   * Force merge join.
   * <p>PostgreSQL: enable_mergejoin = on
   */
  public static final String MERGE_JOIN = "MERGE_JOIN";

  /**
   * Read from primary/master only (no replicas).
   * <p>MongoDB: readPreference primary
   */
  public static final String READ_PRIMARY = "READ_PRIMARY";

  /**
   * Allow reading from replicas.
   * <p>MongoDB: readPreference secondaryPreferred
   */
  public static final String READ_SECONDARY = "READ_SECONDARY";

  /**
   * Index to use (MongoDB-specific).
   * <p>MongoDB: hint({ field: 1 })
   */
  public static final String INDEX = "INDEX";

  /**
   * Explain query execution plan.
   * <p>Most databases: EXPLAIN
   */
  public static final String EXPLAIN = "EXPLAIN";

  /**
   * Create a query hint.
   *
   * @param name the hint name
   * @param value the hint value
   * @return a new query hint
   */
  public static QueryHint hint(String name, String value) {
    return new QueryHint(name, value);
  }

  /**
   * Create a query hint without a value.
   *
   * @param name the hint name
   * @return a new query hint
   */
  public static QueryHint hint(String name) {
    return new QueryHint(name);
  }

  @Override
  public String toString() {
    return value != null ? name + "=" + value : name;
  }
}
