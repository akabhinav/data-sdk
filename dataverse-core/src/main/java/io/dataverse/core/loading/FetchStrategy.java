package io.dataverse.core.loading;

/**
 * Enumeration of fetch strategies for loading related entities.
 *
 * <p>Fetch strategies control when and how related entities are loaded,
 * providing a balance between performance and memory usage.
 *
 * <p><strong>Strategy Comparison:</strong>
 * <table border="1">
 *   <tr>
 *     <th>Strategy</th>
 *     <th>When Loaded</th>
 *     <th>Use Case</th>
 *     <th>Trade-offs</th>
 *   </tr>
 *   <tr>
 *     <td>LAZY</td>
 *     <td>On first access</td>
 *     <td>Large collections, rarely accessed</td>
 *     <td>Fewer queries initially, potential N+1 problem</td>
 *   </tr>
 *   <tr>
 *     <td>EAGER</td>
 *     <td>Immediately with parent</td>
 *     <td>Always needed, small collections</td>
 *     <td>More data loaded upfront, avoids N+1</td>
 *   </tr>
 *   <tr>
 *     <td>BATCH</td>
 *     <td>In batches on access</td>
 *     <td>Collections of moderate size</td>
 *     <td>Balanced approach, good performance</td>
 *   </tr>
 *   <tr>
 *     <td>DEFAULT</td>
 *     <td>Adapter-specific default</td>
 *     <td>Standard behavior</td>
 *     <td>Varies by adapter</td>
 *   </tr>
 * </table>
 *
 * <p><strong>N+1 Query Problem:</strong>
 * <pre>{@code
 * // BAD: Lazy loading causes N+1 queries
 * List<User> users = userRepository.findAll();  // 1 query
 * for (User user : users) {
 *     List<Order> orders = user.getOrders();  // N queries (one per user)
 *     // Process orders...
 * }
 *
 * // GOOD: Eager loading or batch loading
 * @EagerLoad("orders")
 * List<User> users = userRepository.findAll();  // 1 query with JOIN or 2 queries
 * for (User user : users) {
 *     List<Order> orders = user.getOrders();  // No additional queries
 *     // Process orders...
 * }
 * }</pre>
 *
 * <p><strong>Performance Characteristics:</strong>
 * <ul>
 *   <li><strong>LAZY</strong>: 1 initial query + N queries on access = O(1 + N)</li>
 *   <li><strong>EAGER</strong>: 1-2 queries total = O(1) or O(2)</li>
 *   <li><strong>BATCH</strong>: 1 + ceil(N / batch_size) queries = O(1 + N/B)</li>
 * </ul>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public enum FetchStrategy {

  /**
   * Lazy loading - load related entities only when accessed.
   *
   * <p><strong>Pros:</strong>
   * <ul>
   *   <li>Faster initial query</li>
   *   <li>Uses less memory if data not accessed</li>
   *   <li>Good for optional relationships</li>
   * </ul>
   *
   * <p><strong>Cons:</strong>
   * <ul>
   *   <li>N+1 query problem if accessed in loops</li>
   *   <li>Requires active database connection</li>
   *   <li>Unpredictable query count</li>
   * </ul>
   *
   * <p><strong>Best for:</strong>
   * <ul>
   *   <li>Large collections (e.g., user's 10,000 orders)</li>
   *   <li>Rarely accessed relationships</li>
   *   <li>Optional/conditional data loading</li>
   * </ul>
   */
  LAZY,

  /**
   * Eager loading - load related entities immediately with parent.
   *
   * <p><strong>Pros:</strong>
   * <ul>
   *   <li>Predictable query count</li>
   *   <li>No N+1 problem</li>
   *   <li>Works without active connection</li>
   * </ul>
   *
   * <p><strong>Cons:</strong>
   * <ul>
   *   <li>Slower initial query</li>
   *   <li>Uses more memory upfront</li>
   *   <li>Loads data that might not be used</li>
   * </ul>
   *
   * <p><strong>Best for:</strong>
   * <ul>
   *   <li>Small collections (e.g., user's roles)</li>
   *   <li>Always accessed relationships</li>
   *   <li>Read-only/detached entities</li>
   * </ul>
   */
  EAGER,

  /**
   * Batch loading - load related entities in batches when accessed.
   *
   * <p>Combines benefits of lazy and eager loading. Loads related entities
   * in batches (e.g., 100 at a time) on first access, avoiding N+1 problem
   * while not loading everything upfront.
   *
   * <p><strong>Example:</strong>
   * <pre>{@code
   * // Load 100 users
   * List<User> users = userRepository.findAll();
   *
   * // First access triggers batch load of ALL orders in one query
   * for (User user : users) {
   *     List<Order> orders = user.getOrders();  // Loaded in batch
   * }
   * // Total: 2 queries (users + all orders), not 101
   * }</pre>
   *
   * <p><strong>Best for:</strong>
   * <ul>
   *   <li>Medium-sized collections</li>
   *   <li>Frequently accessed in loops</li>
   *   <li>Balance between memory and queries</li>
   * </ul>
   */
  BATCH,

  /**
   * Default loading strategy - use adapter/framework default.
   *
   * <p>Typically:
   * <ul>
   *   <li>Collections: LAZY</li>
   *   <li>Single relationships: EAGER</li>
   *   <li>Configurable via adapter settings</li>
   * </ul>
   */
  DEFAULT
}
