package io.dataverse.spi;

/**
 * Exception thrown when entity serialization or deserialization fails.
 *
 * <p>This exception wraps underlying serialization errors and provides context
 * about what went wrong during the serialization process.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class SerializationException extends RuntimeException {

  /**
   * Constructs a new serialization exception with the specified detail message.
   *
   * @param message the detail message
   */
  public SerializationException(String message) {
    super(message);
  }

  /**
   * Constructs a new serialization exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause
   */
  public SerializationException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new serialization exception with the specified cause.
   *
   * @param cause the cause
   */
  public SerializationException(Throwable cause) {
    super(cause);
  }
}
