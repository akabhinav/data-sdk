package io.dataverse.core.sync;

import java.time.Duration;
import java.time.Instant;

/**
 * Result of a synchronization operation.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class SyncResult {

  private final int recordsSynced;
  private final int conflictsResolved;
  private final int recordsDeleted;
  private final Instant startTime;
  private final Instant endTime;
  private final Duration duration;

  private SyncResult(Builder builder) {
    this.recordsSynced = builder.recordsSynced;
    this.conflictsResolved = builder.conflictsResolved;
    this.recordsDeleted = builder.recordsDeleted;
    this.startTime = builder.startTime;
    this.endTime = builder.endTime;
    this.duration = builder.duration;
  }

  public int getRecordsSynced() {
    return recordsSynced;
  }

  public int getConflictsResolved() {
    return conflictsResolved;
  }

  public int getRecordsDeleted() {
    return recordsDeleted;
  }

  public Instant getStartTime() {
    return startTime;
  }

  public Instant getEndTime() {
    return endTime;
  }

  public Duration getDuration() {
    return duration;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public String toString() {
    return "SyncResult{" +
        "recordsSynced=" + recordsSynced +
        ", conflictsResolved=" + conflictsResolved +
        ", recordsDeleted=" + recordsDeleted +
        ", duration=" + (duration != null ? duration.toMillis() + "ms" : "N/A") +
        '}';
  }

  /**
   * Builder for SyncResult.
   */
  public static class Builder {
    private int recordsSynced;
    private int conflictsResolved;
    private int recordsDeleted;
    private Instant startTime = Instant.now();
    private Instant endTime;
    private Duration duration;

    public Builder recordsSynced(int recordsSynced) {
      this.recordsSynced = recordsSynced;
      return this;
    }

    public Builder conflictsResolved(int conflictsResolved) {
      this.conflictsResolved = conflictsResolved;
      return this;
    }

    public Builder recordsDeleted(int recordsDeleted) {
      this.recordsDeleted = recordsDeleted;
      return this;
    }

    public Builder startTime(Instant startTime) {
      this.startTime = startTime;
      return this;
    }

    public Builder endTime(Instant endTime) {
      this.endTime = endTime;
      return this;
    }

    public Builder duration(Duration duration) {
      this.duration = duration;
      return this;
    }

    public SyncResult build() {
      if (endTime == null) {
        endTime = Instant.now();
      }
      if (duration == null && startTime != null && endTime != null) {
        duration = Duration.between(startTime, endTime);
      }
      return new SyncResult(this);
    }
  }
}
