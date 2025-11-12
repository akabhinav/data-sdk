# DataVerse SDK Architecture

> A deep dive into the design decisions, patterns, and principles behind DataVerse SDK

## Table of Contents

1. [Architectural Principles](#architectural-principles)
2. [Zero Dependency Architecture](#zero-dependency-architecture)
3. [Layered Architecture](#layered-architecture)
4. [Plugin Architecture (SPI)](#plugin-architecture-spi)
5. [Concurrency Model](#concurrency-model)
6. [Design Patterns](#design-patterns)
7. [Performance Considerations](#performance-considerations)
8. [Security Considerations](#security-considerations)

## Architectural Principles

DataVerse SDK is built on five core principles:

### 1. Zero Dependency Core

**Principle**: The core module must have ZERO external dependencies, relying only on Java 21 standard library.

**Rationale**:
- Eliminates dependency conflicts
- Reduces JAR size (< 500 KB)
- Improves startup time
- Simplifies security audits
- Enables use in restrictive environments

**Implementation**:
```xml
<!-- dataverse-core/pom.xml -->
<dependencies>
    <!-- NO compile-scope dependencies! -->
    <!-- Only test-scope dependencies allowed -->
</dependencies>
```

### 2. Loose Coupling

**Principle**: Depend on abstractions, not implementations. No module should depend on another module's implementation details.

**Dependency Rules**:
```
✅ Allowed:
   - Adapters → Core (via API/SPI)
   - Applications → Core
   - Optional modules → Core

❌ Forbidden:
   - Core → Adapters
   - Adapter A → Adapter B
   - Core → Spring/Framework
```

**Enforcement**:
- ArchUnit tests validate dependency rules
- Maven dependency analyzer
- CI/CD pipeline checks

### 3. High Extensibility

**Principle**: Easy to extend with new adapters, cache providers, metrics collectors without modifying core.

**Extension Points**:
- `DataSourceAdapter` - Add new data sources
- `CacheProvider` - Add new caching strategies
- `MetricsCollector` - Add new monitoring systems
- `QueryTranslator` - Customize query translation

### 4. Performance First

**Principle**: Optimize for Java 21 virtual threads, zero-copy operations, and efficient resource usage.

**Performance Strategies**:
- Virtual threads for blocking I/O
- Connection pooling (custom implementation)
- Object pooling for frequent allocations
- Lazy initialization
- Efficient data structures
- Non-blocking where beneficial

### 5. Framework Agnostic

**Principle**: Work standalone without any framework, but integrate seamlessly when needed.

**Approach**:
- Core has no framework dependencies
- Optional `dataverse-spring-boot-starter` for Spring Boot
- Could add Quarkus, Micronaut integrations as separate modules

## Zero Dependency Architecture

### Module Isolation Matrix

| Module | Dependencies | Size Limit | External Deps |
|--------|-------------|------------|---------------|
| `dataverse-core` | **NONE** | 500 KB | **0** |
| `dataverse-adapter-dynamodb` | core + AWS SDK | 200 KB | 1 |
| `dataverse-adapter-redis` | core + Jedis | 150 KB | 1 |
| `dataverse-adapter-mongodb` | core + Mongo Driver | 200 KB | 1 |

### Dependency Validation

```java
// ArchUnit test to enforce zero dependencies
@Test
void coreShouldHaveZeroDependencies() {
    noClasses()
        .that().resideInAPackage("io.dataverse..")
        .should().dependOnClassesThat()
        .resideOutsideOfPackages("java..", "io.dataverse..")
        .check(importedClasses);
}
```

## Layered Architecture

### Layer Diagram

```
┌─────────────────────────────────────────────────────────┐
│                  APPLICATION LAYER                      │
│            (User Code - Any Framework)                  │
└─────────────────────────────────────────────────────────┘
                          ↓ Uses
┌─────────────────────────────────────────────────────────┐
│                   PUBLIC API LAYER                      │
│         (Interfaces Only - Zero Implementation)         │
│  • Repository<T, ID>                                    │
│  • QueryBuilder<T>                                      │
│  • Entity<ID>                                           │
└─────────────────────────────────────────────────────────┘
                          ↓ Implements
┌─────────────────────────────────────────────────────────┐
│                   CORE ENGINE LAYER                     │
│          (Pure Java 21 - Zero Dependencies)             │
│  • Query Engine                                         │
│  • Connection Pool Manager                              │
│  • Cache Manager                                        │
│  • Resilience Manager                                   │
│  • Metrics Collector                                    │
│  • Validation Engine                                    │
└─────────────────────────────────────────────────────────┘
                          ↓ Defines
┌─────────────────────────────────────────────────────────┐
│              SERVICE PROVIDER INTERFACE                 │
│            (SPI - Plugin Contracts)                     │
│  • DataSourceAdapter                                    │
│  • QueryTranslator                                      │
│  • ConnectionProvider                                   │
│  • ResultMapper                                         │
│  • CacheProvider                                        │
│  • MetricsCollector                                     │
└─────────────────────────────────────────────────────────┘
                          ↑ Implements
┌─────────────────────────────────────────────────────────┐
│              ADAPTER IMPLEMENTATIONS                    │
│         (Pluggable - Discovered via SPI)                │
│  DynamoDB │ Redis │ MongoDB │ Cassandra │ PostgreSQL   │
└─────────────────────────────────────────────────────────┘
                          ↓ Uses
┌─────────────────────────────────────────────────────────┐
│                 NATIVE CLIENTS                          │
│     AWS SDK, Jedis, MongoDB Driver, Datastax, etc.      │
└─────────────────────────────────────────────────────────┘
```

### Layer Responsibilities

**Application Layer**:
- Business logic
- Entity definitions
- Application-specific workflows

**Public API Layer**:
- Clean, type-safe interfaces
- No implementation details
- Stable contract

**Core Engine Layer**:
- Query execution
- Connection management
- Caching logic
- Resilience patterns
- Metrics collection

**SPI Layer**:
- Plugin contracts
- Extension points
- Discovery mechanism

**Adapter Layer**:
- Data source-specific implementations
- Query translation
- Native client integration

## Plugin Architecture (SPI)

### ServiceLoader Discovery

```java
// 1. Define SPI interface (in core)
package io.dataverse.spi;

public interface DataSourceAdapter {
    String getAdapterId();
    void initialize(AdapterConfig config);
    <T extends Entity<ID>, ID extends Serializable>
        Repository<T, ID> createRepository(Class<T> entityClass);
}

// 2. Implement in adapter module
package io.dataverse.adapter.dynamodb;

public class DynamoDBAdapter implements DataSourceAdapter {
    // Implementation
}

// 3. Register via META-INF/services
// File: META-INF/services/io.dataverse.spi.DataSourceAdapter
// Content: io.dataverse.adapter.dynamodb.DynamoDBAdapter

// 4. Runtime discovery
ServiceLoader<DataSourceAdapter> loader =
    ServiceLoader.load(DataSourceAdapter.class);

for (DataSourceAdapter adapter : loader) {
    registry.register(adapter);
}
```

### Plugin Lifecycle

```
┌──────────────┐
│  DISCOVERY   │ ServiceLoader finds all adapter implementations
└──────┬───────┘
       │
       ↓
┌──────────────┐
│ REGISTRATION │ Adapters registered in AdapterRegistry
└──────┬───────┘
       │
       ↓
┌──────────────┐
│INITIALIZATION│ adapter.initialize(config) called
└──────┬───────┘
       │
       ↓
┌──────────────┐
│    ACTIVE    │ Adapter serves requests
└──────┬───────┘
       │
       ↓
┌──────────────┐
│   SHUTDOWN   │ adapter.shutdown() releases resources
└──────────────┘
```

### Multiple Adapter Instances

```java
// Different configurations of the same adapter
AdapterConfig prodConfig = AdapterConfig.builder()
    .property("endpoint", "https://dynamodb.us-east-1.amazonaws.com")
    .property("region", "us-east-1")
    .build();

AdapterConfig devConfig = AdapterConfig.builder()
    .property("endpoint", "http://localhost:8000")
    .property("region", "us-east-1")
    .build();

DataSourceAdapter prodAdapter = new DynamoDBAdapter();
prodAdapter.initialize(prodConfig);

DataSourceAdapter devAdapter = new DynamoDBAdapter();
devAdapter.initialize(devConfig);
```

## Concurrency Model

### Java 21 Virtual Threads

DataVerse SDK is optimized for virtual threads (Project Loom):

```java
// Old approach: Limited platform threads
ExecutorService executor = Executors.newFixedThreadPool(100);
// Problem: Only 100 concurrent operations

// DataVerse approach: Unlimited virtual threads
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
// Solution: 10,000+ concurrent operations easily
```

### Virtual Thread Benefits

1. **Massive Concurrency**: Handle 100,000+ concurrent requests
2. **Simple Code**: Write blocking code that performs like async
3. **No Thread Pool Tuning**: Virtual threads are cheap
4. **Better Resource Usage**: Less memory per thread

### Connection Pool Design

```java
// Virtual thread-friendly connection pool
public class VirtualThreadConnectionPool<C> {

    private final BlockingQueue<C> connections;
    private final Semaphore permits;

    public C acquire() {
        // Blocking is cheap with virtual threads!
        permits.acquire();
        try {
            return connections.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConnectionException("Interrupted", e);
        }
    }

    public void release(C connection) {
        connections.offer(connection);
        permits.release();
    }
}
```

### Async API with Virtual Threads

```java
@Override
public CompletableFuture<T> saveAsync(T entity) {
    return CompletableFuture.supplyAsync(
        () -> save(entity),
        virtualThreadExecutor  // Uses virtual threads
    );
}
```

## Design Patterns

### Repository Pattern

**Intent**: Abstract data access behind a collection-like interface

```java
public interface Repository<T, ID> {
    T save(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    void deleteById(ID id);
    QueryBuilder<T> query();
}
```

**Benefits**:
- Hides persistence details
- Easy to mock for testing
- Consistent API across data sources

### Builder Pattern

**Intent**: Provide fluent API for complex object construction

```java
List<User> users = repository.query()
    .where("status").eq("ACTIVE")
    .and("age").greaterThan(18)
    .orderBy("name").ascending()
    .limit(100)
    .execute();
```

**Benefits**:
- Readable, self-documenting code
- Compile-time type safety
- Immutable query objects

### Adapter Pattern

**Intent**: Convert one interface to another

```java
// Generic DataVerse API
repository.save(user);

// Translated to DynamoDB
PutItemRequest request = PutItemRequest.builder()
    .tableName("users")
    .item(convertToAttributeValues(user))
    .build();
client.putItem(request);
```

### Strategy Pattern

**Intent**: Encapsulate algorithms and make them interchangeable

```java
// Different caching strategies
CacheProvider<K, V> cache = new CaffeineCache<>();  // L1
CacheProvider<K, V> cache = new RedisCache<>();     // L2
CacheProvider<K, V> cache = new MultiLevelCache<>();// L1+L2

repository.configureCache(cache);
```

### Template Method Pattern

**Intent**: Define algorithm skeleton, let subclasses implement steps

```java
public abstract class AbstractRepository<T, ID>
    implements Repository<T, ID> {

    @Override
    public final T save(T entity) {
        validate(entity);
        T saved = doSave(entity);
        afterSave(saved);
        return saved;
    }

    protected abstract T doSave(T entity);
    protected void afterSave(T entity) {
        // Hook for subclasses
    }
}
```

## Performance Considerations

### Connection Pooling

```java
// Custom connection pool optimized for virtual threads
public class VirtualThreadConnectionPool<C> {
    private final int minSize = 10;
    private final int maxSize = 100;
    private final Duration timeout = Duration.ofSeconds(30);

    // No complex pool tuning needed!
    // Virtual threads make blocking cheap
}
```

### Query Optimization

```java
// Lazy result streaming (avoid loading all results)
Stream<User> stream = repository.query()
    .where("status").eq("ACTIVE")
    .stream();  // Fetch on-demand

// Batch operations
repository.saveAll(users);  // Optimized batch insert
```

### Caching Strategy

```java
// Multi-level cache: L1 (in-memory) + L2 (distributed)
CacheProvider<K, V> l1 = new CaffeineCache<>();
CacheProvider<K, V> l2 = new RedisCache<>();
CacheProvider<K, V> multiLevel = new MultiLevelCache<>(l1, l2);

// Cache-aside pattern
V value = cache.get(key, () -> repository.findById(key));
```

### Metrics Collection

```java
// Low-overhead metrics
MetricsCollector metrics = new MicrometerCollector();

// Automatic instrumentation
metrics.time("query.execution", () -> {
    return repository.query().execute();
});

// Exported metrics:
// - query_execution_time_seconds (histogram)
// - connection_pool_active (gauge)
// - cache_hit_total (counter)
```

## Security Considerations

### Input Validation

```java
// Prevent injection attacks
repository.query()
    .where("userId").eq(sanitize(userInput))  // Parameterized
    .execute();

// ❌ NEVER do this:
String query = "SELECT * FROM users WHERE id = '" + userInput + "'";
```

### Credential Management

```java
// Use secure credential providers
AdapterConfig config = AdapterConfig.builder()
    .property("credentialsProvider", DefaultCredentialsProvider.create())
    .build();

// ❌ NEVER hardcode credentials:
// .property("accessKey", "AKIAIOSFODNN7EXAMPLE")
```

### Connection Security

```java
// TLS/SSL by default
AdapterConfig config = AdapterConfig.builder()
    .property("ssl.enabled", true)
    .property("ssl.verifyServerCertificate", true)
    .build();
```

### Audit Logging

```java
// Log all data access operations
MetricsCollector auditLog = new AuditLogCollector();
adapter.configureMetrics(auditLog);

// Captures: who, what, when, where
```

## Conclusion

DataVerse SDK's architecture is designed for:
- **Simplicity**: Easy to understand and use
- **Flexibility**: Easy to extend and customize
- **Performance**: Leverages Java 21 features
- **Reliability**: Built-in resilience and monitoring
- **Security**: Secure by default

The zero-dependency core, loose coupling, and plugin architecture make it suitable for a wide range of applications, from startups to enterprise systems.
