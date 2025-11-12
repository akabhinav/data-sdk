package io.dataverse.core.encryption;

/**
 * No-op encryption provider that returns data unchanged.
 *
 * <p>Used when encryption is disabled or not configured.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class NoEncryptionProvider implements EncryptionProvider {

  @Override
  public String encrypt(String plaintext, String keyId, boolean deterministic) {
    return plaintext;
  }

  @Override
  public String decrypt(String ciphertext, String keyId) {
    return ciphertext;
  }

  @Override
  public String getName() {
    return "NoEncryption";
  }

  @Override
  public boolean isHealthy() {
    return true;
  }

  @Override
  public void shutdown() {
    // No-op
  }
}
