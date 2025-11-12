package io.dataverse.core.join;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for JoinBuilder.
 *
 * @author DataVerse SDK Team
 */
class JoinBuilderTest {

  private List<User> users;
  private List<Order> orders;

  @BeforeEach
  void setUp() {
    users = List.of(
        new User(1L, "Alice"),
        new User(2L, "Bob"),
        new User(3L, "Charlie")
    );

    orders = List.of(
        new Order(101L, 1L, 100.0),  // Alice's order
        new Order(102L, 1L, 200.0),  // Alice's order
        new Order(103L, 2L, 150.0)   // Bob's order
        // Charlie has no orders
    );
  }

  @Test
  void testInnerJoin() {
    List<UserWithOrders> results = JoinBuilder
        .from(users)
        .innerJoin(
            orders,
            User::getId,
            Order::getUserId,
            (user, userOrders) -> new UserWithOrders(
                user.getName(),
                userOrders.size(),
                userOrders.stream().mapToDouble(Order::getAmount).sum()
            )
        )
        .execute();

    // Only users with orders (Alice and Bob)
    assertEquals(2, results.size());

    UserWithOrders alice = results.stream()
        .filter(u -> u.getUserName().equals("Alice"))
        .findFirst()
        .orElseThrow();
    assertEquals(2, alice.getOrderCount());
    assertEquals(300.0, alice.getTotalAmount(), 0.01);

    UserWithOrders bob = results.stream()
        .filter(u -> u.getUserName().equals("Bob"))
        .findFirst()
        .orElseThrow();
    assertEquals(1, bob.getOrderCount());
    assertEquals(150.0, bob.getTotalAmount(), 0.01);
  }

  @Test
  void testLeftJoin() {
    List<UserWithOrders> results = JoinBuilder
        .from(users)
        .leftJoin(
            orders,
            User::getId,
            Order::getUserId,
            (user, userOrders) -> new UserWithOrders(
                user.getName(),
                userOrders.size(),
                userOrders.stream().mapToDouble(Order::getAmount).sum()
            )
        )
        .execute();

    // All users (including Charlie with no orders)
    assertEquals(3, results.size());

    UserWithOrders charlie = results.stream()
        .filter(u -> u.getUserName().equals("Charlie"))
        .findFirst()
        .orElseThrow();
    assertEquals(0, charlie.getOrderCount());
    assertEquals(0.0, charlie.getTotalAmount(), 0.01);
  }

  @Test
  void testInnerJoin_WithFilter() {
    List<UserWithOrders> results = JoinBuilder
        .from(users)
        .innerJoin(
            orders,
            User::getId,
            Order::getUserId,
            (user, userOrders) -> new UserWithOrders(
                user.getName(),
                userOrders.size(),
                userOrders.stream().mapToDouble(Order::getAmount).sum()
            )
        )
        .where(result -> result.getTotalAmount() > 200)
        .execute();

    // Only Alice (total = 300)
    assertEquals(1, results.size());
    assertEquals("Alice", results.get(0).getUserName());
  }

  @Test
  void testInnerJoin_WithLimit() {
    List<UserWithOrders> results = JoinBuilder
        .from(users)
        .innerJoin(
            orders,
            User::getId,
            Order::getUserId,
            (user, userOrders) -> new UserWithOrders(
                user.getName(),
                userOrders.size(),
                userOrders.stream().mapToDouble(Order::getAmount).sum()
            )
        )
        .limit(1)
        .execute();

    assertEquals(1, results.size());
  }

  @Test
  void testInnerJoin_WithOffset() {
    List<UserWithOrders> results = JoinBuilder
        .from(users)
        .innerJoin(
            orders,
            User::getId,
            Order::getUserId,
            (user, userOrders) -> new UserWithOrders(
                user.getName(),
                userOrders.size(),
                userOrders.stream().mapToDouble(Order::getAmount).sum()
            )
        )
        .offset(1)
        .execute();

    // Skip first result
    assertEquals(1, results.size());
  }

  @Test
  void testInnerJoin_EmptyLeft() {
    List<UserWithOrders> results = JoinBuilder
        .from(List.<User>of())
        .innerJoin(
            orders,
            User::getId,
            Order::getUserId,
            (user, userOrders) -> new UserWithOrders(
                user.getName(),
                userOrders.size(),
                0.0
            )
        )
        .execute();

    assertTrue(results.isEmpty());
  }

  @Test
  void testInnerJoin_EmptyRight() {
    List<UserWithOrders> results = JoinBuilder
        .from(users)
        .innerJoin(
            List.<Order>of(),
            User::getId,
            Order::getUserId,
            (user, userOrders) -> new UserWithOrders(
                user.getName(),
                userOrders.size(),
                0.0
            )
        )
        .execute();

    assertTrue(results.isEmpty());
  }

  @Test
  void testLeftJoin_EmptyRight() {
    List<UserWithOrders> results = JoinBuilder
        .from(users)
        .leftJoin(
            List.<Order>of(),
            User::getId,
            Order::getUserId,
            (user, userOrders) -> new UserWithOrders(
                user.getName(),
                userOrders.size(),
                0.0
            )
        )
        .execute();

    // All users, but no orders
    assertEquals(3, results.size());
    assertTrue(results.stream().allMatch(r -> r.getOrderCount() == 0));
  }

  @Test
  void testMultipleFilters() {
    List<UserWithOrders> results = JoinBuilder
        .from(users)
        .innerJoin(
            orders,
            User::getId,
            Order::getUserId,
            (user, userOrders) -> new UserWithOrders(
                user.getName(),
                userOrders.size(),
                userOrders.stream().mapToDouble(Order::getAmount).sum()
            )
        )
        .where(result -> result.getOrderCount() > 0)
        .where(result -> result.getTotalAmount() > 100)
        .execute();

    // Alice and Bob both match both filters
    assertEquals(2, results.size());
  }

  // Test data classes

  static class User {
    private final Long id;
    private final String name;

    User(Long id, String name) {
      this.id = id;
      this.name = name;
    }

    Long getId() {
      return id;
    }

    String getName() {
      return name;
    }
  }

  static class Order {
    private final Long id;
    private final Long userId;
    private final double amount;

    Order(Long id, Long userId, double amount) {
      this.id = id;
      this.userId = userId;
      this.amount = amount;
    }

    Long getId() {
      return id;
    }

    Long getUserId() {
      return userId;
    }

    double getAmount() {
      return amount;
    }
  }

  static class UserWithOrders {
    private final String userName;
    private final int orderCount;
    private final double totalAmount;

    UserWithOrders(String userName, int orderCount, double totalAmount) {
      this.userName = userName;
      this.orderCount = orderCount;
      this.totalAmount = totalAmount;
    }

    String getUserName() {
      return userName;
    }

    int getOrderCount() {
      return orderCount;
    }

    double getTotalAmount() {
      return totalAmount;
    }
  }
}
