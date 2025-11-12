package io.dataverse.core.query;

import java.util.Objects;

/**
 * Represents a sort order specification.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public final class SortOrder {

  private final String fieldName;
  private final Direction direction;

  /**
   * Constructs a new sort order.
   *
   * @param fieldName the field name to sort by
   * @param direction the sort direction
   */
  public SortOrder(String fieldName, Direction direction) {
    this.fieldName = Objects.requireNonNull(fieldName, "Field name must not be null");
    this.direction = Objects.requireNonNull(direction, "Direction must not be null");
  }

  public String getFieldName() {
    return fieldName;
  }

  public Direction getDirection() {
    return direction;
  }

  /**
   * Sort direction.
   */
  public enum Direction {
    ASCENDING,
    DESCENDING
  }

  /**
   * Creates an ascending sort order.
   *
   * @param fieldName the field name
   * @return a new sort order
   */
  public static SortOrder asc(String fieldName) {
    return new SortOrder(fieldName, Direction.ASCENDING);
  }

  /**
   * Creates a descending sort order.
   *
   * @param fieldName the field name
   * @return a new sort order
   */
  public static SortOrder desc(String fieldName) {
    return new SortOrder(fieldName, Direction.DESCENDING);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SortOrder sortOrder = (SortOrder) o;
    return Objects.equals(fieldName, sortOrder.fieldName) && direction == sortOrder.direction;
  }

  @Override
  public int hashCode() {
    return Objects.hash(fieldName, direction);
  }

  @Override
  public String toString() {
    return String.format("SortOrder{%s %s}", fieldName, direction);
  }
}
