package io.dataverse.core.connection;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Environment-aware configuration loader.
 *
 * <p>Feature #5: Environment-Aware Config - Auto-detect and load configurations
 *
 * <p>Configuration precedence (highest to lowest):
 * <ol>
 *   <li>Explicit programmatic configuration</li>
 *   <li>System properties</li>
 *   <li>Environment variables</li>
 *   <li>Profile-specific config file (dataverse-{profile}.properties)</li>
 *   <li>Default config file (dataverse.properties)</li>
 * </ol>
 *
 * @since 1.0.0
 */
public class EnvironmentConfig {

    private static final String CONFIG_PREFIX = "dataverse.";
    private static final String DEFAULT_CONFIG_FILE = "dataverse.properties";
    private static final String ENV_PREFIX = "DATAVERSE_";

    private final Environment environment;
    private final Map<String, String> config = new HashMap<>();

    public EnvironmentConfig() {
        this.environment = detectEnvironment();
        loadConfiguration();
    }

    public EnvironmentConfig(Environment environment) {
        this.environment = Objects.requireNonNull(environment);
        loadConfiguration();
    }

    /**
     * Get the detected environment.
     */
    public Environment getEnvironment() {
        return environment;
    }

    /**
     * Get a configuration value.
     */
    public String get(String key) {
        return config.get(key);
    }

    /**
     * Get a configuration value with default.
     */
    public String get(String key, String defaultValue) {
        return config.getOrDefault(key, defaultValue);
    }

    /**
     * Get an integer configuration value.
     */
    public int getInt(String key, int defaultValue) {
        String value = config.get(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Get a boolean configuration value.
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = config.get(key);
        if (value == null) return defaultValue;
        return Boolean.parseBoolean(value);
    }

    /**
     * Get a long configuration value.
     */
    public long getLong(String key, long defaultValue) {
        String value = config.get(key);
        if (value == null) return defaultValue;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Get all configuration entries.
     */
    public Map<String, String> getAll() {
        return Map.copyOf(config);
    }

    /**
     * Override a configuration value.
     */
    public void set(String key, String value) {
        config.put(key, value);
    }

    /**
     * Build a ConnectionConfig from environment settings.
     */
    public ConnectionConfig buildConnectionConfig() {
        return buildConnectionConfig("default");
    }

    /**
     * Build a named ConnectionConfig from environment settings.
     */
    public ConnectionConfig buildConnectionConfig(String name) {
        String prefix = name.equals("default") ? "" : name + ".";

        ConnectionConfig.Builder builder = ConnectionConfig.builder(name);

        String dbType = get(prefix + "database.type");
        if (dbType != null) {
            builder.databaseType(DatabaseType.fromName(dbType));
        }

        String jdbcUrl = get(prefix + "database.url");
        if (jdbcUrl != null) {
            builder.jdbcUrl(jdbcUrl);
        }

        String host = get(prefix + "database.host");
        if (host != null) {
            builder.host(host);
        }

        String port = get(prefix + "database.port");
        if (port != null) {
            builder.port(Integer.parseInt(port));
        }

        String database = get(prefix + "database.name");
        if (database != null) {
            builder.database(database);
        }

        String username = get(prefix + "database.username");
        String password = get(prefix + "database.password");
        if (username != null) {
            builder.credentials(username, password);
        }

        // Pool settings
        builder.minimumPoolSize(getInt(prefix + "pool.min-size", 5));
        builder.maximumPoolSize(getInt(prefix + "pool.max-size", 20));

        // Timeout settings
        int connTimeout = getInt(prefix + "timeout.connection", 30);
        builder.connectionTimeout(java.time.Duration.ofSeconds(connTimeout));

        int idleTimeout = getInt(prefix + "timeout.idle", 600);
        builder.idleTimeout(java.time.Duration.ofSeconds(idleTimeout));

        // Retry settings
        builder.maxRetryAttempts(getInt(prefix + "retry.max-attempts", 3));
        builder.retryWithJitter(getBoolean(prefix + "retry.jitter", true));

        return builder.build();
    }

    private Environment detectEnvironment() {
        // Check system property first
        String env = System.getProperty("dataverse.environment");
        if (env != null) {
            return Environment.fromName(env);
        }

        // Check environment variable
        env = System.getenv("DATAVERSE_ENVIRONMENT");
        if (env != null) {
            return Environment.fromName(env);
        }

        // Check common environment indicators
        env = System.getenv("ENVIRONMENT");
        if (env != null) {
            return Environment.fromName(env);
        }

        env = System.getenv("ENV");
        if (env != null) {
            return Environment.fromName(env);
        }

        // Check Spring profile
        env = System.getProperty("spring.profiles.active");
        if (env != null) {
            return Environment.fromName(env.split(",")[0].trim());
        }

        // Default to development
        return Environment.DEVELOPMENT;
    }

    private void loadConfiguration() {
        // Load default config
        loadPropertiesFile(DEFAULT_CONFIG_FILE);

        // Load environment-specific config
        String envConfig = String.format("dataverse-%s.properties", environment.getName());
        loadPropertiesFile(envConfig);

        // Load from system properties
        loadSystemProperties();

        // Load from environment variables
        loadEnvironmentVariables();
    }

    private void loadPropertiesFile(String filename) {
        // Try classpath first
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(filename)) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                props.forEach((k, v) -> config.put(normalizeKey(k.toString()), v.toString()));
                return;
            }
        } catch (IOException e) {
            // Ignore and try file system
        }

        // Try file system
        Path path = Path.of(filename);
        if (Files.exists(path)) {
            try (InputStream is = Files.newInputStream(path)) {
                Properties props = new Properties();
                props.load(is);
                props.forEach((k, v) -> config.put(normalizeKey(k.toString()), v.toString()));
            } catch (IOException e) {
                // Log warning but continue
            }
        }
    }

    private void loadSystemProperties() {
        System.getProperties().forEach((k, v) -> {
            String key = k.toString();
            if (key.startsWith(CONFIG_PREFIX)) {
                config.put(key.substring(CONFIG_PREFIX.length()), v.toString());
            }
        });
    }

    private void loadEnvironmentVariables() {
        System.getenv().forEach((k, v) -> {
            if (k.startsWith(ENV_PREFIX)) {
                String key = k.substring(ENV_PREFIX.length())
                    .toLowerCase()
                    .replace("_", ".");
                config.put(key, v);
            }
        });
    }

    private String normalizeKey(String key) {
        if (key.startsWith(CONFIG_PREFIX)) {
            return key.substring(CONFIG_PREFIX.length());
        }
        return key;
    }

    /**
     * Environment enumeration.
     */
    public enum Environment {
        DEVELOPMENT("development", "dev"),
        TESTING("testing", "test"),
        STAGING("staging", "stage"),
        PRODUCTION("production", "prod");

        private final String name;
        private final String shortName;

        Environment(String name, String shortName) {
            this.name = name;
            this.shortName = shortName;
        }

        public String getName() {
            return name;
        }

        public String getShortName() {
            return shortName;
        }

        public boolean isDevelopment() {
            return this == DEVELOPMENT;
        }

        public boolean isProduction() {
            return this == PRODUCTION;
        }

        public static Environment fromName(String name) {
            if (name == null || name.isEmpty()) {
                return DEVELOPMENT;
            }

            String lower = name.toLowerCase().trim();
            for (Environment env : values()) {
                if (env.name.equals(lower) || env.shortName.equals(lower)) {
                    return env;
                }
            }

            // Common aliases
            return switch (lower) {
                case "local", "localhost" -> DEVELOPMENT;
                case "qa", "uat" -> STAGING;
                case "live", "main" -> PRODUCTION;
                default -> DEVELOPMENT;
            };
        }
    }
}
