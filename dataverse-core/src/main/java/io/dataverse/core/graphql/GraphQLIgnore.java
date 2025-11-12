package io.dataverse.core.graphql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excludes a field from GraphQL schema generation.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * @GraphQLEntity
 * public class User {
 *     private Long id;
 *     private String password;
 *
 *     public Long getId() { return id; }
 *
 *     @GraphQLIgnore
 *     public String getPassword() {
 *         return password;  // Not exposed in GraphQL
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GraphQLIgnore {
}
