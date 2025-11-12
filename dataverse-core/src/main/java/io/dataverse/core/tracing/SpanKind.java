package io.dataverse.core.tracing;

/**
 * Type of span in a distributed trace.
 *
 * <p>SpanKind describes the relationship between the span and its parent/children.
 *
 * <p><strong>Usage Guidelines:</strong>
 * <ul>
 *   <li><strong>CLIENT</strong> - Outbound synchronous request (e.g., HTTP client, database client)</li>
 *   <li><strong>SERVER</strong> - Inbound synchronous request handler</li>
 *   <li><strong>PRODUCER</strong> - Asynchronous message sender (e.g., Kafka producer)</li>
 *   <li><strong>CONSUMER</strong> - Asynchronous message receiver (e.g., Kafka consumer)</li>
 *   <li><strong>INTERNAL</strong> - Internal operation within a service</li>
 * </ul>
 *
 * <p><strong>Examples:</strong>
 * <pre>{@code
 * // Database query - CLIENT
 * Span span = tracer.spanBuilder("SELECT users")
 *     .setSpanKind(SpanKind.CLIENT)
 *     .startSpan();
 *
 * // HTTP endpoint handler - SERVER
 * Span span = tracer.spanBuilder("GET /users")
 *     .setSpanKind(SpanKind.SERVER)
 *     .startSpan();
 *
 * // Internal method - INTERNAL
 * Span span = tracer.spanBuilder("validateUser")
 *     .setSpanKind(SpanKind.INTERNAL)
 *     .startSpan();
 *
 * // Message queue send - PRODUCER
 * Span span = tracer.spanBuilder("send-user-event")
 *     .setSpanKind(SpanKind.PRODUCER)
 *     .startSpan();
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public enum SpanKind {

  /**
   * Internal operation within the application.
   *
   * <p>Default span kind. Use for spans that don't cross process boundaries.
   */
  INTERNAL,

  /**
   * Synchronous outbound request (client-side).
   *
   * <p>Use for:
   * <ul>
   *   <li>Database queries</li>
   *   <li>HTTP client requests</li>
   *   <li>RPC client calls</li>
   *   <li>Cache operations</li>
   * </ul>
   */
  CLIENT,

  /**
   * Synchronous inbound request handler (server-side).
   *
   * <p>Use for:
   * <ul>
   *   <li>HTTP request handlers</li>
   *   <li>RPC server handlers</li>
   *   <li>GraphQL resolvers</li>
   * </ul>
   */
  SERVER,

  /**
   * Asynchronous message producer.
   *
   * <p>Use for:
   * <ul>
   *   <li>Kafka producer</li>
   *   <li>RabbitMQ publisher</li>
   *   <li>SQS send message</li>
   * </ul>
   */
  PRODUCER,

  /**
   * Asynchronous message consumer.
   *
   * <p>Use for:
   * <ul>
   *   <li>Kafka consumer</li>
   *   <li>RabbitMQ consumer</li>
   *   <li>SQS receive message</li>
   * </ul>
   */
  CONSUMER
}
