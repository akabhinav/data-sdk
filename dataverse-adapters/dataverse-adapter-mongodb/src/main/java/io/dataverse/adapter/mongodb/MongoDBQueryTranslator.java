package io.dataverse.adapter.mongodb;

import io.dataverse.core.query.Condition;
import io.dataverse.core.query.Query;
import io.dataverse.core.query.SortOrder;
import io.dataverse.spi.QueryTranslationException;
import io.dataverse.spi.QueryTranslator;
import org.bson.Document;
import org.bson.conversions.Bson;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

/**
 * Query translator for MongoDB.
 *
 * <p>Translates generic DataVerse queries to MongoDB BSON queries.
 *
 * @since 1.0.0
 */
public class MongoDBQueryTranslator implements QueryTranslator {

  @Override
  public NativeQuery translate(Query query) {
    try {
      Document filter = translateConditions(query);
      Document sort = translateSorting(query);

      return new MongoDBNativeQuery(filter, sort, query.getLimit(), query.getOffset());
    } catch (Exception e) {
      throw new QueryTranslationException("Failed to translate query to MongoDB format", e);
    }
  }

  @Override
  public boolean supports(Query query) {
    // MongoDB supports most query types
    return true;
  }

  @Override
  public String getQueryLanguage() {
    return "MongoDB BSON Query";
  }

  /**
   * Translates conditions to MongoDB filter.
   */
  private Document translateConditions(Query query) {
    if (!query.hasConditions()) {
      return new Document(); // Empty filter (match all)
    }

    var andFilters = new java.util.ArrayList<Bson>();
    var orFilters = new java.util.ArrayList<Bson>();

    Condition.LogicalOperator currentOp = null;

    for (Condition condition : query.getConditions()) {
      Bson filter = translateCondition(condition);

      if (currentOp == null || currentOp == Condition.LogicalOperator.AND) {
        if (condition.getLogicalOperator() == Condition.LogicalOperator.OR) {
          // Switch to OR
          orFilters.add(filter);
          currentOp = Condition.LogicalOperator.OR;
        } else {
          andFilters.add(filter);
          currentOp = Condition.LogicalOperator.AND;
        }
      } else {
        orFilters.add(filter);
      }
    }

    // Combine filters
    Document result = new Document();

    if (!andFilters.isEmpty()) {
      if (andFilters.size() == 1) {
        result = (Document) andFilters.get(0);
      } else {
        result.put("$and", andFilters);
      }
    }

    if (!orFilters.isEmpty()) {
      if (result.isEmpty()) {
        result.put("$or", orFilters);
      } else {
        // Combine AND and OR
        var combined = new Document();
        combined.put("$and", java.util.List.of(result, new Document("$or", orFilters)));
        result = combined;
      }
    }

    return result;
  }

  /**
   * Translates a single condition to MongoDB filter.
   */
  private Bson translateCondition(Condition condition) {
    String field = condition.getFieldName();
    Object value = condition.getValue();

    return switch (condition.getOperator()) {
      case EQUALS -> Filters.eq(field, value);
      case NOT_EQUALS -> Filters.ne(field, value);
      case GREATER_THAN -> Filters.gt(field, value);
      case GREATER_THAN_OR_EQUAL -> Filters.gte(field, value);
      case LESS_THAN -> Filters.lt(field, value);
      case LESS_THAN_OR_EQUAL -> Filters.lte(field, value);
      case IN -> Filters.in(field, (Object[]) value);
      case NOT_IN -> Filters.nin(field, (Object[]) value);
      case LIKE -> Filters.regex(field, value.toString());
      case STARTS_WITH -> Filters.regex(field, "^" + value.toString());
      case ENDS_WITH -> Filters.regex(field, value.toString() + "$");
      case CONTAINS -> Filters.regex(field, ".*" + value.toString() + ".*");
      case IS_NULL -> Filters.eq(field, null);
      case IS_NOT_NULL -> Filters.ne(field, null);
      case IS_TRUE -> Filters.eq(field, true);
      case IS_FALSE -> Filters.eq(field, false);
      case BETWEEN -> {
        Object[] values = (Object[]) value;
        yield Filters.and(Filters.gte(field, values[0]), Filters.lte(field, values[1]));
      }
    };
  }

  /**
   * Translates sorting to MongoDB sort document.
   */
  private Document translateSorting(Query query) {
    if (!query.hasSorting()) {
      return new Document();
    }

    var sortList = new java.util.ArrayList<Bson>();
    for (SortOrder sortOrder : query.getSortOrders()) {
      Bson sort = sortOrder.getDirection() == SortOrder.Direction.ASCENDING
          ? Sorts.ascending(sortOrder.getFieldName())
          : Sorts.descending(sortOrder.getFieldName());
      sortList.add(sort);
    }

    return sortList.isEmpty() ? new Document() : (Document) Sorts.orderBy(sortList);
  }

  /**
   * MongoDB native query representation.
   */
  public static class MongoDBNativeQuery implements NativeQuery {
    private final Document filter;
    private final Document sort;
    private final Integer limit;
    private final Integer offset;

    public MongoDBNativeQuery(Document filter, Document sort, Integer limit, Integer offset) {
      this.filter = filter;
      this.sort = sort;
      this.limit = limit;
      this.offset = offset;
    }

    public Document getFilter() {
      return filter;
    }

    public Document getSort() {
      return sort;
    }

    public Integer getLimit() {
      return limit;
    }

    public Integer getOffset() {
      return offset;
    }

    @Override
    public Object getNativeQueryObject() {
      return filter;
    }

    @Override
    public String toQueryString() {
      return String.format("db.collection.find(%s).sort(%s).skip(%d).limit(%d)",
          filter.toJson(),
          sort.toJson(),
          offset != null ? offset : 0,
          limit != null ? limit : 0);
    }
  }
}
