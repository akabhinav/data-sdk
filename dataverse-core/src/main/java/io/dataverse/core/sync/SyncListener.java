package io.dataverse.core.sync;

/**
 * Listener for synchronization events.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * syncManager.addListener(new SyncListener<User>() {
 *     @Override
 *     public void onInsert(User record) {
 *         System.out.println("Inserted: " + record.getId());
 *     }
 *
 *     @Override
 *     public void onUpdate(User record) {
 *         System.out.println("Updated: " + record.getId());
 *     }
 *
 *     @Override
 *     public void onComplete(SyncResult result) {
 *         System.out.println("Synced: " + result.getRecordsSynced());
 *     }
 * });
 * }</pre>
 *
 * @param <T> entity type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface SyncListener<T> {

  /**
   * Called when a record is inserted.
   *
   * @param record the inserted record
   */
  default void onInsert(T record) {
  }

  /**
   * Called when a record is updated.
   *
   * @param record the updated record
   */
  default void onUpdate(T record) {
  }

  /**
   * Called when a record is deleted.
   *
   * @param record the deleted record
   */
  default void onDelete(T record) {
  }

  /**
   * Called when sync completes successfully.
   *
   * @param result the sync result
   */
  default void onComplete(SyncResult result) {
  }

  /**
   * Called when sync encounters an error.
   *
   * @param error the error
   */
  default void onError(Exception error) {
  }
}
