# DataVerse SDK - Basic Example

This example demonstrates basic usage of the DataVerse SDK with DynamoDB.

## What This Example Covers

1. **Adapter Configuration** - How to configure a data source adapter
2. **Adapter Initialization** - How to initialize and health-check an adapter
3. **Repository Creation** - How to create a type-safe repository
4. **CRUD Operations** - How to save, find, and delete entities
5. **Query Builder** - How to build complex queries with the fluent API
6. **Async Operations** - How to use CompletableFuture-based async API
7. **Adapter Capabilities** - How to query adapter capabilities

## Running the Example

### Prerequisites

- Java 21 or later
- Maven 3.9+
- DynamoDB Local (optional, for actual data operations)

### Option 1: Run Without Database (API Demonstration)

```bash
# Build the project
mvn clean package

# Run the example
java -jar target/example-basic-1.0.0-SNAPSHOT.jar
```

This will demonstrate the API without connecting to a real database.

### Option 2: Run With DynamoDB Local

1. **Start DynamoDB Local**:
```bash
# Download and run DynamoDB Local
docker run -p 8000:8000 amazon/dynamodb-local
```

2. **Create the User table**:
```bash
aws dynamodb create-table \
    --table-name User \
    --attribute-definitions AttributeName=id,AttributeType=S \
    --key-schema AttributeName=id,KeyType=HASH \
    --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 \
    --endpoint-url http://localhost:8000
```

3. **Run the example**:
```bash
mvn clean package
java -jar target/example-basic-1.0.0-SNAPSHOT.jar
```

## Code Walkthrough

### 1. Entity Definition

```java
public class User implements Entity<String> {
    private String id;
    private String email;
    private String name;
    private String status;
    private int age;

    // Getters, setters, constructors...
}
```

### 2. Adapter Configuration

```java
AdapterConfig config = AdapterConfig.builder()
    .property("region", "us-east-1")
    .property("endpoint", "http://localhost:8000")
    .property("pool.minSize", 10)
    .property("pool.maxSize", 100)
    .build();
```

### 3. Repository Creation

```java
DataSourceAdapter adapter = new DynamoDBAdapter();
adapter.initialize(config);

Repository<User, String> repository = adapter.createRepository(User.class);
```

### 4. Save Operation

```java
User user = new User("john@example.com", "John Doe", 30);
User saved = repository.save(user);
```

### 5. Query Examples

```java
// Find active users
List<User> users = repository.query()
    .where("status").eq("ACTIVE")
    .orderBy("name").ascending()
    .execute();

// Find users by age range
List<User> users = repository.query()
    .where("age").between(25, 35)
    .and("status").eq("ACTIVE")
    .limit(10)
    .execute();

// Find with email domain filter
List<User> users = repository.query()
    .where("email").endsWith("@example.com")
    .select("id", "name", "email")
    .execute();
```

### 6. Async Operations

```java
CompletableFuture<User> futureUser = repository.saveAsync(user);

futureUser.thenAccept(saved ->
    System.out.println("User saved: " + saved)
);
```

## Key Takeaways

1. **Type-Safe**: All operations are type-safe at compile time
2. **Fluent API**: Readable, chainable query building
3. **Framework Agnostic**: No Spring or other framework required
4. **Zero Dependencies**: Core module has zero external dependencies
5. **Virtual Threads**: Built-in support for Java 21 virtual threads
6. **Extensible**: Easy to add support for new data sources

## Next Steps

- See `example-multi-source` for using multiple data sources together
- See `example-spring-boot` for Spring Boot integration
- Check the API documentation for advanced features

## Troubleshooting

**Connection refused**: Make sure DynamoDB Local is running on port 8000

**Table doesn't exist**: Create the User table using the AWS CLI command above

**ClassNotFoundException**: Make sure all dependencies are in the classpath
