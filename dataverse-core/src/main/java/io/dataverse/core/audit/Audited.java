package io.dataverse.core.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an entity class for automatic audit trail tracking.
 *
 * <p>When an entity is annotated with @Audited, all modifications (INSERT, UPDATE, DELETE)
 * are automatically tracked and stored in the audit log.
 *
 * <p>Example usage:
 * <pre>
 * @Audited
 * public class Product implements Entity<Long> {
 *     private Long id;
 *     private String name;
 *     private double price;
 *     // ...
 * }
 * </pre>
 *
 * <p>Query audit history:
 * <pre>
 * List<AuditEntry<Product>> history = repository.audit()
 *     .forEntity(product)
 *     .between(startDate, endDate)
 *     .execute();
 * </pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

  /**
   * Specifies which operations to audit.
   * Default is all operations (INSERT, UPDATE, DELETE).
   */
  AuditOperation[] operations() default {
      AuditOperation.INSERT,
      AuditOperation.UPDATE,
      AuditOperation.DELETE
  };

  /**
   * Custom audit table name. If not specified, defaults to "{entity_name}_audit".
   */
  String tableName() default "";

  /**
   * Whether to store the full entity state (before and after values).
   * Default is true. If false, only stores that a change occurred.
   */
  boolean storeState() default true;
}
