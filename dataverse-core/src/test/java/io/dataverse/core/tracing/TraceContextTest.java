package io.dataverse.core.tracing;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for TraceContext.
 *
 * @author DataVerse SDK Team
 */
class TraceContextTest {

  @Test
  void testCreate() {
    TraceContext context = TraceContext.create();

    assertNotNull(context.getTraceId());
    assertNotNull(context.getSpanId());
    assertNull(context.getParentSpanId());
    assertTrue(context.isSampled());
    assertTrue(context.getAllBaggage().isEmpty());
  }

  @Test
  void testCreateWithTraceId() {
    String traceId = "0123456789abcdef0123456789abcdef";
    TraceContext context = TraceContext.create(traceId);

    assertEquals(traceId, context.getTraceId());
    assertNotNull(context.getSpanId());
    assertTrue(context.isSampled());
  }

  @Test
  void testCreateChild() {
    TraceContext parent = TraceContext.create();
    TraceContext child = parent.createChild();

    // Child should have same trace ID
    assertEquals(parent.getTraceId(), child.getTraceId());

    // Child should have different span ID
    assertNotEquals(parent.getSpanId(), child.getSpanId());

    // Child's parent should be parent's span
    assertEquals(parent.getSpanId(), child.getParentSpanId());

    // Child should inherit sampled flag
    assertEquals(parent.isSampled(), child.isSampled());
  }

  @Test
  void testBaggage() {
    TraceContext context = TraceContext.create();

    context.putBaggage("user.id", "12345");
    context.putBaggage("tenant.id", "acme");

    assertEquals("12345", context.getBaggage("user.id").orElse(null));
    assertEquals("acme", context.getBaggage("tenant.id").orElse(null));
    assertTrue(context.getBaggage("nonexistent").isEmpty());

    assertEquals(2, context.getAllBaggage().size());
  }

  @Test
  void testBaggage_Inheritance() {
    TraceContext parent = TraceContext.create();
    parent.putBaggage("user.id", "12345");

    TraceContext child = parent.createChild();

    // Child should inherit parent's baggage
    assertEquals("12345", child.getBaggage("user.id").orElse(null));

    // Modifying child doesn't affect parent
    child.putBaggage("request.id", "abc");
    assertTrue(parent.getBaggage("request.id").isEmpty());
  }

  @Test
  void testToHeaders() {
    TraceContext context = TraceContext.builder()
        .traceId("0123456789abcdef0123456789abcdef")
        .spanId("0123456789abcdef")
        .sampled(true)
        .build();

    context.putBaggage("user.id", "12345");

    Map<String, String> headers = context.toHeaders();

    // Check traceparent header
    String traceparent = headers.get("traceparent");
    assertNotNull(traceparent);
    assertTrue(traceparent.startsWith("00-"));
    assertTrue(traceparent.contains("0123456789abcdef0123456789abcdef"));
    assertTrue(traceparent.contains("0123456789abcdef"));
    assertTrue(traceparent.endsWith("-01"));  // Sampled flag

    // Check baggage header
    String baggage = headers.get("baggage");
    assertNotNull(baggage);
    assertTrue(baggage.contains("user.id=12345"));
  }

  @Test
  void testToHeaders_NotSampled() {
    TraceContext context = TraceContext.builder()
        .traceId("0123456789abcdef0123456789abcdef")
        .spanId("0123456789abcdef")
        .sampled(false)
        .build();

    Map<String, String> headers = context.toHeaders();
    String traceparent = headers.get("traceparent");

    assertTrue(traceparent.endsWith("-00"));  // Not sampled
  }

  @Test
  void testToHeaders_NoBaggage() {
    TraceContext context = TraceContext.create();

    Map<String, String> headers = context.toHeaders();

    assertNotNull(headers.get("traceparent"));
    assertNull(headers.get("baggage"));
  }

  @Test
  void testFromHeaders() {
    Map<String, String> headers = Map.of(
        "traceparent", "00-0123456789abcdef0123456789abcdef-0123456789abcdef-01",
        "baggage", "user.id=12345,tenant.id=acme"
    );

    TraceContext context = TraceContext.fromHeaders(headers).orElseThrow();

    assertEquals("0123456789abcdef0123456789abcdef", context.getTraceId());
    assertEquals("0123456789abcdef", context.getSpanId());
    assertTrue(context.isSampled());
    assertEquals("12345", context.getBaggage("user.id").orElse(null));
    assertEquals("acme", context.getBaggage("tenant.id").orElse(null));
  }

  @Test
  void testFromHeaders_CaseInsensitive() {
    Map<String, String> headers = Map.of(
        "Traceparent", "00-0123456789abcdef0123456789abcdef-0123456789abcdef-01"
    );

    TraceContext context = TraceContext.fromHeaders(headers).orElseThrow();

    assertEquals("0123456789abcdef0123456789abcdef", context.getTraceId());
  }

  @Test
  void testFromHeaders_NoTraceHeaders() {
    Map<String, String> headers = Map.of("Content-Type", "application/json");

    assertTrue(TraceContext.fromHeaders(headers).isEmpty());
  }

  @Test
  void testFromHeaders_InvalidFormat() {
    Map<String, String> headers = Map.of(
        "traceparent", "invalid"
    );

    assertTrue(TraceContext.fromHeaders(headers).isEmpty());
  }

  @Test
  void testBuilder_RequiredFields() {
    assertThrows(IllegalArgumentException.class, () ->
        TraceContext.builder().build()
    );

    assertThrows(IllegalArgumentException.class, () ->
        TraceContext.builder()
            .traceId("0123456789abcdef0123456789abcdef")
            .build()
    );

    assertThrows(IllegalArgumentException.class, () ->
        TraceContext.builder()
            .spanId("0123456789abcdef")
            .build()
    );
  }

  @Test
  void testBuilder_FullConfiguration() {
    TraceContext context = TraceContext.builder()
        .traceId("0123456789abcdef0123456789abcdef")
        .spanId("0123456789abcdef")
        .parentSpanId("fedcba9876543210")
        .sampled(false)
        .baggage("key1", "value1")
        .baggage("key2", "value2")
        .build();

    assertEquals("0123456789abcdef0123456789abcdef", context.getTraceId());
    assertEquals("0123456789abcdef", context.getSpanId());
    assertEquals("fedcba9876543210", context.getParentSpanId());
    assertFalse(context.isSampled());
    assertEquals("value1", context.getBaggage("key1").orElse(null));
    assertEquals("value2", context.getBaggage("key2").orElse(null));
  }

  @Test
  void testToString() {
    TraceContext context = TraceContext.create();
    String str = context.toString();

    assertTrue(str.contains("TraceContext"));
    assertTrue(str.contains("traceId"));
    assertTrue(str.contains("spanId"));
  }

  @Test
  void testRoundTrip() {
    // Create context
    TraceContext original = TraceContext.create();
    original.putBaggage("user.id", "12345");

    // Convert to headers
    Map<String, String> headers = original.toHeaders();

    // Reconstruct from headers
    TraceContext reconstructed = TraceContext.fromHeaders(headers).orElseThrow();

    // Should have same trace and span IDs
    assertEquals(original.getTraceId(), reconstructed.getTraceId());
    assertEquals(original.getSpanId(), reconstructed.getSpanId());
    assertEquals(original.isSampled(), reconstructed.isSampled());
    assertEquals(original.getBaggage("user.id"), reconstructed.getBaggage("user.id"));
  }
}
