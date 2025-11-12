package io.dataverse.spi;

/**
 * Describes the capabilities supported by a data source adapter.
 *
 * <p>Capabilities allow the core framework to query what features an adapter supports,
 * enabling feature detection and graceful degradation.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface AdapterCapabilities {

  /**
   * Checks if the adapter supports transactions.
   *
   * @return {@code true} if transactions are supported
   */
  boolean supportsTransactions();

  /**
   * Checks if the adapter supports batch operations.
   *
   * @return {@code true} if batch operations are supported
   */
  boolean supportsBatchOperations();

  /**
   * Checks if the adapter supports sorting.
   *
   * @return {@code true} if sorting is supported
   */
  boolean supportsSorting();

  /**
   * Checks if the adapter supports pagination.
   *
   * @return {@code true} if pagination is supported
   */
  boolean supportsPagination();

  /**
   * Checks if the adapter supports full-text search.
   *
   * @return {@code true} if full-text search is supported
   */
  boolean supportsFullTextSearch();

  /**
   * Checks if the adapter supports aggregations.
   *
   * @return {@code true} if aggregations are supported
   */
  boolean supportsAggregations();

  /**
   * Checks if the adapter supports secondary indexes.
   *
   * @return {@code true} if secondary indexes are supported
   */
  boolean supportsSecondaryIndexes();

  /**
   * Checks if the adapter supports TTL (time-to-live) for records.
   *
   * @return {@code true} if TTL is supported
   */
  boolean supportsTTL();

  /**
   * Checks if the adapter supports optimistic locking.
   *
   * @return {@code true} if optimistic locking is supported
   */
  boolean supportsOptimisticLocking();

  /**
   * Checks if the adapter supports asynchronous operations.
   *
   * @return {@code true} if async operations are supported
   */
  boolean supportsAsync();

  /**
   * Returns the maximum batch size supported.
   *
   * @return the maximum batch size, or -1 if unlimited
   */
  int getMaxBatchSize();

  /**
   * Returns the maximum number of query conditions supported.
   *
   * @return the maximum query conditions, or -1 if unlimited
   */
  int getMaxQueryConditions();
}
