package io.dataverse.core.hotreload;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Manages hot reloading of configuration and resources without application restart.
 *
 * <p>HotReloadManager watches files and directories for changes, automatically
 * reloading configurations when modifications are detected.
 *
 * <p><strong>Supported Reloadables:</strong>
 * <ul>
 *   <li>Configuration files (properties, YAML, JSON)</li>
 *   <li>Repository definitions</li>
 *   <li>Query mappings</li>
 *   <li>Cache configurations</li>
 *   <li>Connection pools</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Create hot reload manager
 * HotReloadManager manager = new HotReloadManager();
 *
 * // Register configuration file
 * manager.watch(
 *     Path.of("config/database.properties"),
 *     () -> reloadDatabaseConfig()
 * );
 *
 * // Register directory
 * manager.watchDirectory(
 *     Path.of("config/queries"),
 *     () -> reloadQueryMappings()
 * );
 *
 * // Start watching
 * manager.start();
 *
 * // Configuration changes are automatically detected and reloaded
 *
 * // Stop when shutting down
 * manager.stop();
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class HotReloadManager {

  private static final Logger logger = System.getLogger(HotReloadManager.class.getName());

  private final Map<Path, Reloadable> watchedPaths = new ConcurrentHashMap<>();
  private final List<ReloadListener> listeners = new ArrayList<>();
  private ScheduledExecutorService executor;
  private volatile boolean running = false;

  /**
   * Watches a file for changes.
   *
   * @param path the file path to watch
   * @param reloadAction action to execute on change
   */
  public void watch(Path path, Runnable reloadAction) {
    watch(path, new SimpleReloadable(path, reloadAction));
  }

  /**
   * Watches a file with a reloadable.
   *
   * @param path the file path
   * @param reloadable the reloadable handler
   */
  public void watch(Path path, Reloadable reloadable) {
    watchedPaths.put(path, reloadable);
    logger.log(Level.INFO, "Watching file: " + path);
  }

  /**
   * Watches a directory for changes.
   *
   * @param directory the directory to watch
   * @param reloadAction action to execute on any file change
   */
  public void watchDirectory(Path directory, Runnable reloadAction) {
    watch(directory, new SimpleReloadable(directory, reloadAction));
  }

  /**
   * Unwatches a path.
   *
   * @param path the path to stop watching
   */
  public void unwatch(Path path) {
    watchedPaths.remove(path);
    logger.log(Level.INFO, "Stopped watching: " + path);
  }

  /**
   * Adds a reload listener.
   *
   * @param listener the listener
   */
  public void addListener(ReloadListener listener) {
    listeners.add(listener);
  }

  /**
   * Removes a reload listener.
   *
   * @param listener the listener
   */
  public void removeListener(ReloadListener listener) {
    listeners.remove(listener);
  }

  /**
   * Starts hot reload monitoring.
   */
  public void start() {
    if (running) {
      throw new IllegalStateException("Hot reload already running");
    }

    running = true;
    logger.log(Level.INFO, "Starting hot reload manager");

    // Use virtual thread for file watching
    executor = Executors.newScheduledThreadPool(
        1,
        Thread.ofVirtual().name("hotreload-watcher").factory()
    );

    executor.scheduleAtFixedRate(
        this::checkForChanges,
        0,
        1,
        TimeUnit.SECONDS
    );
  }

  /**
   * Stops hot reload monitoring.
   */
  public void stop() {
    if (!running) {
      return;
    }

    running = false;
    logger.log(Level.INFO, "Stopping hot reload manager");

    if (executor != null && !executor.isShutdown()) {
      executor.shutdown();
      try {
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
          executor.shutdownNow();
        }
      } catch (InterruptedException e) {
        executor.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Checks if hot reload is running.
   *
   * @return true if running
   */
  public boolean isRunning() {
    return running;
  }

  /**
   * Manually triggers reload for a specific path.
   *
   * @param path the path to reload
   */
  public void reload(Path path) {
    Reloadable reloadable = watchedPaths.get(path);
    if (reloadable != null) {
      performReload(reloadable, path);
    } else {
      logger.log(Level.WARNING, "No reloadable found for path: " + path);
    }
  }

  /**
   * Manually triggers reload for all watched paths.
   */
  public void reloadAll() {
    logger.log(Level.INFO, "Reloading all configurations");

    for (Map.Entry<Path, Reloadable> entry : watchedPaths.entrySet()) {
      performReload(entry.getValue(), entry.getKey());
    }
  }

  /**
   * Gets the number of watched paths.
   *
   * @return watched path count
   */
  public int getWatchedPathCount() {
    return watchedPaths.size();
  }

  /**
   * Checks for file changes.
   */
  private void checkForChanges() {
    try {
      for (Map.Entry<Path, Reloadable> entry : watchedPaths.entrySet()) {
        Path path = entry.getKey();
        Reloadable reloadable = entry.getValue();

        if (hasChanged(path, reloadable)) {
          logger.log(Level.INFO, "Detected change in: " + path);
          performReload(reloadable, path);
        }
      }
    } catch (Exception e) {
      logger.log(Level.ERROR, "Error checking for changes", e);
    }
  }

  /**
   * Checks if a path has changed since last check.
   */
  private boolean hasChanged(Path path, Reloadable reloadable) {
    try {
      if (!path.toFile().exists()) {
        return false;
      }

      long lastModified = path.toFile().lastModified();
      long previousModified = reloadable.getLastModified().toEpochMilli();

      return lastModified > previousModified;

    } catch (Exception e) {
      logger.log(Level.DEBUG, "Error checking file modification: " + path, e);
      return false;
    }
  }

  /**
   * Performs reload for a reloadable.
   */
  private void performReload(Reloadable reloadable, Path path) {
    try {
      notifyBeforeReload(path);

      reloadable.reload();
      reloadable.updateLastModified();

      notifyAfterReload(path);

      logger.log(Level.INFO, "Successfully reloaded: " + path);

    } catch (Exception e) {
      logger.log(Level.ERROR, "Failed to reload: " + path, e);
      notifyReloadError(path, e);
    }
  }

  private void notifyBeforeReload(Path path) {
    listeners.forEach(l -> l.beforeReload(path));
  }

  private void notifyAfterReload(Path path) {
    listeners.forEach(l -> l.afterReload(path));
  }

  private void notifyReloadError(Path path, Exception error) {
    listeners.forEach(l -> l.onReloadError(path, error));
  }

  /**
   * Simple reloadable implementation.
   */
  private static class SimpleReloadable implements Reloadable {
    private final Path path;
    private final Runnable action;
    private volatile Instant lastModified = Instant.ofEpochMilli(0);

    SimpleReloadable(Path path, Runnable action) {
      this.path = path;
      this.action = action;
    }

    @Override
    public void reload() {
      action.run();
    }

    @Override
    public Instant getLastModified() {
      return lastModified;
    }

    @Override
    public void updateLastModified() {
      try {
        lastModified = Instant.ofEpochMilli(path.toFile().lastModified());
      } catch (Exception e) {
        lastModified = Instant.now();
      }
    }
  }
}
