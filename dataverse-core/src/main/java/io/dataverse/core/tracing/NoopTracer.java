package io.dataverse.core.tracing;

import java.time.Instant;
import java.util.Map;

/**
 * No-op tracer implementation that doesn't record anything.
 *
 * <p>Used as a default when tracing is disabled or no tracer is configured.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
final class NoopTracer implements Tracer {

  static final NoopTracer INSTANCE = new NoopTracer();

  private NoopTracer() {
  }

  @Override
  public SpanBuilder spanBuilder(String spanName) {
    return NoopSpanBuilder.INSTANCE;
  }

  @Override
  public Span getCurrentSpan() {
    return NoopSpan.INSTANCE;
  }

  /**
   * No-op span builder.
   */
  private static final class NoopSpanBuilder implements SpanBuilder {
    static final NoopSpanBuilder INSTANCE = new NoopSpanBuilder();

    @Override
    public SpanBuilder setSpanKind(SpanKind spanKind) {
      return this;
    }

    @Override
    public SpanBuilder setParent(Span parent) {
      return this;
    }

    @Override
    public SpanBuilder setParent(SpanContext parentContext) {
      return this;
    }

    @Override
    public SpanBuilder setNoParent() {
      return this;
    }

    @Override
    public SpanBuilder setAttribute(String key, String value) {
      return this;
    }

    @Override
    public SpanBuilder setAttribute(String key, long value) {
      return this;
    }

    @Override
    public SpanBuilder setAttribute(String key, boolean value) {
      return this;
    }

    @Override
    public SpanBuilder setAttribute(String key, double value) {
      return this;
    }

    @Override
    public SpanBuilder setAttributes(Map<String, Object> attributes) {
      return this;
    }

    @Override
    public SpanBuilder setStartTimestamp(Instant startTime) {
      return this;
    }

    @Override
    public Span startSpan() {
      return NoopSpan.INSTANCE;
    }
  }

  /**
   * No-op span implementation.
   */
  static final class NoopSpan implements Span {
    static final NoopSpan INSTANCE = new NoopSpan();

    @Override
    public Span setAttribute(String key, String value) {
      return this;
    }

    @Override
    public Span setAttribute(String key, long value) {
      return this;
    }

    @Override
    public Span setAttribute(String key, boolean value) {
      return this;
    }

    @Override
    public Span setAttribute(String key, double value) {
      return this;
    }

    @Override
    public Span setAttributes(Map<String, Object> attributes) {
      return this;
    }

    @Override
    public Span addEvent(String name) {
      return this;
    }

    @Override
    public Span addEvent(String name, Map<String, Object> attributes) {
      return this;
    }

    @Override
    public Span recordException(Throwable exception) {
      return this;
    }

    @Override
    public Span setStatus(SpanStatus status) {
      return this;
    }

    @Override
    public Span setStatus(SpanStatus status, String description) {
      return this;
    }

    @Override
    public void end() {
      // No-op
    }

    @Override
    public void end(Instant endTime) {
      // No-op
    }

    @Override
    public SpanContext getSpanContext() {
      return NoopSpanContext.INSTANCE;
    }

    @Override
    public boolean isRecording() {
      return false;
    }

    @Override
    public Span updateName(String name) {
      return this;
    }
  }

  /**
   * No-op span context implementation.
   */
  private static final class NoopSpanContext implements SpanContext {
    static final NoopSpanContext INSTANCE = new NoopSpanContext();

    @Override
    public String getTraceId() {
      return "00000000000000000000000000000000";
    }

    @Override
    public String getSpanId() {
      return "0000000000000000";
    }

    @Override
    public boolean isValid() {
      return false;
    }

    @Override
    public boolean isSampled() {
      return false;
    }

    @Override
    public boolean isRemote() {
      return false;
    }
  }
}
