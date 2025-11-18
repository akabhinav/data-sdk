package io.dataverse.core.query;

import java.sql.*;
import java.util.*;

/**
 * Built-in pagination support with offset/limit and cursor-based approaches.
 *
 * <p>Feature #24: Pagination Support - Built-in offset/limit and cursor-based pagination
 *
 * @since 1.0.0
 */
public class PaginationSupport {

    /**
     * Create an offset-based page request.
     */
    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size, null);
    }

    /**
     * Create an offset-based page request with sorting.
     */
    public static PageRequest of(int page, int size, Sort sort) {
        return new PageRequest(page, size, sort);
    }

    /**
     * Create a cursor-based page request.
     */
    public static CursorRequest cursor(String cursor, int size) {
        return new CursorRequest(cursor, size, CursorDirection.FORWARD);
    }

    /**
     * Execute a paginated query with offset/limit.
     */
    public static <T> Page<T> paginate(
            Connection connection,
            String baseQuery,
            Map<String, Object> parameters,
            PageRequest pageRequest,
            StreamingResultSet.ResultSetMapper<T> mapper) throws SQLException {

        // Build count query
        String countQuery = buildCountQuery(baseQuery);
        long totalElements = executeCount(connection, countQuery, parameters);

        // Build paginated query
        String paginatedQuery = buildPaginatedQuery(baseQuery, pageRequest);
        List<T> content = executeQuery(connection, paginatedQuery, parameters, mapper,
            pageRequest.offset(), pageRequest.size());

        return new Page<>(
            content,
            pageRequest.page(),
            pageRequest.size(),
            totalElements
        );
    }

    /**
     * Execute a cursor-based paginated query.
     */
    public static <T> CursorPage<T> paginateCursor(
            Connection connection,
            String baseQuery,
            String cursorColumn,
            Map<String, Object> parameters,
            CursorRequest request,
            StreamingResultSet.ResultSetMapper<T> mapper,
            java.util.function.Function<T, String> cursorExtractor) throws SQLException {

        // Build cursor query
        String cursorQuery = buildCursorQuery(baseQuery, cursorColumn, request);
        Map<String, Object> params = new HashMap<>(parameters);

        if (request.cursor() != null) {
            params.put("cursor", request.cursor());
        }

        List<T> content = executeQuery(connection, cursorQuery, params, mapper, 0, request.size() + 1);

        boolean hasMore = content.size() > request.size();
        if (hasMore) {
            content = content.subList(0, request.size());
        }

        String nextCursor = null;
        String prevCursor = null;

        if (!content.isEmpty()) {
            nextCursor = hasMore ? cursorExtractor.apply(content.get(content.size() - 1)) : null;
            prevCursor = request.cursor();
        }

        return new CursorPage<>(content, nextCursor, prevCursor, hasMore);
    }

    /**
     * Create a slice (page without total count for better performance).
     */
    public static <T> Slice<T> slice(
            Connection connection,
            String baseQuery,
            Map<String, Object> parameters,
            PageRequest pageRequest,
            StreamingResultSet.ResultSetMapper<T> mapper) throws SQLException {

        String paginatedQuery = buildPaginatedQuery(baseQuery, pageRequest);
        // Request one extra to check if more exist
        List<T> content = executeQuery(connection, paginatedQuery, parameters, mapper,
            pageRequest.offset(), pageRequest.size() + 1);

        boolean hasNext = content.size() > pageRequest.size();
        if (hasNext) {
            content = content.subList(0, pageRequest.size());
        }

        return new Slice<>(content, pageRequest.page(), pageRequest.size(), hasNext);
    }

    private static String buildCountQuery(String baseQuery) {
        // Remove ORDER BY clause for count
        String query = baseQuery.replaceAll("(?i)ORDER BY[^)]*$", "");
        return "SELECT COUNT(*) FROM (" + query + ") AS count_query";
    }

    private static String buildPaginatedQuery(String baseQuery, PageRequest request) {
        StringBuilder sb = new StringBuilder(baseQuery);

        if (request.sort() != null) {
            sb.append(" ORDER BY ");
            List<String> orders = new ArrayList<>();
            for (Sort.Order order : request.sort().orders()) {
                orders.add(order.property() + " " + order.direction().name());
            }
            sb.append(String.join(", ", orders));
        }

        sb.append(" LIMIT ").append(request.size());
        sb.append(" OFFSET ").append(request.offset());

        return sb.toString();
    }

    private static String buildCursorQuery(String baseQuery, String cursorColumn, CursorRequest request) {
        StringBuilder sb = new StringBuilder(baseQuery);

        if (request.cursor() != null) {
            String whereKeyword = baseQuery.toUpperCase().contains("WHERE") ? " AND " : " WHERE ";
            String operator = request.direction() == CursorDirection.FORWARD ? ">" : "<";
            sb.append(whereKeyword).append(cursorColumn).append(" ").append(operator).append(" :cursor");
        }

        String direction = request.direction() == CursorDirection.FORWARD ? "ASC" : "DESC";
        sb.append(" ORDER BY ").append(cursorColumn).append(" ").append(direction);
        sb.append(" LIMIT ").append(request.size() + 1);

        return sb.toString();
    }

    private static long executeCount(
            Connection connection,
            String countQuery,
            Map<String, Object> parameters) throws SQLException {

        try (PreparedStatement stmt = connection.prepareStatement(countQuery)) {
            bindParameters(stmt, parameters);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
        }
    }

    private static <T> List<T> executeQuery(
            Connection connection,
            String query,
            Map<String, Object> parameters,
            StreamingResultSet.ResultSetMapper<T> mapper,
            int offset,
            int limit) throws SQLException {

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            bindParameters(stmt, parameters);

            List<T> results = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapper.map(rs));
                }
            }
            return results;
        }
    }

    private static void bindParameters(PreparedStatement stmt, Map<String, Object> parameters) throws SQLException {
        // Simple implementation - would use named parameter parsing in production
        int index = 1;
        for (Object value : parameters.values()) {
            stmt.setObject(index++, value);
        }
    }

    /**
     * Page request for offset-based pagination.
     */
    public record PageRequest(int page, int size, Sort sort) {
        public PageRequest {
            if (page < 1) throw new IllegalArgumentException("Page must be >= 1");
            if (size < 1) throw new IllegalArgumentException("Size must be >= 1");
        }

        public int offset() {
            return (page - 1) * size;
        }

        public PageRequest next() {
            return new PageRequest(page + 1, size, sort);
        }

        public PageRequest previous() {
            return new PageRequest(Math.max(1, page - 1), size, sort);
        }
    }

    /**
     * Cursor request for cursor-based pagination.
     */
    public record CursorRequest(String cursor, int size, CursorDirection direction) {
        public CursorRequest {
            if (size < 1) throw new IllegalArgumentException("Size must be >= 1");
        }
    }

    /**
     * Cursor direction.
     */
    public enum CursorDirection {
        FORWARD, BACKWARD
    }

    /**
     * Page result with total count.
     */
    public record Page<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements
    ) {
        public int totalPages() {
            return (int) Math.ceil((double) totalElements / pageSize);
        }

        public boolean hasNext() {
            return pageNumber < totalPages();
        }

        public boolean hasPrevious() {
            return pageNumber > 1;
        }

        public boolean isFirst() {
            return pageNumber == 1;
        }

        public boolean isLast() {
            return pageNumber >= totalPages();
        }

        public boolean isEmpty() {
            return content.isEmpty();
        }

        public int size() {
            return content.size();
        }
    }

    /**
     * Slice result without total count (more efficient for large datasets).
     */
    public record Slice<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        boolean hasNext
    ) {
        public boolean hasPrevious() {
            return pageNumber > 1;
        }

        public boolean isEmpty() {
            return content.isEmpty();
        }

        public int size() {
            return content.size();
        }
    }

    /**
     * Cursor-based page result.
     */
    public record CursorPage<T>(
        List<T> content,
        String nextCursor,
        String previousCursor,
        boolean hasMore
    ) {
        public boolean isEmpty() {
            return content.isEmpty();
        }

        public int size() {
            return content.size();
        }
    }

    /**
     * Sort specification.
     */
    public record Sort(List<Order> orders) {
        public static Sort by(String... properties) {
            List<Order> orders = new ArrayList<>();
            for (String property : properties) {
                orders.add(new Order(property, Direction.ASC));
            }
            return new Sort(orders);
        }

        public static Sort by(Order... orders) {
            return new Sort(List.of(orders));
        }

        public Sort and(Sort other) {
            List<Order> combined = new ArrayList<>(orders);
            combined.addAll(other.orders());
            return new Sort(combined);
        }

        public record Order(String property, Direction direction) {
            public static Order asc(String property) {
                return new Order(property, Direction.ASC);
            }

            public static Order desc(String property) {
                return new Order(property, Direction.DESC);
            }
        }

        public enum Direction {
            ASC, DESC
        }
    }
}
