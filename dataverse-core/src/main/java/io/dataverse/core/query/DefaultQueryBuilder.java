package io.dataverse.core.query;

import io.dataverse.api.QueryBuilder;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Default implementation of QueryBuilder.
 *
 * <p>This implementation builds a Query object which can then be passed to a QueryTranslator
 * for conversion to a native query format.
 *
 * @param <T> the entity type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class DefaultQueryBuilder<T> implements QueryBuilder<T> {

  private final Query.Builder queryBuilder;
  private final Function<Query, List<T>> executor;
  private String currentField;
  private Condition.LogicalOperator nextLogicalOperator = Condition.LogicalOperator.AND;

  /**
   * Constructs a new query builder.
   *
   * @param executor the function to execute the query
   */
  public DefaultQueryBuilder(Function<Query, List<T>> executor) {
    this.queryBuilder = Query.builder();
    this.executor = executor;
  }

  @Override
  public ConditionBuilder<T> where(String fieldName) {
    validateFieldName(fieldName);
    this.currentField = fieldName;
    this.nextLogicalOperator = Condition.LogicalOperator.AND; // First condition
    return new DefaultConditionBuilder();
  }

  @Override
  public ConditionBuilder<T> and(String fieldName) {
    validateFieldName(fieldName);
    this.currentField = fieldName;
    this.nextLogicalOperator = Condition.LogicalOperator.AND;
    return new DefaultConditionBuilder();
  }

  @Override
  public ConditionBuilder<T> or(String fieldName) {
    validateFieldName(fieldName);
    this.currentField = fieldName;
    this.nextLogicalOperator = Condition.LogicalOperator.OR;
    return new DefaultConditionBuilder();
  }

  @Override
  public OrderBuilder<T> orderBy(String fieldName) {
    validateFieldName(fieldName);
    return new DefaultOrderBuilder(fieldName);
  }

  @Override
  public QueryBuilder<T> limit(int limit) {
    if (limit <= 0) {
      throw new IllegalArgumentException("Limit must be positive, got: " + limit);
    }
    queryBuilder.limit(limit);
    return this;
  }

  @Override
  public QueryBuilder<T> offset(int offset) {
    if (offset < 0) {
      throw new IllegalArgumentException("Offset must not be negative, got: " + offset);
    }
    queryBuilder.offset(offset);
    return this;
  }

  @Override
  public QueryBuilder<T> page(int pageNumber, int pageSize) {
    if (pageNumber < 0) {
      throw new IllegalArgumentException("Page number must not be negative, got: " + pageNumber);
    }
    if (pageSize <= 0) {
      throw new IllegalArgumentException("Page size must be positive, got: " + pageSize);
    }
    queryBuilder.offset(pageNumber * pageSize);
    queryBuilder.limit(pageSize);
    return this;
  }

  @Override
  public QueryBuilder<T> select(String... fieldNames) {
    if (fieldNames == null || fieldNames.length == 0) {
      throw new IllegalArgumentException("Field names must not be null or empty");
    }
    for (String field : fieldNames) {
      queryBuilder.selectField(field);
    }
    return this;
  }

  @Override
  public QueryBuilder<T> exclude(String... fieldNames) {
    if (fieldNames == null || fieldNames.length == 0) {
      throw new IllegalArgumentException("Field names must not be null or empty");
    }
    for (String field : fieldNames) {
      queryBuilder.excludeField(field);
    }
    return this;
  }

  @Override
  public QueryBuilder<T> distinct() {
    queryBuilder.distinct(true);
    return this;
  }

  @Override
  public QueryBuilder<T> hint(String hintName, Object hintValue) {
    // Hints can be stored in a map and passed to the query translator
    // For now, this is a no-op
    return this;
  }

  @Override
  public List<T> execute() {
    Query query = queryBuilder.build();
    return executor.apply(query);
  }

  @Override
  public Optional<T> executeFirst() {
    // Set limit to 1 for efficiency
    queryBuilder.limit(1);
    List<T> results = execute();
    return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
  }

  @Override
  public Optional<T> executeSingle() {
    List<T> results = execute();
    if (results.isEmpty()) {
      return Optional.empty();
    }
    if (results.size() > 1) {
      throw new IllegalStateException(
          "Expected single result but found " + results.size() + " results");
    }
    return Optional.of(results.get(0));
  }

  @Override
  public CompletableFuture<List<T>> executeAsync() {
    return CompletableFuture.supplyAsync(this::execute);
  }

  @Override
  public long count() {
    // This would need to be implemented by the adapter
    // For now, execute and count the results (inefficient but functional)
    return execute().size();
  }

  @Override
  public boolean exists() {
    queryBuilder.limit(1);
    return !execute().isEmpty();
  }

  private void validateFieldName(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      throw new IllegalArgumentException("Field name must not be null or blank");
    }
  }

  private void addCondition(Condition.Operator operator, Object value) {
    Condition condition =
        new Condition(currentField, operator, value, nextLogicalOperator);
    queryBuilder.addCondition(condition);
  }

  /**
   * Default implementation of ConditionBuilder.
   */
  private class DefaultConditionBuilder implements ConditionBuilder<T> {

    @Override
    public QueryBuilder<T> eq(Object value) {
      addCondition(Condition.Operator.EQUALS, value);
      return DefaultQueryBuilder.this;
    }

    @Override
    public QueryBuilder<T> notEq(Object value) {
      addCondition(Condition.Operator.NOT_EQUALS, value);
      return DefaultQueryBuilder.this;
    }

    @Override
    public QueryBuilder<T> greaterThan(Object value) {
      addCondition(Condition.Operator.GREATER_THAN, value);
      return DefaultQueryBuilder.this;
    }

    @Override
    public QueryBuilder<T> greaterThanOrEq(Object value) {
      addCondition(Condition.Operator.GREATER_THAN_OR_EQUAL, value);
      return DefaultQueryBuilder.this;
    }

    @Override
    public QueryBuilder<T> lessThan(Object value) {
      addCondition(Condition.Operator.LESS_THAN, value);
      return DefaultQueryBuilder.this;
    }

    @Override
    public QueryBuilder<T> lessThanOrEq(Object value) {
      addCondition(Condition.Operator.LESS_THAN_OR_EQUAL, value);
      return DefaultQueryBuilder.this;
    }

    @Override
    public QueryBuilder<T> in(Object... values) {
      if (values == null || values.length == 0) {
        throw new IllegalArgumentException("IN values must not be null or empty");
      }
      addCondition(Condition.Operator.IN, values);
      return DefaultQueryBuilder.this;
    }

    @Override
    public QueryBuilder<T> notIn(Object... values) {
      if (values == null || values.length == 0) {
        throw new IllegalArgumentException("NOT IN values must not be null or empty");
      }
      addCondition(Condition.Operator.NOT_IN, values);
      return DefaultQueryBuilder.this;
    }

    @Override
    public QueryBuilder<T> like(String pattern) {
      if (pattern == null) {
        throw new IllegalArgumentException("LIKE pattern must not be null");
      }
      addCondition(Condition.Operator.LIKE, pattern);
      return DefaultQueryBuilder.this;
    }

    @Override
    public QueryBuilder<T> startsWith(String prefix) {
      if (prefix == null) {
        throw new IllegalArgumentException("Prefix must not be null");
      }
      addCondition(Condition.Operator.STARTS_WITH, prefix);
      return DefaultQueryBuilder.this;
    }

    @Override
    public QueryBuilder<T> endsWith(String suffix) {
      if (suffix == null) {
        throw new IllegalArgumentException("Suffix must not be null");
      }
      addCondition(Condition.Operator.ENDS_WITH, suffix);
      return DefaultQueryBuilder.this;
    }

    @Override
    public QueryBuilder<T> contains(String substring) {
      if (substring == null) {
        throw new IllegalArgumentException("Substring must not be null");
      }
      addCondition(Condition.Operator.CONTAINS, substring);
      return DefaultQueryBuilder.this;
    }

    @Override
    public QueryBuilder<T> isNull() {
      addCondition(Condition.Operator.IS_NULL, null);
      return DefaultQueryBuilder.this;
    }

    @Override
    public QueryBuilder<T> isNotNull() {
      addCondition(Condition.Operator.IS_NOT_NULL, null);
      return DefaultQueryBuilder.this;
    }

    @Override
    public QueryBuilder<T> isTrue() {
      addCondition(Condition.Operator.IS_TRUE, true);
      return DefaultQueryBuilder.this;
    }

    @Override
    public QueryBuilder<T> isFalse() {
      addCondition(Condition.Operator.IS_FALSE, false);
      return DefaultQueryBuilder.this;
    }

    @Override
    public QueryBuilder<T> between(Object start, Object end) {
      if (start == null || end == null) {
        throw new IllegalArgumentException("BETWEEN values must not be null");
      }
      addCondition(Condition.Operator.BETWEEN, new Object[] {start, end});
      return DefaultQueryBuilder.this;
    }
  }

  /**
   * Default implementation of OrderBuilder.
   */
  private class DefaultOrderBuilder implements OrderBuilder<T> {
    private final String fieldName;

    DefaultOrderBuilder(String fieldName) {
      this.fieldName = fieldName;
    }

    @Override
    public QueryBuilder<T> ascending() {
      queryBuilder.addSortOrder(SortOrder.asc(fieldName));
      return DefaultQueryBuilder.this;
    }

    @Override
    public QueryBuilder<T> descending() {
      queryBuilder.addSortOrder(SortOrder.desc(fieldName));
      return DefaultQueryBuilder.this;
    }
  }
}
