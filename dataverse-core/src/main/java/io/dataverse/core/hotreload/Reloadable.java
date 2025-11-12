package io.dataverse.core.hotreload;

import java.time.Instant;

/**
 * Interface for reloadable resources.
 *
 * <p>Implementations define how to reload their state when files change.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface Reloadable {

  /**
   * Reloads the resource.
   *
   * @throws Exception if reload fails
   */
  void reload() throws Exception;

  /**
   * Gets the last modified timestamp.
   *
   * @return last modified time
   */
  Instant getLastModified();

  /**
   * Updates the last modified timestamp to current time.
   */
  void updateLastModified();
}
