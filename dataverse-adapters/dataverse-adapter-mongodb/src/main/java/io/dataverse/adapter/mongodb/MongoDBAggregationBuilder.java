package io.dataverse.adapter.mongodb;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.dataverse.api.AggregationBuilder;
import io.dataverse.api.QueryBuilder;
import io.dataverse.core.query.Condition;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.*;

import static com.mongodb.client.model.Accumulators.*;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;

/**
 * MongoDB implementation of AggregationBuilder using MongoDB Aggregation Pipeline.
 *
 * <p>This implementation leverages MongoDB's powerful aggregation framework for
 * high-performance analytics and group operations.
 *
 * <p><strong>Supported Operations:</strong>
 * <ul>
 *   <li>$group - Group by one or more fields</li>
 *   <li>$match - Filter documents</li>
 *   <li>$count - Count documents</li>
 *   <li>$sum - Sum numeric fields</li>
 *   <li>$avg - Calculate averages</li>
 *   <li>$min - Find minimum values</li>
 *   <li>$max - Find maximum values</li>
 * </ul>
 *
 * @param <T> the entity type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class MongoDBAggregationBuilder<T> implements AggregationBuilder<T> {

  private final MongoCollection<Document> collection;
  private final List<Bson> pipeline = new ArrayList<>();
  private final List<String> groupByFields = new ArrayList<>();
  private final List<Condition> filterConditions = new ArrayList<>();
  private final Map<String, AggregationOp> aggregations = new HashMap<>();

  private enum AggregationOp {
    COUNT, SUM, AVG, MIN, MAX
  }

  public MongoDBAggregationBuilder(MongoDatabase database, String collectionName) {
    this.collection = database.getCollection(collectionName);
  }

  @Override
  public AggregationBuilder<T> groupBy(String... fieldNames) {
    if (fieldNames == null || fieldNames.length == 0) {
      throw new IllegalArgumentException("Group by fields must not be empty");
    }
    Collections.addAll(groupByFields, fieldNames);
    return this;
  }

  @Override
  public QueryBuilder.ConditionBuilder<T> where(String fieldName) {
    return new MongoConditionBuilder(fieldName);
  }

  @Override
  public AggregationBuilder<T> count() {
    aggregations.put("count", AggregationOp.COUNT);
    return this;
  }

  @Override
  public AggregationBuilder<T> sum(String fieldName) {
    aggregations.put("sum_" + fieldName, AggregationOp.SUM);
    return this;
  }

  @Override
  public AggregationBuilder<T> avg(String fieldName) {
    aggregations.put("avg_" + fieldName, AggregationOp.AVG);
    return this;
  }

  @Override
  public AggregationBuilder<T> min(String fieldName) {
    aggregations.put("min_" + fieldName, AggregationOp.MIN);
    return this;
  }

  @Override
  public AggregationBuilder<T> max(String fieldName) {
    aggregations.put("max_" + fieldName, AggregationOp.MAX);
    return this;
  }

  @Override
  public <K, V> Map<K, V> execute() {
    buildPipeline();

    Map<Object, Object> results = new HashMap<>();
    AggregateIterable<Document> aggResult = collection.aggregate(pipeline);

    for (Document doc : aggResult) {
      Object groupKey = doc.get("_id");
      Object aggValue = doc.get(aggregations.keySet().iterator().next());
      results.put(groupKey, aggValue);
    }

    @SuppressWarnings("unchecked")
    Map<K, V> typedResults = (Map<K, V>) results;
    return typedResults;
  }

  @Override
  public AggregationResult executeDetailed() {
    buildPipeline();

    List<Map<String, Object>> groups = new ArrayList<>();
    Map<Map<String, Object>, Map<String, Object>> groupData = new HashMap<>();

    AggregateIterable<Document> aggResult = collection.aggregate(pipeline);

    for (Document doc : aggResult) {
      Map<String, Object> groupValue = new HashMap<>();

      // Extract group keys
      Object idValue = doc.get("_id");
      if (idValue instanceof Document) {
        Document idDoc = (Document) idValue;
        groupValue.putAll(idDoc);
      } else {
        groupValue.put("value", idValue);
      }

      groups.add(groupValue);

      // Extract aggregation results
      Map<String, Object> aggData = new HashMap<>();
      for (String aggKey : aggregations.keySet()) {
        Object aggValue = doc.get(aggKey);
        if (aggValue != null) {
          aggData.put(aggKey, aggValue);
        }
      }

      groupData.put(groupValue, aggData);
    }

    return new MongoAggregationResult(groups, groupData);
  }

  @Override
  public long executeCount() {
    if (!filterConditions.isEmpty()) {
      return collection.countDocuments(buildFilterDocument());
    } else {
      return collection.countDocuments();
    }
  }

  @Override
  public double executeSum() {
    buildPipeline();

    String sumField = aggregations.entrySet().stream()
        .filter(e -> e.getValue() == AggregationOp.SUM)
        .map(Map.Entry::getKey)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No sum field specified"));

    AggregateIterable<Document> result = collection.aggregate(pipeline);
    Document doc = result.first();

    return doc != null ? doc.getDouble(sumField, 0.0) : 0.0;
  }

  @Override
  public double executeAvg() {
    buildPipeline();

    String avgField = aggregations.entrySet().stream()
        .filter(e -> e.getValue() == AggregationOp.AVG)
        .map(Map.Entry::getKey)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No avg field specified"));

    AggregateIterable<Document> result = collection.aggregate(pipeline);
    Document doc = result.first();

    return doc != null ? doc.getDouble(avgField, 0.0) : 0.0;
  }

  @Override
  public <V> V executeMin() {
    buildPipeline();

    String minField = aggregations.entrySet().stream()
        .filter(e -> e.getValue() == AggregationOp.MIN)
        .map(Map.Entry::getKey)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No min field specified"));

    AggregateIterable<Document> result = collection.aggregate(pipeline);
    Document doc = result.first();

    @SuppressWarnings("unchecked")
    V value = doc != null ? (V) doc.get(minField) : null;
    return value;
  }

  @Override
  public <V> V executeMax() {
    buildPipeline();

    String maxField = aggregations.entrySet().stream()
        .filter(e -> e.getValue() == AggregationOp.MAX)
        .map(Map.Entry::getKey)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No max field specified"));

    AggregateIterable<Document> result = collection.aggregate(pipeline);
    Document doc = result.first();

    @SuppressWarnings("unchecked")
    V value = doc != null ? (V) doc.get(maxField) : null;
    return value;
  }

  private void buildPipeline() {
    pipeline.clear();

    if (!filterConditions.isEmpty()) {
      pipeline.add(match(buildFilterDocument()));
    }

    if (!groupByFields.isEmpty() || !aggregations.isEmpty()) {
      pipeline.add(buildGroupStage());
    }
  }

  private Bson buildGroupStage() {
    Document groupId = new Document();
    for (String field : groupByFields) {
      groupId.put(field, "$" + field);
    }

    List<Bson> accumulators = new ArrayList<>();

    for (Map.Entry<String, AggregationOp> entry : aggregations.entrySet()) {
      String aggKey = entry.getKey();
      AggregationOp op = entry.getValue();

      String fieldName = aggKey.contains("_") ?
          aggKey.substring(aggKey.indexOf("_") + 1) : aggKey;

      Bson accumulator = switch (op) {
        case COUNT -> sum(aggKey, 1);
        case SUM -> sum(aggKey, "$" + fieldName);
        case AVG -> avg(aggKey, "$" + fieldName);
        case MIN -> min(aggKey, "$" + fieldName);
        case MAX -> max(aggKey, "$" + fieldName);
      };

      accumulators.add(accumulator);
    }

    if (groupByFields.isEmpty()) {
      return group(null, accumulators);
    } else {
      return group(groupId, accumulators);
    }
  }

  private Bson buildFilterDocument() {
    if (filterConditions.isEmpty()) {
      return new Document();
    }

    List<Bson> filters = new ArrayList<>();
    for (Condition condition : filterConditions) {
      filters.add(translateCondition(condition));
    }

    return and(filters);
  }

  private Bson translateCondition(Condition condition) {
    String field = condition.getFieldName();
    Object value = condition.getValue();

    return switch (condition.getOperator()) {
      case EQUALS -> eq(field, value);
      case NOT_EQUALS -> ne(field, value);
      case GREATER_THAN -> gt(field, value);
      case GREATER_THAN_OR_EQUAL -> gte(field, value);
      case LESS_THAN -> lt(field, value);
      case LESS_THAN_OR_EQUAL -> lte(field, value);
      case IN -> in(field, (Object[]) value);
      case NOT_IN -> nin(field, (Object[]) value);
      case IS_NULL -> eq(field, null);
      case IS_NOT_NULL -> ne(field, null);
      case IS_TRUE -> eq(field, true);
      case IS_FALSE -> eq(field, false);
      case BETWEEN -> {
        Object[] values = (Object[]) value;
        yield and(gte(field, values[0]), lte(field, values[1]));
      }
      default -> throw new UnsupportedOperationException(
          "Operator not supported: " + condition.getOperator());
    };
  }

  private class MongoConditionBuilder implements QueryBuilder.ConditionBuilder<T> {
    private final String fieldName;

    MongoConditionBuilder(String fieldName) {
      this.fieldName = fieldName;
    }

    @Override
    public AggregationBuilder<T> eq(Object value) {
      filterConditions.add(new Condition(fieldName, Condition.Operator.EQUALS, value,
          Condition.LogicalOperator.AND));
      return MongoDBAggregationBuilder.this;
    }

    @Override
    public AggregationBuilder<T> notEq(Object value) {
      filterConditions.add(new Condition(fieldName, Condition.Operator.NOT_EQUALS, value,
          Condition.LogicalOperator.AND));
      return MongoDBAggregationBuilder.this;
    }

    @Override
    public AggregationBuilder<T> greaterThan(Object value) {
      filterConditions.add(new Condition(fieldName, Condition.Operator.GREATER_THAN, value,
          Condition.LogicalOperator.AND));
      return MongoDBAggregationBuilder.this;
    }

    @Override
    public AggregationBuilder<T> greaterThanOrEq(Object value) {
      filterConditions.add(new Condition(fieldName, Condition.Operator.GREATER_THAN_OR_EQUAL, value,
          Condition.LogicalOperator.AND));
      return MongoDBAggregationBuilder.this;
    }

    @Override
    public AggregationBuilder<T> lessThan(Object value) {
      filterConditions.add(new Condition(fieldName, Condition.Operator.LESS_THAN, value,
          Condition.LogicalOperator.AND));
      return MongoDBAggregationBuilder.this;
    }

    @Override
    public AggregationBuilder<T> lessThanOrEq(Object value) {
      filterConditions.add(new Condition(fieldName, Condition.Operator.LESS_THAN_OR_EQUAL, value,
          Condition.LogicalOperator.AND));
      return MongoDBAggregationBuilder.this;
    }

    @Override
    public AggregationBuilder<T> in(Object... values) {
      filterConditions.add(new Condition(fieldName, Condition.Operator.IN, values,
          Condition.LogicalOperator.AND));
      return MongoDBAggregationBuilder.this;
    }

    @Override
    public AggregationBuilder<T> notIn(Object... values) {
      filterConditions.add(new Condition(fieldName, Condition.Operator.NOT_IN, values,
          Condition.LogicalOperator.AND));
      return MongoDBAggregationBuilder.this;
    }

    @Override
    public AggregationBuilder<T> like(String pattern) {
      throw new UnsupportedOperationException("LIKE not supported in aggregations");
    }

    @Override
    public AggregationBuilder<T> startsWith(String prefix) {
      throw new UnsupportedOperationException("STARTS_WITH not supported in aggregations");
    }

    @Override
    public AggregationBuilder<T> endsWith(String suffix) {
      throw new UnsupportedOperationException("ENDS_WITH not supported in aggregations");
    }

    @Override
    public AggregationBuilder<T> contains(String substring) {
      throw new UnsupportedOperationException("CONTAINS not supported in aggregations");
    }

    @Override
    public AggregationBuilder<T> isNull() {
      filterConditions.add(new Condition(fieldName, Condition.Operator.IS_NULL, null,
          Condition.LogicalOperator.AND));
      return MongoDBAggregationBuilder.this;
    }

    @Override
    public AggregationBuilder<T> isNotNull() {
      filterConditions.add(new Condition(fieldName, Condition.Operator.IS_NOT_NULL, null,
          Condition.LogicalOperator.AND));
      return MongoDBAggregationBuilder.this;
    }

    @Override
    public AggregationBuilder<T> isTrue() {
      filterConditions.add(new Condition(fieldName, Condition.Operator.IS_TRUE, true,
          Condition.LogicalOperator.AND));
      return MongoDBAggregationBuilder.this;
    }

    @Override
    public AggregationBuilder<T> isFalse() {
      filterConditions.add(new Condition(fieldName, Condition.Operator.IS_FALSE, false,
          Condition.LogicalOperator.AND));
      return MongoDBAggregationBuilder.this;
    }

    @Override
    public AggregationBuilder<T> between(Object start, Object end) {
      filterConditions.add(new Condition(fieldName, Condition.Operator.BETWEEN,
          new Object[]{start, end}, Condition.LogicalOperator.AND));
      return MongoDBAggregationBuilder.this;
    }
  }

  private static class MongoAggregationResult implements AggregationResult {
    private final List<Map<String, Object>> groups;
    private final Map<Map<String, Object>, Map<String, Object>> groupData;

    MongoAggregationResult(
        List<Map<String, Object>> groups,
        Map<Map<String, Object>, Map<String, Object>> groupData) {
      this.groups = groups;
      this.groupData = groupData;
    }

    @Override
    public List<Map<String, Object>> getGroups() {
      return groups;
    }

    @Override
    public long getCount(Map<String, Object> groupValue) {
      Map<String, Object> data = groupData.get(groupValue);
      if (data != null && data.containsKey("count")) {
        return ((Number) data.get("count")).longValue();
      }
      return 0;
    }

    @Override
    public double getSum(Map<String, Object> groupValue, String fieldName) {
      Map<String, Object> data = groupData.get(groupValue);
      String key = "sum_" + fieldName;
      if (data != null && data.containsKey(key)) {
        return ((Number) data.get(key)).doubleValue();
      }
      return 0.0;
    }

    @Override
    public double getAvg(Map<String, Object> groupValue, String fieldName) {
      Map<String, Object> data = groupData.get(groupValue);
      String key = "avg_" + fieldName;
      if (data != null && data.containsKey(key)) {
        return ((Number) data.get(key)).doubleValue();
      }
      return 0.0;
    }

    @Override
    public <V> V getMin(Map<String, Object> groupValue, String fieldName) {
      Map<String, Object> data = groupData.get(groupValue);
      String key = "min_" + fieldName;
      if (data != null && data.containsKey(key)) {
        @SuppressWarnings("unchecked")
        V value = (V) data.get(key);
        return value;
      }
      return null;
    }

    @Override
    public <V> V getMax(Map<String, Object> groupValue, String fieldName) {
      Map<String, Object> data = groupData.get(groupValue);
      String key = "max_" + fieldName;
      if (data != null && data.containsKey(key)) {
        @SuppressWarnings("unchecked")
        V value = (V) data.get(key);
        return value;
      }
      return null;
    }

    @Override
    public Map<String, Object> getGroupData(Map<String, Object> groupValue) {
      return groupData.getOrDefault(groupValue, Collections.emptyMap());
    }

    @Override
    public long getTotalCount() {
      return groups.stream()
          .mapToLong(this::getCount)
          .sum();
    }
  }
}
