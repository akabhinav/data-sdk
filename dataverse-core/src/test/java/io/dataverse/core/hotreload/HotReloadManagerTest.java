package io.dataverse.core.hotreload;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for HotReloadManager.
 *
 * @author DataVerse SDK Team
 */
class HotReloadManagerTest {

  @TempDir
  Path tempDir;

  private HotReloadManager manager;

  @BeforeEach
  void setUp() {
    manager = new HotReloadManager();
  }

  @AfterEach
  void tearDown() {
    if (manager.isRunning()) {
      manager.stop();
    }
  }

  @Test
  void testWatch() throws IOException {
    Path file = tempDir.resolve("config.properties");
    Files.writeString(file, "test=value");

    AtomicInteger reloadCount = new AtomicInteger(0);
    manager.watch(file, reloadCount::incrementAndGet);

    assertEquals(1, manager.getWatchedPathCount());
  }

  @Test
  void testUnwatch() throws IOException {
    Path file = tempDir.resolve("config.properties");
    Files.writeString(file, "test=value");

    manager.watch(file, () -> {});
    assertEquals(1, manager.getWatchedPathCount());

    manager.unwatch(file);
    assertEquals(0, manager.getWatchedPathCount());
  }

  @Test
  void testStartStop() {
    assertFalse(manager.isRunning());

    manager.start();
    assertTrue(manager.isRunning());

    manager.stop();
    assertFalse(manager.isRunning());
  }

  @Test
  void testStartTwice_ThrowsException() {
    manager.start();

    assertThrows(IllegalStateException.class, () -> manager.start());
  }

  @Test
  void testManualReload() throws IOException {
    Path file = tempDir.resolve("config.properties");
    Files.writeString(file, "test=value");

    AtomicInteger reloadCount = new AtomicInteger(0);
    manager.watch(file, reloadCount::incrementAndGet);

    manager.reload(file);

    assertEquals(1, reloadCount.get());
  }

  @Test
  void testReloadAll() throws IOException {
    Path file1 = tempDir.resolve("config1.properties");
    Path file2 = tempDir.resolve("config2.properties");
    Files.writeString(file1, "test1=value1");
    Files.writeString(file2, "test2=value2");

    AtomicInteger reloadCount = new AtomicInteger(0);
    manager.watch(file1, reloadCount::incrementAndGet);
    manager.watch(file2, reloadCount::incrementAndGet);

    manager.reloadAll();

    assertEquals(2, reloadCount.get());
  }

  @Test
  void testReloadListener() throws IOException {
    Path file = tempDir.resolve("config.properties");
    Files.writeString(file, "test=value");

    AtomicInteger beforeCount = new AtomicInteger(0);
    AtomicInteger afterCount = new AtomicInteger(0);

    manager.addListener(new ReloadListener() {
      @Override
      public void beforeReload(Path path) {
        beforeCount.incrementAndGet();
      }

      @Override
      public void afterReload(Path path) {
        afterCount.incrementAndGet();
      }
    });

    manager.watch(file, () -> {});
    manager.reload(file);

    assertEquals(1, beforeCount.get());
    assertEquals(1, afterCount.get());
  }

  @Test
  void testReloadError() throws IOException {
    Path file = tempDir.resolve("config.properties");
    Files.writeString(file, "test=value");

    AtomicInteger errorCount = new AtomicInteger(0);

    manager.addListener(new ReloadListener() {
      @Override
      public void onReloadError(Path path, Exception error) {
        errorCount.incrementAndGet();
      }
    });

    manager.watch(file, () -> {
      throw new RuntimeException("Test error");
    });

    manager.reload(file);

    assertEquals(1, errorCount.get());
  }

  @Test
  void testRemoveListener() throws IOException {
    Path file = tempDir.resolve("config.properties");
    Files.writeString(file, "test=value");

    AtomicInteger count = new AtomicInteger(0);

    ReloadListener listener = new ReloadListener() {
      @Override
      public void afterReload(Path path) {
        count.incrementAndGet();
      }
    };

    manager.addListener(listener);
    manager.watch(file, () -> {});
    manager.reload(file);

    assertEquals(1, count.get());

    manager.removeListener(listener);
    manager.reload(file);

    // Still 1 - listener was removed
    assertEquals(1, count.get());
  }

  @Test
  void testReloadNonExistentPath() {
    Path nonExistent = tempDir.resolve("nonexistent.properties");

    // Should not throw exception
    assertDoesNotThrow(() -> manager.reload(nonExistent));
  }

  @Test
  void testCustomReloadable() throws IOException {
    Path file = tempDir.resolve("config.properties");
    Files.writeString(file, "test=value");

    AtomicInteger reloadCount = new AtomicInteger(0);

    Reloadable reloadable = new Reloadable() {
      private Instant lastModified = Instant.ofEpochMilli(0);

      @Override
      public void reload() {
        reloadCount.incrementAndGet();
      }

      @Override
      public Instant getLastModified() {
        return lastModified;
      }

      @Override
      public void updateLastModified() {
        lastModified = Instant.now();
      }
    };

    manager.watch(file, reloadable);
    manager.reload(file);

    assertEquals(1, reloadCount.get());
  }

  @Test
  void testWatchDirectory() {
    Path directory = tempDir;

    AtomicInteger reloadCount = new AtomicInteger(0);
    manager.watchDirectory(directory, reloadCount::incrementAndGet);

    assertEquals(1, manager.getWatchedPathCount());

    manager.reload(directory);
    assertEquals(1, reloadCount.get());
  }
}
