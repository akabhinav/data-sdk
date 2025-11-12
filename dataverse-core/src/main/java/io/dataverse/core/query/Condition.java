package io.dataverse.core.query;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a query condition (predicate).
 *
 * <p>Conditions are created by the QueryBuilder and used by QueryTranslator to generate
 * data source-specific query clauses.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public final class Condition {

  private final String fieldName;
  private final Operator operator;
  private final Object value;
  private final LogicalOperator logicalOperator;

  /**
   * Constructs a new condition.
   *
   * @param fieldName the field name
   * @param operator the comparison operator
   * @param value the value to compare against
   * @param logicalOperator the logical operator (AND/OR)
   */
  public Condition(
      String fieldName, Operator operator, Object value, LogicalOperator logicalOperator) {
    this.fieldName = Objects.requireNonNull(fieldName, "Field name must not be null");
    this.operator = Objects.requireNonNull(operator, "Operator must not be null");
    this.value = value;
    this.logicalOperator =
        logicalOperator != null ? logicalOperator : LogicalOperator.AND; // Default to AND
  }

  public String getFieldName() {
    return fieldName;
  }

  public Operator getOperator() {
    return operator;
  }

  public Object getValue() {
    return value;
  }

  public LogicalOperator getLogicalOperator() {
    return logicalOperator;
  }

  /**
   * Comparison operators supported by the query API.
   */
  public enum Operator {
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    IN,
    NOT_IN,
    LIKE,
    STARTS_WITH,
    ENDS_WITH,
    CONTAINS,
    IS_NULL,
    IS_NOT_NULL,
    IS_TRUE,
    IS_FALSE,
    BETWEEN
  }

  /**
   * Logical operators for combining conditions.
   */
  public enum LogicalOperator {
    AND,
    OR
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Condition condition = (Condition) o;
    return Objects.equals(fieldName, condition.fieldName)
        && operator == condition.operator
        && Objects.equals(value, condition.value)
        && logicalOperator == condition.logicalOperator;
  }

  @Override
  public int hashCode() {
    return Objects.hash(fieldName, operator, value, logicalOperator);
  }

  @Override
  public String toString() {
    String valueStr =
        value != null && value.getClass().isArray()
            ? Arrays.toString((Object[]) value)
            : String.valueOf(value);

    return String.format(
        "Condition{%s %s %s %s}",
        logicalOperator, fieldName, operator.name().toLowerCase(), valueStr);
  }
}
