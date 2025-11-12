package io.dataverse.api;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Fluent API for building type-safe queries.
 *
 * <p>QueryBuilder provides a chainable interface for constructing complex queries without
 * writing data source-specific query languages. All queries are translated to the appropriate
 * native format by the underlying adapter.
 *
 * <p><strong>Example Usage:</strong>
 * <pre>{@code
 * List<User> users = repository.query()
 *     .where("status").eq("ACTIVE")
 *     .and("age").greaterThan(18)
 *     .and("email").endsWith("@company.com")
 *     .orderBy("lastName").ascending()
 *     .orderBy("firstName").ascending()
 *     .limit(100)
 *     .execute();
 *
 * // With pagination
 * Page<User> page = repository.query()
 *     .where("department").eq("Engineering")
 *     .page(0, 20)
 *     .execute();
 *
 * // Aggregations
 * long count = repository.query()
 *     .where("status").eq("ACTIVE")
 *     .count();
 * }</pre>
 *
 * @param <T> the entity type
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface QueryBuilder<T> {

  // Where clause builders

  /**
   * Starts a new where clause for the specified field.
   *
   * @param fieldName the field name, must not be {@code null}
   * @return a condition builder for the field
   * @throws IllegalArgumentException if fieldName is {@code null} or empty
   */
  ConditionBuilder<T> where(String fieldName);

  /**
   * Adds an AND condition for the specified field.
   *
   * @param fieldName the field name, must not be {@code null}
   * @return a condition builder for the field
   * @throws IllegalArgumentException if fieldName is {@code null} or empty
   */
  ConditionBuilder<T> and(String fieldName);

  /**
   * Adds an OR condition for the specified field.
   *
   * @param fieldName the field name, must not be {@code null}
   * @return a condition builder for the field
   * @throws IllegalArgumentException if fieldName is {@code null} or empty
   */
  ConditionBuilder<T> or(String fieldName);

  // Sorting

  /**
   * Specifies a field to order by.
   *
   * @param fieldName the field name, must not be {@code null}
   * @return an order builder for the field
   * @throws IllegalArgumentException if fieldName is {@code null} or empty
   */
  OrderBuilder<T> orderBy(String fieldName);

  // Pagination

  /**
   * Limits the number of results.
   *
   * @param limit the maximum number of results, must be positive
   * @return this query builder
   * @throws IllegalArgumentException if limit is not positive
   */
  QueryBuilder<T> limit(int limit);

  /**
   * Skips the specified number of results.
   *
   * @param offset the number of results to skip, must not be negative
   * @return this query builder
   * @throws IllegalArgumentException if offset is negative
   */
  QueryBuilder<T> offset(int offset);

  /**
   * Configures page-based pagination.
   *
   * @param pageNumber the page number (zero-based), must not be negative
   * @param pageSize the page size, must be positive
   * @return this query builder
   * @throws IllegalArgumentException if pageNumber is negative or pageSize is not positive
   */
  QueryBuilder<T> page(int pageNumber, int pageSize);

  // Projection

  /**
   * Specifies which fields to include in the results.
   *
   * @param fieldNames the field names to include, must not be {@code null}
   * @return this query builder
   * @throws IllegalArgumentException if fieldNames is {@code null} or empty
   */
  QueryBuilder<T> select(String... fieldNames);

  /**
   * Specifies fields to exclude from the results.
   *
   * @param fieldNames the field names to exclude, must not be {@code null}
   * @return this query builder
   * @throws IllegalArgumentException if fieldNames is {@code null} or empty
   */
  QueryBuilder<T> exclude(String... fieldNames);

  // Execution

  /**
   * Executes the query and returns all matching results.
   *
   * @return list of matching entities, never {@code null}
   */
  List<T> execute();

  /**
   * Executes the query and returns the first result if any.
   *
   * @return an Optional containing the first result, or empty if no results
   */
  Optional<T> executeFirst();

  /**
   * Executes the query and returns a single result.
   *
   * @return an Optional containing the single result, or empty if no results
   * @throws IllegalStateException if more than one result is found
   */
  Optional<T> executeSingle();

  /**
   * Asynchronously executes the query using virtual threads.
   *
   * @return a CompletableFuture that will complete with the query results
   */
  CompletableFuture<List<T>> executeAsync();

  // Aggregations

  /**
   * Counts the number of entities matching the query criteria.
   *
   * @return the count of matching entities
   */
  long count();

  /**
   * Checks if any entities match the query criteria.
   *
   * @return {@code true} if at least one entity matches, {@code false} otherwise
   */
  boolean exists();

  // Advanced

  /**
   * Enables distinct results (removes duplicates).
   *
   * @return this query builder
   */
  QueryBuilder<T> distinct();

  /**
   * Sets a hint for the underlying data source query optimizer.
   *
   * <p>Hints are data source-specific and may be ignored if not supported.
   *
   * @param hintName the hint name
   * @param hintValue the hint value
   * @return this query builder
   */
  QueryBuilder<T> hint(String hintName, Object hintValue);

  /**
   * Condition builder for creating field-level predicates.
   *
   * @param <T> the entity type
   */
  interface ConditionBuilder<T> {

    /**
     * Equals condition (field = value).
     *
     * @param value the value to compare
     * @return the query builder for chaining
     */
    QueryBuilder<T> eq(Object value);

    /**
     * Not equals condition (field != value).
     *
     * @param value the value to compare
     * @return the query builder for chaining
     */
    QueryBuilder<T> notEq(Object value);

    /**
     * Greater than condition (field &gt; value).
     *
     * @param value the value to compare
     * @return the query builder for chaining
     */
    QueryBuilder<T> greaterThan(Object value);

    /**
     * Greater than or equal condition (field &gt;= value).
     *
     * @param value the value to compare
     * @return the query builder for chaining
     */
    QueryBuilder<T> greaterThanOrEq(Object value);

    /**
     * Less than condition (field &lt; value).
     *
     * @param value the value to compare
     * @return the query builder for chaining
     */
    QueryBuilder<T> lessThan(Object value);

    /**
     * Less than or equal condition (field &lt;= value).
     *
     * @param value the value to compare
     * @return the query builder for chaining
     */
    QueryBuilder<T> lessThanOrEq(Object value);

    /**
     * IN condition (field IN values).
     *
     * @param values the values to match
     * @return the query builder for chaining
     */
    QueryBuilder<T> in(Object... values);

    /**
     * NOT IN condition (field NOT IN values).
     *
     * @param values the values to exclude
     * @return the query builder for chaining
     */
    QueryBuilder<T> notIn(Object... values);

    /**
     * LIKE condition (field LIKE pattern).
     *
     * <p>Pattern syntax: {@code %} matches any sequence, {@code _} matches any single character.
     *
     * @param pattern the pattern to match
     * @return the query builder for chaining
     */
    QueryBuilder<T> like(String pattern);

    /**
     * Starts with condition (field starts with prefix).
     *
     * @param prefix the prefix to match
     * @return the query builder for chaining
     */
    QueryBuilder<T> startsWith(String prefix);

    /**
     * Ends with condition (field ends with suffix).
     *
     * @param suffix the suffix to match
     * @return the query builder for chaining
     */
    QueryBuilder<T> endsWith(String suffix);

    /**
     * Contains condition (field contains substring).
     *
     * @param substring the substring to match
     * @return the query builder for chaining
     */
    QueryBuilder<T> contains(String substring);

    /**
     * Is null condition (field IS NULL).
     *
     * @return the query builder for chaining
     */
    QueryBuilder<T> isNull();

    /**
     * Is not null condition (field IS NOT NULL).
     *
     * @return the query builder for chaining
     */
    QueryBuilder<T> isNotNull();

    /**
     * Is true condition (field = true) for boolean fields.
     *
     * @return the query builder for chaining
     */
    QueryBuilder<T> isTrue();

    /**
     * Is false condition (field = false) for boolean fields.
     *
     * @return the query builder for chaining
     */
    QueryBuilder<T> isFalse();

    /**
     * Between condition (field BETWEEN start AND end).
     *
     * @param start the start value (inclusive)
     * @param end the end value (inclusive)
     * @return the query builder for chaining
     */
    QueryBuilder<T> between(Object start, Object end);
  }

  /**
   * Order builder for creating sort specifications.
   *
   * @param <T> the entity type
   */
  interface OrderBuilder<T> {

    /**
     * Orders in ascending order.
     *
     * @return the query builder for chaining
     */
    QueryBuilder<T> ascending();

    /**
     * Orders in descending order.
     *
     * @return the query builder for chaining
     */
    QueryBuilder<T> descending();
  }
}
