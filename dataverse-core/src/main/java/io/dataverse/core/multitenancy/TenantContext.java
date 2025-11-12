package io.dataverse.core.multitenancy;

/**
 * Thread-local storage for current tenant context.
 *
 * <p>Stores the current tenant ID in a ThreadLocal, ensuring tenant isolation
 * across concurrent requests in multi-tenant applications.
 *
 * <p><strong>Usage Pattern:</strong>
 * <pre>{@code
 * // Set tenant at request start (e.g., in authentication filter)
 * TenantContext.setCurrentTenant(user.getTenantId());
 *
 * try {
 *     // All database operations automatically scoped to this tenant
 *     List<Order> orders = orderRepository.findAll();
 *
 *     // Create new entities with tenant automatically set
 *     Order order = new Order();
 *     orderRepository.save(order);
 * } finally {
 *     // Always clear tenant at request end
 *     TenantContext.clear();
 * }
 * }</pre>
 *
 * <p><strong>Spring Integration:</strong>
 * <pre>{@code
 * @Component
 * public class TenantInterceptor implements HandlerInterceptor {
 *     @Override
 *     public boolean preHandle(HttpServletRequest request,
 *                              HttpServletResponse response,
 *                              Object handler) {
 *         String tenantId = extractTenantId(request);
 *         TenantContext.setCurrentTenant(tenantId);
 *         return true;
 *     }
 *
 *     @Override
 *     public void afterCompletion(HttpServletRequest request,
 *                                 HttpServletResponse response,
 *                                 Object handler,
 *                                 Exception ex) {
 *         TenantContext.clear();
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class TenantContext {

  private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

  /**
   * Sets the current tenant ID for this thread.
   *
   * @param tenantId the tenant ID
   */
  public static void setCurrentTenant(String tenantId) {
    currentTenant.set(tenantId);
  }

  /**
   * Gets the current tenant ID for this thread.
   *
   * @return the tenant ID, or null if not set
   */
  public static String getCurrentTenant() {
    return currentTenant.get();
  }

  /**
   * Clears the current tenant ID for this thread.
   *
   * <p><strong>Important:</strong> Always call this in a finally block
   * to prevent tenant ID leaking between requests.
   */
  public static void clear() {
    currentTenant.remove();
  }

  /**
   * Checks if a tenant is currently set.
   *
   * @return true if tenant is set, false otherwise
   */
  public static boolean isSet() {
    return currentTenant.get() != null;
  }
}
