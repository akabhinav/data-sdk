package io.dataverse.core.tracing;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a distributed tracing context.
 *
 * <p>TraceContext carries trace identifiers and baggage across service boundaries,
 * enabling correlation of operations in distributed systems.
 *
 * <p><strong>Key Concepts:</strong>
 * <ul>
 *   <li><strong>Trace ID</strong> - Unique identifier for entire request flow</li>
 *   <li><strong>Span ID</strong> - Unique identifier for single operation</li>
 *   <li><strong>Parent Span ID</strong> - Links child operations to parent</li>
 *   <li><strong>Baggage</strong> - Key-value pairs propagated with context</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Create root trace context
 * TraceContext context = TraceContext.create();
 *
 * // Create child span
 * TraceContext childContext = context.createChild();
 *
 * // Add baggage
 * context.putBaggage("user.id", "12345");
 * context.putBaggage("tenant.id", "acme-corp");
 *
 * // Extract propagation headers
 * Map<String, String> headers = context.toHeaders();
 * // Send headers with HTTP request...
 *
 * // Reconstruct context from headers
 * TraceContext receivedContext = TraceContext.fromHeaders(headers);
 * }</pre>
 *
 * <p><strong>W3C Trace Context Format:</strong>
 * <pre>
 * traceparent: 00-{trace-id}-{span-id}-{trace-flags}
 * tracestate: vendor1=value1,vendor2=value2
 * </pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class TraceContext {

  private final String traceId;
  private final String spanId;
  private final String parentSpanId;
  private final boolean sampled;
  private final Map<String, String> baggage;

  private TraceContext(Builder builder) {
    this.traceId = builder.traceId;
    this.spanId = builder.spanId;
    this.parentSpanId = builder.parentSpanId;
    this.sampled = builder.sampled;
    this.baggage = new HashMap<>(builder.baggage);
  }

  /**
   * Gets the trace ID.
   *
   * @return trace ID (32 hex characters)
   */
  public String getTraceId() {
    return traceId;
  }

  /**
   * Gets the span ID.
   *
   * @return span ID (16 hex characters)
   */
  public String getSpanId() {
    return spanId;
  }

  /**
   * Gets the parent span ID.
   *
   * @return parent span ID, or null if root span
   */
  public String getParentSpanId() {
    return parentSpanId;
  }

  /**
   * Checks if this trace is sampled.
   *
   * @return true if sampled
   */
  public boolean isSampled() {
    return sampled;
  }

  /**
   * Gets baggage value.
   *
   * @param key baggage key
   * @return optional containing value
   */
  public Optional<String> getBaggage(String key) {
    return Optional.ofNullable(baggage.get(key));
  }

  /**
   * Gets all baggage.
   *
   * @return immutable baggage map
   */
  public Map<String, String> getAllBaggage() {
    return Map.copyOf(baggage);
  }

  /**
   * Adds baggage to context.
   *
   * @param key baggage key
   * @param value baggage value
   */
  public void putBaggage(String key, String value) {
    baggage.put(key, value);
  }

  /**
   * Creates a child trace context.
   *
   * @return new child context
   */
  public TraceContext createChild() {
    return builder()
        .traceId(traceId)
        .spanId(generateSpanId())
        .parentSpanId(spanId)
        .sampled(sampled)
        .baggage(baggage)
        .build();
  }

  /**
   * Converts context to HTTP headers for propagation.
   *
   * <p>Uses W3C Trace Context format:
   * <pre>
   * traceparent: 00-{trace-id}-{span-id}-{flags}
   * </pre>
   *
   * @return header map
   */
  public Map<String, String> toHeaders() {
    Map<String, String> headers = new HashMap<>();

    // W3C traceparent header
    String flags = sampled ? "01" : "00";
    headers.put("traceparent", String.format("00-%s-%s-%s", traceId, spanId, flags));

    // Baggage headers
    if (!baggage.isEmpty()) {
      StringBuilder baggageHeader = new StringBuilder();
      baggage.forEach((key, value) -> {
        if (baggageHeader.length() > 0) {
          baggageHeader.append(",");
        }
        baggageHeader.append(key).append("=").append(value);
      });
      headers.put("baggage", baggageHeader.toString());
    }

    return headers;
  }

  /**
   * Creates context from HTTP headers.
   *
   * @param headers HTTP headers
   * @return trace context, or empty if no trace headers
   */
  public static Optional<TraceContext> fromHeaders(Map<String, String> headers) {
    String traceparent = headers.get("traceparent");
    if (traceparent == null) {
      traceparent = headers.get("Traceparent");  // Case-insensitive
    }

    if (traceparent == null) {
      return Optional.empty();
    }

    // Parse W3C traceparent: 00-{trace-id}-{span-id}-{flags}
    String[] parts = traceparent.split("-");
    if (parts.length != 4) {
      return Optional.empty();
    }

    String traceId = parts[1];
    String spanId = parts[2];
    boolean sampled = "01".equals(parts[3]);

    Builder builder = builder()
        .traceId(traceId)
        .spanId(spanId)
        .sampled(sampled);

    // Parse baggage header
    String baggageHeader = headers.getOrDefault("baggage", headers.get("Baggage"));
    if (baggageHeader != null) {
      for (String item : baggageHeader.split(",")) {
        String[] kv = item.split("=", 2);
        if (kv.length == 2) {
          builder.baggage(kv[0].trim(), kv[1].trim());
        }
      }
    }

    return Optional.of(builder.build());
  }

  /**
   * Creates a new root trace context.
   *
   * @return new trace context
   */
  public static TraceContext create() {
    return builder()
        .traceId(generateTraceId())
        .spanId(generateSpanId())
        .sampled(true)
        .build();
  }

  /**
   * Creates a new root trace context with custom trace ID.
   *
   * @param traceId the trace ID
   * @return new trace context
   */
  public static TraceContext create(String traceId) {
    return builder()
        .traceId(traceId)
        .spanId(generateSpanId())
        .sampled(true)
        .build();
  }

  /**
   * Creates a builder.
   *
   * @return new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Generates a random trace ID (32 hex characters).
   *
   * @return trace ID
   */
  private static String generateTraceId() {
    return String.format("%032x", System.nanoTime() * 1000 + System.identityHashCode(new Object()));
  }

  /**
   * Generates a random span ID (16 hex characters).
   *
   * @return span ID
   */
  private static String generateSpanId() {
    return String.format("%016x", System.nanoTime() + System.identityHashCode(new Object()));
  }

  @Override
  public String toString() {
    return "TraceContext{" +
        "traceId='" + traceId + '\'' +
        ", spanId='" + spanId + '\'' +
        ", parentSpanId='" + parentSpanId + '\'' +
        ", sampled=" + sampled +
        '}';
  }

  /**
   * Builder for TraceContext.
   */
  public static class Builder {
    private String traceId;
    private String spanId;
    private String parentSpanId;
    private boolean sampled = true;
    private Map<String, String> baggage = new HashMap<>();

    public Builder traceId(String traceId) {
      this.traceId = traceId;
      return this;
    }

    public Builder spanId(String spanId) {
      this.spanId = spanId;
      return this;
    }

    public Builder parentSpanId(String parentSpanId) {
      this.parentSpanId = parentSpanId;
      return this;
    }

    public Builder sampled(boolean sampled) {
      this.sampled = sampled;
      return this;
    }

    public Builder baggage(String key, String value) {
      this.baggage.put(key, value);
      return this;
    }

    public Builder baggage(Map<String, String> baggage) {
      this.baggage.putAll(baggage);
      return this;
    }

    public TraceContext build() {
      if (traceId == null) {
        throw new IllegalArgumentException("traceId is required");
      }
      if (spanId == null) {
        throw new IllegalArgumentException("spanId is required");
      }
      return new TraceContext(this);
    }
  }
}
