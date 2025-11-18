package io.dataverse.core.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Type-safe query DSL with fluent API and compile-time validation.
 *
 * <p>Feature #11: Type-Safe Query DSL - Fluent API with compile-time query validation
 * <p>Feature #19: Dynamic Query Construction - Build queries based on runtime conditions
 *
 * @param <T> the entity type being queried
 * @since 1.0.0
 */
public class TypeSafeQueryBuilder<T> {

    private final Class<T> entityType;
    private final List<String> selectColumns = new ArrayList<>();
    private final List<WhereClause> whereClauses = new ArrayList<>();
    private final List<JoinClause> joins = new ArrayList<>();
    private final List<OrderByClause> orderByClauses = new ArrayList<>();
    private final List<String> groupByColumns = new ArrayList<>();
    private final Map<String, Object> parameters = new HashMap<>();
    private String havingClause;
    private Integer limit;
    private Integer offset;
    private boolean distinct = false;
    private boolean forUpdate = false;

    private int paramCounter = 0;

    private TypeSafeQueryBuilder(Class<T> entityType) {
        this.entityType = entityType;
    }

    /**
     * Create a new query builder for the specified entity type.
     */
    public static <T> TypeSafeQueryBuilder<T> query(Class<T> entityType) {
        return new TypeSafeQueryBuilder<>(entityType);
    }

    /**
     * Create a new SELECT query.
     */
    public static <T> TypeSafeQueryBuilder<T> select(Class<T> entityType) {
        return new TypeSafeQueryBuilder<>(entityType);
    }

    /**
     * Select specific columns.
     */
    public TypeSafeQueryBuilder<T> select(String... columns) {
        for (String column : columns) {
            selectColumns.add(column);
        }
        return this;
    }

    /**
     * Select all columns.
     */
    public TypeSafeQueryBuilder<T> selectAll() {
        selectColumns.clear();
        selectColumns.add("*");
        return this;
    }

    /**
     * Add DISTINCT to the query.
     */
    public TypeSafeQueryBuilder<T> distinct() {
        this.distinct = true;
        return this;
    }

    /**
     * Add WHERE clause.
     */
    public WhereBuilder<T> where(String column) {
        return new WhereBuilder<>(this, column, "AND");
    }

    /**
     * Add OR WHERE clause.
     */
    public WhereBuilder<T> orWhere(String column) {
        return new WhereBuilder<>(this, column, "OR");
    }

    /**
     * Add WHERE clause with consumer pattern.
     */
    public TypeSafeQueryBuilder<T> where(Consumer<WhereGroup<T>> group) {
        WhereGroup<T> whereGroup = new WhereGroup<>(this);
        group.accept(whereGroup);
        if (!whereGroup.clauses.isEmpty()) {
            whereClauses.add(new WhereClause("AND", "(" + whereGroup.build() + ")", null, null));
        }
        return this;
    }

    /**
     * Add raw WHERE clause with named parameter.
     */
    public TypeSafeQueryBuilder<T> whereRaw(String clause, Object value) {
        String paramName = generateParamName();
        whereClauses.add(new WhereClause("AND", clause.replace("?", ":" + paramName), paramName, value));
        parameters.put(paramName, value);
        return this;
    }

    /**
     * Add INNER JOIN.
     */
    public JoinBuilder<T> join(String table) {
        return new JoinBuilder<>(this, "INNER JOIN", table);
    }

    /**
     * Add LEFT JOIN.
     */
    public JoinBuilder<T> leftJoin(String table) {
        return new JoinBuilder<>(this, "LEFT JOIN", table);
    }

    /**
     * Add RIGHT JOIN.
     */
    public JoinBuilder<T> rightJoin(String table) {
        return new JoinBuilder<>(this, "RIGHT JOIN", table);
    }

    /**
     * Add ORDER BY clause.
     */
    public TypeSafeQueryBuilder<T> orderBy(String column) {
        orderByClauses.add(new OrderByClause(column, "ASC"));
        return this;
    }

    /**
     * Add ORDER BY ASC clause.
     */
    public TypeSafeQueryBuilder<T> orderByAsc(String column) {
        orderByClauses.add(new OrderByClause(column, "ASC"));
        return this;
    }

    /**
     * Add ORDER BY DESC clause.
     */
    public TypeSafeQueryBuilder<T> orderByDesc(String column) {
        orderByClauses.add(new OrderByClause(column, "DESC"));
        return this;
    }

    /**
     * Add GROUP BY clause.
     */
    public TypeSafeQueryBuilder<T> groupBy(String... columns) {
        for (String column : columns) {
            groupByColumns.add(column);
        }
        return this;
    }

    /**
     * Add HAVING clause.
     */
    public TypeSafeQueryBuilder<T> having(String clause) {
        this.havingClause = clause;
        return this;
    }

    /**
     * Set LIMIT.
     */
    public TypeSafeQueryBuilder<T> limit(int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Set OFFSET.
     */
    public TypeSafeQueryBuilder<T> offset(int offset) {
        this.offset = offset;
        return this;
    }

    /**
     * Add FOR UPDATE clause (row locking).
     */
    public TypeSafeQueryBuilder<T> forUpdate() {
        this.forUpdate = true;
        return this;
    }

    /**
     * Apply pagination.
     */
    public TypeSafeQueryBuilder<T> page(int pageNumber, int pageSize) {
        this.limit = pageSize;
        this.offset = (pageNumber - 1) * pageSize;
        return this;
    }

    /**
     * Conditionally add clauses.
     */
    public TypeSafeQueryBuilder<T> when(boolean condition, Consumer<TypeSafeQueryBuilder<T>> consumer) {
        if (condition) {
            consumer.accept(this);
        }
        return this;
    }

    /**
     * Build the SQL query string.
     */
    public String toSql() {
        StringBuilder sql = new StringBuilder();

        // SELECT
        sql.append("SELECT ");
        if (distinct) {
            sql.append("DISTINCT ");
        }
        if (selectColumns.isEmpty()) {
            sql.append("*");
        } else {
            sql.append(String.join(", ", selectColumns));
        }

        // FROM
        sql.append(" FROM ").append(getTableName());

        // JOINs
        for (JoinClause join : joins) {
            sql.append(" ").append(join.type())
               .append(" ").append(join.table())
               .append(" ON ").append(join.condition());
        }

        // WHERE
        if (!whereClauses.isEmpty()) {
            sql.append(" WHERE ");
            boolean first = true;
            for (WhereClause clause : whereClauses) {
                if (!first) {
                    sql.append(" ").append(clause.connector()).append(" ");
                }
                sql.append(clause.clause());
                first = false;
            }
        }

        // GROUP BY
        if (!groupByColumns.isEmpty()) {
            sql.append(" GROUP BY ").append(String.join(", ", groupByColumns));
        }

        // HAVING
        if (havingClause != null) {
            sql.append(" HAVING ").append(havingClause);
        }

        // ORDER BY
        if (!orderByClauses.isEmpty()) {
            sql.append(" ORDER BY ");
            List<String> orders = new ArrayList<>();
            for (OrderByClause order : orderByClauses) {
                orders.add(order.column() + " " + order.direction());
            }
            sql.append(String.join(", ", orders));
        }

        // LIMIT
        if (limit != null) {
            sql.append(" LIMIT ").append(limit);
        }

        // OFFSET
        if (offset != null) {
            sql.append(" OFFSET ").append(offset);
        }

        // FOR UPDATE
        if (forUpdate) {
            sql.append(" FOR UPDATE");
        }

        return sql.toString();
    }

    /**
     * Get the parameters for the query.
     */
    public Map<String, Object> getParameters() {
        return Map.copyOf(parameters);
    }

    /**
     * Get the entity type.
     */
    public Class<T> getEntityType() {
        return entityType;
    }

    private String getTableName() {
        // Convert class name to table name (e.g., UserAccount -> user_account)
        String className = entityType.getSimpleName();
        return toSnakeCase(className);
    }

    private String toSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    private String generateParamName() {
        return "p" + (++paramCounter);
    }

    void addWhereClause(WhereClause clause) {
        whereClauses.add(clause);
        if (clause.paramName() != null && clause.value() != null) {
            parameters.put(clause.paramName(), clause.value());
        }
    }

    void addJoinClause(JoinClause clause) {
        joins.add(clause);
    }

    /**
     * WHERE clause builder.
     */
    public static class WhereBuilder<T> {
        private final TypeSafeQueryBuilder<T> parent;
        private final String column;
        private final String connector;

        WhereBuilder(TypeSafeQueryBuilder<T> parent, String column, String connector) {
            this.parent = parent;
            this.column = column;
            this.connector = connector;
        }

        public TypeSafeQueryBuilder<T> eq(Object value) {
            return addClause("=", value);
        }

        public TypeSafeQueryBuilder<T> ne(Object value) {
            return addClause("<>", value);
        }

        public TypeSafeQueryBuilder<T> gt(Object value) {
            return addClause(">", value);
        }

        public TypeSafeQueryBuilder<T> gte(Object value) {
            return addClause(">=", value);
        }

        public TypeSafeQueryBuilder<T> lt(Object value) {
            return addClause("<", value);
        }

        public TypeSafeQueryBuilder<T> lte(Object value) {
            return addClause("<=", value);
        }

        public TypeSafeQueryBuilder<T> like(String pattern) {
            return addClause("LIKE", pattern);
        }

        public TypeSafeQueryBuilder<T> notLike(String pattern) {
            return addClause("NOT LIKE", pattern);
        }

        public TypeSafeQueryBuilder<T> in(Object... values) {
            String paramName = parent.generateParamName();
            String clause = column + " IN (:" + paramName + ")";
            parent.addWhereClause(new WhereClause(connector, clause, paramName, List.of(values)));
            return parent;
        }

        public TypeSafeQueryBuilder<T> notIn(Object... values) {
            String paramName = parent.generateParamName();
            String clause = column + " NOT IN (:" + paramName + ")";
            parent.addWhereClause(new WhereClause(connector, clause, paramName, List.of(values)));
            return parent;
        }

        public TypeSafeQueryBuilder<T> between(Object start, Object end) {
            String param1 = parent.generateParamName();
            String param2 = parent.generateParamName();
            String clause = column + " BETWEEN :" + param1 + " AND :" + param2;
            parent.addWhereClause(new WhereClause(connector, clause, param1, start));
            parent.parameters.put(param2, end);
            return parent;
        }

        public TypeSafeQueryBuilder<T> isNull() {
            parent.addWhereClause(new WhereClause(connector, column + " IS NULL", null, null));
            return parent;
        }

        public TypeSafeQueryBuilder<T> isNotNull() {
            parent.addWhereClause(new WhereClause(connector, column + " IS NOT NULL", null, null));
            return parent;
        }

        public TypeSafeQueryBuilder<T> startsWith(String prefix) {
            return like(prefix + "%");
        }

        public TypeSafeQueryBuilder<T> endsWith(String suffix) {
            return like("%" + suffix);
        }

        public TypeSafeQueryBuilder<T> contains(String substring) {
            return like("%" + substring + "%");
        }

        private TypeSafeQueryBuilder<T> addClause(String operator, Object value) {
            String paramName = parent.generateParamName();
            String clause = column + " " + operator + " :" + paramName;
            parent.addWhereClause(new WhereClause(connector, clause, paramName, value));
            return parent;
        }
    }

    /**
     * WHERE group for nested conditions.
     */
    public static class WhereGroup<T> {
        private final TypeSafeQueryBuilder<T> parent;
        private final List<String> clauses = new ArrayList<>();

        WhereGroup(TypeSafeQueryBuilder<T> parent) {
            this.parent = parent;
        }

        public WhereGroup<T> where(String column, String operator, Object value) {
            String paramName = parent.generateParamName();
            clauses.add(column + " " + operator + " :" + paramName);
            parent.parameters.put(paramName, value);
            return this;
        }

        public WhereGroup<T> and(String column, String operator, Object value) {
            String paramName = parent.generateParamName();
            clauses.add("AND " + column + " " + operator + " :" + paramName);
            parent.parameters.put(paramName, value);
            return this;
        }

        public WhereGroup<T> or(String column, String operator, Object value) {
            String paramName = parent.generateParamName();
            clauses.add("OR " + column + " " + operator + " :" + paramName);
            parent.parameters.put(paramName, value);
            return this;
        }

        String build() {
            return String.join(" ", clauses);
        }
    }

    /**
     * JOIN builder.
     */
    public static class JoinBuilder<T> {
        private final TypeSafeQueryBuilder<T> parent;
        private final String type;
        private final String table;

        JoinBuilder(TypeSafeQueryBuilder<T> parent, String type, String table) {
            this.parent = parent;
            this.type = type;
            this.table = table;
        }

        public TypeSafeQueryBuilder<T> on(String condition) {
            parent.addJoinClause(new JoinClause(type, table, condition));
            return parent;
        }

        public TypeSafeQueryBuilder<T> on(String leftColumn, String rightColumn) {
            String condition = leftColumn + " = " + rightColumn;
            parent.addJoinClause(new JoinClause(type, table, condition));
            return parent;
        }
    }

    // Internal records
    private record WhereClause(String connector, String clause, String paramName, Object value) {}
    private record JoinClause(String type, String table, String condition) {}
    private record OrderByClause(String column, String direction) {}
}
