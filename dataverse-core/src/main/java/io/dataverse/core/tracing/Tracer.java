package io.dataverse.core.tracing;

/**
 * Interface for creating and managing distributed traces.
 *
 * <p>Tracer is the entry point for instrumenting code with distributed tracing.
 * It creates spans that represent individual operations in a trace.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * public class UserRepository {
 *     private final Tracer tracer;
 *
 *     public User findById(Long id) {
 *         Span span = tracer.spanBuilder("UserRepository.findById")
 *             .setSpanKind(SpanKind.INTERNAL)
 *             .setAttribute("user.id", id)
 *             .startSpan();
 *
 *         try {
 *             // Database operation
 *             Span dbSpan = tracer.spanBuilder("SELECT users")
 *                 .setSpanKind(SpanKind.CLIENT)
 *                 .setAttribute("db.system", "postgresql")
 *                 .setAttribute("db.statement", "SELECT * FROM users WHERE id = ?")
 *                 .setParent(span)
 *                 .startSpan();
 *
 *             try {
 *                 User user = executeQuery(id);
 *                 dbSpan.setStatus(SpanStatus.OK);
 *                 return user;
 *             } finally {
 *                 dbSpan.end();
 *             }
 *         } finally {
 *             span.end();
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Try-with-resources Pattern:</strong>
 * <pre>{@code
 * try (Scope scope = tracer.spanBuilder("operation")
 *         .startSpan()
 *         .makeCurrent()) {
 *     // Span is automatically current in this scope
 *     // and will be ended when scope closes
 *     doWork();
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface Tracer {

  /**
   * Creates a span builder for constructing a new span.
   *
   * @param spanName the span name
   * @return span builder
   */
  SpanBuilder spanBuilder(String spanName);

  /**
   * Gets the currently active span.
   *
   * @return current span, or NoopSpan if none active
   */
  Span getCurrentSpan();

  /**
   * Creates a noop tracer that doesn't record anything.
   *
   * <p>Useful for disabling tracing or as a default when no tracer is configured.
   *
   * @return noop tracer
   */
  static Tracer noop() {
    return NoopTracer.INSTANCE;
  }
}
