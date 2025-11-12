package io.dataverse.core.graphql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Customizes GraphQL field generation.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * @GraphQLField(
 *     name = "userId",
 *     description = "Unique identifier",
 *     nullable = false
 * )
 * public Long getId() {
 *     return id;
 * }
 *
 * @GraphQLField(
 *     description = "User's orders",
 *     type = "Order"
 * )
 * public List<Order> getOrders() {
 *     return orders;
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GraphQLField {

  /**
   * GraphQL field name.
   *
   * <p>Defaults to property name.
   *
   * @return field name
   */
  String name() default "";

  /**
   * GraphQL field description.
   *
   * @return field description
   */
  String description() default "";

  /**
   * GraphQL type override.
   *
   * <p>Use for custom types or when type inference is insufficient.
   *
   * @return GraphQL type
   */
  String type() default "";

  /**
   * Whether field is nullable.
   *
   * @return true if nullable
   */
  boolean nullable() default true;

  /**
   * Whether field is deprecated.
   *
   * @return true if deprecated
   */
  boolean deprecated() default false;

  /**
   * Deprecation reason.
   *
   * @return deprecation reason
   */
  String deprecationReason() default "";
}
