package io.dataverse.core.tracing;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for TracingProvider.
 *
 * @author DataVerse SDK Team
 */
class TracingProviderTest {

  @BeforeEach
  void setUp() {
    TracingProvider.reset();
  }

  @AfterEach
  void tearDown() {
    TracingProvider.reset();
  }

  @Test
  void testDefaultTracer() {
    Tracer tracer = TracingProvider.getTracer();
    assertNotNull(tracer);

    // Default should be noop tracer
    Span span = tracer.spanBuilder("test").startSpan();
    assertFalse(span.isRecording());
  }

  @Test
  void testSetGlobalTracer() {
    Tracer mockTracer = Tracer.noop();
    TracingProvider.setGlobalTracer(mockTracer);

    assertEquals(mockTracer, TracingProvider.getTracer());
  }

  @Test
  void testSetGlobalTracer_Null() {
    TracingProvider.setGlobalTracer(null);

    // Should fall back to noop
    Tracer tracer = TracingProvider.getTracer();
    assertNotNull(tracer);
  }

  @Test
  void testCurrentSpan_Default() {
    Span span = TracingProvider.getCurrentSpan();

    assertNotNull(span);
    assertFalse(span.isRecording());
  }

  @Test
  void testSetCurrentSpan() {
    Tracer tracer = TracingProvider.getTracer();
    Span span = tracer.spanBuilder("test").startSpan();

    TracingProvider.setCurrentSpan(span);

    assertEquals(span, TracingProvider.getCurrentSpan());
  }

  @Test
  void testClearCurrentSpan() {
    Tracer tracer = TracingProvider.getTracer();
    Span span = tracer.spanBuilder("test").startSpan();

    TracingProvider.setCurrentSpan(span);
    assertEquals(span, TracingProvider.getCurrentSpan());

    TracingProvider.clearCurrentSpan();

    // Should return noop span after clear
    Span current = TracingProvider.getCurrentSpan();
    assertFalse(current.isRecording());
  }

  @Test
  void testWithSpan_Runnable() {
    Tracer tracer = TracingProvider.getTracer();
    Span span = tracer.spanBuilder("test").startSpan();

    final Span[] capturedSpan = {null};

    TracingProvider.withSpan(span, () -> {
      capturedSpan[0] = TracingProvider.getCurrentSpan();
    });

    assertEquals(span, capturedSpan[0]);

    // Should be cleared after execution
    assertNotEquals(span, TracingProvider.getCurrentSpan());
  }

  @Test
  void testWithSpan_Callable() throws Exception {
    Tracer tracer = TracingProvider.getTracer();
    Span span = tracer.spanBuilder("test").startSpan();

    String result = TracingProvider.withSpan(span, () -> {
      assertEquals(span, TracingProvider.getCurrentSpan());
      return "success";
    });

    assertEquals("success", result);
  }

  @Test
  void testWithSpan_RestoresPreviousSpan() {
    Tracer tracer = TracingProvider.getTracer();
    Span outerSpan = tracer.spanBuilder("outer").startSpan();
    Span innerSpan = tracer.spanBuilder("inner").startSpan();

    TracingProvider.setCurrentSpan(outerSpan);

    TracingProvider.withSpan(innerSpan, () -> {
      assertEquals(innerSpan, TracingProvider.getCurrentSpan());
    });

    // Outer span should be restored
    assertEquals(outerSpan, TracingProvider.getCurrentSpan());
  }

  @Test
  void testWithSpan_CallableThrowsException() {
    Tracer tracer = TracingProvider.getTracer();
    Span span = tracer.spanBuilder("test").startSpan();

    assertThrows(RuntimeException.class, () ->
        TracingProvider.withSpan(span, () -> {
          throw new RuntimeException("test error");
        })
    );

    // Span should still be cleared
    assertNotEquals(span, TracingProvider.getCurrentSpan());
  }

  @Test
  void testChildSpanBuilder() {
    Tracer tracer = TracingProvider.getTracer();
    Span parentSpan = tracer.spanBuilder("parent").startSpan();

    TracingProvider.setCurrentSpan(parentSpan);

    SpanBuilder childBuilder = TracingProvider.childSpanBuilder("child");
    assertNotNull(childBuilder);

    // Note: Can't easily test parent relationship with noop tracer
  }

  @Test
  void testChildSpanBuilder_NoCurrentSpan() {
    SpanBuilder builder = TracingProvider.childSpanBuilder("test");
    assertNotNull(builder);

    Span span = builder.startSpan();
    assertNotNull(span);
  }

  @Test
  void testReset() {
    Tracer mockTracer = Tracer.noop();
    Span mockSpan = mockTracer.spanBuilder("test").startSpan();

    TracingProvider.setGlobalTracer(mockTracer);
    TracingProvider.setCurrentSpan(mockSpan);

    TracingProvider.reset();

    // Should be reset to defaults
    assertNotEquals(mockTracer, TracingProvider.getTracer());
    assertNotEquals(mockSpan, TracingProvider.getCurrentSpan());
  }

  @Test
  void testThreadIsolation() throws InterruptedException {
    Tracer tracer = TracingProvider.getTracer();
    Span span1 = tracer.spanBuilder("span1").startSpan();
    Span span2 = tracer.spanBuilder("span2").startSpan();

    TracingProvider.setCurrentSpan(span1);

    Thread thread = new Thread(() -> {
      TracingProvider.setCurrentSpan(span2);
      assertEquals(span2, TracingProvider.getCurrentSpan());
    });

    thread.start();
    thread.join();

    // Main thread should still have span1
    assertEquals(span1, TracingProvider.getCurrentSpan());
  }
}
