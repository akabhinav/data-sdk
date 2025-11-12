# Contributing to DataVerse SDK

Thank you for your interest in contributing to DataVerse SDK! This document provides guidelines and instructions for contributing.

## Table of Contents

1. [Code of Conduct](#code-of-conduct)
2. [Getting Started](#getting-started)
3. [Development Setup](#development-setup)
4. [Project Structure](#project-structure)
5. [Coding Standards](#coding-standards)
6. [Testing Requirements](#testing-requirements)
7. [Pull Request Process](#pull-request-process)
8. [Creating Custom Adapters](#creating-custom-adapters)

## Code of Conduct

### Our Pledge

We pledge to make participation in our project a harassment-free experience for everyone, regardless of age, body size, disability, ethnicity, gender identity and expression, level of experience, education, socio-economic status, nationality, personal appearance, race, religion, or sexual identity and orientation.

### Our Standards

**Positive behaviors**:
- Using welcoming and inclusive language
- Being respectful of differing viewpoints
- Gracefully accepting constructive criticism
- Focusing on what is best for the community

**Unacceptable behaviors**:
- Trolling, insulting/derogatory comments, and personal attacks
- Public or private harassment
- Publishing others' private information without permission
- Other conduct which could reasonably be considered inappropriate

## Getting Started

### Prerequisites

- **Java 21** (OpenJDK, Oracle JDK, Amazon Corretto, or Azul Zulu)
- **Maven 3.9+**
- **Git 2.x+**
- **IDE** (IntelliJ IDEA recommended, but any Java IDE works)

### Fork and Clone

```bash
# Fork the repository on GitHub
# Then clone your fork
git clone https://github.com/YOUR_USERNAME/dataverse-sdk.git
cd dataverse-sdk

# Add upstream remote
git remote add upstream https://github.com/dataverse-sdk/dataverse-sdk.git
```

## Development Setup

### Build the Project

```bash
# Full build with tests
mvn clean install

# Fast build (skip tests)
mvn clean install -Pfast

# Build specific module
cd dataverse-core
mvn clean install
```

### Run Tests

```bash
# Run unit tests only
mvn test

# Run integration tests
mvn verify -Pintegration-test

# Run with coverage report
mvn clean verify -Pcoverage
open target/site/jacoco/index.html

# Run specific test
mvn test -Dtest=RepositoryTest
```

### Code Quality Checks

```bash
# Format code
mvn spotless:apply

# Check code style
mvn spotless:check

# Run static analysis
mvn checkstyle:check
mvn spotbugs:check

# Run all quality checks
mvn clean verify -Pci
```

### IDE Setup

#### IntelliJ IDEA

1. **Import Project**:
   - File → Open → Select `pom.xml`
   - Choose "Open as Project"
   - Wait for Maven import to complete

2. **Configure Java 21**:
   - File → Project Structure → Project SDK → Add JDK → Select Java 21

3. **Code Style**:
   - Install "google-java-format" plugin
   - Settings → Tools → Actions on Save → Enable "Reformat code"

4. **Enable Annotation Processing**:
   - Settings → Build, Execution, Deployment → Compiler → Annotation Processors
   - Check "Enable annotation processing"

#### Visual Studio Code

1. **Install Extensions**:
   - Language Support for Java(TM) by Red Hat
   - Debugger for Java
   - Maven for Java
   - Test Runner for Java

2. **Configure Java**:
   ```json
   {
     "java.jdt.ls.java.home": "/path/to/java-21"
   }
   ```

## Project Structure

```
dataverse-sdk/
├── dataverse-bom/                 # Bill of Materials
├── dataverse-core/                # Core module (ZERO dependencies!)
│   ├── src/main/java/
│   │   └── io/dataverse/
│   │       ├── api/               # Public API interfaces
│   │       ├── spi/               # Service Provider Interfaces
│   │       ├── core/              # Core implementations
│   │       └── exception/         # Exception classes
│   └── src/test/java/             # Unit tests
├── dataverse-adapters/            # Adapter modules
│   ├── dataverse-adapter-dynamodb/
│   ├── dataverse-adapter-mongodb/
│   └── ...
├── dataverse-cache/               # Cache providers
├── dataverse-metrics/             # Metrics collectors
├── dataverse-spring-boot/         # Spring Boot integration
├── dataverse-test-support/        # Test utilities
└── dataverse-examples/            # Examples
```

### Module Dependency Rules

**CRITICAL**: Follow these rules strictly:

1. **Core Module**:
   - ✅ MAY depend on: Java 21 standard library only
   - ❌ MUST NOT depend on: Any external library

2. **Adapter Modules**:
   - ✅ MAY depend on: dataverse-core + native client library
   - ❌ MUST NOT depend on: Other adapters

3. **Optional Modules** (cache, metrics, Spring):
   - ✅ MAY depend on: dataverse-core + their specific libraries
   - ❌ MUST NOT depend on: Adapters

**Validation**: Run `mvn dependency:analyze` to check dependencies.

## Coding Standards

### Java Code Style

We use **Google Java Style** with minor modifications:

```java
// ✅ Good: Clear naming, proper indentation, JavaDoc
/**
 * Retrieves a user by their unique identifier.
 *
 * @param id the user ID, must not be {@code null}
 * @return an Optional containing the user if found
 * @throws IllegalArgumentException if id is {@code null}
 */
public Optional<User> findById(String id) {
  if (id == null) {
    throw new IllegalArgumentException("User ID must not be null");
  }
  return repository.findById(id);
}

// ❌ Bad: No JavaDoc, poor naming, missing validation
public Optional<User> get(String i) {
  return repository.findById(i);
}
```

### Naming Conventions

- **Classes**: `PascalCase` (e.g., `DynamoDBAdapter`, `QueryBuilder`)
- **Interfaces**: `PascalCase` (e.g., `Repository`, `DataSourceAdapter`)
- **Methods**: `camelCase` (e.g., `findById`, `createRepository`)
- **Constants**: `UPPER_SNAKE_CASE` (e.g., `MAX_RETRY_ATTEMPTS`)
- **Packages**: `lowercase` (e.g., `io.dataverse.adapter.dynamodb`)

### Code Organization

```java
public class MyClass {
  // 1. Static fields
  private static final String CONSTANT = "value";

  // 2. Instance fields
  private final String requiredField;
  private Optional<String> optionalField = Optional.empty();

  // 3. Constructor(s)
  public MyClass(String requiredField) {
    this.requiredField = requiredField;
  }

  // 4. Public methods
  public void doSomething() { }

  // 5. Package-private methods
  void helperMethod() { }

  // 6. Private methods
  private void internalHelper() { }

  // 7. Inner classes/enums
  private enum State { ACTIVE, INACTIVE }
}
```

### JavaDoc Requirements

**ALL public APIs MUST have JavaDoc:**

```java
/**
 * Brief one-line description.
 *
 * <p>Detailed description with multiple paragraphs if needed.
 * Explain purpose, behavior, and usage.
 *
 * <p><strong>Example Usage:</strong>
 * <pre>{@code
 * Repository<User, String> repo = adapter.createRepository(User.class);
 * User user = repo.findById("123").orElseThrow();
 * }</pre>
 *
 * @param <T> the entity type
 * @param entityClass the entity class, must not be {@code null}
 * @return a repository instance, never {@code null}
 * @throws IllegalArgumentException if entityClass is {@code null}
 * @throws AdapterException if repository creation fails
 * @since 1.0.0
 */
public <T extends Entity<ID>, ID extends Serializable>
    Repository<T, ID> createRepository(Class<T> entityClass);
```

### Error Handling

```java
// ✅ Good: Specific exceptions, clear messages
public User findById(String id) {
  if (id == null || id.isBlank()) {
    throw new IllegalArgumentException("User ID must not be null or blank");
  }

  try {
    return client.getUser(id);
  } catch (NetworkException e) {
    throw new ConnectionException("Failed to retrieve user: " + id, e);
  }
}

// ❌ Bad: Generic exceptions, poor error messages
public User findById(String id) throws Exception {
  return client.getUser(id);  // What if it fails?
}
```

### Null Safety

```java
// ✅ Good: Use Optional for nullable values
public Optional<User> findById(String id) {
  User user = repository.get(id);
  return Optional.ofNullable(user);
}

// ✅ Good: Fail fast on null parameters
public void save(User user) {
  Objects.requireNonNull(user, "User must not be null");
  repository.save(user);
}

// ❌ Bad: Returning null
public User findById(String id) {
  return null;  // Don't do this!
}
```

## Testing Requirements

### Test Coverage Requirements

| Module Type | Line Coverage | Branch Coverage |
|-------------|---------------|-----------------|
| Core | > 90% | > 85% |
| Adapters | > 80% | > 75% |
| Optional modules | > 75% | > 70% |

### Unit Test Guidelines

```java
// ✅ Good: Clear test structure, descriptive name
@Test
@DisplayName("Should throw IllegalArgumentException when user ID is null")
void shouldThrowExceptionWhenUserIdIsNull() {
  // Given
  Repository<User, String> repository = createRepository();

  // When & Then
  assertThatThrownBy(() -> repository.findById(null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("User ID must not be null");
}

// ✅ Good: Test edge cases
@Test
void shouldHandleEmptyResultsGracefully() {
  // Given
  Repository<User, String> repository = createRepository();

  // When
  List<User> users = repository.query()
      .where("status").eq("NONEXISTENT")
      .execute();

  // Then
  assertThat(users).isEmpty();
}
```

### Integration Test Guidelines

```java
@Testcontainers
class DynamoDBAdapterIntegrationTest {

  @Container
  static LocalStackContainer localstack = new LocalStackContainer(
      DockerImageName.parse("localstack/localstack:latest"))
      .withServices(LocalStackContainer.Service.DYNAMODB);

  private DynamoDBAdapter adapter;

  @BeforeEach
  void setUp() {
    AdapterConfig config = AdapterConfig.builder()
        .property("endpoint", localstack.getEndpointOverride(Service.DYNAMODB))
        .property("region", localstack.getRegion())
        .build();

    adapter = new DynamoDBAdapter();
    adapter.initialize(config);
  }

  @Test
  void shouldSaveAndRetrieveEntity() {
    // Integration test implementation
  }
}
```

## Pull Request Process

### Before Submitting

1. **Create a feature branch**:
   ```bash
   git checkout -b feature/my-awesome-feature
   ```

2. **Make your changes** following coding standards

3. **Run all checks**:
   ```bash
   # Format code
   mvn spotless:apply

   # Run tests
   mvn clean verify

   # Check coverage
   mvn verify -Pcoverage
   ```

4. **Commit with clear messages**:
   ```bash
   git commit -m "feat: add caching support to QueryBuilder"
   git commit -m "fix: resolve connection leak in DynamoDBAdapter"
   git commit -m "docs: update README with new examples"
   ```

   **Commit Message Format**:
   - `feat`: New feature
   - `fix`: Bug fix
   - `docs`: Documentation changes
   - `test`: Adding/updating tests
   - `refactor`: Code refactoring
   - `perf`: Performance improvement
   - `chore`: Build/tooling changes

5. **Push to your fork**:
   ```bash
   git push origin feature/my-awesome-feature
   ```

### Submitting the PR

1. Go to GitHub and create a Pull Request
2. Fill in the PR template with:
   - Description of changes
   - Related issues (if any)
   - Testing performed
   - Screenshots (if UI changes)

3. Wait for CI checks to pass
4. Address review comments
5. Squash commits if requested

### PR Review Criteria

Your PR will be reviewed for:

- ✅ Adherence to coding standards
- ✅ Sufficient test coverage
- ✅ Clear, descriptive commits
- ✅ Updated documentation
- ✅ No breaking changes (or clearly marked)
- ✅ Performance considerations
- ✅ Security implications

## Creating Custom Adapters

### Step-by-Step Guide

1. **Create a new module**:
   ```bash
   cd dataverse-adapters
   mkdir dataverse-adapter-mycustomdb
   ```

2. **Create pom.xml**:
   ```xml
   <dependencies>
       <!-- ONLY dataverse-core + your native client -->
       <dependency>
           <groupId>io.dataverse</groupId>
           <artifactId>dataverse-core</artifactId>
       </dependency>
       <dependency>
           <groupId>com.example</groupId>
           <artifactId>mycustomdb-client</artifactId>
       </dependency>
   </dependencies>
   ```

3. **Implement DataSourceAdapter**:
   ```java
   public class MyCustomDBAdapter implements DataSourceAdapter {
       @Override
       public String getAdapterId() {
           return "mycustomdb";
       }

       @Override
       public void initialize(AdapterConfig config) {
           // Initialize your client
       }

       // Implement other SPI methods...
   }
   ```

4. **Register via SPI**:
   ```
   File: src/main/resources/META-INF/services/io.dataverse.spi.DataSourceAdapter
   Content: com.example.MyCustomDBAdapter
   ```

5. **Write tests**:
   ```java
   @Test
   void shouldInitializeAdapter() {
       AdapterConfig config = AdapterConfig.builder()
           .property("endpoint", "localhost:9000")
           .build();

       MyCustomDBAdapter adapter = new MyCustomDBAdapter();
       adapter.initialize(config);

       assertThat(adapter.isHealthy()).isTrue();
   }
   ```

6. **Submit a PR** to add your adapter to the main repository!

## Questions?

- **GitHub Discussions**: Ask questions, share ideas
- **GitHub Issues**: Report bugs, request features
- **Email**: dev@dataverse-sdk.io

## Thank You!

Your contributions make DataVerse SDK better for everyone. We appreciate your time and effort!

---

**Happy Coding! 🚀**
