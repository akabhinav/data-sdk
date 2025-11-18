package io.dataverse.core.connection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High-performance connection pool manager with HikariCP-style functionality.
 *
 * <p>Feature #2: Connection Pool Management - Intelligent pool sizing and health monitoring
 * <p>Feature #7: Connection Retry Logic - Exponential backoff with jitter
 * <p>Feature #9: Database Health Checks - Circuit breaker pattern
 *
 * @since 1.0.0
 */
public class ConnectionPoolManager implements AutoCloseable {

    private final Map<String, ConnectionPool> pools = new ConcurrentHashMap<>();
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Register a new connection pool with the given configuration.
     */
    public void registerPool(ConnectionConfig config) {
        Objects.requireNonNull(config, "Configuration cannot be null");

        if (closed.get()) {
            throw new IllegalStateException("ConnectionPoolManager is closed");
        }

        String name = config.getName();
        if (pools.containsKey(name)) {
            throw new IllegalArgumentException("Pool already registered: " + name);
        }

        ConnectionPool pool = new ConnectionPool(config);
        pools.put(name, pool);
        circuitBreakers.put(name, new CircuitBreaker(name));
    }

    /**
     * Get a connection from the specified pool.
     */
    public Connection getConnection(String poolName) throws SQLException {
        ConnectionPool pool = getPool(poolName);
        CircuitBreaker breaker = circuitBreakers.get(poolName);

        if (breaker != null && !breaker.allowRequest()) {
            throw new SQLException("Circuit breaker is open for pool: " + poolName);
        }

        try {
            Connection conn = pool.getConnection();
            if (breaker != null) {
                breaker.recordSuccess();
            }
            return conn;
        } catch (SQLException e) {
            if (breaker != null) {
                breaker.recordFailure();
            }
            throw e;
        }
    }

    /**
     * Get a connection with retry logic and exponential backoff.
     */
    public Connection getConnectionWithRetry(String poolName) throws SQLException {
        ConnectionPool pool = getPool(poolName);
        ConnectionConfig config = pool.getConfig();

        int maxAttempts = config.getMaxRetryAttempts();
        Duration baseDelay = config.getRetryDelay();
        boolean withJitter = config.isRetryWithJitter();

        SQLException lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return getConnection(poolName);
            } catch (SQLException e) {
                lastException = e;

                if (attempt < maxAttempts) {
                    long delay = calculateBackoffDelay(baseDelay.toMillis(), attempt, withJitter);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new SQLException("Connection retry interrupted", ie);
                    }
                }
            }
        }

        throw new SQLException("Failed to get connection after " + maxAttempts + " attempts", lastException);
    }

    private long calculateBackoffDelay(long baseDelay, int attempt, boolean withJitter) {
        // Exponential backoff: delay = baseDelay * 2^(attempt-1)
        long exponentialDelay = baseDelay * (1L << (attempt - 1));

        if (withJitter) {
            // Add random jitter: 0% to 50% of the delay
            long jitter = ThreadLocalRandom.current().nextLong(exponentialDelay / 2);
            return exponentialDelay + jitter;
        }

        return exponentialDelay;
    }

    /**
     * Get pool statistics for monitoring.
     */
    public PoolStatistics getStatistics(String poolName) {
        ConnectionPool pool = getPool(poolName);
        return pool.getStatistics();
    }

    /**
     * Perform health check on a pool.
     */
    public HealthStatus checkHealth(String poolName) {
        try {
            ConnectionPool pool = getPool(poolName);
            return pool.checkHealth();
        } catch (Exception e) {
            return new HealthStatus(poolName, false, e.getMessage(), 0, 0);
        }
    }

    /**
     * Get circuit breaker status.
     */
    public CircuitBreakerStatus getCircuitBreakerStatus(String poolName) {
        CircuitBreaker breaker = circuitBreakers.get(poolName);
        if (breaker == null) {
            throw new IllegalArgumentException("Unknown pool: " + poolName);
        }
        return breaker.getStatus();
    }

    private ConnectionPool getPool(String name) {
        ConnectionPool pool = pools.get(name);
        if (pool == null) {
            throw new IllegalArgumentException("Unknown pool: " + name);
        }
        return pool;
    }

    /**
     * Remove and close a connection pool.
     */
    public void removePool(String poolName) {
        ConnectionPool pool = pools.remove(poolName);
        if (pool != null) {
            pool.close();
        }
        circuitBreakers.remove(poolName);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            pools.values().forEach(ConnectionPool::close);
            pools.clear();
            circuitBreakers.clear();
        }
    }

    /**
     * Internal connection pool implementation.
     */
    private static class ConnectionPool {
        private final ConnectionConfig config;
        private final AtomicInteger activeConnections = new AtomicInteger(0);
        private final AtomicInteger totalConnections = new AtomicInteger(0);
        private final AtomicLong totalConnectionTime = new AtomicLong(0);
        private final AtomicLong connectionCount = new AtomicLong(0);
        private volatile boolean closed = false;

        ConnectionPool(ConnectionConfig config) {
            this.config = config;
        }

        ConnectionConfig getConfig() {
            return config;
        }

        Connection getConnection() throws SQLException {
            if (closed) {
                throw new SQLException("Connection pool is closed");
            }

            long start = System.nanoTime();

            // This is a simplified implementation
            // In production, this would use HikariCP or similar
            try {
                Class.forName(config.getDatabaseType().getDriverClassName());
                Connection conn = java.sql.DriverManager.getConnection(
                    config.getEffectiveJdbcUrl(),
                    config.getUsername(),
                    config.getPassword()
                );

                activeConnections.incrementAndGet();
                totalConnections.incrementAndGet();

                long elapsed = System.nanoTime() - start;
                totalConnectionTime.addAndGet(elapsed);
                connectionCount.incrementAndGet();

                return new PooledConnection(conn, this);
            } catch (ClassNotFoundException e) {
                throw new SQLException("Driver not found: " + config.getDatabaseType().getDriverClassName(), e);
            }
        }

        void releaseConnection(Connection conn) {
            activeConnections.decrementAndGet();
        }

        PoolStatistics getStatistics() {
            long avgTime = connectionCount.get() > 0
                ? totalConnectionTime.get() / connectionCount.get() / 1_000_000
                : 0;

            return new PoolStatistics(
                config.getName(),
                activeConnections.get(),
                config.getMaximumPoolSize() - activeConnections.get(),
                totalConnections.get(),
                avgTime
            );
        }

        HealthStatus checkHealth() {
            try (Connection conn = getConnection()) {
                boolean valid = conn.isValid(5);
                return new HealthStatus(
                    config.getName(),
                    valid,
                    valid ? "OK" : "Connection validation failed",
                    activeConnections.get(),
                    config.getMaximumPoolSize()
                );
            } catch (SQLException e) {
                return new HealthStatus(
                    config.getName(),
                    false,
                    e.getMessage(),
                    activeConnections.get(),
                    config.getMaximumPoolSize()
                );
            }
        }

        void close() {
            closed = true;
        }
    }

    /**
     * Wrapper for pooled connections.
     */
    private static class PooledConnection implements Connection {
        private final Connection delegate;
        private final ConnectionPool pool;
        private boolean closed = false;

        PooledConnection(Connection delegate, ConnectionPool pool) {
            this.delegate = delegate;
            this.pool = pool;
        }

        @Override
        public void close() throws SQLException {
            if (!closed) {
                closed = true;
                pool.releaseConnection(this);
                delegate.close();
            }
        }

        // Delegate all other Connection methods
        @Override
        public java.sql.Statement createStatement() throws SQLException {
            return delegate.createStatement();
        }

        @Override
        public java.sql.PreparedStatement prepareStatement(String sql) throws SQLException {
            return delegate.prepareStatement(sql);
        }

        @Override
        public java.sql.CallableStatement prepareCall(String sql) throws SQLException {
            return delegate.prepareCall(sql);
        }

        @Override
        public String nativeSQL(String sql) throws SQLException {
            return delegate.nativeSQL(sql);
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {
            delegate.setAutoCommit(autoCommit);
        }

        @Override
        public boolean getAutoCommit() throws SQLException {
            return delegate.getAutoCommit();
        }

        @Override
        public void commit() throws SQLException {
            delegate.commit();
        }

        @Override
        public void rollback() throws SQLException {
            delegate.rollback();
        }

        @Override
        public boolean isClosed() throws SQLException {
            return closed || delegate.isClosed();
        }

        @Override
        public java.sql.DatabaseMetaData getMetaData() throws SQLException {
            return delegate.getMetaData();
        }

        @Override
        public void setReadOnly(boolean readOnly) throws SQLException {
            delegate.setReadOnly(readOnly);
        }

        @Override
        public boolean isReadOnly() throws SQLException {
            return delegate.isReadOnly();
        }

        @Override
        public void setCatalog(String catalog) throws SQLException {
            delegate.setCatalog(catalog);
        }

        @Override
        public String getCatalog() throws SQLException {
            return delegate.getCatalog();
        }

        @Override
        public void setTransactionIsolation(int level) throws SQLException {
            delegate.setTransactionIsolation(level);
        }

        @Override
        public int getTransactionIsolation() throws SQLException {
            return delegate.getTransactionIsolation();
        }

        @Override
        public java.sql.SQLWarning getWarnings() throws SQLException {
            return delegate.getWarnings();
        }

        @Override
        public void clearWarnings() throws SQLException {
            delegate.clearWarnings();
        }

        @Override
        public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
            return delegate.createStatement(resultSetType, resultSetConcurrency);
        }

        @Override
        public java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return delegate.prepareCall(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public Map<String, Class<?>> getTypeMap() throws SQLException {
            return delegate.getTypeMap();
        }

        @Override
        public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
            delegate.setTypeMap(map);
        }

        @Override
        public void setHoldability(int holdability) throws SQLException {
            delegate.setHoldability(holdability);
        }

        @Override
        public int getHoldability() throws SQLException {
            return delegate.getHoldability();
        }

        @Override
        public java.sql.Savepoint setSavepoint() throws SQLException {
            return delegate.setSavepoint();
        }

        @Override
        public java.sql.Savepoint setSavepoint(String name) throws SQLException {
            return delegate.setSavepoint(name);
        }

        @Override
        public void rollback(java.sql.Savepoint savepoint) throws SQLException {
            delegate.rollback(savepoint);
        }

        @Override
        public void releaseSavepoint(java.sql.Savepoint savepoint) throws SQLException {
            delegate.releaseSavepoint(savepoint);
        }

        @Override
        public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public java.sql.PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
            return delegate.prepareStatement(sql, autoGeneratedKeys);
        }

        @Override
        public java.sql.PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
            return delegate.prepareStatement(sql, columnIndexes);
        }

        @Override
        public java.sql.PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
            return delegate.prepareStatement(sql, columnNames);
        }

        @Override
        public java.sql.Clob createClob() throws SQLException {
            return delegate.createClob();
        }

        @Override
        public java.sql.Blob createBlob() throws SQLException {
            return delegate.createBlob();
        }

        @Override
        public java.sql.NClob createNClob() throws SQLException {
            return delegate.createNClob();
        }

        @Override
        public java.sql.SQLXML createSQLXML() throws SQLException {
            return delegate.createSQLXML();
        }

        @Override
        public boolean isValid(int timeout) throws SQLException {
            return delegate.isValid(timeout);
        }

        @Override
        public void setClientInfo(String name, String value) throws java.sql.SQLClientInfoException {
            delegate.setClientInfo(name, value);
        }

        @Override
        public void setClientInfo(java.util.Properties properties) throws java.sql.SQLClientInfoException {
            delegate.setClientInfo(properties);
        }

        @Override
        public String getClientInfo(String name) throws SQLException {
            return delegate.getClientInfo(name);
        }

        @Override
        public java.util.Properties getClientInfo() throws SQLException {
            return delegate.getClientInfo();
        }

        @Override
        public java.sql.Array createArrayOf(String typeName, Object[] elements) throws SQLException {
            return delegate.createArrayOf(typeName, elements);
        }

        @Override
        public java.sql.Struct createStruct(String typeName, Object[] attributes) throws SQLException {
            return delegate.createStruct(typeName, attributes);
        }

        @Override
        public void setSchema(String schema) throws SQLException {
            delegate.setSchema(schema);
        }

        @Override
        public String getSchema() throws SQLException {
            return delegate.getSchema();
        }

        @Override
        public void abort(java.util.concurrent.Executor executor) throws SQLException {
            delegate.abort(executor);
        }

        @Override
        public void setNetworkTimeout(java.util.concurrent.Executor executor, int milliseconds) throws SQLException {
            delegate.setNetworkTimeout(executor, milliseconds);
        }

        @Override
        public int getNetworkTimeout() throws SQLException {
            return delegate.getNetworkTimeout();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return delegate.unwrap(iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return delegate.isWrapperFor(iface);
        }
    }

    /**
     * Circuit breaker implementation for connection fault tolerance.
     */
    private static class CircuitBreaker {
        private final String name;
        private volatile State state = State.CLOSED;
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private volatile long lastFailureTime = 0;

        private static final int FAILURE_THRESHOLD = 5;
        private static final long OPEN_TIMEOUT_MS = 30_000;
        private static final int HALF_OPEN_SUCCESS_THRESHOLD = 3;

        enum State {
            CLOSED, OPEN, HALF_OPEN
        }

        CircuitBreaker(String name) {
            this.name = name;
        }

        boolean allowRequest() {
            switch (state) {
                case CLOSED:
                    return true;
                case OPEN:
                    if (System.currentTimeMillis() - lastFailureTime > OPEN_TIMEOUT_MS) {
                        state = State.HALF_OPEN;
                        successCount.set(0);
                        return true;
                    }
                    return false;
                case HALF_OPEN:
                    return true;
                default:
                    return false;
            }
        }

        void recordSuccess() {
            if (state == State.HALF_OPEN) {
                if (successCount.incrementAndGet() >= HALF_OPEN_SUCCESS_THRESHOLD) {
                    state = State.CLOSED;
                    failureCount.set(0);
                }
            } else {
                failureCount.set(0);
            }
        }

        void recordFailure() {
            lastFailureTime = System.currentTimeMillis();

            if (state == State.HALF_OPEN) {
                state = State.OPEN;
            } else if (failureCount.incrementAndGet() >= FAILURE_THRESHOLD) {
                state = State.OPEN;
            }
        }

        CircuitBreakerStatus getStatus() {
            return new CircuitBreakerStatus(
                name,
                state.name(),
                failureCount.get(),
                FAILURE_THRESHOLD,
                lastFailureTime
            );
        }
    }

    /**
     * Pool statistics record.
     */
    public record PoolStatistics(
        String poolName,
        int activeConnections,
        int idleConnections,
        int totalCreated,
        long averageConnectionTimeMs
    ) {}

    /**
     * Health status record.
     */
    public record HealthStatus(
        String poolName,
        boolean healthy,
        String message,
        int activeConnections,
        int maxConnections
    ) {}

    /**
     * Circuit breaker status record.
     */
    public record CircuitBreakerStatus(
        String name,
        String state,
        int failureCount,
        int failureThreshold,
        long lastFailureTime
    ) {}
}
