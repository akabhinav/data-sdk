package io.dataverse.core.tracing;

/**
 * Semantic attribute keys for distributed tracing.
 *
 * <p>Defines standard attribute names following OpenTelemetry semantic conventions
 * for database operations, network, and general service attributes.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * Span span = tracer.spanBuilder("db.query")
 *     .setAttribute(SemanticAttributes.DB_SYSTEM, "postgresql")
 *     .setAttribute(SemanticAttributes.DB_NAME, "myapp")
 *     .setAttribute(SemanticAttributes.DB_STATEMENT, "SELECT * FROM users")
 *     .setAttribute(SemanticAttributes.DB_OPERATION, "SELECT")
 *     .setAttribute(SemanticAttributes.NET_PEER_NAME, "localhost")
 *     .setAttribute(SemanticAttributes.NET_PEER_PORT, 5432)
 *     .startSpan();
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/">OpenTelemetry Semantic Conventions</a>
 */
public final class SemanticAttributes {

  // Database attributes

  /**
   * Database system identifier (e.g., "postgresql", "mongodb", "redis").
   */
  public static final String DB_SYSTEM = "db.system";

  /**
   * Database name being accessed.
   */
  public static final String DB_NAME = "db.name";

  /**
   * Database statement/query being executed.
   */
  public static final String DB_STATEMENT = "db.statement";

  /**
   * Database operation name (e.g., "SELECT", "INSERT", "UPDATE", "find", "aggregate").
   */
  public static final String DB_OPERATION = "db.operation";

  /**
   * Database user for the connection.
   */
  public static final String DB_USER = "db.user";

  /**
   * Database connection string (sanitized, without credentials).
   */
  public static final String DB_CONNECTION_STRING = "db.connection_string";

  /**
   * SQL table name being accessed.
   */
  public static final String DB_SQL_TABLE = "db.sql.table";

  /**
   * MongoDB collection name.
   */
  public static final String DB_MONGODB_COLLECTION = "db.mongodb.collection";

  /**
   * Redis database index.
   */
  public static final String DB_REDIS_DATABASE_INDEX = "db.redis.database_index";

  /**
   * Number of rows/documents returned by query.
   */
  public static final String DB_ROWS_RETURNED = "db.rows_returned";

  /**
   * Number of rows/documents affected by operation.
   */
  public static final String DB_ROWS_AFFECTED = "db.rows_affected";

  // Network attributes

  /**
   * Remote hostname or IP address.
   */
  public static final String NET_PEER_NAME = "net.peer.name";

  /**
   * Remote port number.
   */
  public static final String NET_PEER_PORT = "net.peer.port";

  /**
   * Transport protocol (e.g., "tcp", "udp").
   */
  public static final String NET_TRANSPORT = "net.transport";

  // Service attributes

  /**
   * Service name.
   */
  public static final String SERVICE_NAME = "service.name";

  /**
   * Service namespace (e.g., "production", "staging").
   */
  public static final String SERVICE_NAMESPACE = "service.namespace";

  /**
   * Service instance ID.
   */
  public static final String SERVICE_INSTANCE_ID = "service.instance.id";

  /**
   * Service version.
   */
  public static final String SERVICE_VERSION = "service.version";

  // Error attributes

  /**
   * Error type (exception class name).
   */
  public static final String ERROR_TYPE = "error.type";

  /**
   * Error message.
   */
  public static final String ERROR_MESSAGE = "error.message";

  /**
   * Error stack trace.
   */
  public static final String ERROR_STACK_TRACE = "error.stack_trace";

  // HTTP attributes (for REST adapters)

  /**
   * HTTP method (GET, POST, etc.).
   */
  public static final String HTTP_METHOD = "http.method";

  /**
   * HTTP URL.
   */
  public static final String HTTP_URL = "http.url";

  /**
   * HTTP status code.
   */
  public static final String HTTP_STATUS_CODE = "http.status_code";

  /**
   * HTTP user agent.
   */
  public static final String HTTP_USER_AGENT = "http.user_agent";

  // DataVerse-specific attributes

  /**
   * DataVerse entity type.
   */
  public static final String DATAVERSE_ENTITY_TYPE = "dataverse.entity.type";

  /**
   * DataVerse repository class.
   */
  public static final String DATAVERSE_REPOSITORY = "dataverse.repository";

  /**
   * DataVerse adapter type.
   */
  public static final String DATAVERSE_ADAPTER = "dataverse.adapter";

  /**
   * DataVerse operation type (save, find, delete, query).
   */
  public static final String DATAVERSE_OPERATION = "dataverse.operation";

  /**
   * Whether query used cache.
   */
  public static final String DATAVERSE_CACHE_HIT = "dataverse.cache.hit";

  /**
   * Query fetch strategy (eager, lazy, batch).
   */
  public static final String DATAVERSE_FETCH_STRATEGY = "dataverse.fetch_strategy";

  private SemanticAttributes() {
    // Utility class
  }
}
