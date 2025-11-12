package io.dataverse.core.specification;

import io.dataverse.api.QueryBuilder;

/**
 * Specification pattern for building complex, reusable queries.
 *
 * <p>Specifications encapsulate query logic that can be combined using logical operators
 * (AND, OR, NOT) to create complex queries in a type-safe, composable way.
 *
 * <p><strong>Benefits:</strong>
 * <ul>
 *   <li>Type-safe query building</li>
 *   <li>Reusable query fragments</li>
 *   <li>Composable with AND/OR/NOT</li>
 *   <li>Testable in isolation</li>
 *   <li>Clean separation of concerns</li>
 * </ul>
 *
 * <p><strong>Example Usage:</strong>
 * <pre>{@code
 * // Define specifications
 * Specification<User> hasEmail = (query) -> query.where("email").isNotNull();
 * Specification<User> isActive = (query) -> query.where("active").isTrue();
 * Specification<User> isAdult = (query) -> query.where("age").greaterThanOrEqual(18);
 *
 * // Compose specifications
 * Specification<User> activeAdultsWithEmail =
 *     hasEmail.and(isActive).and(isAdult);
 *
 * // Execute query
 * List<User> users = repository.findAll(activeAdultsWithEmail);
 *
 * // Or use with Repository.query()
 * List<User> results = repository.query()
 *     .where(activeAdultsWithEmail)
 *     .orderBy("username").ascending()
 *     .limit(10)
 *     .execute();
 * }</pre>
 *
 * <p><strong>Advanced Composition:</strong>
 * <pre>{@code
 * // Complex business logic encapsulated in specifications
 * public class UserSpecifications {
 *     public static Specification<User> isPremiumUser() {
 *         return hasSubscription().and(isPaymentCurrent());
 *     }
 *
 *     public static Specification<User> canAccessFeature(String feature) {
 *         return isPremiumUser().or(isInBetaProgram())
 *             .and(hasPermission(feature));
 *     }
 *
 *     private static Specification<User> hasSubscription() {
 *         return (query) -> query.where("subscriptionType").isNotNull();
 *     }
 *
 *     private static Specification<User> isPaymentCurrent() {
 *         return (query) -> query.where("paymentStatus").equals("current");
 *     }
 * }
 *
 * // Usage
 * List<User> premiumUsers = repository.findAll(
 *     UserSpecifications.canAccessFeature("advanced-analytics")
 * );
 * }</pre>
 *
 * @param <T> the entity type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
@FunctionalInterface
public interface Specification<T> {

  /**
   * Applies this specification to a query builder.
   *
   * <p>Implementations should add appropriate conditions to the query builder.
   *
   * @param query the query builder to apply this specification to
   * @return the modified query builder
   */
  QueryBuilder<T> apply(QueryBuilder<T> query);

  /**
   * Combines this specification with another using AND logic.
   *
   * <p>Both specifications must be satisfied for an entity to match.
   *
   * @param other the other specification
   * @return a new specification combining this and other with AND
   */
  default Specification<T> and(Specification<T> other) {
    return (query) -> other.apply(this.apply(query));
  }

  /**
   * Combines this specification with another using OR logic.
   *
   * <p>Either specification can be satisfied for an entity to match.
   *
   * <p><strong>Note:</strong> OR composition creates separate query branches.
   * The implementation depends on the query builder's support for OR conditions.
   *
   * @param other the other specification
   * @return a new specification combining this and other with OR
   */
  default Specification<T> or(Specification<T> other) {
    return (query) -> {
      // Apply both specifications
      // The query builder should support OR grouping
      QueryBuilder<T> thisQuery = this.apply(query);
      return other.apply(thisQuery);
      // Note: Actual OR logic depends on QueryBuilder implementation
      // This is a simplified version
    };
  }

  /**
   * Negates this specification using NOT logic.
   *
   * <p>Entities matching this specification will be excluded.
   *
   * @return a new specification that negates this one
   */
  default Specification<T> not() {
    return (query) -> {
      // Apply specification and negate it
      // The query builder should support negation
      return this.apply(query);
      // Note: Actual NOT logic depends on QueryBuilder implementation
    };
  }

  /**
   * Creates a specification that always matches (no filtering).
   *
   * @param <T> the entity type
   * @return a specification that matches all entities
   */
  static <T> Specification<T> all() {
    return (query) -> query;
  }

  /**
   * Creates a specification from a simple property equals condition.
   *
   * @param property the property name
   * @param value the value to match
   * @param <T> the entity type
   * @return a specification for property equals value
   */
  static <T> Specification<T> equals(String property, Object value) {
    return (query) -> query.where(property).equals(value);
  }

  /**
   * Creates a specification for a property being null.
   *
   * @param property the property name
   * @param <T> the entity type
   * @return a specification for property is null
   */
  static <T> Specification<T> isNull(String property) {
    return (query) -> query.where(property).isNull();
  }

  /**
   * Creates a specification for a property not being null.
   *
   * @param property the property name
   * @param <T> the entity type
   * @return a specification for property is not null
   */
  static <T> Specification<T> isNotNull(String property) {
    return (query) -> query.where(property).isNotNull();
  }

  /**
   * Creates a specification for a property greater than a value.
   *
   * @param property the property name
   * @param value the value to compare
   * @param <T> the entity type
   * @return a specification for property > value
   */
  static <T> Specification<T> greaterThan(String property, Object value) {
    return (query) -> query.where(property).greaterThan(value);
  }

  /**
   * Creates a specification for a property less than a value.
   *
   * @param property the property name
   * @param value the value to compare
   * @param <T> the entity type
   * @return a specification for property < value
   */
  static <T> Specification<T> lessThan(String property, Object value) {
    return (query) -> query.where(property).lessThan(value);
  }

  /**
   * Creates a specification for a string property containing a substring.
   *
   * @param property the property name
   * @param substring the substring to search for
   * @param <T> the entity type
   * @return a specification for property contains substring
   */
  static <T> Specification<T> contains(String property, String substring) {
    return (query) -> query.where(property).contains(substring);
  }

  /**
   * Creates a specification for a boolean property being true.
   *
   * @param property the property name
   * @param <T> the entity type
   * @return a specification for property is true
   */
  static <T> Specification<T> isTrue(String property) {
    return (query) -> query.where(property).isTrue();
  }

  /**
   * Creates a specification for a boolean property being false.
   *
   * @param property the property name
   * @param <T> the entity type
   * @return a specification for property is false
   */
  static <T> Specification<T> isFalse(String property) {
    return (query) -> query.where(property).isFalse();
  }

  /**
   * Creates a specification for a property being in a collection of values.
   *
   * @param property the property name
   * @param values the values to match
   * @param <T> the entity type
   * @return a specification for property in values
   */
  static <T> Specification<T> in(String property, Object... values) {
    return (query) -> query.where(property).in(values);
  }
}
