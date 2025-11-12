package io.dataverse.adapter.postgresql;

import io.dataverse.api.BatchOperations;
import io.dataverse.api.Entity;
import io.dataverse.core.AbstractRepository;
import io.dataverse.core.DefaultBatchResult;

import javax.sql.DataSource;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;

/**
 * JDBC-based repository implementation for PostgreSQL.
 *
 * <p>Features:
 * <ul>
 *   <li>CRUD operations via PreparedStatements</li>
 *   <li>Batch operations with executeBatch</li>
 *   <li>SQL injection prevention</li>
 *   <li>Automatic table name derivation</li>
 *   <li>Reflection-based field mapping</li>
 * </ul>
 *
 * @param <T> the entity type
 * @param <ID> the ID type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class JDBCRepository<T extends Entity<ID>, ID extends Serializable>
    extends AbstractRepository<T, ID> {

  private final DataSource dataSource;
  private final String tableName;
  private final PostgreSQLAdapter adapter;

  public JDBCRepository(Class<T> entityClass, DataSource dataSource, PostgreSQLAdapter adapter) {
    super(entityClass);
    this.dataSource = dataSource;
    this.adapter = adapter;
    this.tableName = deriveTableName(entityClass);
  }

  /**
   * Derives table name from entity class name (converts camelCase to snake_case).
   */
  private String deriveTableName(Class<T> entityClass) {
    String className = entityClass.getSimpleName();
    return className.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase() + "s";
  }

  @Override
  protected T doSave(T entity) {
    if (entity.getId() == null) {
      return insert(entity);
    } else {
      return update(entity);
    }
  }

  private T insert(T entity) {
    List<Field> fields = getAllFields(entityClass);
    StringBuilder sql = new StringBuilder("INSERT INTO " + tableName + " (");
    StringBuilder values = new StringBuilder("VALUES (");

    boolean first = true;
    for (Field field : fields) {
      if (field.getName().equals("id")) continue;  // Skip ID for insert

      if (!first) {
        sql.append(", ");
        values.append(", ");
      }
      sql.append(toSnakeCase(field.getName()));
      values.append("?");
      first = false;
    }

    sql.append(") ").append(values).append(") RETURNING id");

    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

      int paramIndex = 1;
      for (Field field : fields) {
        if (field.getName().equals("id")) continue;

        field.setAccessible(true);
        stmt.setObject(paramIndex++, field.get(entity));
      }

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          Object generatedId = rs.getObject(1);
          entity.setId((ID) generatedId);
        }
      }

      return entity;

    } catch (SQLException | IllegalAccessException e) {
      throw new RuntimeException("Failed to insert entity", e);
    }
  }

  private T update(T entity) {
    List<Field> fields = getAllFields(entityClass);
    StringBuilder sql = new StringBuilder("UPDATE " + tableName + " SET ");

    boolean first = true;
    for (Field field : fields) {
      if (field.getName().equals("id")) continue;

      if (!first) sql.append(", ");
      sql.append(toSnakeCase(field.getName())).append(" = ?");
      first = false;
    }

    sql.append(" WHERE id = ?");

    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

      int paramIndex = 1;
      for (Field field : fields) {
        if (field.getName().equals("id")) continue;

        field.setAccessible(true);
        stmt.setObject(paramIndex++, field.get(entity));
      }

      stmt.setObject(paramIndex, entity.getId());
      stmt.executeUpdate();

      return entity;

    } catch (SQLException | IllegalAccessException e) {
      throw new RuntimeException("Failed to update entity", e);
    }
  }

  @Override
  protected Optional<T> doFindById(ID id) {
    String sql = "SELECT * FROM " + tableName + " WHERE id = ?";

    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setObject(1, id);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapResultSetToEntity(rs));
        }
      }

      return Optional.empty();

    } catch (SQLException e) {
      throw new RuntimeException("Failed to find entity by ID", e);
    }
  }

  @Override
  protected List<T> doFindAll() {
    String sql = "SELECT * FROM " + tableName;
    List<T> results = new ArrayList<>();

    try (Connection conn = dataSource.getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

      while (rs.next()) {
        results.add(mapResultSetToEntity(rs));
      }

      return results;

    } catch (SQLException e) {
      throw new RuntimeException("Failed to find all entities", e);
    }
  }

  @Override
  protected void doDeleteById(ID id) {
    String sql = "DELETE FROM " + tableName + " WHERE id = ?";

    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setObject(1, id);
      stmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException("Failed to delete entity", e);
    }
  }

  @Override
  protected boolean doExistsById(ID id) {
    String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE id = ?";

    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setObject(1, id);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1) > 0;
        }
      }

      return false;

    } catch (SQLException e) {
      throw new RuntimeException("Failed to check if entity exists", e);
    }
  }

  @Override
  protected long doCount() {
    String sql = "SELECT COUNT(*) FROM " + tableName;

    try (Connection conn = dataSource.getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

      if (rs.next()) {
        return rs.getLong(1);
      }

      return 0;

    } catch (SQLException e) {
      throw new RuntimeException("Failed to count entities", e);
    }
  }

  @Override
  public BatchOperations<T, ID> batch() {
    return new JDBCBatchOperations<>(this, dataSource, tableName, entityClass);
  }

  /**
   * Maps a ResultSet row to an entity instance.
   */
  T mapResultSetToEntity(ResultSet rs) throws SQLException {
    try {
      T entity = entityClass.getDeclaredConstructor().newInstance();
      List<Field> fields = getAllFields(entityClass);

      for (Field field : fields) {
        field.setAccessible(true);
        String columnName = toSnakeCase(field.getName());

        try {
          Object value = rs.getObject(columnName);
          if (value != null) {
            field.set(entity, value);
          }
        } catch (SQLException e) {
          // Column might not exist, skip it
        }
      }

      return entity;

    } catch (Exception e) {
      throw new SQLException("Failed to map ResultSet to entity", e);
    }
  }

  /**
   * Gets all fields including inherited fields.
   */
  private List<Field> getAllFields(Class<?> type) {
    List<Field> fields = new ArrayList<>();
    Class<?> currentClass = type;

    while (currentClass != null && currentClass != Object.class) {
      fields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
      currentClass = currentClass.getSuperclass();
    }

    return fields;
  }

  /**
   * Converts camelCase to snake_case.
   */
  private String toSnakeCase(String camelCase) {
    return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
  }

  /**
   * Gets the table name.
   */
  public String getTableName() {
    return tableName;
  }

  /**
   * Gets the data source.
   */
  public DataSource getDataSource() {
    return dataSource;
  }
}
