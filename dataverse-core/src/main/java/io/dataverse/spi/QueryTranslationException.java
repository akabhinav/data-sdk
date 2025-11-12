package io.dataverse.spi;

/**
 * Exception thrown when query translation fails.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class QueryTranslationException extends AdapterException {

  /**
   * Constructs a new query translation exception with the specified detail message.
   *
   * @param message the detail message
   */
  public QueryTranslationException(String message) {
    super(message);
  }

  /**
   * Constructs a new query translation exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause
   */
  public QueryTranslationException(String message, Throwable cause) {
    super(message, cause);
  }
}
