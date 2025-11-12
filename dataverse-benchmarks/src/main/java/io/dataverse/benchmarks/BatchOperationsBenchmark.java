package io.dataverse.benchmarks;

import io.dataverse.adapter.dynamodb.DynamoDBAdapter;
import io.dataverse.adapter.mongodb.MongoDBAdapter;
import io.dataverse.adapter.redis.RedisAdapter;
import io.dataverse.api.BatchOperations;
import io.dataverse.api.Entity;
import io.dataverse.api.Repository;
import io.dataverse.spi.AdapterConfig;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for batch operations across different adapters.
 *
 * <p>Measures throughput and latency for:
 * - Batch writes (upsert)
 * - Batch reads (findAllById)
 * - Single-item operations (baseline)
 *
 * <p>Run with:
 * <pre>
 * mvn clean package
 * java -jar target/benchmarks.jar
 * </pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 5, time = 10)
@Fork(value = 1, jvmArgs = {"-Xms2g", "-Xmx2g"})
public class BatchOperationsBenchmark {

  /**
   * Test entity.
   */
  public static class BenchmarkEntity implements Entity<String> {
    private String id;
    private String name;
    private String email;
    private int value;

    public BenchmarkEntity() {
    }

    public BenchmarkEntity(String name, String email, int value) {
      this.name = name;
      this.email = email;
      this.value = value;
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public void setId(String id) {
      this.id = id;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
  }

  /**
   * Benchmark state for MongoDB.
   */
  @State(Scope.Benchmark)
  public static class MongoDBState {
    MongoDBAdapter adapter;
    Repository<BenchmarkEntity, String> repository;
    BatchOperations<BenchmarkEntity, String> batchOps;
    List<BenchmarkEntity> entities;
    List<String> entityIds;

    @Setup(Level.Trial)
    public void setup() {
      AdapterConfig config = AdapterConfig.builder()
          .property("connectionString", "mongodb://localhost:27017")
          .property("database", "benchmark")
          .build();

      adapter = new MongoDBAdapter();
      adapter.initialize(config);
      repository = adapter.createRepository(BenchmarkEntity.class);
      batchOps = repository.batch();

      // Prepare test data
      entities = new ArrayList<>();
      for (int i = 0; i < 100; i++) {
        entities.add(new BenchmarkEntity("Entity " + i, "entity" + i + "@bench.com", i));
      }

      // Save entities and collect IDs
      BatchOperations.BatchResult<BenchmarkEntity> result = batchOps.upsertAll(entities);
      entityIds = result.getSuccessful().stream().map(BenchmarkEntity::getId).toList();
    }

    @TearDown(Level.Trial)
    public void teardown() {
      if (adapter != null) {
        adapter.shutdown();
      }
    }
  }

  /**
   * Benchmark state for Redis.
   */
  @State(Scope.Benchmark)
  public static class RedisState {
    RedisAdapter adapter;
    Repository<BenchmarkEntity, String> repository;
    BatchOperations<BenchmarkEntity, String> batchOps;
    List<BenchmarkEntity> entities;
    List<String> entityIds;

    @Setup(Level.Trial)
    public void setup() {
      AdapterConfig config = AdapterConfig.builder()
          .property("host", "localhost")
          .property("port", 6379)
          .property("maxConnections", 20)
          .build();

      adapter = new RedisAdapter();
      adapter.initialize(config);
      repository = adapter.createRepository(BenchmarkEntity.class);
      batchOps = repository.batch();

      // Prepare test data
      entities = new ArrayList<>();
      for (int i = 0; i < 100; i++) {
        entities.add(new BenchmarkEntity("Entity " + i, "entity" + i + "@bench.com", i));
      }

      // Save entities and collect IDs
      BatchOperations.BatchResult<BenchmarkEntity> result = batchOps.upsertAll(entities);
      entityIds = result.getSuccessful().stream().map(BenchmarkEntity::getId).toList();
    }

    @TearDown(Level.Trial)
    public void teardown() {
      if (adapter != null) {
        adapter.shutdown();
      }
    }
  }

  // ===================================================================
  // MongoDB Benchmarks
  // ===================================================================

  @Benchmark
  @OperationsPerInvocation(100)
  public void mongodbBatchUpsert100(MongoDBState state, Blackhole blackhole) {
    BatchOperations.BatchResult<BenchmarkEntity> result = state.batchOps.upsertAll(state.entities);
    blackhole.consume(result);
  }

  @Benchmark
  @OperationsPerInvocation(100)
  public void mongodbBatchRead100(MongoDBState state, Blackhole blackhole) {
    BatchOperations.BatchResult<BenchmarkEntity> result = state.batchOps.findAllById(state.entityIds);
    blackhole.consume(result);
  }

  @Benchmark
  public void mongodbSingleWrite(MongoDBState state, Blackhole blackhole) {
    BenchmarkEntity entity = new BenchmarkEntity("Single", "single@bench.com", 1);
    BenchmarkEntity saved = state.repository.save(entity);
    blackhole.consume(saved);
  }

  @Benchmark
  public void mongodbSingleRead(MongoDBState state, Blackhole blackhole) {
    if (!state.entityIds.isEmpty()) {
      var result = state.repository.findById(state.entityIds.get(0));
      blackhole.consume(result);
    }
  }

  // ===================================================================
  // Redis Benchmarks
  // ===================================================================

  @Benchmark
  @OperationsPerInvocation(100)
  public void redisBatchUpsert100(RedisState state, Blackhole blackhole) {
    BatchOperations.BatchResult<BenchmarkEntity> result = state.batchOps.upsertAll(state.entities);
    blackhole.consume(result);
  }

  @Benchmark
  @OperationsPerInvocation(100)
  public void redisBatchRead100(RedisState state, Blackhole blackhole) {
    BatchOperations.BatchResult<BenchmarkEntity> result = state.batchOps.findAllById(state.entityIds);
    blackhole.consume(result);
  }

  @Benchmark
  public void redisSingleWrite(RedisState state, Blackhole blackhole) {
    BenchmarkEntity entity = new BenchmarkEntity("Single", "single@bench.com", 1);
    BenchmarkEntity saved = state.repository.save(entity);
    blackhole.consume(saved);
  }

  @Benchmark
  public void redisSingleRead(RedisState state, Blackhole blackhole) {
    if (!state.entityIds.isEmpty()) {
      var result = state.repository.findById(state.entityIds.get(0));
      blackhole.consume(result);
    }
  }

  // ===================================================================
  // Scaling Benchmarks - Test with different batch sizes
  // ===================================================================

  /**
   * Benchmark different batch sizes for Redis (where pipelining shines).
   */
  @State(Scope.Benchmark)
  public static class RedisBatchScalingState {
    RedisAdapter adapter;
    Repository<BenchmarkEntity, String> repository;
    BatchOperations<BenchmarkEntity, String> batchOps;

    List<BenchmarkEntity> batch10;
    List<BenchmarkEntity> batch50;
    List<BenchmarkEntity> batch100;
    List<BenchmarkEntity> batch500;
    List<BenchmarkEntity> batch1000;

    @Setup(Level.Trial)
    public void setup() {
      AdapterConfig config = AdapterConfig.builder()
          .property("host", "localhost")
          .property("port", 6379)
          .property("maxConnections", 20)
          .build();

      adapter = new RedisAdapter();
      adapter.initialize(config);
      repository = adapter.createRepository(BenchmarkEntity.class);
      batchOps = repository.batch();

      // Prepare different batch sizes
      batch10 = createEntities(10);
      batch50 = createEntities(50);
      batch100 = createEntities(100);
      batch500 = createEntities(500);
      batch1000 = createEntities(1000);
    }

    private List<BenchmarkEntity> createEntities(int count) {
      List<BenchmarkEntity> entities = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        entities.add(new BenchmarkEntity("Entity " + i, "entity" + i + "@bench.com", i));
      }
      return entities;
    }

    @TearDown(Level.Trial)
    public void teardown() {
      if (adapter != null) {
        adapter.shutdown();
      }
    }
  }

  @Benchmark
  @OperationsPerInvocation(10)
  public void redisBatchSize10(RedisBatchScalingState state, Blackhole blackhole) {
    BatchOperations.BatchResult<BenchmarkEntity> result = state.batchOps.upsertAll(state.batch10);
    blackhole.consume(result);
  }

  @Benchmark
  @OperationsPerInvocation(50)
  public void redisBatchSize50(RedisBatchScalingState state, Blackhole blackhole) {
    BatchOperations.BatchResult<BenchmarkEntity> result = state.batchOps.upsertAll(state.batch50);
    blackhole.consume(result);
  }

  @Benchmark
  @OperationsPerInvocation(100)
  public void redisBatchSize100(RedisBatchScalingState state, Blackhole blackhole) {
    BatchOperations.BatchResult<BenchmarkEntity> result = state.batchOps.upsertAll(state.batch100);
    blackhole.consume(result);
  }

  @Benchmark
  @OperationsPerInvocation(500)
  public void redisBatchSize500(RedisBatchScalingState state, Blackhole blackhole) {
    BatchOperations.BatchResult<BenchmarkEntity> result = state.batchOps.upsertAll(state.batch500);
    blackhole.consume(result);
  }

  @Benchmark
  @OperationsPerInvocation(1000)
  public void redisBatchSize1000(RedisBatchScalingState state, Blackhole blackhole) {
    BatchOperations.BatchResult<BenchmarkEntity> result = state.batchOps.upsertAll(state.batch1000);
    blackhole.consume(result);
  }

  public static void main(String[] args) throws Exception {
    org.openjdk.jmh.Main.main(args);
  }
}
