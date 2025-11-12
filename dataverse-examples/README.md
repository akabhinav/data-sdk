# DataVerse SDK - Examples

Comprehensive examples demonstrating all features of the DataVerse SDK.

## Overview

This module contains detailed, real-world examples for all 20 features implemented in the DataVerse SDK. Each example includes:

- Use case descriptions
- Code samples
- Expected output
- Performance comparisons
- Best practices

## Features Demonstrated

### 1. Read Replicas Support (Feature #9)
**File**: `ReadReplicasExample.java`

Examples include:
- Basic round-robin routing
- Geographic routing for low latency
- Least connections routing
- Automatic failover to primary
- Health checking
- Custom configuration

**Use Cases**:
- E-commerce product catalog queries
- Global SaaS user profiles
- Analytics dashboards
- High-availability setups

---

### 2. Native Query with Type Safety (Feature #3)
**File**: `NativeQueryExample.java`

Examples include:
- Basic named parameters
- Complex queries with joins
- Database-specific features (PostgreSQL full-text search)
- Dynamic parameter binding
- Stored procedure calls
- Repository integration

**Use Cases**:
- Performance-critical queries
- Database-specific optimizations
- Complex reporting
- Legacy database integration

---

### 3. Lazy/Eager Loading Strategies (Feature #8)
**File**: `LazyEagerLoadingExample.java`

Examples include:
- Lazy loading basics
- Eager loading to prevent N+1
- Batch loading
- Declarative fetch plans
- Conditional fetching
- Deep object graphs

**Use Cases**:
- Solving N+1 query problems
- Optimizing entity graph loading
- Memory optimization
- Performance tuning

---

### 4. Schema Migration & Versioning (Feature #16)
**File**: `SchemaMigrationExample.java`

Examples include:
- Basic migrations
- File-based migrations
- Rollback support
- Validation and checksums
- Production deployment
- Team collaboration

**Use Cases**:
- Database schema evolution
- Version control for schema
- Production deployments
- Team collaboration

---

### 5. Distributed Tracing (Feature #14)
**File**: `DistributedTracingExample.java`

Examples include:
- Basic tracing
- Microservice tracing
- Database operation tracing
- W3C context propagation
- Performance monitoring
- Error tracking

**Use Cases**:
- Microservice debugging
- Performance analysis
- Cross-service request tracking
- Error diagnosis

---

### 6. Cross-Datasource Joins (Feature #5)
**File**: `CrossDatasourceJoinsExample.java`

Examples include:
- Basic inner join
- Left join
- Full outer join
- Multiple joins
- Filtering and pagination
- Real-world e-commerce scenario

**Use Cases**:
- Joining PostgreSQL and MongoDB
- Data reconciliation
- Unified reporting
- Multi-system analytics

---

### 7. Data Synchronization (Feature #17)
**File**: `DataSynchronizationExample.java`

Examples include:
- One-way synchronization
- Two-way synchronization
- Conflict resolution strategies
- Batch synchronization
- Continuous synchronization
- Multi-datacenter sync

**Use Cases**:
- Database replication
- Multi-region sync
- Data migration
- Real-time synchronization

---

### 8. Hot Reload (Feature #20)
**File**: `HotReloadExample.java`

Examples include:
- Basic file watching
- Database config reload
- Query mappings reload
- Listener notifications
- Directory watching
- Production scenario

**Use Cases**:
- Configuration without restart
- Feature flag toggling
- Query optimization
- Connection pool tuning

---

### 9. GraphQL Auto-Generation (Feature #19)
**File**: `GraphQLExample.java`

Examples include:
- Basic schema generation
- Custom annotations
- Executing queries
- Mutations
- Multiple entities
- Complete API example

**Use Cases**:
- Rapid API development
- Type-safe APIs
- Unified data access
- Frontend integration

---

## Running the Examples

### Run All Examples

```bash
cd dataverse-examples
mvn exec:java -Dexec.mainClass="io.dataverse.examples.AllExamplesRunner"
```

### Run Specific Example

```bash
# Read Replicas example
mvn exec:java -Dexec.mainClass="io.dataverse.examples.ReadReplicasExample"

# Native Query example
mvn exec:java -Dexec.mainClass="io.dataverse.examples.NativeQueryExample"

# Lazy/Eager Loading example
mvn exec:java -Dexec.mainClass="io.dataverse.examples.LazyEagerLoadingExample"

# And so on...
```

### Run by Number

```bash
# Run example #1 (Read Replicas)
mvn exec:java -Dexec.mainClass="io.dataverse.examples.AllExamplesRunner" -Dexec.args="1"

# Run example #5 (Distributed Tracing)
mvn exec:java -Dexec.mainClass="io.dataverse.examples.AllExamplesRunner" -Dexec.args="5"
```

## Example Structure

Each example follows this structure:

```java
public class FeatureExample {
  public static void main(String[] args) {
    example1_BasicUsage();
    example2_AdvancedUsage();
    example3_RealWorldScenario();
    // ... more examples
  }

  static void example1_BasicUsage() {
    System.out.println("Example 1: Basic Usage");
    System.out.println("Use Case: ...");

    // Code demonstration
    // Expected output
    // Explanation
  }
}
```

## Learning Path

### Beginner
Start with these examples:
1. Read Replicas - Simple routing concepts
2. Native Query - Basic SQL usage
3. Hot Reload - Configuration management

### Intermediate
Progress to:
4. Lazy/Eager Loading - Performance optimization
5. Schema Migration - Database evolution
6. GraphQL - API generation

### Advanced
Master these topics:
7. Distributed Tracing - System observability
8. Cross-Datasource Joins - Complex data operations
9. Data Synchronization - Multi-database coordination

## Performance Comparisons

Many examples include performance comparisons:

```
Without DataVerse: 5000ms, 101 queries
With DataVerse:    100ms, 2 queries
→ 50x faster!
```

## Best Practices

Each example demonstrates:
- ✅ Correct usage patterns
- ✅ Performance optimizations
- ✅ Error handling
- ✅ Production-ready code
- ✅ Type safety
- ✅ Zero external dependencies (core)

## Code Style

All examples use:
- Java 21 features (records, pattern matching, text blocks)
- Virtual threads for concurrency
- Immutable objects where possible
- Builder pattern for configuration
- Clear variable names
- Comprehensive comments

## Integration

To integrate any feature into your application:

1. **Review the example** - Understand the use case
2. **Copy the pattern** - Use similar structure
3. **Customize** - Adapt to your needs
4. **Test** - Validate in your environment

## Additional Resources

- **Documentation**: See README.md in project root
- **Source Code**: All feature implementations in `dataverse-core/`
- **Tests**: Unit tests in `dataverse-core/src/test/`
- **Issues**: Report problems on GitHub

## Contributing

Found a better example or use case? Contributions welcome!

1. Fork the repository
2. Create your feature branch
3. Add your example
4. Submit a pull request

## License

Same as the main DataVerse SDK project.

## Support

- GitHub Issues: https://github.com/dataverse/dataverse-sdk/issues
- Discussions: https://github.com/dataverse/dataverse-sdk/discussions
- Documentation: https://dataverse.io/docs

---

**Happy Coding!** 🚀
