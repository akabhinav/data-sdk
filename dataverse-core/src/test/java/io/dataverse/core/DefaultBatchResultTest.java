package io.dataverse.core;

import io.dataverse.api.BatchOperations.BatchResult;
import io.dataverse.api.Entity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for DefaultBatchResult.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
class DefaultBatchResultTest {

  static class TestEntity implements Entity<String> {
    private String id;
    private String name;

    public TestEntity() {}

    public TestEntity(String id, String name) {
      this.id = id;
      this.name = name;
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public void setId(String id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  @Test
  @DisplayName("Should create empty batch result")
  void shouldCreateEmptyBatchResult() {
    BatchResult<TestEntity> result = DefaultBatchResult.<TestEntity>builder().build();

    assertThat(result.getSuccessful()).isEmpty();
    assertThat(result.getFailed()).isEmpty();
    assertThat(result.getFailures()).isEmpty();
    assertThat(result.getTotalCount()).isEqualTo(0);
    assertThat(result.getSuccessCount()).isEqualTo(0);
    assertThat(result.getFailureCount()).isEqualTo(0);
    assertThat(result.getInsertedCount()).isEqualTo(0);
    assertThat(result.getUpdatedCount()).isEqualTo(0);
    assertThat(result.isAllSuccessful()).isTrue();
    assertThat(result.hasFailures()).isFalse();
  }

  @Test
  @DisplayName("Should add successful entities")
  void shouldAddSuccessfulEntities() {
    var entity1 = new TestEntity("1", "Entity 1");
    var entity2 = new TestEntity("2", "Entity 2");

    BatchResult<TestEntity> result = DefaultBatchResult.<TestEntity>builder()
        .addSuccessful(entity1)
        .addSuccessful(entity2)
        .build();

    assertThat(result.getSuccessful()).containsExactly(entity1, entity2);
    assertThat(result.getSuccessCount()).isEqualTo(2);
    assertThat(result.getTotalCount()).isEqualTo(2);
    assertThat(result.isAllSuccessful()).isTrue();
  }

  @Test
  @DisplayName("Should add failed entities with error messages")
  void shouldAddFailedEntitiesWithErrorMessages() {
    var entity1 = new TestEntity("1", "Entity 1");
    var entity2 = new TestEntity("2", "Entity 2");

    BatchResult<TestEntity> result = DefaultBatchResult.<TestEntity>builder()
        .addFailure(entity1, "Error 1")
        .addFailure(entity2, "Error 2")
        .build();

    assertThat(result.getFailed()).containsExactly(entity1, entity2);
    assertThat(result.getFailureCount()).isEqualTo(2);
    assertThat(result.getTotalCount()).isEqualTo(2);
    assertThat(result.isAllSuccessful()).isFalse();
    assertThat(result.hasFailures()).isTrue();
    assertThat(result.getFailures()).hasSize(2);
    assertThat(result.getFailures().get(entity1)).isEqualTo("Error 1");
    assertThat(result.getFailures().get(entity2)).isEqualTo("Error 2");
  }

  @Test
  @DisplayName("Should track inserted count")
  void shouldTrackInsertedCount() {
    var entity1 = new TestEntity("1", "Entity 1");
    var entity2 = new TestEntity("2", "Entity 2");

    BatchResult<TestEntity> result = DefaultBatchResult.<TestEntity>builder()
        .addSuccessful(entity1)
        .incrementInserted()
        .addSuccessful(entity2)
        .incrementInserted()
        .build();

    assertThat(result.getInsertedCount()).isEqualTo(2);
    assertThat(result.getUpdatedCount()).isEqualTo(0);
  }

  @Test
  @DisplayName("Should track updated count")
  void shouldTrackUpdatedCount() {
    var entity1 = new TestEntity("1", "Entity 1");
    var entity2 = new TestEntity("2", "Entity 2");

    BatchResult<TestEntity> result = DefaultBatchResult.<TestEntity>builder()
        .addSuccessful(entity1)
        .incrementUpdated()
        .addSuccessful(entity2)
        .incrementUpdated()
        .build();

    assertThat(result.getUpdatedCount()).isEqualTo(2);
    assertThat(result.getInsertedCount()).isEqualTo(0);
  }

  @Test
  @DisplayName("Should handle mixed success and failure")
  void shouldHandleMixedSuccessAndFailure() {
    var success1 = new TestEntity("1", "Success 1");
    var success2 = new TestEntity("2", "Success 2");
    var failure1 = new TestEntity("3", "Failure 1");

    BatchResult<TestEntity> result = DefaultBatchResult.<TestEntity>builder()
        .addSuccessful(success1)
        .incrementInserted()
        .addSuccessful(success2)
        .incrementUpdated()
        .addFailure(failure1, "Failed to save")
        .build();

    assertThat(result.getTotalCount()).isEqualTo(3);
    assertThat(result.getSuccessCount()).isEqualTo(2);
    assertThat(result.getFailureCount()).isEqualTo(1);
    assertThat(result.getInsertedCount()).isEqualTo(1);
    assertThat(result.getUpdatedCount()).isEqualTo(1);
    assertThat(result.isAllSuccessful()).isFalse();
    assertThat(result.hasFailures()).isTrue();
  }

  @Test
  @DisplayName("Should create successful result using static factory")
  void shouldCreateSuccessfulResultUsingStaticFactory() {
    var entities = List.of(
        new TestEntity("1", "Entity 1"),
        new TestEntity("2", "Entity 2")
    );

    BatchResult<TestEntity> result = DefaultBatchResult.success(entities);

    assertThat(result.getSuccessful()).hasSize(2);
    assertThat(result.isAllSuccessful()).isTrue();
    assertThat(result.hasFailures()).isFalse();
  }

  @Test
  @DisplayName("Should create inserted result using static factory")
  void shouldCreateInsertedResultUsingStaticFactory() {
    var entities = List.of(
        new TestEntity("1", "Entity 1"),
        new TestEntity("2", "Entity 2")
    );

    BatchResult<TestEntity> result = DefaultBatchResult.inserted(entities);

    assertThat(result.getSuccessful()).hasSize(2);
    assertThat(result.getInsertedCount()).isEqualTo(2);
    assertThat(result.getUpdatedCount()).isEqualTo(0);
  }

  @Test
  @DisplayName("Should create updated result using static factory")
  void shouldCreateUpdatedResultUsingStaticFactory() {
    var entities = List.of(
        new TestEntity("1", "Entity 1"),
        new TestEntity("2", "Entity 2")
    );

    BatchResult<TestEntity> result = DefaultBatchResult.updated(entities);

    assertThat(result.getSuccessful()).hasSize(2);
    assertThat(result.getUpdatedCount()).isEqualTo(2);
    assertThat(result.getInsertedCount()).isEqualTo(0);
  }

  @Test
  @DisplayName("Should add all successful entities using bulk method")
  void shouldAddAllSuccessfulEntitiesUsingBulkMethod() {
    var entities = List.of(
        new TestEntity("1", "Entity 1"),
        new TestEntity("2", "Entity 2"),
        new TestEntity("3", "Entity 3")
    );

    BatchResult<TestEntity> result = DefaultBatchResult.<TestEntity>builder()
        .addAllSuccessful(entities)
        .build();

    assertThat(result.getSuccessful()).hasSize(3);
    assertThat(result.isAllSuccessful()).isTrue();
  }

  @Test
  @DisplayName("Should set counts directly")
  void shouldSetCountsDirectly() {
    BatchResult<TestEntity> result = DefaultBatchResult.<TestEntity>builder()
        .insertedCount(10)
        .updatedCount(5)
        .build();

    assertThat(result.getInsertedCount()).isEqualTo(10);
    assertThat(result.getUpdatedCount()).isEqualTo(5);
  }
}
