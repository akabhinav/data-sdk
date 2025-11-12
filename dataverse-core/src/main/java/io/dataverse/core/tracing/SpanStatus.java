package io.dataverse.core.tracing;

/**
 * Status of a span.
 *
 * <p>Indicates whether the operation completed successfully or with an error.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public enum SpanStatus {

  /**
   * The operation completed successfully.
   */
  OK,

  /**
   * The operation completed with an error.
   */
  ERROR,

  /**
   * Status not set (default).
   */
  UNSET
}
