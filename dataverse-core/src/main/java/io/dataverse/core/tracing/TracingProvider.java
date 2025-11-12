package io.dataverse.core.tracing;

import java.util.concurrent.Callable;

/**
 * Global provider for accessing tracers and managing trace context.
 *
 * <p>TracingProvider is the central point for obtaining tracers and managing
 * the current trace context in a thread-safe manner.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Set global tracer (typically at application startup)
 * TracingProvider.setGlobalTracer(myTracer);
 *
 * // Get tracer
 * Tracer tracer = TracingProvider.getTracer();
 *
 * // Create and use span
 * Span span = tracer.spanBuilder("operation").startSpan();
 * try {
 *     doWork();
 * } finally {
 *     span.end();
 * }
 *
 * // Or use auto-scoping
 * TracingProvider.withSpan(span, () -> {
 *     doWork();
 *     return result;
 * });
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class TracingProvider {

  private static volatile Tracer globalTracer = Tracer.noop();
  private static final ThreadLocal<Span> currentSpan = new ThreadLocal<>();

  /**
   * Sets the global tracer.
   *
   * @param tracer the tracer to use globally
   */
  public static void setGlobalTracer(Tracer tracer) {
    globalTracer = tracer != null ? tracer : Tracer.noop();
  }

  /**
   * Gets the global tracer.
   *
   * @return the global tracer
   */
  public static Tracer getTracer() {
    return globalTracer;
  }

  /**
   * Gets the current span for this thread.
   *
   * @return current span, or NoopSpan if none set
   */
  public static Span getCurrentSpan() {
    Span span = currentSpan.get();
    return span != null ? span : NoopTracer.NoopSpan.INSTANCE;
  }

  /**
   * Sets the current span for this thread.
   *
   * @param span the span to set as current
   */
  public static void setCurrentSpan(Span span) {
    if (span != null) {
      currentSpan.set(span);
    } else {
      currentSpan.remove();
    }
  }

  /**
   * Clears the current span for this thread.
   */
  public static void clearCurrentSpan() {
    currentSpan.remove();
  }

  /**
   * Executes a runnable with the given span as current.
   *
   * <p>The span is automatically set as current before execution
   * and restored after execution completes.
   *
   * @param span the span to make current
   * @param runnable the code to execute
   */
  public static void withSpan(Span span, Runnable runnable) {
    Span previous = currentSpan.get();
    try {
      setCurrentSpan(span);
      runnable.run();
    } finally {
      if (previous != null) {
        setCurrentSpan(previous);
      } else {
        clearCurrentSpan();
      }
    }
  }

  /**
   * Executes a callable with the given span as current.
   *
   * <p>The span is automatically set as current before execution
   * and restored after execution completes.
   *
   * @param <T> the return type
   * @param span the span to make current
   * @param callable the code to execute
   * @return the result
   * @throws Exception if callable throws
   */
  public static <T> T withSpan(Span span, Callable<T> callable) throws Exception {
    Span previous = currentSpan.get();
    try {
      setCurrentSpan(span);
      return callable.call();
    } finally {
      if (previous != null) {
        setCurrentSpan(previous);
      } else {
        clearCurrentSpan();
      }
    }
  }

  /**
   * Creates a child span of the current span.
   *
   * @param spanName the span name
   * @return span builder with current span as parent
   */
  public static SpanBuilder childSpanBuilder(String spanName) {
    Span current = getCurrentSpan();
    SpanBuilder builder = getTracer().spanBuilder(spanName);

    if (current.isRecording()) {
      builder.setParent(current);
    }

    return builder;
  }

  /**
   * Resets the tracing provider to default state.
   *
   * <p>Used primarily for testing.
   */
  public static void reset() {
    globalTracer = Tracer.noop();
    currentSpan.remove();
  }
}
