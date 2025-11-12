package io.dataverse.core.sync;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for SyncResult.
 *
 * @author DataVerse SDK Team
 */
class SyncResultTest {

  @Test
  void testBasicResult() {
    SyncResult result = SyncResult.builder()
        .recordsSynced(100)
        .conflictsResolved(5)
        .recordsDeleted(10)
        .build();

    assertEquals(100, result.getRecordsSynced());
    assertEquals(5, result.getConflictsResolved());
    assertEquals(10, result.getRecordsDeleted());
    assertNotNull(result.getStartTime());
    assertNotNull(result.getEndTime());
    assertNotNull(result.getDuration());
  }

  @Test
  void testResultWithTimestamps() {
    Instant start = Instant.now();
    Instant end = start.plusSeconds(10);

    SyncResult result = SyncResult.builder()
        .recordsSynced(50)
        .startTime(start)
        .endTime(end)
        .build();

    assertEquals(start, result.getStartTime());
    assertEquals(end, result.getEndTime());
    assertEquals(Duration.ofSeconds(10), result.getDuration());
  }

  @Test
  void testResultWithDuration() {
    Duration duration = Duration.ofMinutes(2);

    SyncResult result = SyncResult.builder()
        .recordsSynced(200)
        .duration(duration)
        .build();

    assertEquals(duration, result.getDuration());
  }

  @Test
  void testToString() {
    SyncResult result = SyncResult.builder()
        .recordsSynced(100)
        .conflictsResolved(5)
        .recordsDeleted(10)
        .build();

    String str = result.toString();

    assertTrue(str.contains("100"));
    assertTrue(str.contains("5"));
    assertTrue(str.contains("10"));
    assertTrue(str.contains("ms"));
  }

  @Test
  void testZeroValues() {
    SyncResult result = SyncResult.builder()
        .recordsSynced(0)
        .conflictsResolved(0)
        .recordsDeleted(0)
        .build();

    assertEquals(0, result.getRecordsSynced());
    assertEquals(0, result.getConflictsResolved());
    assertEquals(0, result.getRecordsDeleted());
  }
}
