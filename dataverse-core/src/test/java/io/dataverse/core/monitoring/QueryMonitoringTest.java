package io.dataverse.core.monitoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for query monitoring functionality.
 *
 * @since 1.0.0
 */
@DisplayName("Query Monitoring Tests")
class QueryMonitoringTest {

  @Test
  @DisplayName("Should detect slow queries")
  void shouldDetectSlowQueries() throws Exception {
    // Given
    AtomicInteger slowQueryCount = new AtomicInteger(0);
    List<QueryMetrics> slowQueries = new ArrayList<>();

    QueryMonitoringConfig config = QueryMonitoringConfig.builder()
        .slowQueryThreshold(Duration.ofMillis(50))
        .onSlowQuery(metrics -> {
          slowQueryCount.incrementAndGet();
          slowQueries.add(metrics);
        })
        .build();

    QueryMonitor monitor = new QueryMonitor(config, "TestEntity", "test-adapter");

    // When - Execute fast query
    monitor.execute("fastQuery", () -> {
      Thread.sleep(10);  // 10ms - below threshold
      return "fast";
    });

    // Then - No slow query detected
    assertThat(slowQueryCount.get()).isZero();

    // When - Execute slow query
    monitor.execute("slowQuery", () -> {
      Thread.sleep(100);  // 100ms - above threshold
      return "slow";
    });

    // Then - Slow query detected
    assertThat(slowQueryCount.get()).isEqualTo(1);
    assertThat(slowQueries).hasSize(1);
    assertThat(slowQueries.get(0).getOperation()).isEqualTo("slowQuery");
    assertThat(slowQueries.get(0).getDurationMillis()).isGreaterThan(50);
  }

  @Test
  @DisplayName("Should track all queries when configured")
  void shouldTrackAllQueries() throws Exception {
    // Given
    List<QueryMetrics> allQueries = new ArrayList<>();

    QueryMonitoringConfig config = QueryMonitoringConfig.builder()
        .trackAllQueries(true)
        .onQuery(allQueries::add)
        .build();

    QueryMonitor monitor = new QueryMonitor(config, "TestEntity", "test-adapter");

    // When
    monitor.execute("query1", () -> "result1");
    monitor.execute("query2", () -> "result2");
    monitor.execute("query3", () -> "result3");

    // Then
    assertThat(allQueries).hasSize(3);
    assertThat(allQueries)
        .extracting(QueryMetrics::getOperation)
        .containsExactly("query1", "query2", "query3");
  }

  @Test
  @DisplayName("Should count results correctly")
  void shouldCountResults() throws Exception {
    // Given
    List<QueryMetrics> queries = new ArrayList<>();

    QueryMonitoringConfig config = QueryMonitoringConfig.builder()
        .trackAllQueries(true)
        .onQuery(queries::add)
        .build();

    QueryMonitor monitor = new QueryMonitor(config, "TestEntity", "test-adapter");

    // When - Single result
    monitor.execute("findById", () -> "single-result");

    // When - Multiple results
    List<String> multipleResults = List.of("item1", "item2", "item3");
    monitor.execute("findAll", () -> multipleResults);

    // When - Null result
    monitor.execute("findNonExistent", () -> null);

    // Then
    assertThat(queries).hasSize(3);
    assertThat(queries.get(0).getResultCount()).isEqualTo(1);
    assertThat(queries.get(1).getResultCount()).isEqualTo(3);
    assertThat(queries.get(2).getResultCount()).isZero();
  }

  @Test
  @DisplayName("Should track failed queries")
  void shouldTrackFailedQueries() {
    // Given
    List<QueryMetrics> failedQueries = new ArrayList<>();

    QueryMonitoringConfig config = QueryMonitoringConfig.builder()
        .trackAllQueries(true)
        .onQuery(metrics -> {
          if (!metrics.isSuccess()) {
            failedQueries.add(metrics);
          }
        })
        .build();

    QueryMonitor monitor = new QueryMonitor(config, "TestEntity", "test-adapter");

    // When
    assertThrows(RuntimeException.class, () -> {
      monitor.execute("failingQuery", () -> {
        throw new RuntimeException("Query failed!");
      });
    });

    // Then
    assertThat(failedQueries).hasSize(1);
    assertThat(failedQueries.get(0).isSuccess()).isFalse();
    assertThat(failedQueries.get(0).getError()).isNotNull();
    assertThat(failedQueries.get(0).getError().getMessage()).isEqualTo("Query failed!");
  }

  @Test
  @DisplayName("Should include stack trace when configured")
  void shouldIncludeStackTrace() throws Exception {
    // Given
    List<QueryMetrics> queries = new ArrayList<>();

    QueryMonitoringConfig config = QueryMonitoringConfig.builder()
        .trackAllQueries(true)
        .includeStackTrace(true)
        .onQuery(queries::add)
        .build();

    QueryMonitor monitor = new QueryMonitor(config, "TestEntity", "test-adapter");

    // When
    monitor.execute("queryWithTrace", () -> "result");

    // Then
    assertThat(queries).hasSize(1);
    assertThat(queries.get(0).getStackTrace()).isNotNull();
    assertThat(queries.get(0).getStackTrace()).contains("at ");
  }

  @Test
  @DisplayName("Should execute without monitoring when config is null")
  void shouldExecuteWithoutMonitoring() throws Exception {
    // Given
    QueryMonitor monitor = QueryMonitor.noOp();

    // When
    String result = monitor.execute("unmonitoredQuery", () -> "result");

    // Then
    assertThat(result).isEqualTo("result");
    // No exceptions thrown, monitoring is silently disabled
  }

  @Test
  @DisplayName("Should track query duration accurately")
  void shouldTrackQueryDuration() throws Exception {
    // Given
    List<QueryMetrics> queries = new ArrayList<>();

    QueryMonitoringConfig config = QueryMonitoringConfig.builder()
        .trackAllQueries(true)
        .onQuery(queries::add)
        .build();

    QueryMonitor monitor = new QueryMonitor(config, "TestEntity", "test-adapter");

    // When
    monitor.execute("timedQuery", () -> {
      Thread.sleep(50);
      return "result";
    });

    // Then
    assertThat(queries).hasSize(1);
    assertThat(queries.get(0).getDurationMillis()).isGreaterThanOrEqualTo(50);
    assertThat(queries.get(0).getDurationMillis()).isLessThan(200);
  }
}
