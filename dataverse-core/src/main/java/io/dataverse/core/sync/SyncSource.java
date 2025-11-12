package io.dataverse.core.sync;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Interface for sync data sources.
 *
 * <p>Implementations provide access to records for synchronization,
 * including change tracking and CRUD operations.
 *
 * @param <T> entity type
 * @param <ID> identifier type
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface SyncSource<T, ID> {

  /**
   * Gets all records.
   *
   * @return all records
   */
  List<T> getAllRecords();

  /**
   * Gets records modified since timestamp.
   *
   * @param since timestamp
   * @return modified records
   */
  List<T> getModifiedSince(Instant since);

  /**
   * Extracts the ID from a record.
   *
   * @param record the record
   * @return the ID
   */
  ID extractId(T record);

  /**
   * Gets the last modified timestamp of a record.
   *
   * @param record the record
   * @return last modified time
   */
  Instant getLastModified(T record);

  /**
   * Checks if first record is newer than second.
   *
   * @param first first record
   * @param second second record
   * @return true if first is newer
   */
  boolean isNewer(T first, T second);

  /**
   * Checks if two records are equal (content-wise).
   *
   * @param first first record
   * @param second second record
   * @return true if equal
   */
  boolean equals(T first, T second);

  /**
   * Inserts a new record.
   *
   * @param record the record to insert
   */
  void insert(T record);

  /**
   * Updates an existing record.
   *
   * @param record the record to update
   */
  void update(T record);

  /**
   * Deletes a record by ID.
   *
   * @param id the record ID
   */
  void delete(ID id);

  /**
   * Creates an index of records by ID.
   *
   * @param records the records
   * @return map of ID to record
   */
  Map<ID, T> createIndex(List<T> records);
}
