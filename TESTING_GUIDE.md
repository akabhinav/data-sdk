# Testing DataVerse SDK on Local Computer

Complete guide to test all 20 features of DataVerse SDK on your local machine.

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Quick Start (Docker)](#quick-start-docker)
3. [Manual Setup](#manual-setup)
4. [Running Examples](#running-examples)
5. [Running Unit Tests](#running-unit-tests)
6. [Integration Testing](#integration-testing)
7. [Feature-by-Feature Testing](#feature-by-feature-testing)
8. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software

1. **Java 21 or later**
   ```bash
   java -version
   # Should show: openjdk version "21" or higher
   ```

2. **Maven 3.8+**
   ```bash
   mvn -version
   # Should show: Apache Maven 3.8.x or higher
   ```

3. **Git**
   ```bash
   git --version
   ```

### Optional (for full integration testing)

- **Docker & Docker Compose** (recommended for easy database setup)
- **PostgreSQL 15+** (if not using Docker)
- **MongoDB 7+** (if not using Docker)
- **Redis 7+** (if not using Docker)

---

## Quick Start (Docker)

**Easiest way to test all features!**

### Step 1: Clone Repository

```bash
git clone https://github.com/yourusername/data-sdk.git
cd data-sdk
```

### Step 2: Create Docker Compose File

Create `docker-compose.yml` in project root:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: dataverse-postgres
    environment:
      POSTGRES_DB: dataverse_test
      POSTGRES_USER: dataverse
      POSTGRES_PASSWORD: dataverse123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U dataverse"]
      interval: 5s
      timeout: 5s
      retries: 5

  mongodb:
    image: mongo:7
    container_name: dataverse-mongo
    environment:
      MONGO_INITDB_ROOT_USERNAME: dataverse
      MONGO_INITDB_ROOT_PASSWORD: dataverse123
      MONGO_INITDB_DATABASE: dataverse_test
    ports:
      - "27017:27017"
    volumes:
      - mongo_data:/data/db
    healthcheck:
      test: echo 'db.runCommand("ping").ok' | mongosh localhost:27017/test --quiet
      interval: 5s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: dataverse-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
  mongo_data:
  redis_data:
```

### Step 3: Start Databases

```bash
# Start all databases
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f
```

### Step 4: Build Project

```bash
mvn clean install -DskipTests
```

### Step 5: Run All Examples

```bash
cd dataverse-examples

# Run all examples
mvn exec:java -Dexec.mainClass="io.dataverse.examples.AllExamplesRunner"

# Or run specific example
mvn exec:java -Dexec.mainClass="io.dataverse.examples.ReadReplicasExample"
```

### Step 6: Run All Tests

```bash
# Run all unit tests
mvn test

# Run specific test
mvn test -Dtest=GraphQLSchemaGeneratorTest

# Run with verbose output
mvn test -X
```

### Step 7: Cleanup

```bash
# Stop databases
docker-compose down

# Remove volumes (clean slate)
docker-compose down -v
```

---

## Manual Setup

If you prefer not to use Docker:

### 1. Install PostgreSQL

**macOS:**
```bash
brew install postgresql@15
brew services start postgresql@15
```

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install postgresql-15
sudo systemctl start postgresql
```

**Windows:**
Download from https://www.postgresql.org/download/windows/

**Create Database:**
```bash
psql -U postgres
CREATE DATABASE dataverse_test;
CREATE USER dataverse WITH PASSWORD 'dataverse123';
GRANT ALL PRIVILEGES ON DATABASE dataverse_test TO dataverse;
\q
```

### 2. Install MongoDB

**macOS:**
```bash
brew tap mongodb/brew
brew install mongodb-community@7.0
brew services start mongodb-community@7.0
```

**Ubuntu/Debian:**
```bash
wget -qO - https://www.mongodb.org/static/pgp/server-7.0.asc | sudo apt-key add -
echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu jammy/mongodb-org/7.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list
sudo apt update
sudo apt install -y mongodb-org
sudo systemctl start mongod
```

**Windows:**
Download from https://www.mongodb.com/try/download/community

**Create Database:**
```bash
mongosh
use dataverse_test
db.createUser({
  user: "dataverse",
  pwd: "dataverse123",
  roles: ["readWrite"]
})
```

### 3. Install Redis (Optional)

**macOS:**
```bash
brew install redis
brew services start redis
```

**Ubuntu/Debian:**
```bash
sudo apt install redis-server
sudo systemctl start redis
```

### 4. Configure Connection

Create `dataverse-examples/src/main/resources/application.properties`:

```properties
# PostgreSQL
dataverse.postgresql.url=jdbc:postgresql://localhost:5432/dataverse_test
dataverse.postgresql.username=dataverse
dataverse.postgresql.password=dataverse123

# MongoDB
dataverse.mongodb.url=mongodb://dataverse:dataverse123@localhost:27017/dataverse_test
dataverse.mongodb.database=dataverse_test

# Redis
dataverse.redis.host=localhost
dataverse.redis.port=6379
```

---

## Running Examples

### Option 1: Run All Examples

```bash
cd dataverse-examples

mvn exec:java -Dexec.mainClass="io.dataverse.examples.AllExamplesRunner"
```

**Expected Output:**
```
╔════════════════════════════════════════════════════════════════╗
║            DataVerse SDK - Feature Examples                   ║
╚════════════════════════════════════════════════════════════════╝

Available Examples:
  1. ReadReplicasExample          - Load balancing with automatic failover
  2. NativeQueryExample           - Type-safe native SQL queries
  ...

Running all examples...
```

### Option 2: Run Individual Examples

```bash
# Feature #9: Read Replicas
mvn exec:java -Dexec.mainClass="io.dataverse.examples.ReadReplicasExample"

# Feature #3: Native Query
mvn exec:java -Dexec.mainClass="io.dataverse.examples.NativeQueryExample"

# Feature #8: Lazy/Eager Loading
mvn exec:java -Dexec.mainClass="io.dataverse.examples.LazyEagerLoadingExample"

# Feature #16: Schema Migration
mvn exec:java -Dexec.mainClass="io.dataverse.examples.SchemaMigrationExample"

# Feature #14: Distributed Tracing
mvn exec:java -Dexec.mainClass="io.dataverse.examples.DistributedTracingExample"

# Feature #5: Cross-Datasource Joins
mvn exec:java -Dexec.mainClass="io.dataverse.examples.CrossDatasourceJoinsExample"

# Feature #17: Data Synchronization
mvn exec:java -Dexec.mainClass="io.dataverse.examples.DataSynchronizationExample"

# Feature #20: Hot Reload
mvn exec:java -Dexec.mainClass="io.dataverse.examples.HotReloadExample"

# Feature #19: GraphQL
mvn exec:java -Dexec.mainClass="io.dataverse.examples.GraphQLExample"
```

### Option 3: Run by Number

```bash
cd dataverse-examples

# Run example #1 (Read Replicas)
mvn exec:java -Dexec.mainClass="io.dataverse.examples.AllExamplesRunner" -Dexec.args="1"

# Run example #5 (Distributed Tracing)
mvn exec:java -Dexec.mainClass="io.dataverse.examples.AllExamplesRunner" -Dexec.args="5"
```

---

## Running Unit Tests

### Run All Tests

```bash
# From project root
mvn test

# Skip tests during build
mvn clean install -DskipTests

# Run tests with coverage
mvn clean test jacoco:report
```

### Run Specific Test Class

```bash
# Run single test class
mvn test -Dtest=GraphQLSchemaGeneratorTest

# Run multiple test classes
mvn test -Dtest=GraphQLSchemaGeneratorTest,GraphQLExecutorTest

# Run specific test method
mvn test -Dtest=GraphQLSchemaGeneratorTest#testGenerateSchema
```

### Run Tests by Feature

```bash
# Feature #9: Read Replicas
mvn test -Dtest=ReplicaRouterTest,ReplicaHealthCheckerTest

# Feature #3: Native Query
mvn test -Dtest=NativeQueryExecutorTest,ParameterBinderTest

# Feature #8: Lazy/Eager Loading
mvn test -Dtest=FetchPlanTest

# Feature #16: Schema Migration
mvn test -Dtest=MigrationManagerTest,MigrationLoaderTest

# Feature #14: Distributed Tracing
mvn test -Dtest=TraceContextTest,TracerTest

# Feature #5: Cross-Datasource Joins
mvn test -Dtest=JoinBuilderTest,InnerJoinOperationTest

# Feature #17: Data Synchronization
mvn test -Dtest=SyncManagerTest

# Feature #20: Hot Reload
mvn test -Dtest=HotReloadManagerTest

# Feature #19: GraphQL
mvn test -Dtest=GraphQLSchemaGeneratorTest,GraphQLExecutorTest
```

### Test Output

```bash
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running io.dataverse.core.graphql.GraphQLSchemaGeneratorTest
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 120, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

## Integration Testing

### Create Integration Test Setup

Create `dataverse-integration-tests/src/test/resources/test-config.properties`:

```properties
# Test databases
test.postgresql.url=jdbc:postgresql://localhost:5432/dataverse_test
test.mongodb.url=mongodb://localhost:27017/dataverse_test
test.redis.host=localhost
```

### Run Integration Tests

```bash
# Run integration tests (requires databases)
mvn verify -Pintegration-tests

# Skip integration tests
mvn install -DskipITs
```

### Example Integration Test

Create a test that uses real databases:

```bash
cd dataverse-integration-tests
mvn test -Dtest=CrossDatasourceJoinIntegrationTest
```

---

## Feature-by-Feature Testing

### Feature #9: Read Replicas

**Test:**
```bash
# 1. Run example
mvn exec:java -Dexec.mainClass="io.dataverse.examples.ReadReplicasExample"

# 2. Run unit tests
mvn test -Dtest=ReplicaRouterTest

# 3. Verify output shows:
#    - Round robin routing
#    - Geographic routing
#    - Automatic failover
```

**Expected Output:**
```
Example 1: Basic Round Robin Routing
Query 1 -> replica1.db.example.com
Query 2 -> replica2.db.example.com
Query 3 -> replica3.db.example.com
✓ Queries evenly distributed
```

---

### Feature #3: Native Query

**Test:**
```bash
# 1. Run example
mvn exec:java -Dexec.mainClass="io.dataverse.examples.NativeQueryExample"

# 2. Run unit tests
mvn test -Dtest=ParameterBinderTest

# 3. Verify parameter binding works
```

**Expected Output:**
```
Example 1: Basic Named Parameters
Original: SELECT * FROM users WHERE email = :email
Processed: SELECT * FROM users WHERE email = ?
✓ Parameter bound successfully
```

---

### Feature #8: Lazy/Eager Loading

**Test:**
```bash
# 1. Run example
mvn exec:java -Dexec.mainClass="io.dataverse.examples.LazyEagerLoadingExample"

# 2. Run unit tests
mvn test -Dtest=FetchPlanTest

# 3. Verify N+1 solution
```

**Expected Output:**
```
Example 2: Eager Loading Prevents N+1
Without: 101 queries, ~5000ms
With: 2 queries, ~100ms
✓ 50x faster!
```

---

### Feature #16: Schema Migration

**Test:**
```bash
# 1. Create test migrations
mkdir -p test-migrations
echo "CREATE TABLE test_users (id SERIAL PRIMARY KEY);" > test-migrations/V1__Create_test_table.sql

# 2. Run example
mvn exec:java -Dexec.mainClass="io.dataverse.examples.SchemaMigrationExample"

# 3. Run unit tests
mvn test -Dtest=MigrationManagerTest

# 4. Verify migration tracking table
psql -U dataverse -d dataverse_test -c "SELECT * FROM schema_migrations;"
```

---

### Feature #14: Distributed Tracing

**Test:**
```bash
# 1. Run example
mvn exec:java -Dexec.mainClass="io.dataverse.examples.DistributedTracingExample"

# 2. Run unit tests
mvn test -Dtest=TraceContextTest,TracerTest

# 3. Verify trace context propagation
```

**Expected Output:**
```
Example 4: W3C Context Propagation
HTTP Headers:
  traceparent: 00-{trace-id}-{span-id}-01
✓ Context propagated successfully
```

---

### Feature #5: Cross-Datasource Joins

**Test:**
```bash
# 1. Ensure PostgreSQL and MongoDB are running
docker-compose ps

# 2. Run example
mvn exec:java -Dexec.mainClass="io.dataverse.examples.CrossDatasourceJoinsExample"

# 3. Run unit tests
mvn test -Dtest=JoinBuilderTest

# 4. Verify hash join algorithm
```

**Expected Output:**
```
Example 1: Basic Inner Join
PostgreSQL users: 3 records
MongoDB orders: 4 records
Inner join result: 2 records
✓ Hash Join O(L+R) completed
```

---

### Feature #17: Data Synchronization

**Test:**
```bash
# 1. Setup two databases
# Already done with docker-compose

# 2. Run example
mvn exec:java -Dexec.mainClass="io.dataverse.examples.DataSynchronizationExample"

# 3. Run unit tests
mvn test -Dtest=SyncManagerTest

# 4. Verify sync results
```

**Expected Output:**
```
Example 1: One-Way Synchronization
PostgreSQL → MongoDB
✓ 25 records synchronized
✓ Duration: 450ms
```

---

### Feature #20: Hot Reload

**Test:**
```bash
# 1. Create config directory
mkdir -p config
echo "db.pool.size=10" > config/database.properties

# 2. Run example (in background)
mvn exec:java -Dexec.mainClass="io.dataverse.examples.HotReloadExample" &

# 3. Modify config
echo "db.pool.size=20" > config/database.properties

# 4. Verify reload detected
# Check console output for: "✓ Configuration reloaded!"

# 5. Run unit tests
mvn test -Dtest=HotReloadManagerTest
```

---

### Feature #19: GraphQL

**Test:**
```bash
# 1. Run example
mvn exec:java -Dexec.mainClass="io.dataverse.examples.GraphQLExample"

# 2. Run unit tests
mvn test -Dtest=GraphQLSchemaGeneratorTest,GraphQLExecutorTest

# 3. Verify schema generation
```

**Expected Output:**
```
Example 1: Basic Schema Generation
Generated GraphQL SDL:
type User {
  id: ID!
  name: String
  email: String
}
✓ Schema generated successfully
```

---

## Troubleshooting

### Issue: "Cannot connect to database"

**Solution:**
```bash
# Check database is running
docker-compose ps

# Check PostgreSQL
psql -h localhost -U dataverse -d dataverse_test -c "SELECT 1;"

# Check MongoDB
mongosh --host localhost --port 27017 --eval "db.version()"

# Restart databases
docker-compose restart
```

---

### Issue: "Java version not supported"

**Solution:**
```bash
# Check Java version
java -version

# Should be 21+
# Install Java 21:
# macOS: brew install openjdk@21
# Ubuntu: sudo apt install openjdk-21-jdk
```

---

### Issue: "Maven build fails"

**Solution:**
```bash
# Clean and rebuild
mvn clean install -DskipTests -U

# Clear Maven cache
rm -rf ~/.m2/repository

# Try again
mvn clean install
```

---

### Issue: "Tests fail due to missing dependencies"

**Note:** Core module has ZERO dependencies! Tests use JUnit 5 only.

**Solution:**
```bash
# Update dependencies
mvn dependency:resolve

# Check for conflicts
mvn dependency:tree
```

---

### Issue: "Port already in use"

**Solution:**
```bash
# Check what's using the port
lsof -i :5432  # PostgreSQL
lsof -i :27017 # MongoDB
lsof -i :6379  # Redis

# Kill process or change port in docker-compose.yml
```

---

### Issue: "Examples don't produce output"

**Solution:**
```bash
# Run with verbose logging
mvn exec:java -Dexec.mainClass="io.dataverse.examples.ReadReplicasExample" -X

# Check Java version
java -version  # Must be 21+

# Rebuild project
mvn clean install
```

---

## Quick Test Commands

### Test Everything
```bash
# Start databases
docker-compose up -d

# Build project
mvn clean install

# Run all unit tests
mvn test

# Run all examples
cd dataverse-examples && mvn exec:java -Dexec.mainClass="io.dataverse.examples.AllExamplesRunner"

# Stop databases
docker-compose down
```

### Test Single Feature
```bash
# Example: Test GraphQL feature
mvn test -Dtest=GraphQLSchemaGeneratorTest,GraphQLExecutorTest
mvn exec:java -Dexec.mainClass="io.dataverse.examples.GraphQLExample"
```

---

## Expected Test Results

### Unit Tests
- **Total Tests**: 120+
- **Expected Failures**: 0
- **Expected Errors**: 0
- **Coverage**: >80%

### Examples
- **Total Examples**: 60+
- **All should run**: Without errors
- **Output**: Clear demonstrations

---

## Performance Benchmarks

Run these to verify performance claims:

```bash
# N+1 Query Problem (Feature #8)
# Expected: 50x improvement

# Cross-Database Joins (Feature #5)
# Expected: 15x fewer queries

# Hot Reload (Feature #20)
# Expected: <1s reload time
```

---

## Next Steps

1. ✅ Start databases with Docker Compose
2. ✅ Build project with Maven
3. ✅ Run all unit tests
4. ✅ Run example programs
5. ✅ Experiment with your own data
6. ✅ Integrate into your application

---

## Support

- **Documentation**: See project README
- **Issues**: GitHub Issues
- **Examples**: `dataverse-examples/` directory

Happy Testing! 🚀
