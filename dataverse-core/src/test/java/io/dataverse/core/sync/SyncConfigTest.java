package io.dataverse.core.sync;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for SyncConfig.
 *
 * @author DataVerse SDK Team
 */
class SyncConfigTest {

  @Test
  void testDefaultConfig() {
    SyncConfig config = SyncConfig.builder().build();

    assertEquals(SyncMode.ONE_WAY, config.getSyncMode());
    assertEquals(ConflictResolution.LAST_WRITE_WINS, config.getConflictResolution());
    assertEquals(Duration.ofMinutes(5), config.getSyncInterval());
    assertEquals(100, config.getBatchSize());
    assertTrue(config.isContinueOnError());
  }

  @Test
  void testCustomConfig() {
    SyncConfig config = SyncConfig.builder()
        .syncMode(SyncMode.TWO_WAY)
        .conflictResolution(ConflictResolution.PRIMARY_WINS)
        .syncInterval(Duration.ofMinutes(10))
        .batchSize(500)
        .continueOnError(false)
        .build();

    assertEquals(SyncMode.TWO_WAY, config.getSyncMode());
    assertEquals(ConflictResolution.PRIMARY_WINS, config.getConflictResolution());
    assertEquals(Duration.ofMinutes(10), config.getSyncInterval());
    assertEquals(500, config.getBatchSize());
    assertFalse(config.isContinueOnError());
  }

  @Test
  void testAllSyncModes() {
    SyncConfig oneWay = SyncConfig.builder()
        .syncMode(SyncMode.ONE_WAY)
        .build();
    assertEquals(SyncMode.ONE_WAY, oneWay.getSyncMode());

    SyncConfig twoWay = SyncConfig.builder()
        .syncMode(SyncMode.TWO_WAY)
        .build();
    assertEquals(SyncMode.TWO_WAY, twoWay.getSyncMode());

    SyncConfig masterSlave = SyncConfig.builder()
        .syncMode(SyncMode.MASTER_SLAVE)
        .build();
    assertEquals(SyncMode.MASTER_SLAVE, masterSlave.getSyncMode());
  }

  @Test
  void testAllConflictResolutions() {
    assertEquals(ConflictResolution.LAST_WRITE_WINS,
        SyncConfig.builder().conflictResolution(ConflictResolution.LAST_WRITE_WINS)
            .build().getConflictResolution());

    assertEquals(ConflictResolution.PRIMARY_WINS,
        SyncConfig.builder().conflictResolution(ConflictResolution.PRIMARY_WINS)
            .build().getConflictResolution());

    assertEquals(ConflictResolution.SECONDARY_WINS,
        SyncConfig.builder().conflictResolution(ConflictResolution.SECONDARY_WINS)
            .build().getConflictResolution());

    assertEquals(ConflictResolution.MANUAL,
        SyncConfig.builder().conflictResolution(ConflictResolution.MANUAL)
            .build().getConflictResolution());

    assertEquals(ConflictResolution.CUSTOM,
        SyncConfig.builder().conflictResolution(ConflictResolution.CUSTOM)
            .build().getConflictResolution());
  }

  @Test
  void testCustomResolver() {
    SyncConfig config = SyncConfig.builder()
        .conflictResolution(ConflictResolution.CUSTOM)
        .<String>customResolver((a, b) -> a + b)
        .build();

    String result = config.<String>getCustomResolver().apply("Hello", "World");
    assertEquals("HelloWorld", result);
  }
}
