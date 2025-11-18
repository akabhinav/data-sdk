package io.dataverse.core.query;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Memory-efficient streaming result set for large datasets using virtual threads.
 *
 * <p>Feature #14: Streaming Results - Memory-efficient result streaming for large datasets
 * <p>Feature #15: Async Query Execution - Non-blocking queries using CompletableFuture
 *
 * @param <T> the mapped entity type
 * @since 1.0.0
 */
public class StreamingResultSet<T> implements AutoCloseable, Iterable<T> {

    private final Connection connection;
    private final PreparedStatement statement;
    private final ResultSet resultSet;
    private final ResultSetMapper<T> mapper;
    private final int fetchSize;

    private boolean closed = false;

    private StreamingResultSet(
            Connection connection,
            PreparedStatement statement,
            ResultSet resultSet,
            ResultSetMapper<T> mapper,
            int fetchSize) {
        this.connection = connection;
        this.statement = statement;
        this.resultSet = resultSet;
        this.mapper = mapper;
        this.fetchSize = fetchSize;
    }

    /**
     * Create a streaming result set from a query.
     */
    public static <T> StreamingResultSet<T> stream(
            Connection connection,
            String sql,
            ResultSetMapper<T> mapper) throws SQLException {
        return stream(connection, sql, mapper, 1000);
    }

    /**
     * Create a streaming result set with custom fetch size.
     */
    public static <T> StreamingResultSet<T> stream(
            Connection connection,
            String sql,
            ResultSetMapper<T> mapper,
            int fetchSize) throws SQLException {

        PreparedStatement stmt = connection.prepareStatement(
            sql,
            ResultSet.TYPE_FORWARD_ONLY,
            ResultSet.CONCUR_READ_ONLY
        );

        stmt.setFetchSize(fetchSize);

        ResultSet rs = stmt.executeQuery();

        return new StreamingResultSet<>(connection, stmt, rs, mapper, fetchSize);
    }

    /**
     * Create an async streaming result set using virtual threads.
     */
    public static <T> CompletableFuture<StreamingResultSet<T>> streamAsync(
            Connection connection,
            String sql,
            ResultSetMapper<T> mapper,
            Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return stream(connection, sql, mapper);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to create streaming result set", e);
            }
        }, executor);
    }

    /**
     * Get a Java Stream for processing results.
     */
    public Stream<T> toStream() {
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED),
            false
        ).onClose(this::closeQuietly);
    }

    /**
     * Get a parallel Stream for processing results.
     */
    public Stream<T> toParallelStream() {
        return toStream().parallel();
    }

    /**
     * Process each result with a consumer.
     */
    public void forEach(Consumer<T> consumer) throws SQLException {
        while (resultSet.next()) {
            T item = mapper.map(resultSet);
            consumer.accept(item);
        }
    }

    /**
     * Process results asynchronously using virtual threads.
     */
    public CompletableFuture<Void> forEachAsync(Consumer<T> consumer, Executor executor) {
        return CompletableFuture.runAsync(() -> {
            try {
                forEach(consumer);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to process results", e);
            }
        }, executor);
    }

    /**
     * Map results to another type.
     */
    public <R> Stream<R> map(Function<T, R> mapper) {
        return toStream().map(mapper);
    }

    /**
     * Filter results.
     */
    public Stream<T> filter(java.util.function.Predicate<T> predicate) {
        return toStream().filter(predicate);
    }

    /**
     * Count all results (consumes the stream).
     */
    public long count() throws SQLException {
        long count = 0;
        while (resultSet.next()) {
            count++;
        }
        return count;
    }

    @Override
    public Iterator<T> iterator() {
        return new ResultSetIterator();
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
            } catch (SQLException e) {
                // Log but don't throw
            }
        }
    }

    private void closeQuietly() {
        try {
            close();
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Iterator implementation for streaming results.
     */
    private class ResultSetIterator implements Iterator<T> {
        private boolean hasNext = false;
        private boolean checked = false;

        @Override
        public boolean hasNext() {
            if (!checked) {
                try {
                    hasNext = resultSet.next();
                    checked = true;
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to check for next result", e);
                }
            }
            return hasNext;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            try {
                T item = mapper.map(resultSet);
                checked = false;
                return item;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to map result", e);
            }
        }
    }

    /**
     * Functional interface for mapping result sets to entities.
     */
    @FunctionalInterface
    public interface ResultSetMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    /**
     * Builder for creating streaming queries with more configuration options.
     */
    public static class Builder<T> {
        private final Connection connection;
        private String sql;
        private ResultSetMapper<T> mapper;
        private int fetchSize = 1000;
        private int queryTimeout = 0;
        private int maxRows = 0;

        public Builder(Connection connection) {
            this.connection = connection;
        }

        public Builder<T> sql(String sql) {
            this.sql = sql;
            return this;
        }

        public Builder<T> mapper(ResultSetMapper<T> mapper) {
            this.mapper = mapper;
            return this;
        }

        public Builder<T> fetchSize(int fetchSize) {
            this.fetchSize = fetchSize;
            return this;
        }

        public Builder<T> queryTimeout(int seconds) {
            this.queryTimeout = seconds;
            return this;
        }

        public Builder<T> maxRows(int maxRows) {
            this.maxRows = maxRows;
            return this;
        }

        public StreamingResultSet<T> execute() throws SQLException {
            PreparedStatement stmt = connection.prepareStatement(
                sql,
                ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY
            );

            stmt.setFetchSize(fetchSize);

            if (queryTimeout > 0) {
                stmt.setQueryTimeout(queryTimeout);
            }

            if (maxRows > 0) {
                stmt.setMaxRows(maxRows);
            }

            ResultSet rs = stmt.executeQuery();
            return new StreamingResultSet<>(connection, stmt, rs, mapper, fetchSize);
        }

        public CompletableFuture<StreamingResultSet<T>> executeAsync(Executor executor) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return execute();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to execute streaming query", e);
                }
            }, executor);
        }
    }
}
