package io.dataverse.core.query;

import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Advanced query executor with timeout management, statement pooling, and named parameters.
 *
 * <p>Feature #16: Query Timeouts - Per-query and global timeout configuration
 * <p>Feature #17: Prepared Statement Pooling - Automatic statement caching
 * <p>Feature #18: Named Parameters - Support for :paramName style
 * <p>Feature #20: SQL Injection Prevention - Automatic parameterization
 * <p>Feature #21: Multi-Statement Execution - Execute multiple statements
 *
 * @since 1.0.0
 */
public class QueryExecutor implements AutoCloseable {

    private static final Pattern NAMED_PARAM_PATTERN = Pattern.compile(":([a-zA-Z][a-zA-Z0-9_]*)");

    private final Connection connection;
    private final Map<String, PreparedStatement> statementCache = new ConcurrentHashMap<>();
    private final Map<String, StatementMetrics> statementMetrics = new ConcurrentHashMap<>();

    private Duration defaultTimeout = Duration.ofSeconds(30);
    private int maxCachedStatements = 100;
    private boolean enableMetrics = true;

    public QueryExecutor(Connection connection) {
        this.connection = Objects.requireNonNull(connection);
    }

    /**
     * Set the default query timeout.
     */
    public void setDefaultTimeout(Duration timeout) {
        this.defaultTimeout = Objects.requireNonNull(timeout);
    }

    /**
     * Set maximum cached statements.
     */
    public void setMaxCachedStatements(int max) {
        this.maxCachedStatements = max;
    }

    /**
     * Execute a query with named parameters and return results.
     */
    public <T> List<T> executeQuery(
            String sql,
            Map<String, Object> parameters,
            StreamingResultSet.ResultSetMapper<T> mapper) throws SQLException {
        return executeQuery(sql, parameters, mapper, defaultTimeout);
    }

    /**
     * Execute a query with custom timeout.
     */
    public <T> List<T> executeQuery(
            String sql,
            Map<String, Object> parameters,
            StreamingResultSet.ResultSetMapper<T> mapper,
            Duration timeout) throws SQLException {

        ParsedQuery parsed = parseNamedParameters(sql);
        PreparedStatement stmt = getOrCreateStatement(parsed.sql());

        try {
            setQueryTimeout(stmt, timeout);
            bindParameters(stmt, parsed, parameters);

            long startTime = System.nanoTime();
            List<T> results = new ArrayList<>();

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapper.map(rs));
                }
            }

            recordMetrics(sql, System.nanoTime() - startTime, results.size());
            return results;
        } finally {
            // Don't close - return to cache
        }
    }

    /**
     * Execute an update with named parameters.
     */
    public int executeUpdate(String sql, Map<String, Object> parameters) throws SQLException {
        return executeUpdate(sql, parameters, defaultTimeout);
    }

    /**
     * Execute an update with custom timeout.
     */
    public int executeUpdate(
            String sql,
            Map<String, Object> parameters,
            Duration timeout) throws SQLException {

        ParsedQuery parsed = parseNamedParameters(sql);
        PreparedStatement stmt = getOrCreateStatement(parsed.sql());

        try {
            setQueryTimeout(stmt, timeout);
            bindParameters(stmt, parsed, parameters);

            long startTime = System.nanoTime();
            int affected = stmt.executeUpdate();
            recordMetrics(sql, System.nanoTime() - startTime, affected);

            return affected;
        } finally {
            // Keep in cache
        }
    }

    /**
     * Execute a query asynchronously using virtual threads.
     */
    public <T> CompletableFuture<List<T>> executeQueryAsync(
            String sql,
            Map<String, Object> parameters,
            StreamingResultSet.ResultSetMapper<T> mapper,
            Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeQuery(sql, parameters, mapper);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    /**
     * Execute multiple statements in a single call.
     */
    public MultiStatementResult executeMultiple(String... statements) throws SQLException {
        List<Integer> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (String sql : statements) {
            try (Statement stmt = connection.createStatement()) {
                setQueryTimeout(stmt, defaultTimeout);
                int affected = stmt.executeUpdate(sql);
                results.add(affected);
            } catch (SQLException e) {
                errors.add(sql + ": " + e.getMessage());
                results.add(-1);
            }
        }

        return new MultiStatementResult(results, errors);
    }

    /**
     * Execute multiple statements in a transaction.
     */
    public MultiStatementResult executeMultipleInTransaction(String... statements) throws SQLException {
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        try {
            MultiStatementResult result = executeMultiple(statements);
            if (result.hasErrors()) {
                connection.rollback();
            } else {
                connection.commit();
            }
            return result;
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    /**
     * Execute a single value query (e.g., COUNT, SUM).
     */
    public <T> Optional<T> executeScalar(
            String sql,
            Map<String, Object> parameters,
            Class<T> type) throws SQLException {

        ParsedQuery parsed = parseNamedParameters(sql);
        PreparedStatement stmt = getOrCreateStatement(parsed.sql());

        try {
            setQueryTimeout(stmt, defaultTimeout);
            bindParameters(stmt, parsed, parameters);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Object value = rs.getObject(1);
                    if (value == null) {
                        return Optional.empty();
                    }
                    return Optional.of(type.cast(value));
                }
                return Optional.empty();
            }
        } finally {
            // Keep in cache
        }
    }

    /**
     * Execute INSERT and return generated keys.
     */
    public List<Object> executeInsertReturningKeys(
            String sql,
            Map<String, Object> parameters) throws SQLException {

        ParsedQuery parsed = parseNamedParameters(sql);

        try (PreparedStatement stmt = connection.prepareStatement(
                parsed.sql(), Statement.RETURN_GENERATED_KEYS)) {

            setQueryTimeout(stmt, defaultTimeout);
            bindParameters(stmt, parsed, parameters);
            stmt.executeUpdate();

            List<Object> keys = new ArrayList<>();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                while (rs.next()) {
                    keys.add(rs.getObject(1));
                }
            }
            return keys;
        }
    }

    /**
     * Get statement cache statistics.
     */
    public CacheStatistics getCacheStatistics() {
        return new CacheStatistics(
            statementCache.size(),
            maxCachedStatements,
            statementMetrics.values().stream()
                .mapToLong(StatementMetrics::executionCount)
                .sum()
        );
    }

    /**
     * Get metrics for a specific query.
     */
    public Optional<StatementMetrics> getMetrics(String sql) {
        return Optional.ofNullable(statementMetrics.get(sql));
    }

    /**
     * Clear the statement cache.
     */
    public void clearCache() {
        statementCache.values().forEach(stmt -> {
            try {
                stmt.close();
            } catch (SQLException e) {
                // Ignore
            }
        });
        statementCache.clear();
    }

    @Override
    public void close() {
        clearCache();
    }

    private PreparedStatement getOrCreateStatement(String sql) throws SQLException {
        return statementCache.computeIfAbsent(sql, key -> {
            try {
                // Evict oldest if at capacity
                if (statementCache.size() >= maxCachedStatements) {
                    evictOldestStatement();
                }
                return connection.prepareStatement(key);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to create prepared statement", e);
            }
        });
    }

    private void evictOldestStatement() {
        // Simple LRU eviction - find least recently used
        String oldest = statementMetrics.entrySet().stream()
            .min(Comparator.comparingLong(e -> e.getValue().lastUsed()))
            .map(Map.Entry::getKey)
            .orElse(null);

        if (oldest != null) {
            PreparedStatement stmt = statementCache.remove(oldest);
            statementMetrics.remove(oldest);
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    // Ignore
                }
            }
        }
    }

    private void setQueryTimeout(Statement stmt, Duration timeout) throws SQLException {
        if (timeout != null && !timeout.isZero()) {
            stmt.setQueryTimeout((int) timeout.toSeconds());
        }
    }

    private ParsedQuery parseNamedParameters(String sql) {
        List<String> paramNames = new ArrayList<>();
        Matcher matcher = NAMED_PARAM_PATTERN.matcher(sql);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            paramNames.add(matcher.group(1));
            matcher.appendReplacement(result, "?");
        }
        matcher.appendTail(result);

        return new ParsedQuery(result.toString(), paramNames);
    }

    private void bindParameters(
            PreparedStatement stmt,
            ParsedQuery parsed,
            Map<String, Object> parameters) throws SQLException {

        for (int i = 0; i < parsed.paramNames().size(); i++) {
            String paramName = parsed.paramNames().get(i);
            Object value = parameters.get(paramName);

            if (value == null) {
                stmt.setNull(i + 1, Types.NULL);
            } else if (value instanceof String) {
                stmt.setString(i + 1, (String) value);
            } else if (value instanceof Integer) {
                stmt.setInt(i + 1, (Integer) value);
            } else if (value instanceof Long) {
                stmt.setLong(i + 1, (Long) value);
            } else if (value instanceof Double) {
                stmt.setDouble(i + 1, (Double) value);
            } else if (value instanceof Boolean) {
                stmt.setBoolean(i + 1, (Boolean) value);
            } else if (value instanceof java.util.Date) {
                stmt.setTimestamp(i + 1, new Timestamp(((java.util.Date) value).getTime()));
            } else if (value instanceof java.time.LocalDate) {
                stmt.setDate(i + 1, Date.valueOf((java.time.LocalDate) value));
            } else if (value instanceof java.time.LocalDateTime) {
                stmt.setTimestamp(i + 1, Timestamp.valueOf((java.time.LocalDateTime) value));
            } else if (value instanceof Collection) {
                // Handle IN clause parameters
                Array array = connection.createArrayOf("VARCHAR",
                    ((Collection<?>) value).toArray());
                stmt.setArray(i + 1, array);
            } else {
                stmt.setObject(i + 1, value);
            }
        }
    }

    private void recordMetrics(String sql, long durationNanos, int rowCount) {
        if (!enableMetrics) return;

        statementMetrics.compute(sql, (key, existing) -> {
            if (existing == null) {
                return new StatementMetrics(1, durationNanos, durationNanos, durationNanos,
                    rowCount, System.currentTimeMillis());
            }
            return new StatementMetrics(
                existing.executionCount() + 1,
                existing.totalTimeNanos() + durationNanos,
                Math.min(existing.minTimeNanos(), durationNanos),
                Math.max(existing.maxTimeNanos(), durationNanos),
                existing.totalRows() + rowCount,
                System.currentTimeMillis()
            );
        });
    }

    // Record types
    private record ParsedQuery(String sql, List<String> paramNames) {}

    public record MultiStatementResult(List<Integer> results, List<String> errors) {
        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public int totalAffected() {
            return results.stream().filter(r -> r > 0).mapToInt(Integer::intValue).sum();
        }
    }

    public record CacheStatistics(int cachedStatements, int maxStatements, long totalExecutions) {}

    public record StatementMetrics(
        long executionCount,
        long totalTimeNanos,
        long minTimeNanos,
        long maxTimeNanos,
        long totalRows,
        long lastUsed
    ) {
        public double avgTimeMillis() {
            if (executionCount == 0) return 0;
            return (totalTimeNanos / executionCount) / 1_000_000.0;
        }

        public double avgRows() {
            if (executionCount == 0) return 0;
            return (double) totalRows / executionCount;
        }
    }
}
