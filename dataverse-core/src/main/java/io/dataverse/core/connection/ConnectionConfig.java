package io.dataverse.core.connection;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable connection configuration for database connections.
 *
 * <p>Feature #4: Configuration-as-Code - Fluent builder API for programmatic configuration
 *
 * @since 1.0.0
 */
public final class ConnectionConfig {
    private final String name;
    private final DatabaseType databaseType;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final String jdbcUrl;

    // Pool settings
    private final int minimumPoolSize;
    private final int maximumPoolSize;
    private final Duration connectionTimeout;
    private final Duration idleTimeout;
    private final Duration maxLifetime;
    private final Duration leakDetectionThreshold;

    // Retry settings
    private final int maxRetryAttempts;
    private final Duration retryDelay;
    private final boolean retryWithJitter;

    // Read/Write split
    private final boolean readOnly;
    private final String[] readReplicaUrls;

    // Additional properties
    private final Map<String, String> properties;

    private ConnectionConfig(Builder builder) {
        this.name = builder.name;
        this.databaseType = builder.databaseType;
        this.host = builder.host;
        this.port = builder.port;
        this.database = builder.database;
        this.username = builder.username;
        this.password = builder.password;
        this.jdbcUrl = builder.jdbcUrl;
        this.minimumPoolSize = builder.minimumPoolSize;
        this.maximumPoolSize = builder.maximumPoolSize;
        this.connectionTimeout = builder.connectionTimeout;
        this.idleTimeout = builder.idleTimeout;
        this.maxLifetime = builder.maxLifetime;
        this.leakDetectionThreshold = builder.leakDetectionThreshold;
        this.maxRetryAttempts = builder.maxRetryAttempts;
        this.retryDelay = builder.retryDelay;
        this.retryWithJitter = builder.retryWithJitter;
        this.readOnly = builder.readOnly;
        this.readReplicaUrls = builder.readReplicaUrls != null
            ? builder.readReplicaUrls.clone()
            : new String[0];
        this.properties = Map.copyOf(builder.properties);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(String name) {
        return new Builder().name(name);
    }

    // Getters
    public String getName() { return name; }
    public DatabaseType getDatabaseType() { return databaseType; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getDatabase() { return database; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getJdbcUrl() { return jdbcUrl; }
    public int getMinimumPoolSize() { return minimumPoolSize; }
    public int getMaximumPoolSize() { return maximumPoolSize; }
    public Duration getConnectionTimeout() { return connectionTimeout; }
    public Duration getIdleTimeout() { return idleTimeout; }
    public Duration getMaxLifetime() { return maxLifetime; }
    public Duration getLeakDetectionThreshold() { return leakDetectionThreshold; }
    public int getMaxRetryAttempts() { return maxRetryAttempts; }
    public Duration getRetryDelay() { return retryDelay; }
    public boolean isRetryWithJitter() { return retryWithJitter; }
    public boolean isReadOnly() { return readOnly; }
    public String[] getReadReplicaUrls() { return readReplicaUrls.clone(); }
    public Map<String, String> getProperties() { return properties; }

    /**
     * Get the effective JDBC URL, either explicit or constructed.
     */
    public String getEffectiveJdbcUrl() {
        if (jdbcUrl != null && !jdbcUrl.isEmpty()) {
            return jdbcUrl;
        }
        return buildJdbcUrl();
    }

    private String buildJdbcUrl() {
        if (databaseType == null) {
            throw new IllegalStateException("Database type must be set to build JDBC URL");
        }

        return switch (databaseType) {
            case POSTGRESQL -> String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
            case MYSQL -> String.format("jdbc:mysql://%s:%d/%s", host, port, database);
            case ORACLE -> String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, database);
            case SQLSERVER -> String.format("jdbc:sqlserver://%s:%d;databaseName=%s", host, port, database);
            case H2 -> String.format("jdbc:h2:mem:%s", database);
            case SQLITE -> String.format("jdbc:sqlite:%s", database);
            case MARIADB -> String.format("jdbc:mariadb://%s:%d/%s", host, port, database);
            default -> throw new UnsupportedOperationException("Cannot build JDBC URL for " + databaseType);
        };
    }

    /**
     * Builder for ConnectionConfig with fluent API.
     */
    public static class Builder {
        private String name = "default";
        private DatabaseType databaseType;
        private String host = "localhost";
        private int port = -1;
        private String database;
        private String username;
        private String password;
        private String jdbcUrl;
        private int minimumPoolSize = 5;
        private int maximumPoolSize = 20;
        private Duration connectionTimeout = Duration.ofSeconds(30);
        private Duration idleTimeout = Duration.ofMinutes(10);
        private Duration maxLifetime = Duration.ofMinutes(30);
        private Duration leakDetectionThreshold = Duration.ofSeconds(60);
        private int maxRetryAttempts = 3;
        private Duration retryDelay = Duration.ofMillis(100);
        private boolean retryWithJitter = true;
        private boolean readOnly = false;
        private String[] readReplicaUrls;
        private final Map<String, String> properties = new HashMap<>();

        public Builder name(String name) {
            this.name = Objects.requireNonNull(name, "Name cannot be null");
            return this;
        }

        public Builder databaseType(DatabaseType type) {
            this.databaseType = Objects.requireNonNull(type, "Database type cannot be null");
            if (this.port == -1) {
                this.port = type.getDefaultPort();
            }
            return this;
        }

        public Builder postgresql() {
            return databaseType(DatabaseType.POSTGRESQL);
        }

        public Builder mysql() {
            return databaseType(DatabaseType.MYSQL);
        }

        public Builder oracle() {
            return databaseType(DatabaseType.ORACLE);
        }

        public Builder sqlServer() {
            return databaseType(DatabaseType.SQLSERVER);
        }

        public Builder mongodb() {
            return databaseType(DatabaseType.MONGODB);
        }

        public Builder h2() {
            return databaseType(DatabaseType.H2);
        }

        public Builder host(String host) {
            this.host = Objects.requireNonNull(host, "Host cannot be null");
            return this;
        }

        public Builder port(int port) {
            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("Port must be between 1 and 65535");
            }
            this.port = port;
            return this;
        }

        public Builder database(String database) {
            this.database = Objects.requireNonNull(database, "Database cannot be null");
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder credentials(String username, String password) {
            this.username = username;
            this.password = password;
            return this;
        }

        public Builder jdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
            if (databaseType == null && jdbcUrl != null) {
                this.databaseType = DatabaseType.fromJdbcUrl(jdbcUrl);
            }
            return this;
        }

        public Builder minimumPoolSize(int size) {
            this.minimumPoolSize = size;
            return this;
        }

        public Builder maximumPoolSize(int size) {
            this.maximumPoolSize = size;
            return this;
        }

        public Builder poolSize(int min, int max) {
            this.minimumPoolSize = min;
            this.maximumPoolSize = max;
            return this;
        }

        public Builder connectionTimeout(Duration timeout) {
            this.connectionTimeout = Objects.requireNonNull(timeout);
            return this;
        }

        public Builder idleTimeout(Duration timeout) {
            this.idleTimeout = Objects.requireNonNull(timeout);
            return this;
        }

        public Builder maxLifetime(Duration lifetime) {
            this.maxLifetime = Objects.requireNonNull(lifetime);
            return this;
        }

        public Builder leakDetectionThreshold(Duration threshold) {
            this.leakDetectionThreshold = Objects.requireNonNull(threshold);
            return this;
        }

        public Builder maxRetryAttempts(int attempts) {
            this.maxRetryAttempts = attempts;
            return this;
        }

        public Builder retryDelay(Duration delay) {
            this.retryDelay = Objects.requireNonNull(delay);
            return this;
        }

        public Builder retryWithJitter(boolean withJitter) {
            this.retryWithJitter = withJitter;
            return this;
        }

        public Builder readOnly(boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        public Builder readReplicas(String... urls) {
            this.readReplicaUrls = urls;
            return this;
        }

        public Builder property(String key, String value) {
            this.properties.put(key, value);
            return this;
        }

        public Builder properties(Map<String, String> properties) {
            this.properties.putAll(properties);
            return this;
        }

        public ConnectionConfig build() {
            validate();
            return new ConnectionConfig(this);
        }

        private void validate() {
            if (jdbcUrl == null && database == null) {
                throw new IllegalStateException("Either jdbcUrl or database must be set");
            }
            if (maximumPoolSize < minimumPoolSize) {
                throw new IllegalStateException("Maximum pool size must be >= minimum pool size");
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectionConfig that = (ConnectionConfig) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return String.format("ConnectionConfig{name='%s', type=%s, host='%s', port=%d, database='%s'}",
            name, databaseType, host, port, database);
    }
}
