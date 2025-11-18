package io.dataverse.core.connection;

/**
 * Enumeration of supported database types.
 *
 * <p>Feature #1: Multi-Database Support - PostgreSQL, MySQL, Oracle, SQL Server, MongoDB, Cassandra, DynamoDB
 *
 * @since 1.0.0
 */
public enum DatabaseType {
    /**
     * PostgreSQL database.
     */
    POSTGRESQL("postgresql", "org.postgresql.Driver", 5432, "jdbc:postgresql"),

    /**
     * MySQL database.
     */
    MYSQL("mysql", "com.mysql.cj.jdbc.Driver", 3306, "jdbc:mysql"),

    /**
     * Oracle database.
     */
    ORACLE("oracle", "oracle.jdbc.OracleDriver", 1521, "jdbc:oracle:thin"),

    /**
     * Microsoft SQL Server.
     */
    SQLSERVER("sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver", 1433, "jdbc:sqlserver"),

    /**
     * MongoDB NoSQL database.
     */
    MONGODB("mongodb", "mongodb.jdbc.MongoDriver", 27017, "mongodb"),

    /**
     * Apache Cassandra NoSQL database.
     */
    CASSANDRA("cassandra", "com.datastax.oss.jdbc.CassandraDriver", 9042, "jdbc:cassandra"),

    /**
     * Amazon DynamoDB.
     */
    DYNAMODB("dynamodb", "software.amazon.documentdb.jdbc.DocumentDbDriver", 443, "dynamodb"),

    /**
     * H2 in-memory database for testing.
     */
    H2("h2", "org.h2.Driver", 9092, "jdbc:h2"),

    /**
     * SQLite embedded database.
     */
    SQLITE("sqlite", "org.sqlite.JDBC", 0, "jdbc:sqlite"),

    /**
     * MariaDB database.
     */
    MARIADB("mariadb", "org.mariadb.jdbc.Driver", 3306, "jdbc:mariadb");

    private final String name;
    private final String driverClassName;
    private final int defaultPort;
    private final String jdbcPrefix;

    DatabaseType(String name, String driverClassName, int defaultPort, String jdbcPrefix) {
        this.name = name;
        this.driverClassName = driverClassName;
        this.defaultPort = defaultPort;
        this.jdbcPrefix = jdbcPrefix;
    }

    public String getName() {
        return name;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    public String getJdbcPrefix() {
        return jdbcPrefix;
    }

    /**
     * Check if this database type supports JDBC.
     */
    public boolean supportsJdbc() {
        return jdbcPrefix.startsWith("jdbc:");
    }

    /**
     * Check if this is a NoSQL database.
     */
    public boolean isNoSql() {
        return this == MONGODB || this == CASSANDRA || this == DYNAMODB;
    }

    /**
     * Check if this database supports transactions.
     */
    public boolean supportsTransactions() {
        return this != DYNAMODB;
    }

    /**
     * Get database type from name.
     */
    public static DatabaseType fromName(String name) {
        for (DatabaseType type : values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown database type: " + name);
    }

    /**
     * Detect database type from JDBC URL.
     */
    public static DatabaseType fromJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            throw new IllegalArgumentException("JDBC URL cannot be null or empty");
        }

        String lowerUrl = jdbcUrl.toLowerCase();
        for (DatabaseType type : values()) {
            if (lowerUrl.startsWith(type.jdbcPrefix.toLowerCase())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Cannot detect database type from URL: " + jdbcUrl);
    }
}
