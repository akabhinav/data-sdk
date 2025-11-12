package io.dataverse.core.projection;

/**
 * Interface for projection types that load partial entity data.
 *
 * <p>Projections allow you to load only the fields you need, reducing data transfer
 * and improving query performance. Useful for read-heavy operations where you don't
 * need the full entity.
 *
 * <p><strong>Benefits:</strong>
 * <ul>
 *   <li>Reduced database load - fetch only needed columns</li>
 *   <li>Reduced network transfer - less data over the wire</li>
 *   <li>Improved performance - faster queries and parsing</li>
 *   <li>Type-safe - compiler-checked field access</li>
 * </ul>
 *
 * <p><strong>Types of Projections:</strong>
 * <ul>
 *   <li><strong>Interface Projections</strong> - Define getters for needed fields</li>
 *   <li><strong>Class Projections (DTOs)</strong> - POJOs with subset of fields</li>
 *   <li><strong>Dynamic Projections</strong> - Runtime field selection</li>
 * </ul>
 *
 * <p><strong>Example - Interface Projection:</strong>
 * <pre>{@code
 * // Entity
 * public class User implements Entity<Long> {
 *     private Long id;
 *     private String username;
 *     private String email;
 *     private String passwordHash;  // Sensitive, shouldn't be loaded always
 *     private String bio;
 *     private byte[] profilePicture;  // Large field
 * }
 *
 * // Projection interface - only load what's needed
 * public interface UserSummary extends Projection {
 *     Long getId();
 *     String getUsername();
 *     String getEmail();
 * }
 *
 * // Repository method
 * public interface UserRepository extends DataRepository<User, Long> {
 *     List<UserSummary> findAllProjectedBy();
 *     UserSummary findProjectedById(Long id);
 *     List<UserSummary> findAllByActiveTrue();
 * }
 *
 * // Usage
 * List<UserSummary> summaries = userRepository.findAllProjectedBy();
 * for (UserSummary summary : summaries) {
 *     System.out.println(summary.getUsername() + ": " + summary.getEmail());
 * }
 * }</pre>
 *
 * <p><strong>Example - DTO Projection:</strong>
 * <pre>{@code
 * // DTO class
 * public class UserDto implements Projection {
 *     private final Long id;
 *     private final String username;
 *     private final String email;
 *
 *     public UserDto(Long id, String username, String email) {
 *         this.id = id;
 *         this.username = username;
 *         this.email = email;
 *     }
 *
 *     // Getters...
 * }
 *
 * // Repository method with projection class
 * <S extends Projection> List<S> findAllProjectedBy(Class<S> projectionType);
 * }</pre>
 *
 * <p><strong>Example - Dynamic Projection:</strong>
 * <pre>{@code
 * // Query with dynamic field selection
 * List<User> users = repository.query()
 *     .select("id", "username", "email")  // Only fetch these fields
 *     .where("active").isTrue()
 *     .execute();
 * }</pre>
 *
 * <p><strong>Performance Impact:</strong>
 * <ul>
 *   <li>Loading 3 fields vs 10 fields: ~70% less data transfer</li>
 *   <li>Especially beneficial for:
 *     <ul>
 *       <li>Entities with large blob fields (images, documents)</li>
 *       <li>List/table views that show summary data</li>
 *       <li>API responses with limited fields</li>
 *       <li>Mobile apps with bandwidth constraints</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface Projection {
  // Marker interface for projection types
  // Implementations define which fields to project
}
