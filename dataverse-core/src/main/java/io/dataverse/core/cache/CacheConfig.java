package io.dataverse.core.cache;

import java.time.Duration;

/**
 * Configuration for multi-level caching.
 *
 * <p>Provides settings for:
 * <ul>
 *   <li>Cache enablement (global on/off switch)</li>
 *   <li>TTL settings for each cache level</li>
 *   <li>Size limits for L1 cache</li>
 *   <li>Cache write strategies (write-through, write-behind)</li>
 *   <li>Eviction policies</li>
 * </ul>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class CacheConfig {

  private final boolean enabled;
  private final boolean l1Enabled;
  private final boolean l2Enabled;
  private final Duration l1Ttl;
  private final Duration l2Ttl;
  private final long l1MaxSize;
  private final WriteStrategy writeStrategy;
  private final boolean recordStats;

  private CacheConfig(Builder builder) {
    this.enabled = builder.enabled;
    this.l1Enabled = builder.l1Enabled;
    this.l2Enabled = builder.l2Enabled;
    this.l1Ttl = builder.l1Ttl;
    this.l2Ttl = builder.l2Ttl;
    this.l1MaxSize = builder.l1MaxSize;
    this.writeStrategy = builder.writeStrategy;
    this.recordStats = builder.recordStats;
  }

  public static Builder builder() {
    return new Builder();
  }

  public boolean isEnabled() {
    return enabled;
  }

  public boolean isL1Enabled() {
    return enabled && l1Enabled;
  }

  public boolean isL2Enabled() {
    return enabled && l2Enabled;
  }

  public Duration getL1Ttl() {
    return l1Ttl;
  }

  public Duration getL2Ttl() {
    return l2Ttl;
  }

  public long getL1MaxSize() {
    return l1MaxSize;
  }

  public WriteStrategy getWriteStrategy() {
    return writeStrategy;
  }

  public boolean isRecordStats() {
    return recordStats;
  }

  /**
   * Cache write strategy enumeration.
   */
  public enum WriteStrategy {
    /**
     * Write-through: Updates are written to cache and database synchronously.
     * Ensures consistency but slower writes.
     */
    WRITE_THROUGH,

    /**
     * Write-behind: Updates are written to cache immediately, database later.
     * Faster writes but potential data loss.
     */
    WRITE_BEHIND,

    /**
     * Write-around: Writes go directly to database, bypassing cache.
     * Good for write-heavy workloads.
     */
    WRITE_AROUND
  }

  public static class Builder {
    private boolean enabled = true;
    private boolean l1Enabled = true;
    private boolean l2Enabled = false;  // L2 requires explicit configuration
    private Duration l1Ttl = Duration.ofMinutes(5);
    private Duration l2Ttl = Duration.ofHours(1);
    private long l1MaxSize = 10_000;
    private WriteStrategy writeStrategy = WriteStrategy.WRITE_THROUGH;
    private boolean recordStats = true;

    /**
     * Enables or disables caching globally.
     *
     * @param enabled true to enable caching
     * @return this builder
     */
    public Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    /**
     * Enables or disables L1 (in-memory) cache.
     *
     * @param l1Enabled true to enable L1 cache
     * @return this builder
     */
    public Builder l1Enabled(boolean l1Enabled) {
      this.l1Enabled = l1Enabled;
      return this;
    }

    /**
     * Enables or disables L2 (distributed) cache.
     *
     * @param l2Enabled true to enable L2 cache
     * @return this builder
     */
    public Builder l2Enabled(boolean l2Enabled) {
      this.l2Enabled = l2Enabled;
      return this;
    }

    /**
     * Sets the time-to-live for L1 cache entries.
     *
     * @param l1Ttl the L1 TTL duration
     * @return this builder
     */
    public Builder l1Ttl(Duration l1Ttl) {
      this.l1Ttl = l1Ttl;
      return this;
    }

    /**
     * Sets the time-to-live for L2 cache entries.
     *
     * @param l2Ttl the L2 TTL duration
     * @return this builder
     */
    public Builder l2Ttl(Duration l2Ttl) {
      this.l2Ttl = l2Ttl;
      return this;
    }

    /**
     * Sets the maximum size for L1 cache.
     *
     * @param l1MaxSize the maximum number of entries
     * @return this builder
     */
    public Builder l1MaxSize(long l1MaxSize) {
      this.l1MaxSize = l1MaxSize;
      return this;
    }

    /**
     * Sets the write strategy for cache updates.
     *
     * @param writeStrategy the write strategy
     * @return this builder
     */
    public Builder writeStrategy(WriteStrategy writeStrategy) {
      this.writeStrategy = writeStrategy;
      return this;
    }

    /**
     * Enables or disables cache statistics recording.
     *
     * @param recordStats true to record statistics
     * @return this builder
     */
    public Builder recordStats(boolean recordStats) {
      this.recordStats = recordStats;
      return this;
    }

    /**
     * Builds the cache configuration.
     *
     * @return the cache configuration
     */
    public CacheConfig build() {
      return new CacheConfig(this);
    }
  }
}
