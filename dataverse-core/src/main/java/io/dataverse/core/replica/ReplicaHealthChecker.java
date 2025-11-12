package io.dataverse.core.replica;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Periodic health checker for read replicas.
 *
 * <p>Runs background health checks on replicas at configured intervals,
 * automatically marking replicas as healthy or unhealthy based on check results.
 *
 * <p><strong>Health Check Process:</strong>
 * <ol>
 *   <li>Execute connection test (e.g., SELECT 1)</li>
 *   <li>Check replication lag (if supported)</li>
 *   <li>Update replica health status</li>
 *   <li>Log any failures</li>
 * </ol>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * ReplicaRouter router = new ReplicaRouter(primaryUrl);
 * router.addReplica("replica1.example.com:5432");
 * router.addReplica("replica2.example.com:5432");
 *
 * // Start health checking
 * ReplicaHealthChecker healthChecker = new ReplicaHealthChecker(
 *     router,
 *     (replicaUrl) -> {
 *         try (Connection conn = DriverManager.getConnection(replicaUrl)) {
 *             return conn.isValid(5);
 *         } catch (SQLException e) {
 *             return false;
 *         }
 *     },
 *     Duration.ofSeconds(30)
 * );
 * healthChecker.start();
 *
 * // Stop when shutting down
 * healthChecker.stop();
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class ReplicaHealthChecker {

  private static final Logger logger = System.getLogger(ReplicaHealthChecker.class.getName());

  private final ReplicaRouter router;
  private final Function<String, Boolean> healthCheckFunction;
  private final Duration checkInterval;
  private ScheduledExecutorService scheduler;
  private volatile boolean running = false;

  /**
   * Creates a new health checker.
   *
   * @param router the replica router to check
   * @param healthCheckFunction function that checks if a replica URL is healthy
   * @param checkInterval how often to check replica health
   */
  public ReplicaHealthChecker(
      ReplicaRouter router,
      Function<String, Boolean> healthCheckFunction,
      Duration checkInterval) {
    this.router = router;
    this.healthCheckFunction = healthCheckFunction;
    this.checkInterval = checkInterval;
  }

  /**
   * Starts periodic health checking.
   */
  public void start() {
    if (running) {
      logger.log(Level.WARNING, "Health checker already running");
      return;
    }

    logger.log(Level.INFO, "Starting replica health checker with interval: " + checkInterval);

    // Use virtual thread executor for efficient concurrent health checks
    scheduler = Executors.newScheduledThreadPool(
        1,
        Thread.ofVirtual().name("replica-health-checker-", 0).factory()
    );

    scheduler.scheduleAtFixedRate(
        this::performHealthChecks,
        0,  // Initial delay
        checkInterval.toSeconds(),
        TimeUnit.SECONDS
    );

    running = true;
  }

  /**
   * Stops health checking.
   */
  public void stop() {
    if (!running) {
      return;
    }

    logger.log(Level.INFO, "Stopping replica health checker");

    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdown();
      try {
        if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
          scheduler.shutdownNow();
        }
      } catch (InterruptedException e) {
        scheduler.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }

    running = false;
  }

  /**
   * Checks if the health checker is running.
   *
   * @return true if running
   */
  public boolean isRunning() {
    return running;
  }

  /**
   * Performs health checks on all replicas.
   */
  private void performHealthChecks() {
    try {
      int totalReplicas = router.getReplicaCount();
      if (totalReplicas == 0) {
        return;
      }

      logger.log(Level.DEBUG, "Performing health checks on " + totalReplicas + " replicas");

      // Get all replica URLs and check each one
      for (int i = 0; i < totalReplicas; i++) {
        ReplicaRouter.ReplicaInfo replica = router.getReplicas().get(i);
        String replicaUrl = replica.getUrl();

        try {
          boolean healthy = healthCheckFunction.apply(replicaUrl);

          if (healthy && !replica.isHealthy()) {
            logger.log(Level.INFO, "Replica " + replicaUrl + " is now healthy");
            router.markHealthy(replicaUrl);
          } else if (!healthy && replica.isHealthy()) {
            logger.log(Level.WARNING, "Replica " + replicaUrl + " is now unhealthy");
            router.markUnhealthy(replicaUrl);
          }
        } catch (Exception e) {
          logger.log(Level.ERROR, "Health check failed for replica " + replicaUrl + ": " + e.getMessage());
          router.markUnhealthy(replicaUrl);
        }
      }

      long healthyCount = router.getHealthyReplicaCount();
      logger.log(Level.DEBUG, "Health check complete: " + healthyCount + "/" + totalReplicas + " replicas healthy");

    } catch (Exception e) {
      logger.log(Level.ERROR, "Error during health check cycle", e);
    }
  }
}
