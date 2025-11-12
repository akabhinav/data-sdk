package io.dataverse.core.sync;

import java.time.Duration;
import java.util.function.BiFunction;

/**
 * Configuration for data synchronization.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * SyncConfig config = SyncConfig.builder()
 *     .syncMode(SyncMode.TWO_WAY)
 *     .conflictResolution(ConflictResolution.LAST_WRITE_WINS)
 *     .syncInterval(Duration.ofMinutes(5))
 *     .batchSize(100)
 *     .continueOnError(true)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class SyncConfig {

  private final SyncMode syncMode;
  private final ConflictResolution conflictResolution;
  private final Duration syncInterval;
  private final int batchSize;
  private final boolean continueOnError;
  private final BiFunction<?, ?, ?> customResolver;

  private SyncConfig(Builder builder) {
    this.syncMode = builder.syncMode;
    this.conflictResolution = builder.conflictResolution;
    this.syncInterval = builder.syncInterval;
    this.batchSize = builder.batchSize;
    this.continueOnError = builder.continueOnError;
    this.customResolver = builder.customResolver;
  }

  public SyncMode getSyncMode() {
    return syncMode;
  }

  public ConflictResolution getConflictResolution() {
    return conflictResolution;
  }

  public Duration getSyncInterval() {
    return syncInterval;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public boolean isContinueOnError() {
    return continueOnError;
  }

  @SuppressWarnings("unchecked")
  public <T> BiFunction<T, T, T> getCustomResolver() {
    return (BiFunction<T, T, T>) customResolver;
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for SyncConfig.
   */
  public static class Builder {
    private SyncMode syncMode = SyncMode.ONE_WAY;
    private ConflictResolution conflictResolution = ConflictResolution.LAST_WRITE_WINS;
    private Duration syncInterval = Duration.ofMinutes(5);
    private int batchSize = 100;
    private boolean continueOnError = true;
    private BiFunction<?, ?, ?> customResolver;

    public Builder syncMode(SyncMode syncMode) {
      this.syncMode = syncMode;
      return this;
    }

    public Builder conflictResolution(ConflictResolution conflictResolution) {
      this.conflictResolution = conflictResolution;
      return this;
    }

    public Builder syncInterval(Duration syncInterval) {
      this.syncInterval = syncInterval;
      return this;
    }

    public Builder batchSize(int batchSize) {
      this.batchSize = batchSize;
      return this;
    }

    public Builder continueOnError(boolean continueOnError) {
      this.continueOnError = continueOnError;
      return this;
    }

    public <T> Builder customResolver(BiFunction<T, T, T> customResolver) {
      this.customResolver = customResolver;
      return this;
    }

    public SyncConfig build() {
      return new SyncConfig(this);
    }
  }
}
