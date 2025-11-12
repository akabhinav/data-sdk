package io.dataverse.examples;

import io.dataverse.core.tracing.SemanticAttributes;
import io.dataverse.core.tracing.Span;
import io.dataverse.core.tracing.TraceContext;
import io.dataverse.core.tracing.Tracer;
import io.dataverse.core.tracing.TracingProvider;

import java.util.Map;

/**
 * Example demonstrating Distributed Tracing with OpenTelemetry (Feature #14).
 *
 * <p>Use Cases:
 * <ul>
 *   <li>Microservice request tracing</li>
 *   <li>Performance bottleneck identification</li>
 *   <li>Cross-service debugging</li>
 *   <li>Database query monitoring</li>
 * </ul>
 *
 * @author DataVerse SDK Team
 */
public class DistributedTracingExample {

  public static void main(String[] args) {
    System.out.println("=== Distributed Tracing Examples ===\n");

    example1_BasicTracing();
    example2_MicroserviceTracing();
    example3_DatabaseOperations();
    example4_W3CContextPropagation();
    example5_PerformanceMonitoring();
    example6_ErrorTracking();
  }

  /**
   * Example 1: Basic Tracing
   *
   * <p>Use Case: Trace a simple operation.
   */
  static void example1_BasicTracing() {
    System.out.println("Example 1: Basic Tracing");
    System.out.println("Use Case: Trace user registration\n");

    Tracer tracer = TracingProvider.getTracer();

    // Start span
    Span span = tracer.startSpan("user.register");
    span.setAttribute(SemanticAttributes.USER_ID, "12345");
    span.setAttribute(SemanticAttributes.USER_EMAIL, "user@example.com");

    try {
      System.out.println("Span started: user.register");
      System.out.println("  Trace ID: " + span.getTraceId());
      System.out.println("  Span ID: " + span.getSpanId());

      // Simulate work
      System.out.println("\n  Executing registration logic...");
      Thread.sleep(50);

      span.addEvent("validation.complete");
      System.out.println("  Event added: validation.complete");

      Thread.sleep(30);

      span.addEvent("database.save");
      System.out.println("  Event added: database.save");

      span.setStatus(Span.Status.OK);
      System.out.println("\n  Status: OK");

    } catch (Exception e) {
      span.recordException(e);
      span.setStatus(Span.Status.ERROR);
      System.out.println("  Status: ERROR");
    } finally {
      span.end();
      System.out.println("\nSpan ended. Duration: 80ms");
    }

    System.out.println("\nResult: Complete request trace captured\n");
    System.out.println("---\n");
  }

  /**
   * Example 2: Microservice Tracing
   *
   * <p>Use Case: Trace request across multiple services.
   */
  static void example2_MicroserviceTracing() {
    System.out.println("Example 2: Microservice Tracing");
    System.out.println("Use Case: E-commerce checkout flow\n");

    System.out.println("Request flow:");
    System.out.println("  Browser → API Gateway → Order Service → Payment Service → Inventory Service");

    Tracer tracer = TracingProvider.getTracer();

    // API Gateway
    System.out.println("\n1. API Gateway:");
    Span rootSpan = tracer.startSpan("http.request");
    rootSpan.setAttribute(SemanticAttributes.HTTP_METHOD, "POST");
    rootSpan.setAttribute(SemanticAttributes.HTTP_URL, "/api/checkout");
    rootSpan.setAttribute(SemanticAttributes.HTTP_STATUS_CODE, 200);
    System.out.println("   Root Trace ID: " + rootSpan.getTraceId());

    // Order Service
    System.out.println("\n2. Order Service:");
    Span orderSpan = tracer.startSpan("order.create", rootSpan);
    orderSpan.setAttribute(SemanticAttributes.SERVICE_NAME, "order-service");
    orderSpan.setAttribute("order.total", 299.99);
    System.out.println("   Parent Span: " + rootSpan.getSpanId());
    System.out.println("   Current Span: " + orderSpan.getSpanId());

    // Payment Service
    System.out.println("\n3. Payment Service:");
    Span paymentSpan = tracer.startSpan("payment.process", orderSpan);
    paymentSpan.setAttribute(SemanticAttributes.SERVICE_NAME, "payment-service");
    paymentSpan.setAttribute("payment.method", "credit_card");
    paymentSpan.setAttribute("payment.amount", 299.99);
    System.out.println("   Parent Span: " + orderSpan.getSpanId());
    System.out.println("   Current Span: " + paymentSpan.getSpanId());

    // Inventory Service
    System.out.println("\n4. Inventory Service:");
    Span inventorySpan = tracer.startSpan("inventory.reserve", orderSpan);
    inventorySpan.setAttribute(SemanticAttributes.SERVICE_NAME, "inventory-service");
    inventorySpan.setAttribute("product.id", "LAPTOP-001");
    inventorySpan.setAttribute("quantity", 1);
    System.out.println("   Parent Span: " + orderSpan.getSpanId());
    System.out.println("   Current Span: " + inventorySpan.getSpanId());

    // Close spans (reverse order)
    inventorySpan.end();
    paymentSpan.end();
    orderSpan.end();
    rootSpan.end();

    System.out.println("\nTrace hierarchy:");
    System.out.println("  http.request (200ms)");
    System.out.println("    ├─ order.create (150ms)");
    System.out.println("    │   ├─ payment.process (80ms)");
    System.out.println("    │   └─ inventory.reserve (40ms)");

    System.out.println("\nResult: Complete cross-service visibility\n");
    System.out.println("---\n");
  }

  /**
   * Example 3: Database Operations
   *
   * <p>Use Case: Trace database queries for performance analysis.
   */
  static void example3_DatabaseOperations() {
    System.out.println("Example 3: Database Operations");
    System.out.println("Use Case: Monitor database query performance\n");

    Tracer tracer = TracingProvider.getTracer();

    // Repository method
    Span repoSpan = tracer.startSpan("repository.findUserOrders");
    repoSpan.setAttribute(SemanticAttributes.USER_ID, "12345");

    // Database query 1: Find user
    System.out.println("Query 1: Find user");
    Span query1 = tracer.startSpan("db.query", repoSpan);
    query1.setAttribute(SemanticAttributes.DB_SYSTEM, "postgresql");
    query1.setAttribute(SemanticAttributes.DB_NAME, "ecommerce");
    query1.setAttribute(SemanticAttributes.DB_OPERATION, "SELECT");
    query1.setAttribute(SemanticAttributes.DB_STATEMENT,
        "SELECT * FROM users WHERE id = ?");
    query1.setAttribute(SemanticAttributes.DB_TABLE, "users");
    System.out.println("  System: postgresql");
    System.out.println("  Table: users");
    System.out.println("  Duration: 12ms");
    query1.end();

    // Database query 2: Find orders
    System.out.println("\nQuery 2: Find orders");
    Span query2 = tracer.startSpan("db.query", repoSpan);
    query2.setAttribute(SemanticAttributes.DB_SYSTEM, "postgresql");
    query2.setAttribute(SemanticAttributes.DB_NAME, "ecommerce");
    query2.setAttribute(SemanticAttributes.DB_OPERATION, "SELECT");
    query2.setAttribute(SemanticAttributes.DB_STATEMENT,
        "SELECT * FROM orders WHERE user_id = ?");
    query2.setAttribute(SemanticAttributes.DB_TABLE, "orders");
    query2.setAttribute("rows.returned", 15);
    System.out.println("  System: postgresql");
    System.out.println("  Table: orders");
    System.out.println("  Rows: 15");
    System.out.println("  Duration: 45ms");
    query2.end();

    repoSpan.end();

    System.out.println("\nTrace visualization:");
    System.out.println("  repository.findUserOrders (57ms)");
    System.out.println("    ├─ db.query [users] (12ms)");
    System.out.println("    └─ db.query [orders] (45ms) ← SLOW!");

    System.out.println("\nInsights:");
    System.out.println("  ✓ Orders query takes 79% of total time");
    System.out.println("  ✓ Returned 15 rows - potential N+1 problem");
    System.out.println("  → Recommendation: Add eager loading");

    System.out.println("\nResult: Identified performance bottleneck\n");
    System.out.println("---\n");
  }

  /**
   * Example 4: W3C Context Propagation
   *
   * <p>Use Case: Propagate trace context across HTTP boundaries.
   */
  static void example4_W3CContextPropagation() {
    System.out.println("Example 4: W3C Context Propagation");
    System.out.println("Use Case: Pass trace context between services\n");

    // Service A: Create trace context
    System.out.println("Service A (User Service):");
    TraceContext context = TraceContext.create();
    context.setSampled(true);

    System.out.println("  Trace ID: " + context.getTraceId());
    System.out.println("  Span ID: " + context.getSpanId());
    System.out.println("  Sampled: " + context.isSampled());

    // Convert to HTTP headers
    Map<String, String> headers = context.toHeaders();
    System.out.println("\n  HTTP Headers:");
    headers.forEach((key, value) ->
        System.out.println("    " + key + ": " + value));

    // Service B: Receive and continue trace
    System.out.println("\nService B (Order Service):");
    TraceContext receivedContext = TraceContext.fromHeaders(headers);
    System.out.println("  Received Trace ID: " + receivedContext.getTraceId());
    System.out.println("  Received Parent Span: " + receivedContext.getSpanId());
    System.out.println("  Continuing same trace!");

    // Create child span
    System.out.println("\n  Creating child span...");
    String newSpanId = TraceContext.generateSpanId();
    System.out.println("  New Span ID: " + newSpanId);
    System.out.println("  Parent Span ID: " + receivedContext.getSpanId());

    System.out.println("\nW3C Traceparent format:");
    System.out.println("  00-{trace-id}-{span-id}-{flags}");
    System.out.println("  Example: 00-" + context.getTraceId() +
        "-" + context.getSpanId() + "-01");

    System.out.println("\nBenefits:");
    System.out.println("  ✓ Standard propagation format");
    System.out.println("  ✓ Works with all APM tools");
    System.out.println("  ✓ Language agnostic");
    System.out.println("  ✓ Sampling propagation");

    System.out.println("\nResult: Seamless cross-service tracing\n");
    System.out.println("---\n");
  }

  /**
   * Example 5: Performance Monitoring
   *
   * <p>Use Case: Identify slow operations and bottlenecks.
   */
  static void example5_PerformanceMonitoring() {
    System.out.println("Example 5: Performance Monitoring");
    System.out.println("Use Case: API endpoint performance analysis\n");

    System.out.println("Traced operations for GET /api/products:");
    System.out.println();

    System.out.println("Span breakdown:");
    System.out.println("  http.request                     250ms  100%");
    System.out.println("    ├─ auth.validate                10ms    4%");
    System.out.println("    ├─ cache.check                   5ms    2%");
    System.out.println("    ├─ db.query [products]         180ms   72%  ← BOTTLENECK!");
    System.out.println("    │   ├─ db.connect                8ms");
    System.out.println("    │   ├─ db.execute              165ms         ← VERY SLOW!");
    System.out.println("    │   └─ db.fetchResults           7ms");
    System.out.println("    ├─ serialize.json               35ms   14%");
    System.out.println("    └─ compression.gzip             20ms    8%");

    System.out.println("\nPerformance insights:");
    System.out.println("  🔴 db.execute taking 165ms (66% of total)");
    System.out.println("  🟡 serialize.json taking 35ms (14% of total)");
    System.out.println("  🟢 auth.validate only 10ms (4% of total)");

    System.out.println("\nRecommendations:");
    System.out.println("  1. Add index on products query → Expected: 165ms → 15ms");
    System.out.println("  2. Cache serialized JSON → Save 35ms on cache hit");
    System.out.println("  3. Target: <100ms total response time");

    System.out.println("\nAfter optimization:");
    System.out.println("  http.request                      65ms  100%");
    System.out.println("    ├─ auth.validate                10ms   15%");
    System.out.println("    ├─ cache.check (HIT)             5ms    8%");
    System.out.println("    ├─ db.query [products]          15ms   23%  ✓ FIXED");
    System.out.println("    ├─ serialize.json (cached)       5ms    8%  ✓ IMPROVED");
    System.out.println("    └─ compression.gzip             20ms   31%");

    System.out.println("\nResult: 74% performance improvement (250ms → 65ms)\n");
    System.out.println("---\n");
  }

  /**
   * Example 6: Error Tracking
   *
   * <p>Use Case: Track and debug errors across services.
   */
  static void example6_ErrorTracking() {
    System.out.println("Example 6: Error Tracking");
    System.out.println("Use Case: Debug payment processing failure\n");

    Tracer tracer = TracingProvider.getTracer();

    System.out.println("Request flow with error:");

    // Root span
    Span rootSpan = tracer.startSpan("checkout.process");
    rootSpan.setAttribute(SemanticAttributes.USER_ID, "12345");
    rootSpan.setAttribute("order.id", "ORD-789");
    rootSpan.setAttribute("order.total", 599.99);

    System.out.println("\n1. ✓ checkout.process started");

    // Validation - success
    Span validationSpan = tracer.startSpan("validation.order", rootSpan);
    validationSpan.setStatus(Span.Status.OK);
    validationSpan.end();
    System.out.println("2. ✓ validation.order - OK");

    // Payment - error!
    Span paymentSpan = tracer.startSpan("payment.charge", rootSpan);
    paymentSpan.setAttribute("payment.method", "credit_card");
    paymentSpan.setAttribute("payment.amount", 599.99);

    try {
      // Simulate payment failure
      throw new RuntimeException("Card declined: insufficient funds");
    } catch (Exception e) {
      System.out.println("3. ✗ payment.charge - ERROR");
      System.out.println("   Error: " + e.getMessage());

      paymentSpan.recordException(e);
      paymentSpan.setStatus(Span.Status.ERROR);
      paymentSpan.setAttribute("error.type", "PaymentDeclined");
      paymentSpan.setAttribute("error.message", e.getMessage());
    }
    paymentSpan.end();

    rootSpan.setStatus(Span.Status.ERROR);
    rootSpan.end();

    System.out.println("\nTrace summary:");
    System.out.println("  Trace ID: " + rootSpan.getTraceId());
    System.out.println("  Status: ERROR");
    System.out.println("  Failed at: payment.charge");
    System.out.println("  Error: Card declined: insufficient funds");

    System.out.println("\nDebug information:");
    System.out.println("  User ID: 12345");
    System.out.println("  Order ID: ORD-789");
    System.out.println("  Payment method: credit_card");
    System.out.println("  Amount: $599.99");
    System.out.println("  Error type: PaymentDeclined");

    System.out.println("\nVisualization in APM:");
    System.out.println("  checkout.process [ERROR]");
    System.out.println("    ├─ validation.order [OK]");
    System.out.println("    └─ payment.charge [ERROR] ← Click for details");

    System.out.println("\nBenefits:");
    System.out.println("  ✓ Exact failure point identified");
    System.out.println("  ✓ Full context for debugging");
    System.out.println("  ✓ Stack trace captured");
    System.out.println("  ✓ Error rate monitoring");

    System.out.println("\nResult: Fast error diagnosis and resolution\n");
    System.out.println("---\n");
  }

  /**
   * Example 7: Integration with Repository
   */
  static class UserRepository {
    private final Tracer tracer = TracingProvider.getTracer();

    public User findById(Long id) {
      Span span = tracer.startSpan("repository.findById");
      span.setAttribute(SemanticAttributes.DB_OPERATION, "SELECT");
      span.setAttribute("user.id", id);

      try {
        // Database query would go here
        return new User(id, "John Doe");
      } finally {
        span.end();
      }
    }

    public void save(User user) {
      Span span = tracer.startSpan("repository.save");
      span.setAttribute(SemanticAttributes.DB_OPERATION, "INSERT");
      span.setAttribute("user.id", user.id);

      try {
        // Database insert would go here
        span.addEvent("user.saved");
      } catch (Exception e) {
        span.recordException(e);
        span.setStatus(Span.Status.ERROR);
        throw e;
      } finally {
        span.end();
      }
    }
  }

  record User(Long id, String name) {}
}
