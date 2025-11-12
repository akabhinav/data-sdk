package io.dataverse.core.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Internal representation of a query.
 *
 * <p>This class is the data structure representation of a query built using the QueryBuilder API.
 * It is used by QueryTranslator implementations to convert to native query formats.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public final class Query {

  private final List<Condition> conditions;
  private final List<SortOrder> sortOrders;
  private final List<String> selectedFields;
  private final List<String> excludedFields;
  private final Integer limit;
  private final Integer offset;
  private final boolean distinct;

  private Query(Builder builder) {
    this.conditions = Collections.unmodifiableList(new ArrayList<>(builder.conditions));
    this.sortOrders = Collections.unmodifiableList(new ArrayList<>(builder.sortOrders));
    this.selectedFields = Collections.unmodifiableList(new ArrayList<>(builder.selectedFields));
    this.excludedFields = Collections.unmodifiableList(new ArrayList<>(builder.excludedFields));
    this.limit = builder.limit;
    this.offset = builder.offset;
    this.distinct = builder.distinct;
  }

  public List<Condition> getConditions() {
    return conditions;
  }

  public List<SortOrder> getSortOrders() {
    return sortOrders;
  }

  public List<String> getSelectedFields() {
    return selectedFields;
  }

  public List<String> getExcludedFields() {
    return excludedFields;
  }

  public Integer getLimit() {
    return limit;
  }

  public Integer getOffset() {
    return offset;
  }

  public boolean isDistinct() {
    return distinct;
  }

  public boolean hasConditions() {
    return !conditions.isEmpty();
  }

  public boolean hasSorting() {
    return !sortOrders.isEmpty();
  }

  public boolean hasProjection() {
    return !selectedFields.isEmpty() || !excludedFields.isEmpty();
  }

  public boolean hasPagination() {
    return limit != null || offset != null;
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for Query.
   */
  public static final class Builder {
    private final List<Condition> conditions = new ArrayList<>();
    private final List<SortOrder> sortOrders = new ArrayList<>();
    private final List<String> selectedFields = new ArrayList<>();
    private final List<String> excludedFields = new ArrayList<>();
    private Integer limit;
    private Integer offset;
    private boolean distinct;

    private Builder() {}

    public Builder addCondition(Condition condition) {
      this.conditions.add(condition);
      return this;
    }

    public Builder addSortOrder(SortOrder sortOrder) {
      this.sortOrders.add(sortOrder);
      return this;
    }

    public Builder selectField(String field) {
      this.selectedFields.add(field);
      return this;
    }

    public Builder excludeField(String field) {
      this.excludedFields.add(field);
      return this;
    }

    public Builder limit(Integer limit) {
      this.limit = limit;
      return this;
    }

    public Builder offset(Integer offset) {
      this.offset = offset;
      return this;
    }

    public Builder distinct(boolean distinct) {
      this.distinct = distinct;
      return this;
    }

    public Query build() {
      return new Query(this);
    }
  }

  @Override
  public String toString() {
    return "Query{"
        + "conditions="
        + conditions
        + ", sortOrders="
        + sortOrders
        + ", limit="
        + limit
        + ", offset="
        + offset
        + ", distinct="
        + distinct
        + '}';
  }
}
