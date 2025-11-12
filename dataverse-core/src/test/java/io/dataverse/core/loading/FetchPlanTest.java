package io.dataverse.core.loading;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for FetchPlan.
 *
 * @author DataVerse SDK Team
 */
class FetchPlanTest {

  @Test
  void testBuilder_SingleEagerField() {
    FetchPlan plan = FetchPlan.builder()
        .eager("orders")
        .build();

    assertEquals(FetchStrategy.EAGER, plan.getStrategy("orders"));
    assertEquals(FetchStrategy.DEFAULT, plan.getStrategy("profile"));  // Not specified
  }

  @Test
  void testBuilder_MultipleEagerFields() {
    FetchPlan plan = FetchPlan.builder()
        .eager("orders", "profile", "roles")
        .build();

    assertEquals(FetchStrategy.EAGER, plan.getStrategy("orders"));
    assertEquals(FetchStrategy.EAGER, plan.getStrategy("profile"));
    assertEquals(FetchStrategy.EAGER, plan.getStrategy("roles"));
  }

  @Test
  void testBuilder_MixedStrategies() {
    FetchPlan plan = FetchPlan.builder()
        .eager("orders")
        .lazy("profile")
        .batch("activities", 100)
        .build();

    assertEquals(FetchStrategy.EAGER, plan.getStrategy("orders"));
    assertEquals(FetchStrategy.LAZY, plan.getStrategy("profile"));
    assertEquals(FetchStrategy.BATCH, plan.getStrategy("activities"));
    assertEquals(100, plan.getBatchSize("activities"));
  }

  @Test
  void testBuilder_NestedPaths() {
    FetchPlan plan = FetchPlan.builder()
        .eager("orders")
        .eager("orders.items")
        .eager("orders.items.product")
        .build();

    assertEquals(FetchStrategy.EAGER, plan.getStrategy("orders"));
    assertEquals(FetchStrategy.EAGER, plan.getStrategy("orders.items"));
    assertEquals(FetchStrategy.EAGER, plan.getStrategy("orders.items.product"));
  }

  @Test
  void testBuilder_CustomDefaultStrategy() {
    FetchPlan plan = FetchPlan.builder()
        .eager("orders")
        .defaultStrategy(FetchStrategy.LAZY)
        .build();

    assertEquals(FetchStrategy.EAGER, plan.getStrategy("orders"));
    assertEquals(FetchStrategy.LAZY, plan.getStrategy("profile"));  // Uses default
    assertEquals(FetchStrategy.LAZY, plan.getDefaultStrategy());
  }

  @Test
  void testEagerAll() {
    FetchPlan plan = FetchPlan.eagerAll();

    assertEquals(FetchStrategy.EAGER, plan.getDefaultStrategy());
    assertEquals(FetchStrategy.EAGER, plan.getStrategy("anything"));
    assertEquals(FetchStrategy.EAGER, plan.getStrategy("orders"));
  }

  @Test
  void testLazyAll() {
    FetchPlan plan = FetchPlan.lazyAll();

    assertEquals(FetchStrategy.LAZY, plan.getDefaultStrategy());
    assertEquals(FetchStrategy.LAZY, plan.getStrategy("anything"));
    assertEquals(FetchStrategy.LAZY, plan.getStrategy("orders"));
  }

  @Test
  void testDefaults() {
    FetchPlan plan = FetchPlan.defaults();

    assertEquals(FetchStrategy.DEFAULT, plan.getDefaultStrategy());
    assertEquals(FetchStrategy.DEFAULT, plan.getStrategy("anything"));
  }

  @Test
  void testGetFieldPaths() {
    FetchPlan plan = FetchPlan.builder()
        .eager("orders")
        .lazy("profile")
        .batch("activities", 50)
        .build();

    var paths = plan.getFieldPaths();
    assertEquals(3, paths.size());
    assertTrue(paths.contains("orders"));
    assertTrue(paths.contains("profile"));
    assertTrue(paths.contains("activities"));
  }

  @Test
  void testGetFieldPaths_Empty() {
    FetchPlan plan = FetchPlan.builder().build();

    assertTrue(plan.getFieldPaths().isEmpty());
  }

  @Test
  void testBatchSize_Default() {
    FetchPlan plan = FetchPlan.builder()
        .eager("orders")
        .build();

    assertEquals(0, plan.getBatchSize("orders"));
    assertEquals(0, plan.getBatchSize("nonexistent"));
  }

  @Test
  void testBatchSize_Specified() {
    FetchPlan plan = FetchPlan.builder()
        .batch("activities", 100)
        .batch("logs", 500)
        .build();

    assertEquals(100, plan.getBatchSize("activities"));
    assertEquals(500, plan.getBatchSize("logs"));
  }

  @Test
  void testFetch_CustomStrategy() {
    FetchPlan plan = FetchPlan.builder()
        .fetch("field1", FetchStrategy.EAGER)
        .fetch("field2", FetchStrategy.LAZY)
        .fetch("field3", FetchStrategy.BATCH)
        .build();

    assertEquals(FetchStrategy.EAGER, plan.getStrategy("field1"));
    assertEquals(FetchStrategy.LAZY, plan.getStrategy("field2"));
    assertEquals(FetchStrategy.BATCH, plan.getStrategy("field3"));
  }

  @Test
  void testOverride_Strategy() {
    FetchPlan plan = FetchPlan.builder()
        .lazy("orders")
        .eager("orders")  // Override previous
        .build();

    assertEquals(FetchStrategy.EAGER, plan.getStrategy("orders"));
  }

  @Test
  void testImmutability_Strategies() {
    FetchPlan plan = FetchPlan.builder()
        .eager("orders")
        .build();

    var paths = plan.getFieldPaths();
    assertThrows(UnsupportedOperationException.class, () ->
        paths.add("newField")
    );
  }

  @Test
  void testComplexPlan() {
    FetchPlan plan = FetchPlan.builder()
        // Eager load orders and their items
        .eager("orders")
        .eager("orders.items")
        .eager("orders.items.product")

        // Lazy load profile
        .lazy("profile")

        // Batch load activities
        .batch("activities", 100)
        .batch("notifications", 50)

        // Default strategy for other fields
        .defaultStrategy(FetchStrategy.LAZY)
        .build();

    // Verify eager loads
    assertEquals(FetchStrategy.EAGER, plan.getStrategy("orders"));
    assertEquals(FetchStrategy.EAGER, plan.getStrategy("orders.items"));
    assertEquals(FetchStrategy.EAGER, plan.getStrategy("orders.items.product"));

    // Verify lazy load
    assertEquals(FetchStrategy.LAZY, plan.getStrategy("profile"));

    // Verify batch loads
    assertEquals(FetchStrategy.BATCH, plan.getStrategy("activities"));
    assertEquals(100, plan.getBatchSize("activities"));
    assertEquals(FetchStrategy.BATCH, plan.getStrategy("notifications"));
    assertEquals(50, plan.getBatchSize("notifications"));

    // Verify default
    assertEquals(FetchStrategy.LAZY, plan.getStrategy("somethingElse"));

    // Verify field paths
    assertEquals(6, plan.getFieldPaths().size());
  }
}
