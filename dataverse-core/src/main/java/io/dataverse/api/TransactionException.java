package io.dataverse.api;

/**
 * Exception thrown when transaction operations fail.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class TransactionException extends RuntimeException {

  /**
   * Constructs a new transaction exception with the specified detail message.
   *
   * @param message the detail message
   */
  public TransactionException(String message) {
    super(message);
  }

  /**
   * Constructs a new transaction exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause
   */
  public TransactionException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new transaction exception with the specified cause.
   *
   * @param cause the cause
   */
  public TransactionException(Throwable cause) {
    super(cause);
  }
}
