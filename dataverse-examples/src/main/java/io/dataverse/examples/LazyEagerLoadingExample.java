package io.dataverse.examples;

import io.dataverse.core.loading.EagerLoad;
import io.dataverse.core.loading.FetchPlan;
import io.dataverse.core.loading.FetchStrategy;
import io.dataverse.core.loading.LazyLoad;

import java.util.ArrayList;
import java.util.List;

/**
 * Example demonstrating Lazy/Eager Loading Strategies (Feature #8).
 *
 * <p>Use Cases:
 * <ul>
 *   <li>Solving N+1 query problem</li>
 *   <li>Optimizing entity graph loading</li>
 *   <li>Reducing memory footprint</li>
 *   <li>Performance tuning for different scenarios</li>
 * </ul>
 *
 * @author DataVerse SDK Team
 */
public class LazyEagerLoadingExample {

  public static void main(String[] args) {
    System.out.println("=== Lazy/Eager Loading Strategies Examples ===\n");

    example1_LazyLoadingBasics();
    example2_EagerLoadingPreventsNPlus1();
    example3_BatchLoading();
    example4_FetchPlanDeclarative();
    example5_ConditionalFetching();
    example6_DeepObjectGraphs();
  }

  /**
   * Example 1: Lazy Loading Basics
   *
   * <p>Use Case: User list without loading full profile details.
   */
  static void example1_LazyLoadingBasics() {
    System.out.println("Example 1: Lazy Loading Basics");
    System.out.println("Use Case: Display user list (ID and name only)\n");

    System.out.println("Without lazy loading:");
    System.out.println("  SELECT * FROM users");
    System.out.println("  SELECT * FROM profiles WHERE user_id IN (...)");
    System.out.println("  SELECT * FROM addresses WHERE user_id IN (...)");
    System.out.println("  → Loads unnecessary data, slow query");

    System.out.println("\nWith lazy loading:");
    System.out.println("  SELECT id, name FROM users");
    System.out.println("  → Fast, minimal data transfer");
    System.out.println("  → Profile and address loaded only when accessed");

    // Example entity
    User user = new User(1L, "John Doe");
    System.out.println("\nUser loaded: " + user.getName());
    System.out.println("Profile NOT loaded yet (lazy)");

    // Access profile - triggers lazy load
    System.out.println("\nAccessing profile...");
    System.out.println("  → NOW executes: SELECT * FROM profiles WHERE user_id = 1");
    // Profile profile = user.getProfile();

    System.out.println("\nResult: Load data only when needed, better performance\n");
    System.out.println("---\n");
  }

  /**
   * Example 2: Eager Loading Prevents N+1 Problem
   *
   * <p>Use Case: Display list of orders with customer names.
   */
  static void example2_EagerLoadingPreventsNPlus1() {
    System.out.println("Example 2: Eager Loading Prevents N+1 Problem");
    System.out.println("Use Case: Order list with customer information\n");

    System.out.println("❌ N+1 Problem (Lazy Loading):");
    System.out.println("  1. SELECT * FROM orders                    -- 1 query");
    System.out.println("  2. SELECT * FROM customers WHERE id = 1    -- Query per order");
    System.out.println("  3. SELECT * FROM customers WHERE id = 2");
    System.out.println("  4. SELECT * FROM customers WHERE id = 3");
    System.out.println("  ...");
    System.out.println("  → 1 + N queries for N orders!");

    System.out.println("\n✓ Solution (Eager Loading):");
    System.out.println("  1. SELECT * FROM orders                            -- 1 query");
    System.out.println("  2. SELECT * FROM customers WHERE id IN (1,2,3,...) -- 1 query");
    System.out.println("  → Only 2 queries total!");

    System.out.println("\nCode example:");
    System.out.println("  @EagerLoad");
    System.out.println("  private Customer customer;");

    System.out.println("\nPerformance comparison:");
    System.out.println("  100 orders:");
    System.out.println("    Lazy:  101 queries, ~5000ms");
    System.out.println("    Eager: 2 queries,   ~100ms");
    System.out.println("    → 50x faster!");

    System.out.println("\nResult: Dramatic performance improvement\n");
    System.out.println("---\n");
  }

  /**
   * Example 3: Batch Loading
   *
   * <p>Use Case: Load related entities in batches.
   */
  static void example3_BatchLoading() {
    System.out.println("Example 3: Batch Loading");
    System.out.println("Use Case: Product catalog with reviews\n");

    System.out.println("Scenario: Display 100 products with their reviews");

    System.out.println("\nStrategy comparison:");

    System.out.println("\n1. Lazy Loading:");
    System.out.println("   - 1 query for products");
    System.out.println("   - 100 queries for reviews (N+1 problem)");
    System.out.println("   - Total: 101 queries");

    System.out.println("\n2. Eager Loading:");
    System.out.println("   - 1 query for products");
    System.out.println("   - 1 query for ALL reviews");
    System.out.println("   - Problem: May load 10,000+ reviews into memory");

    System.out.println("\n3. Batch Loading (Best):");
    System.out.println("   - 1 query for products");
    System.out.println("   - 5 queries for reviews (batches of 20 products)");
    System.out.println("   - Total: 6 queries");
    System.out.println("   - Memory efficient + good performance");

    System.out.println("\nCode example:");
    System.out.println("  FetchPlan plan = FetchPlan.builder()");
    System.out.println("      .batch(\"reviews\", 20)  // Batch size: 20");
    System.out.println("      .build();");

    System.out.println("\nResult: Balanced performance and memory usage\n");
    System.out.println("---\n");
  }

  /**
   * Example 4: Fetch Plan (Declarative)
   *
   * <p>Use Case: Complex object graph with selective loading.
   */
  static void example4_FetchPlanDeclarative() {
    System.out.println("Example 4: Fetch Plan (Declarative)");
    System.out.println("Use Case: Blog post with comments, author, and tags\n");

    FetchPlan plan = FetchPlan.builder()
        .eager("author")                    // Always load author
        .eager("tags")                      // Always load tags
        .batch("comments", 50)              // Load comments in batches
        .lazy("comments.author")            // Comment authors lazy
        .eager("comments.author.profile")   // But profile eager when accessed
        .build();

    System.out.println("Fetch Plan Configuration:");
    System.out.println("  author:                 EAGER");
    System.out.println("  tags:                   EAGER");
    System.out.println("  comments:               BATCH (50)");
    System.out.println("  comments.author:        LAZY");
    System.out.println("  comments.author.profile: EAGER");

    System.out.println("\nExecution plan:");
    System.out.println("  1. SELECT * FROM posts WHERE id = ?");
    System.out.println("  2. SELECT * FROM users WHERE id = ? (author - eager)");
    System.out.println("  3. SELECT * FROM tags WHERE post_id = ? (tags - eager)");
    System.out.println("  4. SELECT * FROM comments WHERE post_id = ? LIMIT 50 (batch 1)");
    System.out.println("  5. SELECT * FROM comments WHERE post_id = ? LIMIT 50 OFFSET 50 (batch 2)");

    System.out.println("\nBenefits:");
    System.out.println("  ✓ Declarative configuration");
    System.out.println("  ✓ Reusable across queries");
    System.out.println("  ✓ Optimized for specific use case");
    System.out.println("  ✓ No code changes needed");

    System.out.println("\nResult: Fine-grained control over data loading\n");
    System.out.println("---\n");
  }

  /**
   * Example 5: Conditional Fetching
   *
   * <p>Use Case: Different loading strategies for different scenarios.
   */
  static void example5_ConditionalFetching() {
    System.out.println("Example 5: Conditional Fetching");
    System.out.println("Use Case: User profile - different views need different data\n");

    System.out.println("Scenario 1: User List Page");
    FetchPlan listPlan = FetchPlan.builder()
        .eager("id", "name", "email")       // Basic info only
        .lazy("profile")                     // Don't load profile
        .lazy("orders")                      // Don't load orders
        .build();
    System.out.println("  Load: ID, name, email only");
    System.out.println("  Skip: Profile, orders, addresses");
    System.out.println("  → Fast list rendering");

    System.out.println("\nScenario 2: User Detail Page");
    FetchPlan detailPlan = FetchPlan.builder()
        .eager("id", "name", "email")       // Basic info
        .eager("profile")                    // Need profile details
        .batch("orders", 10)                 // Recent orders
        .lazy("orders.items")                // Order items on demand
        .build();
    System.out.println("  Load: Everything for detail view");
    System.out.println("  Optimize: Batch orders, lazy items");
    System.out.println("  → Complete user information");

    System.out.println("\nScenario 3: Admin Dashboard");
    FetchPlan adminPlan = FetchPlan.builder()
        .eager("*")                          // Load everything
        .build();
    System.out.println("  Load: All data eagerly");
    System.out.println("  → Complete admin view");

    System.out.println("\nResult: Optimize loading for each use case\n");
    System.out.println("---\n");
  }

  /**
   * Example 6: Deep Object Graphs
   *
   * <p>Use Case: Complex nested relationships.
   */
  static void example6_DeepObjectGraphs() {
    System.out.println("Example 6: Deep Object Graphs");
    System.out.println("Use Case: Organization hierarchy\n");

    System.out.println("Object graph:");
    System.out.println("  Organization");
    System.out.println("    ├─ Departments (eager)");
    System.out.println("    │   ├─ Employees (batch 20)");
    System.out.println("    │   │   ├─ Manager (eager)");
    System.out.println("    │   │   └─ Projects (lazy)");
    System.out.println("    │   └─ Budget (eager)");
    System.out.println("    └─ Locations (eager)");

    FetchPlan plan = FetchPlan.builder()
        .eager("departments")
        .batch("departments.employees", 20)
        .eager("departments.employees.manager")
        .lazy("departments.employees.projects")
        .eager("departments.budget")
        .eager("locations")
        .build();

    System.out.println("\nLoading strategy:");
    System.out.println("  1. Load organization + departments + locations (eager)");
    System.out.println("  2. Load employees in batches of 20");
    System.out.println("  3. Load managers for each employee (eager)");
    System.out.println("  4. Skip projects until accessed (lazy)");

    System.out.println("\nFor 5 departments with 100 employees each:");
    System.out.println("  Queries executed: ~32 queries");
    System.out.println("    - 1 for organization");
    System.out.println("    - 1 for departments");
    System.out.println("    - 25 for employees (5 depts × 5 batches)");
    System.out.println("    - 5 for managers");

    System.out.println("\nWithout optimization:");
    System.out.println("  Would be: 501+ queries (N+1 problem)");

    System.out.println("\nResult: 15x fewer queries with batch loading\n");
    System.out.println("---\n");
  }

  // Example entities with loading annotations
  static class User {
    private Long id;
    private String name;

    @LazyLoad
    private Profile profile;

    @LazyLoad
    private List<Address> addresses;

    @EagerLoad
    private Role role;

    public User(Long id, String name) {
      this.id = id;
      this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
  }

  static class Profile {
    private String bio;
    private String avatar;
    private String phone;
  }

  static class Address {
    private String street;
    private String city;
    private String country;
  }

  static class Role {
    private String name;
    private List<String> permissions;
  }

  static class Order {
    private Long id;
    private Double total;

    @EagerLoad  // Always load customer info
    private Customer customer;

    @LazyLoad   // Load items on demand
    private List<OrderItem> items;
  }

  static class Customer {
    private Long id;
    private String name;
    private String email;
  }

  static class OrderItem {
    private Long productId;
    private Integer quantity;
    private Double price;
  }
}
