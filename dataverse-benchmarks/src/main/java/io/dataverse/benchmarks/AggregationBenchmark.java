package io.dataverse.benchmarks;

import io.dataverse.adapter.mongodb.MongoDBAdapter;
import io.dataverse.api.AggregationBuilder;
import io.dataverse.api.Entity;
import io.dataverse.api.Repository;
import io.dataverse.spi.AdapterConfig;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for MongoDB aggregation pipeline operations.
 *
 * <p>Measures performance of:
 * - Simple aggregations (GROUP BY + SUM)
 * - Multi-field grouping
 * - Filtered aggregations
 * - Complex aggregations with multiple operations
 *
 * <p>Run with:
 * <pre>
 * java -jar target/benchmarks.jar AggregationBenchmark
 * </pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 3)
@Measurement(iterations = 3, time = 5)
@Fork(1)
public class AggregationBenchmark {

  /**
   * Order entity for aggregation benchmarks.
   */
  public static class BenchmarkOrder implements Entity<String> {
    private String id;
    private String customerId;
    private String region;
    private String status;
    private double totalAmount;
    private int itemCount;

    public BenchmarkOrder() {
    }

    public BenchmarkOrder(String customerId, String region, String status, double totalAmount, int itemCount) {
      this.customerId = customerId;
      this.region = region;
      this.status = status;
      this.totalAmount = totalAmount;
      this.itemCount = itemCount;
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public void setId(String id) {
      this.id = id;
    }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }
  }

  private MongoDBAdapter adapter;
  private Repository<BenchmarkOrder, String> orderRepository;

  @Setup(Level.Trial)
  public void setup() {
    // Initialize MongoDB adapter
    AdapterConfig config = AdapterConfig.builder()
        .property("connectionString", "mongodb://localhost:27017")
        .property("database", "benchmark")
        .build();

    adapter = new MongoDBAdapter();
    adapter.initialize(config);
    orderRepository = adapter.createRepository(BenchmarkOrder.class);

    // Clear existing data
    orderRepository.findAll().forEach(order -> orderRepository.deleteById(order.getId()));

    // Prepare test dataset - 10,000 orders
    List<BenchmarkOrder> orders = new ArrayList<>();
    String[] customers = {"CUST-001", "CUST-002", "CUST-003", "CUST-004", "CUST-005",
                          "CUST-006", "CUST-007", "CUST-008", "CUST-009", "CUST-010"};
    String[] regions = {"US-EAST", "US-WEST", "EU-CENTRAL", "ASIA-PACIFIC"};
    String[] statuses = {"PENDING", "COMPLETED", "CANCELLED"};

    for (int i = 0; i < 10000; i++) {
      orders.add(new BenchmarkOrder(
          customers[i % customers.length],
          regions[i % regions.length],
          statuses[i % statuses.length],
          100.0 + (i % 900),
          1 + (i % 20)
      ));
    }

    // Bulk insert
    orderRepository.batch().upsertAll(orders);

    System.out.println("Loaded 10,000 orders for aggregation benchmarks");
  }

  @TearDown(Level.Trial)
  public void teardown() {
    if (adapter != null) {
      adapter.shutdown();
    }
  }

  @Benchmark
  public void simpleAggregation_GroupByCustomer(Blackhole blackhole) {
    Map<String, Double> result = orderRepository.aggregate()
        .groupBy("customerId")
        .sum("totalAmount")
        .execute();
    blackhole.consume(result);
  }

  @Benchmark
  public void simpleAggregation_CountByStatus(Blackhole blackhole) {
    long count = orderRepository.aggregate()
        .where("status").eq("COMPLETED")
        .executeCount();
    blackhole.consume(count);
  }

  @Benchmark
  public void filteredAggregation_CompletedOrders(Blackhole blackhole) {
    double sum = orderRepository.aggregate()
        .where("status").eq("COMPLETED")
        .sum("totalAmount")
        .executeSum();
    blackhole.consume(sum);
  }

  @Benchmark
  public void multiFieldGrouping_CustomerAndRegion(Blackhole blackhole) {
    AggregationBuilder.AggregationResult result = orderRepository.aggregate()
        .groupBy("customerId", "region")
        .count()
        .sum("totalAmount")
        .avg("itemCount")
        .executeDetailed();
    blackhole.consume(result);
  }

  @Benchmark
  public void complexAggregation_FilteredMultiField(Blackhole blackhole) {
    AggregationBuilder.AggregationResult result = orderRepository.aggregate()
        .where("status").eq("COMPLETED")
        .and("totalAmount").greaterThan(200.0)
        .groupBy("customerId", "region")
        .count()
        .sum("totalAmount")
        .avg("itemCount")
        .min("totalAmount")
        .max("totalAmount")
        .executeDetailed();
    blackhole.consume(result);
  }

  @Benchmark
  public void aggregation_MinMaxOperations(Blackhole blackhole) {
    Double min = orderRepository.aggregate()
        .where("status").eq("COMPLETED")
        .min("totalAmount")
        .executeMin();

    Double max = orderRepository.aggregate()
        .where("status").eq("COMPLETED")
        .max("totalAmount")
        .executeMax();

    blackhole.consume(min);
    blackhole.consume(max);
  }

  @Benchmark
  public void aggregation_AverageCalculation(Blackhole blackhole) {
    double avg = orderRepository.aggregate()
        .where("region").eq("US-EAST")
        .avg("totalAmount")
        .executeAvg();
    blackhole.consume(avg);
  }

  @Benchmark
  public void aggregation_GroupByRegion(Blackhole blackhole) {
    Map<String, Double> result = orderRepository.aggregate()
        .groupBy("region")
        .sum("totalAmount")
        .execute();
    blackhole.consume(result);
  }

  public static void main(String[] args) throws Exception {
    org.openjdk.jmh.Main.main(args);
  }
}
