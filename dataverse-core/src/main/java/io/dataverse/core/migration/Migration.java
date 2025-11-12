package io.dataverse.core.migration;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a single database schema migration.
 *
 * <p>Migrations are versioned changes to the database schema that can be
 * applied forward (upgrade) or backward (rollback).
 *
 * <p><strong>Naming Convention:</strong>
 * <pre>
 * V{version}__{description}.sql
 *
 * Examples:
 * V001__create_users_table.sql
 * V002__add_email_index.sql
 * V003__add_orders_table.sql
 * </pre>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * Migration migration = Migration.builder()
 *     .version(1)
 *     .description("Create users table")
 *     .script("""
 *         CREATE TABLE users (
 *             id BIGSERIAL PRIMARY KEY,
 *             username VARCHAR(255) NOT NULL,
 *             email VARCHAR(255) NOT NULL UNIQUE,
 *             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
 *         );
 *         """)
 *     .rollbackScript("""
 *         DROP TABLE IF EXISTS users;
 *         """)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class Migration {

  private final int version;
  private final String description;
  private final String script;
  private final String rollbackScript;
  private final String checksum;
  private final MigrationType type;
  private final Instant executedAt;
  private final long executionTimeMs;
  private final MigrationStatus status;

  private Migration(Builder builder) {
    this.version = builder.version;
    this.description = builder.description;
    this.script = builder.script;
    this.rollbackScript = builder.rollbackScript;
    this.checksum = builder.checksum != null ? builder.checksum : calculateChecksum(script);
    this.type = builder.type;
    this.executedAt = builder.executedAt;
    this.executionTimeMs = builder.executionTimeMs;
    this.status = builder.status;
  }

  public int getVersion() {
    return version;
  }

  public String getDescription() {
    return description;
  }

  public String getScript() {
    return script;
  }

  public String getRollbackScript() {
    return rollbackScript;
  }

  public String getChecksum() {
    return checksum;
  }

  public MigrationType getType() {
    return type;
  }

  public Instant getExecutedAt() {
    return executedAt;
  }

  public long getExecutionTimeMs() {
    return executionTimeMs;
  }

  public MigrationStatus getStatus() {
    return status;
  }

  public boolean hasRollbackScript() {
    return rollbackScript != null && !rollbackScript.isBlank();
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Calculates a checksum for the migration script.
   *
   * @param script the script content
   * @return checksum string
   */
  private static String calculateChecksum(String script) {
    if (script == null || script.isBlank()) {
      return "";
    }
    // Simple checksum - in production would use SHA-256 or similar
    return String.valueOf(script.hashCode());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Migration migration = (Migration) o;
    return version == migration.version;
  }

  @Override
  public int hashCode() {
    return Objects.hash(version);
  }

  @Override
  public String toString() {
    return "Migration{" +
        "version=" + version +
        ", description='" + description + '\'' +
        ", type=" + type +
        ", status=" + status +
        '}';
  }

  /**
   * Migration type enumeration.
   */
  public enum MigrationType {
    /** SQL migration script */
    SQL,

    /** Java-based migration */
    JAVA,

    /** Baseline migration (marks current state) */
    BASELINE
  }

  /**
   * Migration status enumeration.
   */
  public enum MigrationStatus {
    /** Pending execution */
    PENDING,

    /** Currently executing */
    EXECUTING,

    /** Successfully executed */
    SUCCESS,

    /** Execution failed */
    FAILED,

    /** Rolled back */
    ROLLED_BACK
  }

  /**
   * Builder for Migration.
   */
  public static class Builder {
    private int version;
    private String description;
    private String script;
    private String rollbackScript;
    private String checksum;
    private MigrationType type = MigrationType.SQL;
    private Instant executedAt;
    private long executionTimeMs;
    private MigrationStatus status = MigrationStatus.PENDING;

    public Builder version(int version) {
      this.version = version;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder script(String script) {
      this.script = script;
      return this;
    }

    public Builder rollbackScript(String rollbackScript) {
      this.rollbackScript = rollbackScript;
      return this;
    }

    public Builder checksum(String checksum) {
      this.checksum = checksum;
      return this;
    }

    public Builder type(MigrationType type) {
      this.type = type;
      return this;
    }

    public Builder executedAt(Instant executedAt) {
      this.executedAt = executedAt;
      return this;
    }

    public Builder executionTimeMs(long executionTimeMs) {
      this.executionTimeMs = executionTimeMs;
      return this;
    }

    public Builder status(MigrationStatus status) {
      this.status = status;
      return this;
    }

    public Migration build() {
      if (version <= 0) {
        throw new IllegalArgumentException("Version must be positive");
      }
      if (description == null || description.isBlank()) {
        throw new IllegalArgumentException("Description is required");
      }
      if (script == null || script.isBlank()) {
        throw new IllegalArgumentException("Script is required");
      }
      return new Migration(this);
    }
  }
}
