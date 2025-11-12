package io.dataverse.core.encryption;

/**
 * Interface for encryption providers.
 *
 * <p>Encryption providers handle the actual encryption and decryption of data.
 * Different implementations can support various encryption backends:
 * <ul>
 *   <li>Local AES encryption (in-memory keys)</li>
 *   <li>AWS KMS (Key Management Service)</li>
 *   <li>HashiCorp Vault</li>
 *   <li>Azure Key Vault</li>
 *   <li>Google Cloud KMS</li>
 * </ul>
 *
 * <p>Implementations must be thread-safe as they may be called concurrently.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface EncryptionProvider {

  /**
   * Encrypts plaintext data.
   *
   * @param plaintext the data to encrypt (never null)
   * @param keyId the key identifier (may be null for default key)
   * @param deterministic true for deterministic encryption, false for randomized
   * @return the encrypted data as Base64-encoded string
   * @throws EncryptionException if encryption fails
   */
  String encrypt(String plaintext, String keyId, boolean deterministic) throws EncryptionException;

  /**
   * Decrypts ciphertext data.
   *
   * @param ciphertext the encrypted data (Base64-encoded)
   * @param keyId the key identifier (may be null for default key)
   * @return the decrypted plaintext
   * @throws EncryptionException if decryption fails
   */
  String decrypt(String ciphertext, String keyId) throws EncryptionException;

  /**
   * Encrypts plaintext data using the default key.
   *
   * @param plaintext the data to encrypt
   * @return the encrypted data
   * @throws EncryptionException if encryption fails
   */
  default String encrypt(String plaintext) throws EncryptionException {
    return encrypt(plaintext, null, false);
  }

  /**
   * Decrypts ciphertext data using the default key.
   *
   * @param ciphertext the encrypted data
   * @return the decrypted plaintext
   * @throws EncryptionException if decryption fails
   */
  default String decrypt(String ciphertext) throws EncryptionException {
    return decrypt(ciphertext, null);
  }

  /**
   * Gets the name of this encryption provider.
   *
   * @return the provider name (e.g., "AES", "AWS-KMS", "Vault")
   */
  String getName();

  /**
   * Checks if this encryption provider is healthy and available.
   *
   * @return true if healthy, false otherwise
   */
  boolean isHealthy();

  /**
   * Shuts down the encryption provider and releases resources.
   */
  void shutdown();
}
