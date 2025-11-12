package io.dataverse.core.sync;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Manages bidirectional data synchronization between data sources.
 *
 * <p>SyncManager coordinates data flow between primary and secondary data sources,
 * handling conflicts, tracking changes, and ensuring eventual consistency.
 *
 * <p><strong>Synchronization Modes:</strong>
 * <ul>
 *   <li><strong>ONE_WAY</strong> - Primary → Secondary (read replica)</li>
 *   <li><strong>TWO_WAY</strong> - Bidirectional sync with conflict resolution</li>
 *   <li><strong>MASTER_SLAVE</strong> - Primary writes, secondary reads only</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Configure sync between PostgreSQL and MongoDB
 * SyncConfig config = SyncConfig.builder()
 *     .syncMode(SyncMode.TWO_WAY)
 *     .conflictResolution(ConflictResolution.LAST_WRITE_WINS)
 *     .syncInterval(Duration.ofMinutes(5))
 *     .batchSize(100)
 *     .build();
 *
 * SyncManager<User, Long> syncManager = new SyncManager<>(
 *     postgresUserRepo,  // Primary
 *     mongoUserRepo,     // Secondary
 *     config
 * );
 *
 * // Start continuous sync
 * syncManager.start();
 *
 * // Perform one-time sync
 * SyncResult result = syncManager.syncNow();
 * System.out.println("Synced: " + result.getRecordsSynced());
 *
 * // Stop sync
 * syncManager.stop();
 * }</pre>
 *
 * <p><strong>Conflict Resolution Strategies:</strong>
 * <ul>
 *   <li>LAST_WRITE_WINS - Most recent timestamp wins</li>
 *   <li>PRIMARY_WINS - Primary always wins</li>
 *   <li>SECONDARY_WINS - Secondary always wins</li>
 *   <li>MANUAL - Requires manual resolution</li>
 *   <li>CUSTOM - User-provided resolution logic</li>
 * </ul>
 *
 * @param <T> entity type
 * @param <ID> identifier type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class SyncManager<T, ID> {

  private final SyncSource<T, ID> primary;
  private final SyncSource<T, ID> secondary;
  private final SyncConfig config;
  private final List<SyncListener<T>> listeners = new ArrayList<>();

  private SyncState state = SyncState.STOPPED;
  private Thread syncThread;
  private volatile boolean running = false;

  /**
   * Creates a sync manager.
   *
   * @param primary the primary data source
   * @param secondary the secondary data source
   * @param config sync configuration
   */
  public SyncManager(
      SyncSource<T, ID> primary,
      SyncSource<T, ID> secondary,
      SyncConfig config) {
    this.primary = primary;
    this.secondary = secondary;
    this.config = config;
  }

  /**
   * Starts continuous synchronization.
   *
   * <p>Runs sync at configured intervals in a background thread.
   */
  public void start() {
    if (running) {
      throw new IllegalStateException("Sync already running");
    }

    running = true;
    state = SyncState.RUNNING;

    syncThread = Thread.ofVirtual().name("sync-manager").start(() -> {
      while (running) {
        try {
          syncNow();
          Thread.sleep(config.getSyncInterval().toMillis());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        } catch (Exception e) {
          notifyError(e);
          if (!config.isContinueOnError()) {
            break;
          }
        }
      }
    });
  }

  /**
   * Stops synchronization.
   */
  public void stop() {
    if (!running) {
      return;
    }

    running = false;
    state = SyncState.STOPPED;

    if (syncThread != null) {
      syncThread.interrupt();
      try {
        syncThread.join(5000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Performs a one-time synchronization.
   *
   * @return sync result
   */
  public SyncResult syncNow() {
    state = SyncState.SYNCING;
    Instant startTime = Instant.now();

    try {
      SyncResult result = switch (config.getSyncMode()) {
        case ONE_WAY -> syncOneWay();
        case TWO_WAY -> syncTwoWay();
        case MASTER_SLAVE -> syncMasterSlave();
      };

      state = SyncState.RUNNING;
      notifyComplete(result);
      return result;

    } catch (Exception e) {
      state = SyncState.ERROR;
      notifyError(e);
      throw new SyncException("Sync failed", e);
    }
  }

  /**
   * Performs asynchronous synchronization.
   *
   * @return future with sync result
   */
  public CompletableFuture<SyncResult> syncAsync() {
    return CompletableFuture.supplyAsync(this::syncNow);
  }

  /**
   * Adds a sync listener.
   *
   * @param listener the listener
   */
  public void addListener(SyncListener<T> listener) {
    listeners.add(listener);
  }

  /**
   * Removes a sync listener.
   *
   * @param listener the listener
   */
  public void removeListener(SyncListener<T> listener) {
    listeners.remove(listener);
  }

  /**
   * Gets the current sync state.
   *
   * @return current state
   */
  public SyncState getState() {
    return state;
  }

  /**
   * Checks if sync is running.
   *
   * @return true if running
   */
  public boolean isRunning() {
    return running;
  }

  /**
   * One-way sync: Primary → Secondary.
   */
  private SyncResult syncOneWay() {
    List<T> primaryRecords = primary.getAllRecords();
    List<T> secondaryRecords = secondary.getAllRecords();

    SyncResult.Builder resultBuilder = SyncResult.builder();
    int synced = 0;
    int conflicts = 0;

    // Create index of secondary records
    var secondaryIndex = secondary.createIndex(secondaryRecords);

    for (T primaryRecord : primaryRecords) {
      ID id = primary.extractId(primaryRecord);
      T secondaryRecord = secondaryIndex.get(id);

      if (secondaryRecord == null) {
        // Insert new record
        secondary.insert(primaryRecord);
        synced++;
        notifyInsert(primaryRecord);
      } else if (primary.isNewer(primaryRecord, secondaryRecord)) {
        // Update existing record
        secondary.update(primaryRecord);
        synced++;
        notifyUpdate(primaryRecord);
      }
    }

    return resultBuilder
        .recordsSynced(synced)
        .conflictsResolved(conflicts)
        .build();
  }

  /**
   * Two-way sync with conflict resolution.
   */
  private SyncResult syncTwoWay() {
    List<T> primaryRecords = primary.getAllRecords();
    List<T> secondaryRecords = secondary.getAllRecords();

    SyncResult.Builder resultBuilder = SyncResult.builder();
    int synced = 0;
    int conflicts = 0;

    var primaryIndex = primary.createIndex(primaryRecords);
    var secondaryIndex = secondary.createIndex(secondaryRecords);

    // Sync primary → secondary
    for (T primaryRecord : primaryRecords) {
      ID id = primary.extractId(primaryRecord);
      T secondaryRecord = secondaryIndex.get(id);

      if (secondaryRecord == null) {
        secondary.insert(primaryRecord);
        synced++;
      } else if (!primary.equals(primaryRecord, secondaryRecord)) {
        // Conflict detected
        T resolved = resolveConflict(primaryRecord, secondaryRecord);
        secondary.update(resolved);
        conflicts++;
        synced++;
      }
    }

    // Sync secondary → primary
    for (T secondaryRecord : secondaryRecords) {
      ID id = secondary.extractId(secondaryRecord);
      T primaryRecord = primaryIndex.get(id);

      if (primaryRecord == null) {
        primary.insert(secondaryRecord);
        synced++;
      }
    }

    return resultBuilder
        .recordsSynced(synced)
        .conflictsResolved(conflicts)
        .build();
  }

  /**
   * Master-slave sync: Primary writes, secondary reads.
   */
  private SyncResult syncMasterSlave() {
    // Same as one-way, but also delete records from secondary
    // that don't exist in primary
    SyncResult oneWayResult = syncOneWay();

    List<T> primaryRecords = primary.getAllRecords();
    List<T> secondaryRecords = secondary.getAllRecords();

    var primaryIndex = primary.createIndex(primaryRecords);
    int deleted = 0;

    for (T secondaryRecord : secondaryRecords) {
      ID id = secondary.extractId(secondaryRecord);
      if (!primaryIndex.containsKey(id)) {
        secondary.delete(id);
        deleted++;
        notifyDelete(secondaryRecord);
      }
    }

    return SyncResult.builder()
        .recordsSynced(oneWayResult.getRecordsSynced())
        .conflictsResolved(oneWayResult.getConflictsResolved())
        .recordsDeleted(deleted)
        .build();
  }

  /**
   * Resolves a conflict between two records.
   */
  private T resolveConflict(T primaryRecord, T secondaryRecord) {
    return switch (config.getConflictResolution()) {
      case LAST_WRITE_WINS -> {
        Instant primaryTime = primary.getLastModified(primaryRecord);
        Instant secondaryTime = secondary.getLastModified(secondaryRecord);
        yield primaryTime.isAfter(secondaryTime) ? primaryRecord : secondaryRecord;
      }
      case PRIMARY_WINS -> primaryRecord;
      case SECONDARY_WINS -> secondaryRecord;
      case MANUAL -> throw new SyncConflictException(
          "Manual conflict resolution required", primaryRecord, secondaryRecord);
      case CUSTOM -> config.getCustomResolver().resolve(primaryRecord, secondaryRecord);
    };
  }

  private void notifyInsert(T record) {
    listeners.forEach(l -> l.onInsert(record));
  }

  private void notifyUpdate(T record) {
    listeners.forEach(l -> l.onUpdate(record));
  }

  private void notifyDelete(T record) {
    listeners.forEach(l -> l.onDelete(record));
  }

  private void notifyComplete(SyncResult result) {
    listeners.forEach(l -> l.onComplete(result));
  }

  private void notifyError(Exception e) {
    listeners.forEach(l -> l.onError(e));
  }

  /**
   * Sync state enumeration.
   */
  public enum SyncState {
    STOPPED,
    RUNNING,
    SYNCING,
    ERROR
  }
}
