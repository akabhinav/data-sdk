package io.dataverse.core.repository;

import io.dataverse.api.Entity;
import io.dataverse.api.Repository;

import java.io.Serializable;

/**
 * Base interface for Spring Data style repositories.
 *
 * <p>Extend this interface to create repositories with automatic query method implementation.
 * Methods are parsed by name and automatically implemented with appropriate queries.
 *
 * <p><strong>Supported Method Name Patterns:</strong>
 * <ul>
 *   <li>{@code findBy[Property][Operator]} - Find entities matching criteria</li>
 *   <li>{@code findAllBy[Property][Operator]} - Find all entities matching criteria</li>
 *   <li>{@code countBy[Property][Operator]} - Count entities matching criteria</li>
 *   <li>{@code existsBy[Property][Operator]} - Check if entities exist matching criteria</li>
 *   <li>{@code deleteBy[Property][Operator]} - Delete entities matching criteria</li>
 * </ul>
 *
 * <p><strong>Supported Operators:</strong>
 * <ul>
 *   <li>None - Equals (e.g., {@code findByUsername})</li>
 *   <li>{@code And} - Logical AND (e.g., {@code findByUsernameAndEmail})</li>
 *   <li>{@code Or} - Logical OR (e.g., {@code findByUsernameOrEmail})</li>
 *   <li>{@code GreaterThan} - Greater than (e.g., {@code findByAgeGreaterThan})</li>
 *   <li>{@code LessThan} - Less than (e.g., {@code findByAgeLessThan})</li>
 *   <li>{@code Like} - String pattern matching (e.g., {@code findByUsernameLike})</li>
 *   <li>{@code StartingWith} - String starts with (e.g., {@code findByUsernameStartingWith})</li>
 *   <li>{@code EndingWith} - String ends with (e.g., {@code findByUsernameEndingWith})</li>
 *   <li>{@code Containing} - String contains (e.g., {@code findByUsernameContaining})</li>
 *   <li>{@code Between} - Value between two bounds (e.g., {@code findByAgeBetween})</li>
 *   <li>{@code IsNull} - Value is null (e.g., {@code findByEmailIsNull})</li>
 *   <li>{@code IsNotNull} - Value is not null (e.g., {@code findByEmailIsNotNull})</li>
 *   <li>{@code True} - Boolean is true (e.g., {@code findByActiveTrue})</li>
 *   <li>{@code False} - Boolean is false (e.g., {@code findByActiveFalse})</li>
 *   <li>{@code In} - Value in collection (e.g., {@code findByUsernameIn})</li>
 *   <li>{@code NotIn} - Value not in collection (e.g., {@code findByUsernameNotIn})</li>
 * </ul>
 *
 * <p><strong>Sorting:</strong>
 * <ul>
 *   <li>{@code OrderBy[Property]Asc} - Ascending order (e.g., {@code findByActiveOrderByUsernameAsc})</li>
 *   <li>{@code OrderBy[Property]Desc} - Descending order (e.g., {@code findByActiveOrderByCreatedAtDesc})</li>
 * </ul>
 *
 * <p><strong>Example Repository:</strong>
 * <pre>{@code
 * public interface UserRepository extends DataRepository<User, Long> {
 *     // Automatic implementation - no code needed!
 *     User findByUsername(String username);
 *     List<User> findByActiveTrue();
 *     List<User> findByAgeGreaterThan(int age);
 *     List<User> findByUsernameAndEmail(String username, String email);
 *     List<User> findByUsernameContaining(String pattern);
 *     List<User> findByAgeBetween(int min, int max);
 *     long countByActiveTrue();
 *     boolean existsByUsername(String username);
 *     void deleteByUsername(String username);
 *     List<User> findByActiveOrderByUsernameAsc(boolean active);
 *
 *     // Custom query
 *     @QueryMethod("username LIKE :pattern AND age > :minAge")
 *     List<User> findCustomUsers(String pattern, int minAge);
 * }
 * }</pre>
 *
 * @param <T> the entity type
 * @param <ID> the identifier type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface DataRepository<T extends Entity<ID>, ID extends Serializable> extends Repository<T, ID> {
  // Marker interface - methods are implemented by proxy
  // All query methods are automatically implemented based on method names
}
