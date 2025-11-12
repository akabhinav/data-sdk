package io.dataverse.core.query;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for DefaultQueryBuilder.
 *
 * @since 1.0.0
 */
@DisplayName("DefaultQueryBuilder Tests")
class DefaultQueryBuilderTest {

  @Test
  @DisplayName("Should build query with single condition")
  void shouldBuildQueryWithSingleCondition() {
    // Given
    TestQueryExecutor executor = new TestQueryExecutor();
    DefaultQueryBuilder<TestEntity> builder = new DefaultQueryBuilder<>(executor);

    // When
    builder.where("name").eq("John").execute();

    // Then
    Query query = executor.getLastQuery();
    assertThat(query).isNotNull();
    assertThat(query.getConditions()).hasSize(1);

    Condition condition = query.getConditions().get(0);
    assertThat(condition.getFieldName()).isEqualTo("name");
    assertThat(condition.getOperator()).isEqualTo(Condition.Operator.EQUALS);
    assertThat(condition.getValue()).isEqualTo("John");
  }

  @Test
  @DisplayName("Should build query with multiple AND conditions")
  void shouldBuildQueryWithMultipleAndConditions() {
    // Given
    TestQueryExecutor executor = new TestQueryExecutor();
    DefaultQueryBuilder<TestEntity> builder = new DefaultQueryBuilder<>(executor);

    // When
    builder
        .where("status").eq("ACTIVE")
        .and("age").greaterThan(18)
        .and("email").endsWith("@company.com")
        .execute();

    // Then
    Query query = executor.getLastQuery();
    assertThat(query.getConditions()).hasSize(3);
    assertThat(query.getConditions())
        .extracting(Condition::getLogicalOperator)
        .containsExactly(
            Condition.LogicalOperator.AND,
            Condition.LogicalOperator.AND,
            Condition.LogicalOperator.AND);
  }

  @Test
  @DisplayName("Should build query with OR conditions")
  void shouldBuildQueryWithOrConditions() {
    // Given
    TestQueryExecutor executor = new TestQueryExecutor();
    DefaultQueryBuilder<TestEntity> builder = new DefaultQueryBuilder<>(executor);

    // When
    builder
        .where("status").eq("ACTIVE")
        .or("status").eq("PENDING")
        .execute();

    // Then
    Query query = executor.getLastQuery();
    assertThat(query.getConditions()).hasSize(2);

    assertThat(query.getConditions().get(0).getLogicalOperator())
        .isEqualTo(Condition.LogicalOperator.AND);
    assertThat(query.getConditions().get(1).getLogicalOperator())
        .isEqualTo(Condition.LogicalOperator.OR);
  }

  @Test
  @DisplayName("Should build query with sorting")
  void shouldBuildQueryWithSorting() {
    // Given
    TestQueryExecutor executor = new TestQueryExecutor();
    DefaultQueryBuilder<TestEntity> builder = new DefaultQueryBuilder<>(executor);

    // When
    builder
        .where("status").eq("ACTIVE")
        .orderBy("lastName").ascending()
        .orderBy("firstName").ascending()
        .execute();

    // Then
    Query query = executor.getLastQuery();
    assertThat(query.getSortOrders()).hasSize(2);
    assertThat(query.getSortOrders().get(0).getFieldName()).isEqualTo("lastName");
    assertThat(query.getSortOrders().get(0).getDirection()).isEqualTo(SortOrder.Direction.ASCENDING);
    assertThat(query.getSortOrders().get(1).getFieldName()).isEqualTo("firstName");
  }

  @Test
  @DisplayName("Should build query with pagination")
  void shouldBuildQueryWithPagination() {
    // Given
    TestQueryExecutor executor = new TestQueryExecutor();
    DefaultQueryBuilder<TestEntity> builder = new DefaultQueryBuilder<>(executor);

    // When
    builder
        .where("status").eq("ACTIVE")
        .limit(10)
        .offset(20)
        .execute();

    // Then
    Query query = executor.getLastQuery();
    assertThat(query.getLimit()).isEqualTo(10);
    assertThat(query.getOffset()).isEqualTo(20);
  }

  @Test
  @DisplayName("Should build query with page-based pagination")
  void shouldBuildQueryWithPageBasedPagination() {
    // Given
    TestQueryExecutor executor = new TestQueryExecutor();
    DefaultQueryBuilder<TestEntity> builder = new DefaultQueryBuilder<>(executor);

    // When
    builder
        .where("status").eq("ACTIVE")
        .page(2, 20) // page 2, size 20
        .execute();

    // Then
    Query query = executor.getLastQuery();
    assertThat(query.getLimit()).isEqualTo(20);
    assertThat(query.getOffset()).isEqualTo(40); // page 2 * size 20
  }

  @Test
  @DisplayName("Should build query with field selection")
  void shouldBuildQueryWithFieldSelection() {
    // Given
    TestQueryExecutor executor = new TestQueryExecutor();
    DefaultQueryBuilder<TestEntity> builder = new DefaultQueryBuilder<>(executor);

    // When
    builder
        .where("status").eq("ACTIVE")
        .select("id", "name", "email")
        .execute();

    // Then
    Query query = executor.getLastQuery();
    assertThat(query.getSelectedFields()).containsExactly("id", "name", "email");
  }

  @Test
  @DisplayName("Should build query with distinct")
  void shouldBuildQueryWithDistinct() {
    // Given
    TestQueryExecutor executor = new TestQueryExecutor();
    DefaultQueryBuilder<TestEntity> builder = new DefaultQueryBuilder<>(executor);

    // When
    builder
        .where("status").eq("ACTIVE")
        .distinct()
        .execute();

    // Then
    Query query = executor.getLastQuery();
    assertThat(query.isDistinct()).isTrue();
  }

  @Test
  @DisplayName("Should support IN operator")
  void shouldSupportInOperator() {
    // Given
    TestQueryExecutor executor = new TestQueryExecutor();
    DefaultQueryBuilder<TestEntity> builder = new DefaultQueryBuilder<>(executor);

    // When
    builder
        .where("status").in("ACTIVE", "PENDING", "PROCESSING")
        .execute();

    // Then
    Query query = executor.getLastQuery();
    Condition condition = query.getConditions().get(0);
    assertThat(condition.getOperator()).isEqualTo(Condition.Operator.IN);
    assertThat((Object[]) condition.getValue())
        .containsExactly("ACTIVE", "PENDING", "PROCESSING");
  }

  @Test
  @DisplayName("Should support BETWEEN operator")
  void shouldSupportBetweenOperator() {
    // Given
    TestQueryExecutor executor = new TestQueryExecutor();
    DefaultQueryBuilder<TestEntity> builder = new DefaultQueryBuilder<>(executor);

    // When
    builder
        .where("age").between(18, 65)
        .execute();

    // Then
    Query query = executor.getLastQuery();
    Condition condition = query.getConditions().get(0);
    assertThat(condition.getOperator()).isEqualTo(Condition.Operator.BETWEEN);
    assertThat((Object[]) condition.getValue()).containsExactly(18, 65);
  }

  @Test
  @DisplayName("Should support string operators")
  void shouldSupportStringOperators() {
    // Given
    TestQueryExecutor executor = new TestQueryExecutor();
    DefaultQueryBuilder<TestEntity> builder = new DefaultQueryBuilder<>(executor);

    // When - test each string operator
    builder.where("email").startsWith("john").execute();
    assertThat(executor.getLastQuery().getConditions().get(0).getOperator())
        .isEqualTo(Condition.Operator.STARTS_WITH);

    builder.where("email").endsWith("@example.com").execute();
    assertThat(executor.getLastQuery().getConditions().get(0).getOperator())
        .isEqualTo(Condition.Operator.ENDS_WITH);

    builder.where("name").contains("Doe").execute();
    assertThat(executor.getLastQuery().getConditions().get(0).getOperator())
        .isEqualTo(Condition.Operator.CONTAINS);

    builder.where("description").like("%pattern%").execute();
    assertThat(executor.getLastQuery().getConditions().get(0).getOperator())
        .isEqualTo(Condition.Operator.LIKE);
  }

  @Test
  @DisplayName("Should throw exception for null field name")
  void shouldThrowExceptionForNullFieldName() {
    // Given
    TestQueryExecutor executor = new TestQueryExecutor();
    DefaultQueryBuilder<TestEntity> builder = new DefaultQueryBuilder<>(executor);

    // When & Then
    assertThatThrownBy(() -> builder.where(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Field name must not be null");
  }

  @Test
  @DisplayName("Should throw exception for invalid limit")
  void shouldThrowExceptionForInvalidLimit() {
    // Given
    TestQueryExecutor executor = new TestQueryExecutor();
    DefaultQueryBuilder<TestEntity> builder = new DefaultQueryBuilder<>(executor);

    // When & Then
    assertThatThrownBy(() -> builder.limit(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Limit must be positive");

    assertThatThrownBy(() -> builder.limit(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Limit must be positive");
  }

  @Test
  @DisplayName("Should throw exception for invalid offset")
  void shouldThrowExceptionForInvalidOffset() {
    // Given
    TestQueryExecutor executor = new TestQueryExecutor();
    DefaultQueryBuilder<TestEntity> builder = new DefaultQueryBuilder<>(executor);

    // When & Then
    assertThatThrownBy(() -> builder.offset(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Offset must not be negative");
  }

  @Test
  @DisplayName("Should execute and return first result")
  void shouldExecuteAndReturnFirstResult() {
    // Given
    TestEntity entity1 = new TestEntity("1", "John");
    TestEntity entity2 = new TestEntity("2", "Jane");
    TestQueryExecutor executor = new TestQueryExecutor(List.of(entity1, entity2));
    DefaultQueryBuilder<TestEntity> builder = new DefaultQueryBuilder<>(executor);

    // When
    var result = builder.where("status").eq("ACTIVE").executeFirst();

    // Then
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(entity1);
    assertThat(executor.getLastQuery().getLimit()).isEqualTo(1); // Should optimize with limit
  }

  @Test
  @DisplayName("Should execute and return single result")
  void shouldExecuteAndReturnSingleResult() {
    // Given
    TestEntity entity = new TestEntity("1", "John");
    TestQueryExecutor executor = new TestQueryExecutor(List.of(entity));
    DefaultQueryBuilder<TestEntity> builder = new DefaultQueryBuilder<>(executor);

    // When
    var result = builder.where("id").eq("1").executeSingle();

    // Then
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(entity);
  }

  @Test
  @DisplayName("Should throw exception when multiple results for executeSingle")
  void shouldThrowExceptionWhenMultipleResultsForExecuteSingle() {
    // Given
    TestEntity entity1 = new TestEntity("1", "John");
    TestEntity entity2 = new TestEntity("2", "Jane");
    TestQueryExecutor executor = new TestQueryExecutor(List.of(entity1, entity2));
    DefaultQueryBuilder<TestEntity> builder = new DefaultQueryBuilder<>(executor);

    // When & Then
    assertThatThrownBy(() -> builder.where("status").eq("ACTIVE").executeSingle())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Expected single result but found 2 results");
  }

  // Test helper classes

  static class TestEntity {
    private final String id;
    private final String name;

    TestEntity(String id, String name) {
      this.id = id;
      this.name = name;
    }

    public String getId() {
      return id;
    }

    public String getName() {
      return name;
    }
  }

  static class TestQueryExecutor implements java.util.function.Function<Query, List<TestEntity>> {
    private Query lastQuery;
    private final List<TestEntity> results;

    TestQueryExecutor() {
      this(List.of());
    }

    TestQueryExecutor(List<TestEntity> results) {
      this.results = results;
    }

    @Override
    public List<TestEntity> apply(Query query) {
      this.lastQuery = query;
      return results;
    }

    Query getLastQuery() {
      return lastQuery;
    }
  }
}
