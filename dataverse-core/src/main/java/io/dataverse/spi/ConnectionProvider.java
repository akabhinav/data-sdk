package io.dataverse.spi;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Service Provider Interface for managing connections to data sources.
 *
 * <p>ConnectionProvider handles connection lifecycle including acquisition, validation,
 * and release. Implementations should provide connection pooling optimized for Java 21
 * virtual threads.
 *
 * <p><strong>Thread Safety:</strong> All methods must be thread-safe and support concurrent
 * access from multiple virtual threads.
 *
 * @param <C> the native connection type
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface ConnectionProvider<C> {

  /**
   * Acquires a connection from the pool.
   *
   * <p>This method blocks if no connection is immediately available. Use with virtual threads
   * for optimal performance - blocking is cheap with virtual threads.
   *
   * @return a connection, never {@code null}
   * @throws ConnectionException if connection acquisition fails or times out
   */
  C acquire();

  /**
   * Acquires a connection with a custom timeout.
   *
   * @param timeout the maximum time to wait for a connection
   * @return a connection, never {@code null}
   * @throws ConnectionException if connection acquisition fails or times out
   * @throws IllegalArgumentException if timeout is {@code null} or negative
   */
  C acquire(Duration timeout);

  /**
   * Asynchronously acquires a connection.
   *
   * @return a CompletableFuture that will complete with a connection
   */
  CompletableFuture<C> acquireAsync();

  /**
   * Releases a connection back to the pool.
   *
   * <p>Always call this method in a try-finally block or use try-with-resources
   * if the connection implements AutoCloseable.
   *
   * @param connection the connection to release, must not be {@code null}
   * @throws IllegalArgumentException if connection is {@code null}
   */
  void release(C connection);

  /**
   * Validates that a connection is still usable.
   *
   * @param connection the connection to validate, must not be {@code null}
   * @return {@code true} if the connection is valid, {@code false} otherwise
   * @throws IllegalArgumentException if connection is {@code null}
   */
  boolean validate(C connection);

  /**
   * Executes an operation with a connection, automatically handling acquisition and release.
   *
   * <p><strong>Example Usage:</strong>
   * <pre>{@code
   * String result = connectionProvider.execute(conn -> {
   *     return performDatabaseOperation(conn);
   * });
   * }</pre>
   *
   * @param operation the operation to execute, must not be {@code null}
   * @param <R> the result type
   * @return the operation result
   * @throws ConnectionException if connection acquisition fails
   * @throws IllegalArgumentException if operation is {@code null}
   */
  <R> R execute(ConnectionOperation<C, R> operation);

  /**
   * Returns current pool statistics.
   *
   * @return pool statistics, never {@code null}
   */
  PoolStatistics getPoolStatistics();

  /**
   * Evicts idle connections from the pool.
   *
   * @return the number of connections evicted
   */
  int evictIdleConnections();

  /**
   * Closes all connections and shuts down the pool.
   */
  void shutdown();

  /**
   * Functional interface for connection operations.
   *
   * @param <C> the connection type
   * @param <R> the result type
   */
  @FunctionalInterface
  interface ConnectionOperation<C, R> {
    /**
     * Executes the operation with the provided connection.
     *
     * @param connection the connection to use
     * @return the operation result
     * @throws Exception if the operation fails
     */
    R execute(C connection) throws Exception;
  }

  /**
   * Connection pool statistics.
   */
  interface PoolStatistics {
    /** Total number of connections in the pool. */
    int getTotalConnections();

    /** Number of active (in-use) connections. */
    int getActiveConnections();

    /** Number of idle connections. */
    int getIdleConnections();

    /** Number of connections waiting to be acquired. */
    int getWaitingThreads();

    /** Average connection acquisition time in milliseconds. */
    long getAverageAcquisitionTimeMs();

    /** Maximum connection acquisition time in milliseconds. */
    long getMaxAcquisitionTimeMs();

    /** Total number of connection acquisitions. */
    long getTotalAcquisitions();

    /** Number of connection timeouts. */
    long getTimeoutCount();
  }
}
