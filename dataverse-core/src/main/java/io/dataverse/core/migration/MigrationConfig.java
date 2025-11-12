package io.dataverse.core.migration;

/**
 * Configuration for migration behavior.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * MigrationConfig config = MigrationConfig.builder()
 *     .validateChecksums(true)
 *     .rollbackOnFailure(true)
 *     .migrationsTable("schema_migrations")
 *     .migrationsLocation("classpath:db/migrations")
 *     .baselineOnMigrate(false)
 *     .outOfOrder(false)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class MigrationConfig {

  private final boolean validateChecksums;
  private final boolean rollbackOnFailure;
  private final String migrationsTable;
  private final String migrationsLocation;
  private final boolean baselineOnMigrate;
  private final int baselineVersion;
  private final boolean outOfOrder;
  private final boolean cleanOnValidationError;

  private MigrationConfig(Builder builder) {
    this.validateChecksums = builder.validateChecksums;
    this.rollbackOnFailure = builder.rollbackOnFailure;
    this.migrationsTable = builder.migrationsTable;
    this.migrationsLocation = builder.migrationsLocation;
    this.baselineOnMigrate = builder.baselineOnMigrate;
    this.baselineVersion = builder.baselineVersion;
    this.outOfOrder = builder.outOfOrder;
    this.cleanOnValidationError = builder.cleanOnValidationError;
  }

  /**
   * Whether to validate checksums of applied migrations.
   *
   * @return true if checksums should be validated
   */
  public boolean isValidateChecksums() {
    return validateChecksums;
  }

  /**
   * Whether to rollback on migration failure.
   *
   * @return true if rollback on failure is enabled
   */
  public boolean isRollbackOnFailure() {
    return rollbackOnFailure;
  }

  /**
   * Gets the name of the migrations tracking table.
   *
   * @return table name
   */
  public String getMigrationsTable() {
    return migrationsTable;
  }

  /**
   * Gets the location of migration scripts.
   *
   * @return migrations location
   */
  public String getMigrationsLocation() {
    return migrationsLocation;
  }

  /**
   * Whether to create baseline on first migrate.
   *
   * @return true if baseline on migrate is enabled
   */
  public boolean isBaselineOnMigrate() {
    return baselineOnMigrate;
  }

  /**
   * Gets the baseline version number.
   *
   * @return baseline version
   */
  public int getBaselineVersion() {
    return baselineVersion;
  }

  /**
   * Whether to allow out-of-order migrations.
   *
   * @return true if out-of-order is allowed
   */
  public boolean isOutOfOrder() {
    return outOfOrder;
  }

  /**
   * Whether to clean database on validation error.
   *
   * @return true if clean on validation error is enabled
   */
  public boolean isCleanOnValidationError() {
    return cleanOnValidationError;
  }

  /**
   * Creates a default configuration.
   *
   * @return default config
   */
  public static MigrationConfig defaults() {
    return builder().build();
  }

  /**
   * Creates a builder for MigrationConfig.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for MigrationConfig.
   */
  public static class Builder {
    private boolean validateChecksums = true;
    private boolean rollbackOnFailure = true;
    private String migrationsTable = "schema_migrations";
    private String migrationsLocation = "classpath:db/migrations";
    private boolean baselineOnMigrate = false;
    private int baselineVersion = 1;
    private boolean outOfOrder = false;
    private boolean cleanOnValidationError = false;

    /**
     * Sets whether to validate checksums.
     *
     * @param validateChecksums true to validate checksums
     * @return this builder
     */
    public Builder validateChecksums(boolean validateChecksums) {
      this.validateChecksums = validateChecksums;
      return this;
    }

    /**
     * Sets whether to rollback on failure.
     *
     * @param rollbackOnFailure true to rollback on failure
     * @return this builder
     */
    public Builder rollbackOnFailure(boolean rollbackOnFailure) {
      this.rollbackOnFailure = rollbackOnFailure;
      return this;
    }

    /**
     * Sets the migrations table name.
     *
     * @param migrationsTable the table name
     * @return this builder
     */
    public Builder migrationsTable(String migrationsTable) {
      this.migrationsTable = migrationsTable;
      return this;
    }

    /**
     * Sets the migrations location.
     *
     * @param migrationsLocation the migrations location
     * @return this builder
     */
    public Builder migrationsLocation(String migrationsLocation) {
      this.migrationsLocation = migrationsLocation;
      return this;
    }

    /**
     * Sets whether to create baseline on migrate.
     *
     * @param baselineOnMigrate true to baseline on migrate
     * @return this builder
     */
    public Builder baselineOnMigrate(boolean baselineOnMigrate) {
      this.baselineOnMigrate = baselineOnMigrate;
      return this;
    }

    /**
     * Sets the baseline version.
     *
     * @param baselineVersion the baseline version
     * @return this builder
     */
    public Builder baselineVersion(int baselineVersion) {
      this.baselineVersion = baselineVersion;
      return this;
    }

    /**
     * Sets whether to allow out-of-order migrations.
     *
     * @param outOfOrder true to allow out-of-order
     * @return this builder
     */
    public Builder outOfOrder(boolean outOfOrder) {
      this.outOfOrder = outOfOrder;
      return this;
    }

    /**
     * Sets whether to clean on validation error.
     *
     * @param cleanOnValidationError true to clean on error
     * @return this builder
     */
    public Builder cleanOnValidationError(boolean cleanOnValidationError) {
      this.cleanOnValidationError = cleanOnValidationError;
      return this;
    }

    /**
     * Builds the configuration.
     *
     * @return the migration config
     */
    public MigrationConfig build() {
      return new MigrationConfig(this);
    }
  }
}
