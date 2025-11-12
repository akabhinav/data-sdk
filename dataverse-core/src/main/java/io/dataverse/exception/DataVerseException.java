package io.dataverse.exception;

/**
 * Base exception for all DataVerse SDK exceptions.
 *
 * <p>This is the root of the DataVerse exception hierarchy. All checked and unchecked
 * exceptions thrown by the SDK extend this class.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class DataVerseException extends RuntimeException {

  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message the detail message
   */
  public DataVerseException(String message) {
    super(message);
  }

  /**
   * Constructs a new exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause
   */
  public DataVerseException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new exception with the specified cause.
   *
   * @param cause the cause
   */
  public DataVerseException(Throwable cause) {
    super(cause);
  }
}
