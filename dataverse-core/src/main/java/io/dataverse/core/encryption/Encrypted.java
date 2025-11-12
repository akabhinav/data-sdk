package io.dataverse.core.encryption;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field for automatic encryption/decryption.
 *
 * <p>Fields annotated with {@code @Encrypted} will be automatically encrypted before
 * being stored in the database and decrypted when retrieved.
 *
 * <p>Example usage:
 * <pre>{@code
 * public class User implements Entity<Long> {
 *     private Long id;
 *     private String username;
 *
 *     @Encrypted
 *     private String email;
 *
 *     @Encrypted
 *     private String socialSecurityNumber;
 *
 *     @Encrypted(algorithm = "AES-256-GCM")
 *     private String creditCardNumber;
 * }
 * }</pre>
 *
 * <p><strong>Security Considerations:</strong>
 * <ul>
 *   <li>Encryption keys should be stored securely (e.g., AWS KMS, HashiCorp Vault)</li>
 *   <li>Never hardcode encryption keys in source code</li>
 *   <li>Use environment variables or key management systems</li>
 *   <li>Rotate encryption keys periodically</li>
 *   <li>Consider using different keys for different data sensitivity levels</li>
 * </ul>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Encrypted {

  /**
   * The encryption algorithm to use.
   * Default is AES-256-GCM (Authenticated Encryption with Associated Data).
   *
   * <p>Supported algorithms:
   * <ul>
   *   <li>AES-256-GCM (default) - Best balance of security and performance</li>
   *   <li>AES-256-CBC - Legacy compatibility</li>
   *   <li>ChaCha20-Poly1305 - Modern alternative to AES</li>
   * </ul>
   *
   * @return the encryption algorithm
   */
  String algorithm() default "AES-256-GCM";

  /**
   * The key identifier for key management systems.
   *
   * <p>Examples:
   * <ul>
   *   <li>AWS KMS: "arn:aws:kms:us-east-1:123456789012:key/12345678-1234-1234-1234-123456789012"</li>
   *   <li>Vault: "transit/keys/my-key"</li>
   *   <li>Environment variable: "${ENCRYPTION_KEY}"</li>
   * </ul>
   *
   * <p>If not specified, uses the default encryption key from configuration.
   *
   * @return the key identifier
   */
  String keyId() default "";

  /**
   * Whether to enable deterministic encryption.
   *
   * <p>Deterministic encryption produces the same ciphertext for the same plaintext,
   * which enables equality searches on encrypted data. However, it's less secure than
   * randomized encryption.
   *
   * <p><strong>Use Cases:</strong>
   * <ul>
   *   <li>Searching/filtering on encrypted fields</li>
   *   <li>Uniqueness constraints on encrypted fields</li>
   *   <li>Indexing encrypted data</li>
   * </ul>
   *
   * <p><strong>Warning:</strong> Only use deterministic encryption when necessary.
   * It reveals when two values are the same, which can leak information.
   *
   * @return true for deterministic encryption, false for randomized
   */
  boolean deterministic() default false;
}
