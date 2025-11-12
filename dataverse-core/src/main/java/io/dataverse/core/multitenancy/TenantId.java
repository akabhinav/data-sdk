package io.dataverse.core.multitenancy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as the tenant identifier for multi-tenancy support.
 *
 * <p>In multi-tenant applications, data is isolated by tenant. This annotation
 * identifies which field contains the tenant ID, enabling automatic tenant filtering.
 *
 * <p><strong>Multi-Tenancy Strategies:</strong>
 * <ul>
 *   <li><strong>Discriminator Column</strong> - All tenants share tables, tenant ID in each row</li>
 *   <li><strong>Schema Per Tenant</strong> - Each tenant has separate database schema</li>
 *   <li><strong>Database Per Tenant</strong> - Each tenant has separate database</li>
 * </ul>
 *
 * <p><strong>Example Usage (Discriminator Column):</strong>
 * <pre>{@code
 * public class Order implements Entity<Long> {
 *     private Long id;
 *
 *     @TenantId
 *     private String tenantId;  // Automatically populated and filtered
 *
 *     private String orderNumber;
 *     private double amount;
 * }
 *
 * // Configure tenant context
 * TenantContext.setCurrentTenant("tenant-123");
 *
 * // All operations automatically scoped to tenant
 * Order order = new Order();
 * order.setOrderNumber("ORD-001");
 * repository.save(order);  // tenantId automatically set to "tenant-123"
 *
 * // Queries automatically filtered by tenant
 * List<Order> orders = repository.findAll();  // Only returns orders for "tenant-123"
 * }</pre>
 *
 * <p><strong>Security Considerations:</strong>
 * <ul>
 *   <li>Always validate tenant ID from authenticated user context</li>
 *   <li>Never accept tenant ID from client input</li>
 *   <li>Ensure tenant isolation at database query level</li>
 *   <li>Audit cross-tenant access attempts</li>
 *   <li>Use row-level security policies when available</li>
 * </ul>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TenantId {

  /**
   * Whether the tenant ID is required.
   *
   * <p>If true, operations will fail if no current tenant is set.
   *
   * @return true if tenant ID is required
   */
  boolean required() default true;
}
