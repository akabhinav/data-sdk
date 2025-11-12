package io.dataverse.core.repository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for defining custom queries on repository methods.
 *
 * <p>Use this annotation to provide explicit query logic when method name parsing
 * is insufficient or when you want to use native queries.
 *
 * <p>Example usage:
 * <pre>{@code
 * public interface UserRepository extends DataRepository<User, Long> {
 *     // Method name parsing (automatic)
 *     User findByUsername(String username);
 *
 *     // Custom query with @QueryMethod
 *     @QueryMethod("username = :username AND active = true")
 *     User findActiveUserByUsername(String username);
 *
 *     // Native query
 *     @QueryMethod(value = "SELECT * FROM users WHERE email LIKE :pattern", native = true)
 *     List<User> findByEmailPattern(String pattern);
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryMethod {

  /**
   * The query string. Format depends on the {@code native} flag:
   * <ul>
   *   <li>If native=false: DataVerse query syntax (field = :param)</li>
   *   <li>If native=true: Database-specific query syntax</li>
   * </ul>
   *
   * <p>Parameters are referenced with colon notation: {@code :paramName}
   *
   * @return the query string
   */
  String value();

  /**
   * Whether this is a native query.
   *
   * <p>Native queries bypass the query builder and execute directly against
   * the data source using its native query language.
   *
   * @return true for native queries, false for DataVerse queries
   */
  boolean nativeQuery() default false;

  /**
   * Whether to count results instead of returning entities.
   *
   * <p>Useful for pagination: {@code long countByActiveTrue();}
   *
   * @return true to count results
   */
  boolean count() default false;

  /**
   * Whether this query can return null.
   *
   * <p>By default, query methods that return a single entity throw an exception
   * if no result is found. Set this to true to return null instead.
   *
   * @return true if null is a valid result
   */
  boolean nullable() default true;
}
