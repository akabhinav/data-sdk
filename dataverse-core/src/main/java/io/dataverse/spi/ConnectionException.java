package io.dataverse.spi;

/**
 * Exception thrown when connection operations fail.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class ConnectionException extends AdapterException {

  /**
   * Constructs a new connection exception with the specified detail message.
   *
   * @param message the detail message
   */
  public ConnectionException(String message) {
    super(message);
  }

  /**
   * Constructs a new connection exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause
   */
  public ConnectionException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new connection exception with the specified cause.
   *
   * @param cause the cause
   */
  public ConnectionException(Throwable cause) {
    super(cause);
  }
}
