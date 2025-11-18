package io.dataverse.core.query;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Optimized batch operations for bulk inserts, updates, and deletes.
 *
 * <p>Feature #13: Batch Operations - Optimized batch inserts/updates with configurable batch sizes
 *
 * @since 1.0.0
 */
public class BatchOperations {

    private static final int DEFAULT_BATCH_SIZE = 1000;

    /**
     * Execute batch insert operations.
     */
    public static <T> BatchResult batchInsert(
            Connection connection,
            String sql,
            List<T> items,
            ParameterSetter<T> parameterSetter) throws SQLException {
        return batchInsert(connection, sql, items, parameterSetter, DEFAULT_BATCH_SIZE);
    }

    /**
     * Execute batch insert operations with custom batch size.
     */
    public static <T> BatchResult batchInsert(
            Connection connection,
            String sql,
            List<T> items,
            ParameterSetter<T> parameterSetter,
            int batchSize) throws SQLException {

        if (items == null || items.isEmpty()) {
            return new BatchResult(0, 0, List.of());
        }

        long startTime = System.nanoTime();
        int totalAffected = 0;
        List<BatchError> errors = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int count = 0;
            int batchNumber = 0;

            for (int i = 0; i < items.size(); i++) {
                T item = items.get(i);
                try {
                    parameterSetter.setParameters(stmt, item);
                    stmt.addBatch();
                    count++;

                    if (count >= batchSize) {
                        int[] results = stmt.executeBatch();
                        totalAffected += sumResults(results);
                        count = 0;
                        batchNumber++;
                        stmt.clearBatch();
                    }
                } catch (SQLException e) {
                    errors.add(new BatchError(i, item.toString(), e.getMessage()));
                }
            }

            // Execute remaining items
            if (count > 0) {
                int[] results = stmt.executeBatch();
                totalAffected += sumResults(results);
            }
        }

        long duration = System.nanoTime() - startTime;
        return new BatchResult(items.size(), totalAffected, errors, duration);
    }

    /**
     * Execute batch update operations.
     */
    public static <T> BatchResult batchUpdate(
            Connection connection,
            String sql,
            List<T> items,
            ParameterSetter<T> parameterSetter) throws SQLException {
        return batchInsert(connection, sql, items, parameterSetter, DEFAULT_BATCH_SIZE);
    }

    /**
     * Execute batch delete operations.
     */
    public static BatchResult batchDelete(
            Connection connection,
            String sql,
            List<Object> ids,
            int batchSize) throws SQLException {

        if (ids == null || ids.isEmpty()) {
            return new BatchResult(0, 0, List.of());
        }

        long startTime = System.nanoTime();
        int totalAffected = 0;
        List<BatchError> errors = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int count = 0;

            for (int i = 0; i < ids.size(); i++) {
                try {
                    stmt.setObject(1, ids.get(i));
                    stmt.addBatch();
                    count++;

                    if (count >= batchSize) {
                        int[] results = stmt.executeBatch();
                        totalAffected += sumResults(results);
                        count = 0;
                        stmt.clearBatch();
                    }
                } catch (SQLException e) {
                    errors.add(new BatchError(i, ids.get(i).toString(), e.getMessage()));
                }
            }

            if (count > 0) {
                int[] results = stmt.executeBatch();
                totalAffected += sumResults(results);
            }
        }

        long duration = System.nanoTime() - startTime;
        return new BatchResult(ids.size(), totalAffected, errors, duration);
    }

    /**
     * Execute batch operations asynchronously using virtual threads.
     */
    public static <T> CompletableFuture<BatchResult> batchInsertAsync(
            Connection connection,
            String sql,
            List<T> items,
            ParameterSetter<T> parameterSetter,
            Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return batchInsert(connection, sql, items, parameterSetter);
            } catch (SQLException e) {
                throw new RuntimeException("Batch insert failed", e);
            }
        }, executor);
    }

    /**
     * Create a batch builder for fluent batch operations.
     */
    public static <T> BatchBuilder<T> builder(Connection connection, String sql) {
        return new BatchBuilder<>(connection, sql);
    }

    private static int sumResults(int[] results) {
        int sum = 0;
        for (int result : results) {
            if (result > 0) {
                sum += result;
            } else if (result == PreparedStatement.SUCCESS_NO_INFO) {
                sum++;
            }
        }
        return sum;
    }

    /**
     * Functional interface for setting parameters on a prepared statement.
     */
    @FunctionalInterface
    public interface ParameterSetter<T> {
        void setParameters(PreparedStatement stmt, T item) throws SQLException;
    }

    /**
     * Result of a batch operation.
     */
    public record BatchResult(
        int totalItems,
        int affectedRows,
        List<BatchError> errors,
        long durationNanos
    ) {
        public BatchResult(int totalItems, int affectedRows, List<BatchError> errors) {
            this(totalItems, affectedRows, errors, 0);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public int errorCount() {
            return errors.size();
        }

        public double successRate() {
            if (totalItems == 0) return 100.0;
            return ((double) (totalItems - errors.size()) / totalItems) * 100.0;
        }

        public long durationMillis() {
            return durationNanos / 1_000_000;
        }

        public double itemsPerSecond() {
            if (durationNanos == 0) return 0;
            return (double) totalItems / (durationNanos / 1_000_000_000.0);
        }
    }

    /**
     * Error details for a failed batch item.
     */
    public record BatchError(int index, String item, String message) {}

    /**
     * Fluent builder for batch operations.
     */
    public static class BatchBuilder<T> {
        private final Connection connection;
        private final String sql;
        private int batchSize = DEFAULT_BATCH_SIZE;
        private boolean continueOnError = false;
        private Consumer<BatchProgress> progressCallback;

        BatchBuilder(Connection connection, String sql) {
            this.connection = connection;
            this.sql = sql;
        }

        public BatchBuilder<T> batchSize(int size) {
            this.batchSize = size;
            return this;
        }

        public BatchBuilder<T> continueOnError(boolean continueOnError) {
            this.continueOnError = continueOnError;
            return this;
        }

        public BatchBuilder<T> onProgress(Consumer<BatchProgress> callback) {
            this.progressCallback = callback;
            return this;
        }

        public BatchResult execute(List<T> items, ParameterSetter<T> setter) throws SQLException {
            if (items == null || items.isEmpty()) {
                return new BatchResult(0, 0, List.of());
            }

            long startTime = System.nanoTime();
            int totalAffected = 0;
            List<BatchError> errors = new ArrayList<>();

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                int count = 0;
                int processedBatches = 0;

                for (int i = 0; i < items.size(); i++) {
                    T item = items.get(i);
                    try {
                        setter.setParameters(stmt, item);
                        stmt.addBatch();
                        count++;

                        if (count >= batchSize) {
                            int[] results = stmt.executeBatch();
                            totalAffected += sumResults(results);
                            count = 0;
                            processedBatches++;
                            stmt.clearBatch();

                            if (progressCallback != null) {
                                int processed = processedBatches * batchSize;
                                progressCallback.accept(new BatchProgress(
                                    processed, items.size(),
                                    (double) processed / items.size() * 100
                                ));
                            }
                        }
                    } catch (SQLException e) {
                        errors.add(new BatchError(i, item.toString(), e.getMessage()));
                        if (!continueOnError) {
                            throw e;
                        }
                    }
                }

                if (count > 0) {
                    int[] results = stmt.executeBatch();
                    totalAffected += sumResults(results);
                }
            }

            long duration = System.nanoTime() - startTime;
            return new BatchResult(items.size(), totalAffected, errors, duration);
        }
    }

    /**
     * Progress information during batch execution.
     */
    public record BatchProgress(int processed, int total, double percentComplete) {}
}
