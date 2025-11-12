package io.dataverse.core.tracing;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a single operation in a distributed trace.
 *
 * <p>A span tracks the duration and metadata of an operation, such as a database
 * query, HTTP request, or method execution.
 *
 * <p><strong>Span Lifecycle:</strong>
 * <pre>{@code
 * // Create and start span
 * Span span = tracer.spanBuilder("db.query")
 *     .setSpanKind(SpanKind.CLIENT)
 *     .setAttribute("db.system", "postgresql")
 *     .setAttribute("db.statement", "SELECT * FROM users")
 *     .startSpan();
 *
 * try {
 *     // Perform operation
 *     List<User> users = executeQuery();
 *
 *     // Add result metadata
 *     span.setAttribute("db.rows_returned", users.size());
 *     span.setStatus(SpanStatus.OK);
 *
 * } catch (Exception e) {
 *     // Record error
 *     span.setStatus(SpanStatus.ERROR, e.getMessage());
 *     span.recordException(e);
 *     throw e;
 *
 * } finally {
 *     // Always end span
 *     span.end();
 * }
 * }</pre>
 *
 * <p><strong>Semantic Conventions:</strong>
 * Database operations should include:
 * <ul>
 *   <li>db.system - Database type (postgresql, mongodb, redis)</li>
 *   <li>db.name - Database name</li>
 *   <li>db.statement - Query text (sanitized)</li>
 *   <li>db.operation - Operation type (SELECT, INSERT, UPDATE)</li>
 *   <li>db.user - Database user</li>
 *   <li>net.peer.name - Database host</li>
 *   <li>net.peer.port - Database port</li>
 * </ul>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface Span {

  /**
   * Sets a string attribute on the span.
   *
   * @param key attribute key
   * @param value attribute value
   * @return this span
   */
  Span setAttribute(String key, String value);

  /**
   * Sets a long attribute on the span.
   *
   * @param key attribute key
   * @param value attribute value
   * @return this span
   */
  Span setAttribute(String key, long value);

  /**
   * Sets a boolean attribute on the span.
   *
   * @param key attribute key
   * @param value attribute value
   * @return this span
   */
  Span setAttribute(String key, boolean value);

  /**
   * Sets a double attribute on the span.
   *
   * @param key attribute key
   * @param value attribute value
   * @return this span
   */
  Span setAttribute(String key, double value);

  /**
   * Sets multiple attributes at once.
   *
   * @param attributes map of attributes
   * @return this span
   */
  Span setAttributes(Map<String, Object> attributes);

  /**
   * Adds an event to the span.
   *
   * <p>Events are timestamped messages that can include attributes.
   *
   * @param name event name
   * @return this span
   */
  Span addEvent(String name);

  /**
   * Adds an event with attributes.
   *
   * @param name event name
   * @param attributes event attributes
   * @return this span
   */
  Span addEvent(String name, Map<String, Object> attributes);

  /**
   * Records an exception on the span.
   *
   * @param exception the exception to record
   * @return this span
   */
  Span recordException(Throwable exception);

  /**
   * Sets the span status.
   *
   * @param status the status
   * @return this span
   */
  Span setStatus(SpanStatus status);

  /**
   * Sets the span status with description.
   *
   * @param status the status
   * @param description status description
   * @return this span
   */
  Span setStatus(SpanStatus status, String description);

  /**
   * Ends the span.
   *
   * <p>Must be called exactly once. After calling end(), no further
   * modifications to the span are allowed.
   */
  void end();

  /**
   * Ends the span at a specific timestamp.
   *
   * @param endTime the end timestamp
   */
  void end(Instant endTime);

  /**
   * Gets the span context.
   *
   * @return the span context
   */
  SpanContext getSpanContext();

  /**
   * Checks if this span is recording.
   *
   * <p>Non-recording spans (like NoopSpan) return false.
   *
   * @return true if recording
   */
  boolean isRecording();

  /**
   * Updates the span name.
   *
   * @param name new span name
   * @return this span
   */
  Span updateName(String name);
}
