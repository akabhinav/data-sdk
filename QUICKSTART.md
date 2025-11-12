# DataVerse SDK - Quick Start Guide

Get up and running with DataVerse SDK in 5 minutes!

## 🚀 Super Quick Start

### 1. Prerequisites
- Java 21+
- Maven 3.8+
- Docker & Docker Compose (optional, but recommended)

### 2. Clone & Setup
```bash
git clone https://github.com/yourusername/data-sdk.git
cd data-sdk

# Start test databases (optional)
docker-compose up -d

# Build project
mvn clean install
```

### 3. Run Examples
```bash
cd dataverse-examples

# Run all examples
mvn exec:java -Dexec.mainClass="io.dataverse.examples.AllExamplesRunner"

# Or run specific example
mvn exec:java -Dexec.mainClass="io.dataverse.examples.ReadReplicasExample"
```

### 4. Run Tests
```bash
# From project root
mvn test
```

**That's it!** 🎉

---

## 📋 Option 1: Automated Testing (Easiest)

Use the automated test script:

```bash
# Make script executable (first time only)
chmod +x test-all.sh

# Run interactive test menu
./test-all.sh
```

**Menu Options:**
1. Full test suite - Everything
2. Unit tests only - Fast validation
3. Examples only - See features in action
4. Test specific feature - Focus on one
5. Quick test - Build + unit tests

---

## 📋 Option 2: Docker Setup (Recommended)

### Start Databases
```bash
docker-compose up -d
```

**Includes:**
- PostgreSQL (port 5432)
- PostgreSQL Replica 1 (port 5433)
- PostgreSQL Replica 2 (port 5434)
- MongoDB (port 27017)
- Redis (port 6379)

### Check Status
```bash
docker-compose ps
```

### View Logs
```bash
docker-compose logs -f
```

### Stop Databases
```bash
docker-compose down

# Remove data volumes
docker-compose down -v
```

---

## 📋 Option 3: Manual Setup

### Install PostgreSQL
```bash
# macOS
brew install postgresql@15
brew services start postgresql@15

# Ubuntu/Debian
sudo apt install postgresql-15
sudo systemctl start postgresql

# Windows
# Download from https://www.postgresql.org/download/
```

### Create Database
```bash
psql -U postgres
CREATE DATABASE dataverse_test;
CREATE USER dataverse WITH PASSWORD 'dataverse123';
GRANT ALL PRIVILEGES ON DATABASE dataverse_test TO dataverse;
\q
```

### Install MongoDB (Optional)
```bash
# macOS
brew install mongodb-community@7.0
brew services start mongodb-community

# Ubuntu/Debian
# See: https://docs.mongodb.com/manual/installation/

# Windows
# Download from https://www.mongodb.com/try/download/community
```

---

## 🧪 Running Tests

### All Tests
```bash
mvn test
```

**Expected Output:**
```
Tests run: 120+, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Specific Feature
```bash
# Feature #9: Read Replicas
mvn test -Dtest=ReplicaRouterTest

# Feature #19: GraphQL
mvn test -Dtest=GraphQLSchemaGeneratorTest,GraphQLExecutorTest
```

### With Coverage
```bash
mvn clean test jacoco:report

# View report
open target/site/jacoco/index.html
```

---

## 💡 Running Examples

### All Examples
```bash
cd dataverse-examples
mvn exec:java -Dexec.mainClass="io.dataverse.examples.AllExamplesRunner"
```

### Individual Examples

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

### By Number
```bash
# Run example #1
mvn exec:java -Dexec.mainClass="io.dataverse.examples.AllExamplesRunner" -Dexec.args="1"
```

---

## 🎯 Quick Feature Test

Test a specific feature end-to-end:

```bash
# Example: Test GraphQL feature
mvn test -Dtest=GraphQLSchemaGeneratorTest
cd dataverse-examples
mvn exec:java -Dexec.mainClass="io.dataverse.examples.GraphQLExample"
cd ..
```

---

## 📊 Verify Installation

```bash
# Check Java
java -version
# Should show: version "21" or higher

# Check Maven
mvn -version
# Should show: Apache Maven 3.8+

# Check Docker (optional)
docker --version
docker-compose --version

# Build project
mvn clean install

# Run quick test
mvn test -Dtest=GraphQLSchemaGeneratorTest

# Success! ✅
```

---

## 🆘 Troubleshooting

### Build fails
```bash
# Clean everything
mvn clean
rm -rf ~/.m2/repository/io/dataverse

# Rebuild
mvn clean install -U
```

### Database connection issues
```bash
# Restart databases
docker-compose restart

# Check status
docker-compose ps

# Check logs
docker-compose logs postgres
docker-compose logs mongodb
```

### Port already in use
```bash
# Find process using port
lsof -i :5432

# Kill process or change port in docker-compose.yml
```

### Tests fail
```bash
# Run with verbose output
mvn test -X

# Run specific test
mvn test -Dtest=ClassName#methodName
```

---

## 📚 Next Steps

1. **Read Documentation**
   - `README.md` - Project overview
   - `TESTING_GUIDE.md` - Detailed testing instructions
   - `dataverse-examples/README.md` - Example documentation

2. **Explore Examples**
   - Browse `dataverse-examples/src/main/java/io/dataverse/examples/`
   - Each example has 6-7 use cases

3. **Review Tests**
   - Check `dataverse-core/src/test/java/`
   - 120+ unit tests to learn from

4. **Integrate into Your App**
   - Add Maven dependency
   - Configure data sources
   - Start using features!

---

## 🔗 Resources

- **Full Testing Guide**: `TESTING_GUIDE.md`
- **Examples**: `dataverse-examples/`
- **Source Code**: `dataverse-core/src/main/java/`
- **Tests**: `dataverse-core/src/test/java/`

---

## ⚡ TL;DR

```bash
# Setup
docker-compose up -d
mvn clean install

# Test
mvn test

# Examples
cd dataverse-examples
mvn exec:java -Dexec.mainClass="io.dataverse.examples.AllExamplesRunner"

# Done! 🎉
```

---

**Happy Coding!** 🚀
