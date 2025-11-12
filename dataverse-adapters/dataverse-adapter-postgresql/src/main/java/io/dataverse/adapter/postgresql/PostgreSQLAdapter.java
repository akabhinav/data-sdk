package io.dataverse.adapter.postgresql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.dataverse.api.Entity;
import io.dataverse.api.Repository;
import io.dataverse.spi.AdapterConfig;
import io.dataverse.spi.DataSourceAdapter;

import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * PostgreSQL adapter using JDBC and HikariCP connection pooling.
 *
 * <p>Configuration properties:
 * <ul>
 *   <li>jdbcUrl - JDBC connection URL (required)</li>
 *   <li>username - Database username (optional)</li>
 *   <li>password - Database password (optional)</li>
 *   <li>maxPoolSize - Maximum connection pool size (default: 10)</li>
 *   <li>minPoolSize - Minimum connection pool size (default: 2)</li>
 *   <li>connectionTimeout - Connection timeout in ms (default: 30000)</li>
 * </ul>
 *
 * <p>Example configuration:
 * <pre>
 * AdapterConfig config = AdapterConfig.builder()
 *     .property("jdbcUrl", "jdbc:postgresql://localhost:5432/mydb")
 *     .property("username", "postgres")
 *     .property("password", "secret")
 *     .property("maxPoolSize", "20")
 *     .build();
 *
 * PostgreSQLAdapter adapter = new PostgreSQLAdapter();
 * adapter.initialize(config);
 * </pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class PostgreSQLAdapter implements DataSourceAdapter {

  private HikariDataSource dataSource;
  private AdapterConfig config;

  @Override
  public String getAdapterId() {
    return "postgresql";
  }

  @Override
  public void initialize(AdapterConfig config) {
    this.config = config;

    // Configure HikariCP
    HikariConfig hikariConfig = new HikariConfig();

    // Required properties
    String jdbcUrl = config.getProperty("jdbcUrl");
    if (jdbcUrl == null || jdbcUrl.isEmpty()) {
      throw new IllegalArgumentException("jdbcUrl property is required");
    }
    hikariConfig.setJdbcUrl(jdbcUrl);

    // Optional properties
    String username = config.getProperty("username");
    if (username != null && !username.isEmpty()) {
      hikariConfig.setUsername(username);
    }

    String password = config.getProperty("password");
    if (password != null && !password.isEmpty()) {
      hikariConfig.setPassword(password);
    }

    // Connection pool settings
    int maxPoolSize = Integer.parseInt(config.getProperty("maxPoolSize", "10"));
    int minPoolSize = Integer.parseInt(config.getProperty("minPoolSize", "2"));
    long connectionTimeout = Long.parseLong(config.getProperty("connectionTimeout", "30000"));

    hikariConfig.setMaximumPoolSize(maxPoolSize);
    hikariConfig.setMinimumIdle(minPoolSize);
    hikariConfig.setConnectionTimeout(connectionTimeout);

    // Performance settings
    hikariConfig.setAutoCommit(true);
    hikariConfig.setConnectionTestQuery("SELECT 1");

    // Initialize data source
    this.dataSource = new HikariDataSource(hikariConfig);
  }

  @Override
  public <T extends Entity<ID>, ID extends Serializable>
      Repository<T, ID> createRepository(Class<T> entityClass) {
    return new JDBCRepository<>(entityClass, dataSource, this);
  }

  @Override
  public boolean isHealthy() {
    try (Connection conn = dataSource.getConnection()) {
      return conn.isValid(5);
    } catch (SQLException e) {
      return false;
    }
  }

  @Override
  public void shutdown() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
    }
  }

  @Override
  public Capabilities getCapabilities() {
    return new Capabilities() {
      @Override
      public boolean supportsTransactions() {
        return true;  // JDBC supports transactions
      }

      @Override
      public boolean supportsBatchOperations() {
        return true;  // JDBC supports batch operations
      }

      @Override
      public boolean supportsAsyncOperations() {
        return true;  // Via CompletableFuture
      }

      @Override
      public int getMaxBatchSize() {
        return 1000;  // Reasonable default for JDBC batch
      }

      @Override
      public int getMaxQueryConditions() {
        return -1;  // No practical limit for SQL WHERE clauses
      }
    };
  }

  @Override
  public io.dataverse.api.TransactionManager getTransactionManager() {
    // TODO: Implement JDBC transaction manager
    return null;
  }

  /**
   * Gets the underlying data source for advanced usage.
   *
   * @return the HikariCP data source
   */
  public DataSource getDataSource() {
    return dataSource;
  }

  /**
   * Gets the adapter configuration.
   *
   * @return the adapter configuration
   */
  public AdapterConfig getConfig() {
    return config;
  }
}
