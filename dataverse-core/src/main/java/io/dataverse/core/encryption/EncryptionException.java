package io.dataverse.core.encryption;

/**
 * Exception thrown when encryption or decryption operations fail.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class EncryptionException extends RuntimeException {

  /**
   * Constructs a new encryption exception with the specified message.
   *
   * @param message the detail message
   */
  public EncryptionException(String message) {
    super(message);
  }

  /**
   * Constructs a new encryption exception with the specified message and cause.
   *
   * @param message the detail message
   * @param cause the cause
   */
  public EncryptionException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new encryption exception with the specified cause.
   *
   * @param cause the cause
   */
  public EncryptionException(Throwable cause) {
    super(cause);
  }
}
