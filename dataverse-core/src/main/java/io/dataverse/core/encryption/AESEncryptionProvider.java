package io.dataverse.core.encryption;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AES-256-GCM encryption provider.
 *
 * <p>Provides secure authenticated encryption using AES in Galois/Counter Mode (GCM).
 * GCM provides both confidentiality and authenticity, protecting against tampering.
 *
 * <p><strong>Security Features:</strong>
 * <ul>
 *   <li>256-bit key length for maximum security</li>
 *   <li>128-bit authentication tag for integrity</li>
 *   <li>96-bit random IV for each encryption (non-deterministic mode)</li>
 *   <li>Zero IV for deterministic mode (use with caution)</li>
 * </ul>
 *
 * <p><strong>Key Management:</strong>
 * <ul>
 *   <li>Default key generated automatically if not provided</li>
 *   <li>Multiple keys supported via keyId parameter</li>
 *   <li>Keys stored in memory (consider using KMS for production)</li>
 * </ul>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class AESEncryptionProvider implements EncryptionProvider {

  private static final String ALGORITHM = "AES";
  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int GCM_TAG_LENGTH = 128;  // bits
  private static final int GCM_IV_LENGTH = 12;    // bytes (96 bits)
  private static final int AES_KEY_SIZE = 256;    // bits

  private final Map<String, SecretKey> keys = new ConcurrentHashMap<>();
  private final SecureRandom secureRandom = new SecureRandom();
  private SecretKey defaultKey;

  /**
   * Creates an AES encryption provider with an auto-generated default key.
   *
   * <p><strong>Warning:</strong> This generates a random key that is not persisted.
   * Data encrypted with this key cannot be decrypted after application restart.
   * For production use, provide your own key or use a key management service.
   */
  public AESEncryptionProvider() {
    try {
      this.defaultKey = generateKey();
    } catch (Exception e) {
      throw new EncryptionException("Failed to generate default encryption key", e);
    }
  }

  /**
   * Creates an AES encryption provider with a provided default key.
   *
   * @param encodedKey the Base64-encoded 256-bit AES key
   */
  public AESEncryptionProvider(String encodedKey) {
    try {
      byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
      if (keyBytes.length != 32) {  // 256 bits = 32 bytes
        throw new IllegalArgumentException("Key must be 256 bits (32 bytes)");
      }
      this.defaultKey = new SecretKeySpec(keyBytes, ALGORITHM);
    } catch (Exception e) {
      throw new EncryptionException("Failed to initialize encryption key", e);
    }
  }

  /**
   * Adds an additional key for multi-key encryption.
   *
   * @param keyId the key identifier
   * @param encodedKey the Base64-encoded 256-bit AES key
   */
  public void addKey(String keyId, String encodedKey) {
    try {
      byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
      if (keyBytes.length != 32) {
        throw new IllegalArgumentException("Key must be 256 bits (32 bytes)");
      }
      SecretKey key = new SecretKeySpec(keyBytes, ALGORITHM);
      keys.put(keyId, key);
    } catch (Exception e) {
      throw new EncryptionException("Failed to add encryption key: " + keyId, e);
    }
  }

  @Override
  public String encrypt(String plaintext, String keyId, boolean deterministic) throws EncryptionException {
    if (plaintext == null) {
      return null;
    }

    try {
      SecretKey key = getKey(keyId);
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);

      byte[] iv;
      if (deterministic) {
        // Deterministic: use zero IV (less secure but allows equality searches)
        iv = new byte[GCM_IV_LENGTH];
      } else {
        // Randomized: generate random IV for each encryption
        iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);
      }

      GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

      byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
      byte[] ciphertext = cipher.doFinal(plaintextBytes);

      // Format: IV || ciphertext (IV is prepended for decryption)
      ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
      byteBuffer.put(iv);
      byteBuffer.put(ciphertext);

      return Base64.getEncoder().encodeToString(byteBuffer.array());
    } catch (Exception e) {
      throw new EncryptionException("Encryption failed", e);
    }
  }

  @Override
  public String decrypt(String ciphertext, String keyId) throws EncryptionException {
    if (ciphertext == null) {
      return null;
    }

    try {
      SecretKey key = getKey(keyId);
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);

      byte[] decoded = Base64.getDecoder().decode(ciphertext);

      // Extract IV and ciphertext
      ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
      byte[] iv = new byte[GCM_IV_LENGTH];
      byteBuffer.get(iv);
      byte[] ciphertextBytes = new byte[byteBuffer.remaining()];
      byteBuffer.get(ciphertextBytes);

      GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

      byte[] plaintext = cipher.doFinal(ciphertextBytes);
      return new String(plaintext, StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new EncryptionException("Decryption failed", e);
    }
  }

  @Override
  public String getName() {
    return "AES-256-GCM";
  }

  @Override
  public boolean isHealthy() {
    return defaultKey != null;
  }

  @Override
  public void shutdown() {
    keys.clear();
    defaultKey = null;
  }

  /**
   * Gets the key for the given key ID, or the default key if keyId is null/empty.
   */
  private SecretKey getKey(String keyId) {
    if (keyId == null || keyId.isEmpty()) {
      return defaultKey;
    }

    SecretKey key = keys.get(keyId);
    if (key == null) {
      throw new EncryptionException("Encryption key not found: " + keyId);
    }
    return key;
  }

  /**
   * Generates a new 256-bit AES key.
   */
  private SecretKey generateKey() throws Exception {
    KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
    keyGenerator.init(AES_KEY_SIZE, secureRandom);
    return keyGenerator.generateKey();
  }

  /**
   * Exports the default key as Base64 (for testing/backup purposes).
   *
   * <p><strong>Warning:</strong> Keep encryption keys secure. Never log or expose them.
   *
   * @return the Base64-encoded default key
   */
  public String exportDefaultKey() {
    if (defaultKey == null) {
      throw new IllegalStateException("No default key available");
    }
    return Base64.getEncoder().encodeToString(defaultKey.getEncoded());
  }
}
