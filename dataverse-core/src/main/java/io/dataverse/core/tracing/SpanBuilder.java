package io.dataverse.core.tracing;

import java.time.Instant;
import java.util.Map;

/**
 * Builder for creating spans.
 *
 * <p>SpanBuilder provides a fluent API for configuring span attributes,
 * context, and timing before starting the span.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * Span span = tracer.spanBuilder("db.query.users")
 *     .setSpanKind(SpanKind.CLIENT)
 *     .setAttribute("db.system", "postgresql")
 *     .setAttribute("db.name", "myapp")
 *     .setAttribute("db.statement", "SELECT * FROM users WHERE active = true")
 *     .setAttribute("db.operation", "SELECT")
 *     .setParent(parentSpan)
 *     .setStartTimestamp(Instant.now())
 *     .startSpan();
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface SpanBuilder {

  /**
   * Sets the span kind.
   *
   * @param spanKind the span kind
   * @return this builder
   */
  SpanBuilder setSpanKind(SpanKind spanKind);

  /**
   * Sets the parent span.
   *
   * @param parent the parent span
   * @return this builder
   */
  SpanBuilder setParent(Span parent);

  /**
   * Sets the parent span context.
   *
   * @param parentContext the parent span context
   * @return this builder
   */
  SpanBuilder setParent(SpanContext parentContext);

  /**
   * Sets no parent (creates root span).
   *
   * @return this builder
   */
  SpanBuilder setNoParent();

  /**
   * Sets a string attribute.
   *
   * @param key attribute key
   * @param value attribute value
   * @return this builder
   */
  SpanBuilder setAttribute(String key, String value);

  /**
   * Sets a long attribute.
   *
   * @param key attribute key
   * @param value attribute value
   * @return this builder
   */
  SpanBuilder setAttribute(String key, long value);

  /**
   * Sets a boolean attribute.
   *
   * @param key attribute key
   * @param value attribute value
   * @return this builder
   */
  SpanBuilder setAttribute(String key, boolean value);

  /**
   * Sets a double attribute.
   *
   * @param key attribute key
   * @param value attribute value
   * @return this builder
   */
  SpanBuilder setAttribute(String key, double value);

  /**
   * Sets multiple attributes.
   *
   * @param attributes map of attributes
   * @return this builder
   */
  SpanBuilder setAttributes(Map<String, Object> attributes);

  /**
   * Sets the start timestamp.
   *
   * <p>If not set, uses current time when span is started.
   *
   * @param startTime the start timestamp
   * @return this builder
   */
  SpanBuilder setStartTimestamp(Instant startTime);

  /**
   * Starts the span and makes it current.
   *
   * @return the started span
   */
  Span startSpan();
}
