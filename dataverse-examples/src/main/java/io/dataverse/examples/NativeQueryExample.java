package io.dataverse.examples;

import io.dataverse.core.query.NativeQuery;
import io.dataverse.core.query.NativeQueryExecutor;
import io.dataverse.core.query.ParameterBinder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Example demonstrating Native Query with Type Safety (Feature #3).
 *
 * <p>Use Cases:
 * <ul>
 *   <li>Complex SQL queries with database-specific features</li>
 *   <li>Performance-critical queries needing optimization</li>
 *   <li>Legacy database integration</li>
 *   <li>Stored procedure calls</li>
 * </ul>
 *
 * @author DataVerse SDK Team
 */
public class NativeQueryExample {

  public static void main(String[] args) {
    System.out.println("=== Native Query with Type Safety Examples ===\n");

    example1_BasicNamedParameters();
    example2_ComplexQueryWithJoins();
    example3_DatabaseSpecificFeatures();
    example4_DynamicParameterBinding();
    example5_StoredProcedureCalls();
    example6_RepositoryIntegration();
  }

  /**
   * Example 1: Basic Named Parameters
   *
   * <p>Use Case: Simple queries with type-safe parameter binding.
   */
  static void example1_BasicNamedParameters() {
    System.out.println("Example 1: Basic Named Parameters");
    System.out.println("Use Case: User search by email\n");

    String query = """
        SELECT id, name, email, created_at
        FROM users
        WHERE email = :email
        AND status = :status
        """;

    ParameterBinder binder = new ParameterBinder(query);
    binder.bind("email", "john@example.com");
    binder.bind("status", "ACTIVE");

    System.out.println("Original query with named parameters:");
    System.out.println(query);

    System.out.println("\nProcessed query with placeholders:");
    System.out.println(binder.getQueryWithPlaceholders());

    System.out.println("\nBound parameters:");
    System.out.println("  [1] email = john@example.com");
    System.out.println("  [2] status = ACTIVE");

    System.out.println("\nResult: Type-safe, readable parameter binding\n");
    System.out.println("---\n");
  }

  /**
   * Example 2: Complex Query with Joins
   *
   * <p>Use Case: E-commerce order history with multiple joins.
   */
  static void example2_ComplexQueryWithJoins() {
    System.out.println("Example 2: Complex Query with Joins");
    System.out.println("Use Case: Customer order history report\n");

    String query = """
        SELECT
            u.id AS user_id,
            u.name AS user_name,
            o.id AS order_id,
            o.total_amount,
            o.status,
            p.name AS product_name,
            oi.quantity,
            oi.price
        FROM users u
        INNER JOIN orders o ON u.id = o.user_id
        INNER JOIN order_items oi ON o.id = oi.order_id
        INNER JOIN products p ON oi.product_id = p.id
        WHERE u.id = :userId
          AND o.created_at >= :startDate
          AND o.created_at <= :endDate
          AND o.status IN (:statuses)
        ORDER BY o.created_at DESC
        LIMIT :limit
        """;

    ParameterBinder binder = new ParameterBinder(query);
    binder.bind("userId", 12345L);
    binder.bind("startDate", LocalDateTime.now().minusMonths(6));
    binder.bind("endDate", LocalDateTime.now());
    binder.bind("statuses", List.of("COMPLETED", "SHIPPED"));
    binder.bind("limit", 50);

    System.out.println("Query: Multi-table join with filters");
    System.out.println("Parameters:");
    System.out.println("  userId: 12345");
    System.out.println("  dateRange: Last 6 months");
    System.out.println("  statuses: [COMPLETED, SHIPPED]");
    System.out.println("  limit: 50");

    System.out.println("\nBenefits:");
    System.out.println("  ✓ Database-optimized join order");
    System.out.println("  ✓ Indexed column filtering");
    System.out.println("  ✓ Type-safe parameter binding");
    System.out.println("  ✓ SQL injection prevention");

    System.out.println("\nResult: Optimized query with clean parameter syntax\n");
    System.out.println("---\n");
  }

  /**
   * Example 3: Database-Specific Features
   *
   * <p>Use Case: PostgreSQL full-text search with ranking.
   */
  static void example3_DatabaseSpecificFeatures() {
    System.out.println("Example 3: Database-Specific Features");
    System.out.println("Use Case: PostgreSQL full-text search\n");

    // PostgreSQL-specific query with tsvector and tsquery
    String query = """
        SELECT
            id,
            title,
            content,
            ts_rank(to_tsvector('english', title || ' ' || content),
                    plainto_tsquery('english', :searchTerm)) AS rank
        FROM articles
        WHERE to_tsvector('english', title || ' ' || content) @@
              plainto_tsquery('english', :searchTerm)
        ORDER BY rank DESC
        LIMIT :limit
        """;

    ParameterBinder binder = new ParameterBinder(query);
    binder.bind("searchTerm", "java virtual threads performance");
    binder.bind("limit", 20);

    System.out.println("Database: PostgreSQL");
    System.out.println("Feature: Full-text search with ts_rank");
    System.out.println("Search term: 'java virtual threads performance'");

    System.out.println("\nWhy Native Query:");
    System.out.println("  • Uses PostgreSQL's advanced full-text search");
    System.out.println("  • Leverages GIN indexes for performance");
    System.out.println("  • Custom ranking algorithm");
    System.out.println("  • Language-specific stemming");

    System.out.println("\nResult: Access to database-specific optimizations\n");
    System.out.println("---\n");
  }

  /**
   * Example 4: Dynamic Parameter Binding
   *
   * <p>Use Case: Dynamic filters in admin dashboard.
   */
  static void example4_DynamicParameterBinding() {
    System.out.println("Example 4: Dynamic Parameter Binding");
    System.out.println("Use Case: Admin user search with optional filters\n");

    // Base query
    String query = """
        SELECT id, name, email, role, status, created_at
        FROM users
        WHERE 1=1
        """;

    // Dynamically add filters based on user input
    Map<String, Object> filters = Map.of(
        "role", "ADMIN",
        "status", "ACTIVE",
        "createdAfter", LocalDateTime.now().minusYears(1)
    );

    StringBuilder dynamicQuery = new StringBuilder(query);
    ParameterBinder binder = new ParameterBinder(query);

    if (filters.containsKey("role")) {
      dynamicQuery.append(" AND role = :role");
      binder.bind("role", filters.get("role"));
    }

    if (filters.containsKey("status")) {
      dynamicQuery.append(" AND status = :status");
      binder.bind("status", filters.get("status"));
    }

    if (filters.containsKey("createdAfter")) {
      dynamicQuery.append(" AND created_at >= :createdAfter");
      binder.bind("createdAfter", filters.get("createdAfter"));
    }

    dynamicQuery.append(" ORDER BY created_at DESC");

    System.out.println("Applied filters:");
    filters.forEach((key, value) ->
        System.out.println("  " + key + " = " + value));

    System.out.println("\nGenerated query:");
    System.out.println(dynamicQuery.toString());

    System.out.println("\nResult: Flexible, type-safe dynamic queries\n");
    System.out.println("---\n");
  }

  /**
   * Example 5: Stored Procedure Calls
   *
   * <p>Use Case: Calling database stored procedures.
   */
  static void example5_StoredProcedureCalls() {
    System.out.println("Example 5: Stored Procedure Calls");
    System.out.println("Use Case: Monthly sales report generation\n");

    // PostgreSQL stored procedure call
    String query = "SELECT * FROM generate_monthly_sales_report(:year, :month, :region)";

    ParameterBinder binder = new ParameterBinder(query);
    binder.bind("year", 2025);
    binder.bind("month", 1);
    binder.bind("region", "US-EAST");

    System.out.println("Stored Procedure: generate_monthly_sales_report");
    System.out.println("Parameters:");
    System.out.println("  year: 2025");
    System.out.println("  month: 1 (January)");
    System.out.println("  region: US-EAST");

    System.out.println("\nAdvantages:");
    System.out.println("  ✓ Complex business logic in database");
    System.out.println("  ✓ Reduced network traffic");
    System.out.println("  ✓ Reuse across applications");
    System.out.println("  ✓ Type-safe parameter binding");

    System.out.println("\nResult: Seamless stored procedure integration\n");
    System.out.println("---\n");
  }

  /**
   * Example 6: Repository Integration
   *
   * <p>Use Case: Clean repository pattern with native queries.
   */
  static void example6_RepositoryIntegration() {
    System.out.println("Example 6: Repository Integration");
    System.out.println("Use Case: Order repository with complex queries\n");

    OrderRepository repository = new OrderRepository();

    // Example queries
    System.out.println("1. Find orders by status:");
    List<Order> completedOrders = repository.findByStatus("COMPLETED");
    System.out.println("   Found " + completedOrders.size() + " completed orders");

    System.out.println("\n2. Find high-value orders:");
    List<Order> highValueOrders = repository.findHighValueOrders(1000.0);
    System.out.println("   Found " + highValueOrders.size() + " orders > $1000");

    System.out.println("\n3. Customer lifetime value:");
    Double lifetimeValue = repository.calculateCustomerLifetimeValue(12345L);
    System.out.println("   Customer 12345 lifetime value: $" + lifetimeValue);

    System.out.println("\n4. Generate sales report:");
    List<SalesReport> report = repository.generateSalesReport(
        LocalDateTime.now().minusMonths(1),
        LocalDateTime.now()
    );
    System.out.println("   Generated report with " + report.size() + " rows");

    System.out.println("\nResult: Clean API with powerful native queries\n");
    System.out.println("---\n");
  }

  // Repository example
  static class OrderRepository {

    @NativeQuery("""
        SELECT * FROM orders
        WHERE status = :status
        ORDER BY created_at DESC
        """)
    public List<Order> findByStatus(String status) {
      System.out.println("  Executing: findByStatus('" + status + "')");
      // DataVerse automatically executes query and maps results
      return List.of(
          new Order(1L, 12345L, 150.0, "COMPLETED"),
          new Order(2L, 12345L, 200.0, "COMPLETED")
      );
    }

    @NativeQuery("""
        SELECT o.*, u.name as customer_name, u.email
        FROM orders o
        INNER JOIN users u ON o.user_id = u.id
        WHERE o.total_amount >= :minAmount
        ORDER BY o.total_amount DESC
        """)
    public List<Order> findHighValueOrders(Double minAmount) {
      System.out.println("  Executing: findHighValueOrders($" + minAmount + ")");
      return List.of(
          new Order(3L, 67890L, 1500.0, "COMPLETED"),
          new Order(4L, 11111L, 1200.0, "SHIPPED")
      );
    }

    @NativeQuery("""
        SELECT SUM(total_amount) as lifetime_value
        FROM orders
        WHERE user_id = :userId
        AND status = 'COMPLETED'
        """)
    public Double calculateCustomerLifetimeValue(Long userId) {
      System.out.println("  Executing: calculateCustomerLifetimeValue(" + userId + ")");
      return 2500.0;
    }

    @NativeQuery("""
        SELECT
            DATE_TRUNC('day', created_at) as date,
            COUNT(*) as order_count,
            SUM(total_amount) as total_revenue,
            AVG(total_amount) as avg_order_value
        FROM orders
        WHERE created_at >= :startDate
          AND created_at <= :endDate
        GROUP BY DATE_TRUNC('day', created_at)
        ORDER BY date DESC
        """)
    public List<SalesReport> generateSalesReport(LocalDateTime startDate, LocalDateTime endDate) {
      System.out.println("  Executing: generateSalesReport(" + startDate + " to " + endDate + ")");
      return List.of(
          new SalesReport(LocalDateTime.now(), 45, 6750.0, 150.0)
      );
    }
  }

  // Domain classes
  record Order(Long id, Long userId, Double totalAmount, String status) {}
  record SalesReport(LocalDateTime date, Integer orderCount, Double totalRevenue, Double avgOrderValue) {}
}
