package io.dataverse.core.hotreload;

import java.nio.file.Path;

/**
 * Listener for reload events.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * manager.addListener(new ReloadListener() {
 *     @Override
 *     public void beforeReload(Path path) {
 *         System.out.println("Reloading: " + path);
 *     }
 *
 *     @Override
 *     public void afterReload(Path path) {
 *         System.out.println("Reloaded: " + path);
 *     }
 *
 *     @Override
 *     public void onReloadError(Path path, Exception error) {
 *         System.err.println("Failed to reload: " + path);
 *     }
 * });
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface ReloadListener {

  /**
   * Called before a reload operation.
   *
   * @param path the path being reloaded
   */
  default void beforeReload(Path path) {
  }

  /**
   * Called after successful reload.
   *
   * @param path the path that was reloaded
   */
  default void afterReload(Path path) {
  }

  /**
   * Called when reload fails.
   *
   * @param path the path that failed to reload
   * @param error the error that occurred
   */
  default void onReloadError(Path path, Exception error) {
  }
}
