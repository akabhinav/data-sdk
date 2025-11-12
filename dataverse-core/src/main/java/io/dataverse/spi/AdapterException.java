package io.dataverse.spi;

import io.dataverse.exception.DataVerseException;

/**
 * Exception thrown by adapter operations.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class AdapterException extends DataVerseException {

  /**
   * Constructs a new adapter exception with the specified detail message.
   *
   * @param message the detail message
   */
  public AdapterException(String message) {
    super(message);
  }

  /**
   * Constructs a new adapter exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause
   */
  public AdapterException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new adapter exception with the specified cause.
   *
   * @param cause the cause
   */
  public AdapterException(Throwable cause) {
    super(cause);
  }
}
