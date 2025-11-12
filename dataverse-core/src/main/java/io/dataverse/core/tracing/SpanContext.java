package io.dataverse.core.tracing;

/**
 * Immutable representation of a span's context.
 *
 * <p>SpanContext contains the identifiers needed to correlate spans
 * in a distributed trace.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface SpanContext {

  /**
   * Gets the trace ID.
   *
   * @return trace ID
   */
  String getTraceId();

  /**
   * Gets the span ID.
   *
   * @return span ID
   */
  String getSpanId();

  /**
   * Checks if this context is valid.
   *
   * <p>A context is valid if it has non-zero trace and span IDs.
   *
   * @return true if valid
   */
  boolean isValid();

  /**
   * Checks if this trace is sampled.
   *
   * @return true if sampled
   */
  boolean isSampled();

  /**
   * Checks if this context is remote.
   *
   * <p>Remote contexts are extracted from incoming requests.
   *
   * @return true if remote
   */
  boolean isRemote();
}
