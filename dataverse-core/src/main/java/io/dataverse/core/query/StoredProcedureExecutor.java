package io.dataverse.core.query;

import java.sql.*;
import java.util.*;

/**
 * Stored procedure and function execution support.
 *
 * <p>Feature #22: Stored Procedure Support - Call stored procedures with IN/OUT/INOUT parameters
 * <p>Feature #23: Function Execution - Execute database functions with automatic result mapping
 *
 * @since 1.0.0
 */
public class StoredProcedureExecutor {

    private final Connection connection;

    public StoredProcedureExecutor(Connection connection) {
        this.connection = Objects.requireNonNull(connection);
    }

    /**
     * Call a stored procedure with parameters.
     */
    public ProcedureResult call(String procedureName, Object... inParams) throws SQLException {
        String callString = buildCallString(procedureName, inParams.length, 0);

        try (CallableStatement stmt = connection.prepareCall(callString)) {
            // Set IN parameters
            for (int i = 0; i < inParams.length; i++) {
                setParameter(stmt, i + 1, inParams[i]);
            }

            boolean hasResultSet = stmt.execute();
            return buildResult(stmt, hasResultSet, Collections.emptyMap());
        }
    }

    /**
     * Call a stored procedure with named parameters.
     */
    public ProcedureResult callWithNamedParams(
            String procedureName,
            Map<String, Object> params) throws SQLException {

        String callString = buildCallString(procedureName, params.size(), 0);

        try (CallableStatement stmt = connection.prepareCall(callString)) {
            int index = 1;
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                setParameter(stmt, index++, entry.getValue());
            }

            boolean hasResultSet = stmt.execute();
            return buildResult(stmt, hasResultSet, Collections.emptyMap());
        }
    }

    /**
     * Call a stored procedure with OUT parameters.
     */
    public ProcedureResult callWithOut(
            String procedureName,
            List<Object> inParams,
            Map<Integer, Integer> outParams) throws SQLException {

        int totalParams = inParams.size() + outParams.size();
        String callString = buildCallString(procedureName, totalParams, 0);

        try (CallableStatement stmt = connection.prepareCall(callString)) {
            // Set IN parameters
            int inIndex = 1;
            for (Object param : inParams) {
                setParameter(stmt, inIndex++, param);
            }

            // Register OUT parameters
            for (Map.Entry<Integer, Integer> out : outParams.entrySet()) {
                stmt.registerOutParameter(out.getKey(), out.getValue());
            }

            boolean hasResultSet = stmt.execute();

            // Get OUT parameter values
            Map<Integer, Object> outValues = new HashMap<>();
            for (Integer position : outParams.keySet()) {
                outValues.put(position, stmt.getObject(position));
            }

            return buildResult(stmt, hasResultSet, outValues);
        }
    }

    /**
     * Execute a database function and return the result.
     */
    public <T> T executeFunction(
            String functionName,
            Class<T> returnType,
            Object... params) throws SQLException {

        String callString = buildFunctionCallString(functionName, params.length);

        try (CallableStatement stmt = connection.prepareCall(callString)) {
            // Register return value
            stmt.registerOutParameter(1, getSqlType(returnType));

            // Set parameters
            for (int i = 0; i < params.length; i++) {
                setParameter(stmt, i + 2, params[i]);
            }

            stmt.execute();
            return returnType.cast(stmt.getObject(1));
        }
    }

    /**
     * Execute a function that returns a result set.
     */
    public <T> List<T> executeFunctionReturningSet(
            String functionName,
            StreamingResultSet.ResultSetMapper<T> mapper,
            Object... params) throws SQLException {

        StringBuilder sql = new StringBuilder("SELECT * FROM ");
        sql.append(functionName).append("(");
        for (int i = 0; i < params.length; i++) {
            if (i > 0) sql.append(", ");
            sql.append("?");
        }
        sql.append(")");

        try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.length; i++) {
                setParameter(stmt, i + 1, params[i]);
            }

            List<T> results = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapper.map(rs));
                }
            }
            return results;
        }
    }

    /**
     * Create a fluent procedure call builder.
     */
    public ProcedureCallBuilder procedure(String name) {
        return new ProcedureCallBuilder(this, name);
    }

    /**
     * Create a fluent function call builder.
     */
    public FunctionCallBuilder function(String name) {
        return new FunctionCallBuilder(this, name);
    }

    private String buildCallString(String name, int inCount, int outCount) {
        StringBuilder sb = new StringBuilder("{call ");
        sb.append(name).append("(");
        for (int i = 0; i < inCount + outCount; i++) {
            if (i > 0) sb.append(", ");
            sb.append("?");
        }
        sb.append(")}");
        return sb.toString();
    }

    private String buildFunctionCallString(String name, int paramCount) {
        StringBuilder sb = new StringBuilder("{? = call ");
        sb.append(name).append("(");
        for (int i = 0; i < paramCount; i++) {
            if (i > 0) sb.append(", ");
            sb.append("?");
        }
        sb.append(")}");
        return sb.toString();
    }

    private void setParameter(CallableStatement stmt, int index, Object value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.NULL);
        } else if (value instanceof String) {
            stmt.setString(index, (String) value);
        } else if (value instanceof Integer) {
            stmt.setInt(index, (Integer) value);
        } else if (value instanceof Long) {
            stmt.setLong(index, (Long) value);
        } else if (value instanceof Double) {
            stmt.setDouble(index, (Double) value);
        } else if (value instanceof Boolean) {
            stmt.setBoolean(index, (Boolean) value);
        } else if (value instanceof java.util.Date) {
            stmt.setTimestamp(index, new Timestamp(((java.util.Date) value).getTime()));
        } else if (value instanceof java.time.LocalDateTime) {
            stmt.setTimestamp(index, Timestamp.valueOf((java.time.LocalDateTime) value));
        } else {
            stmt.setObject(index, value);
        }
    }

    private void setParameter(PreparedStatement stmt, int index, Object value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.NULL);
        } else {
            stmt.setObject(index, value);
        }
    }

    private int getSqlType(Class<?> type) {
        if (type == String.class) return Types.VARCHAR;
        if (type == Integer.class || type == int.class) return Types.INTEGER;
        if (type == Long.class || type == long.class) return Types.BIGINT;
        if (type == Double.class || type == double.class) return Types.DOUBLE;
        if (type == Boolean.class || type == boolean.class) return Types.BOOLEAN;
        if (type == java.util.Date.class || type == java.sql.Timestamp.class) return Types.TIMESTAMP;
        return Types.OTHER;
    }

    private ProcedureResult buildResult(
            CallableStatement stmt,
            boolean hasResultSet,
            Map<Integer, Object> outValues) throws SQLException {

        List<Map<String, Object>> resultSets = new ArrayList<>();
        int updateCount = -1;

        if (hasResultSet) {
            try (ResultSet rs = stmt.getResultSet()) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(meta.getColumnName(i), rs.getObject(i));
                    }
                    resultSets.add(row);
                }
            }
        } else {
            updateCount = stmt.getUpdateCount();
        }

        return new ProcedureResult(resultSets, outValues, updateCount);
    }

    /**
     * Result of a stored procedure call.
     */
    public record ProcedureResult(
        List<Map<String, Object>> resultSets,
        Map<Integer, Object> outParameters,
        int updateCount
    ) {
        public boolean hasResults() {
            return !resultSets.isEmpty();
        }

        public boolean hasOutParameters() {
            return !outParameters.isEmpty();
        }

        @SuppressWarnings("unchecked")
        public <T> T getOutParameter(int position, Class<T> type) {
            return (T) outParameters.get(position);
        }
    }

    /**
     * Fluent builder for procedure calls.
     */
    public static class ProcedureCallBuilder {
        private final StoredProcedureExecutor executor;
        private final String name;
        private final List<Object> inParams = new ArrayList<>();
        private final Map<Integer, Integer> outParams = new HashMap<>();

        ProcedureCallBuilder(StoredProcedureExecutor executor, String name) {
            this.executor = executor;
            this.name = name;
        }

        public ProcedureCallBuilder in(Object value) {
            inParams.add(value);
            return this;
        }

        public ProcedureCallBuilder out(int position, int sqlType) {
            outParams.put(position, sqlType);
            return this;
        }

        public ProcedureCallBuilder outString(int position) {
            return out(position, Types.VARCHAR);
        }

        public ProcedureCallBuilder outInt(int position) {
            return out(position, Types.INTEGER);
        }

        public ProcedureCallBuilder outLong(int position) {
            return out(position, Types.BIGINT);
        }

        public ProcedureResult execute() throws SQLException {
            if (outParams.isEmpty()) {
                return executor.call(name, inParams.toArray());
            }
            return executor.callWithOut(name, inParams, outParams);
        }
    }

    /**
     * Fluent builder for function calls.
     */
    public static class FunctionCallBuilder {
        private final StoredProcedureExecutor executor;
        private final String name;
        private final List<Object> params = new ArrayList<>();

        FunctionCallBuilder(StoredProcedureExecutor executor, String name) {
            this.executor = executor;
            this.name = name;
        }

        public FunctionCallBuilder param(Object value) {
            params.add(value);
            return this;
        }

        public <T> T execute(Class<T> returnType) throws SQLException {
            return executor.executeFunction(name, returnType, params.toArray());
        }

        public <T> List<T> executeReturningSet(
                StreamingResultSet.ResultSetMapper<T> mapper) throws SQLException {
            return executor.executeFunctionReturningSet(name, mapper, params.toArray());
        }
    }
}
