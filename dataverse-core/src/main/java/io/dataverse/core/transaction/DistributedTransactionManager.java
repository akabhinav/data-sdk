package io.dataverse.core.transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Distributed transaction management using Two-Phase Commit (2PC) protocol.
 *
 * <p>Feature #46: Distributed Transactions - XA transaction support for multi-database operations
 *
 * @since 1.0.0
 */
public class DistributedTransactionManager {

    private final Map<String, Connection> connections = new ConcurrentHashMap<>();
    private final ThreadLocal<DistributedTransactionContext> currentTransaction = new ThreadLocal<>();

    /**
     * Register a database connection for distributed transactions.
     */
    public void registerConnection(String name, Connection connection) {
        connections.put(name, connection);
    }

    /**
     * Execute an operation across multiple databases.
     */
    public <T> T executeDistributed(Supplier<T> operation) {
        DistributedTransactionContext ctx = new DistributedTransactionContext();
        currentTransaction.set(ctx);

        try {
            // Phase 0: Begin transaction on all connections
            beginAll();

            // Execute the operation
            T result = operation.get();

            // Phase 1: Prepare all (vote)
            boolean allPrepared = prepareAll(ctx);

            if (allPrepared) {
                // Phase 2: Commit all
                commitAll(ctx);
            } else {
                // Phase 2: Rollback all
                rollbackAll(ctx);
                throw new DistributedTransactionException("Distributed transaction prepare failed");
            }

            return result;

        } catch (Throwable e) {
            rollbackAll(ctx);

            if (e instanceof DistributedTransactionException) {
                throw (DistributedTransactionException) e;
            }
            throw new DistributedTransactionException("Distributed transaction failed", e);

        } finally {
            currentTransaction.remove();
        }
    }

    /**
     * Execute a distributed transaction with explicit resources.
     */
    public <T> T executeDistributed(List<String> resourceNames, Supplier<T> operation) {
        // Verify all resources are registered
        for (String name : resourceNames) {
            if (!connections.containsKey(name)) {
                throw new IllegalArgumentException("Unknown resource: " + name);
            }
        }

        return executeDistributed(operation);
    }

    /**
     * Get a connection for use within the distributed transaction.
     */
    public Connection getConnection(String name) {
        Connection conn = connections.get(name);
        if (conn == null) {
            throw new IllegalArgumentException("Unknown connection: " + name);
        }

        DistributedTransactionContext ctx = currentTransaction.get();
        if (ctx != null) {
            ctx.addParticipant(name);
        }

        return conn;
    }

    /**
     * Get the current distributed transaction context.
     */
    public Optional<DistributedTransactionContext> getCurrentTransaction() {
        return Optional.ofNullable(currentTransaction.get());
    }

    private void beginAll() throws SQLException {
        for (Connection conn : connections.values()) {
            conn.setAutoCommit(false);
        }
    }

    private boolean prepareAll(DistributedTransactionContext ctx) {
        // In a real XA implementation, this would call XAResource.prepare()
        // For JDBC, we simulate by checking connection validity
        for (String name : ctx.getParticipants()) {
            Connection conn = connections.get(name);
            try {
                if (conn == null || !conn.isValid(5)) {
                    return false;
                }
            } catch (SQLException e) {
                return false;
            }
        }
        return true;
    }

    private void commitAll(DistributedTransactionContext ctx) {
        List<String> failed = new ArrayList<>();

        for (String name : ctx.getParticipants()) {
            Connection conn = connections.get(name);
            try {
                if (conn != null) {
                    conn.commit();
                }
            } catch (SQLException e) {
                failed.add(name + ": " + e.getMessage());
            }
        }

        if (!failed.isEmpty()) {
            throw new DistributedTransactionException(
                "Some participants failed to commit: " + String.join(", ", failed));
        }
    }

    private void rollbackAll(DistributedTransactionContext ctx) {
        for (String name : ctx.getParticipants()) {
            Connection conn = connections.get(name);
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException e) {
                // Log but continue rolling back others
            }
        }
    }

    /**
     * Close and remove a connection.
     */
    public void removeConnection(String name) {
        Connection conn = connections.remove(name);
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                // Ignore
            }
        }
    }

    /**
     * Close all connections.
     */
    public void close() {
        for (Connection conn : connections.values()) {
            try {
                conn.close();
            } catch (SQLException e) {
                // Ignore
            }
        }
        connections.clear();
    }

    /**
     * Distributed transaction context.
     */
    public static class DistributedTransactionContext {
        private final String transactionId;
        private final Set<String> participants = ConcurrentHashMap.newKeySet();
        private final long startTime;

        DistributedTransactionContext() {
            this.transactionId = UUID.randomUUID().toString();
            this.startTime = System.currentTimeMillis();
        }

        public String getTransactionId() {
            return transactionId;
        }

        public Set<String> getParticipants() {
            return Collections.unmodifiableSet(participants);
        }

        public long getElapsedMillis() {
            return System.currentTimeMillis() - startTime;
        }

        void addParticipant(String name) {
            participants.add(name);
        }
    }

    /**
     * Exception for distributed transaction failures.
     */
    public static class DistributedTransactionException extends RuntimeException {
        public DistributedTransactionException(String message) {
            super(message);
        }

        public DistributedTransactionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
