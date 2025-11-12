package io.dataverse.api;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents a database transaction.
 *
 * <p>Transactions provide ACID guarantees for data operations:
 * <ul>
 *   <li><strong>Atomicity</strong>: All operations succeed or all fail</li>
 *   <li><strong>Consistency</strong>: Database constraints are maintained</li>
 *   <li><strong>Isolation</strong>: Concurrent transactions don't interfere</li>
 *   <li><strong>Durability</strong>: Committed changes are permanent</li>
 * </ul>
 *
 * <p><strong>Example Usage:</strong>
 * <pre>{@code
 * // Manual transaction management
 * Transaction tx = repository.beginTransaction();
 * try {
 *     User user = repository.findById("123").orElseThrow();
 *     user.setBalance(user.getBalance() - 100);
 *     repository.save(user);
 *
 *     Account account = accountRepo.findById("456").orElseThrow();
 *     account.setBalance(account.getBalance() + 100);
 *     accountRepo.save(account);
 *
 *     tx.commit();
 * } catch (Exception e) {
 *     tx.rollback();
 *     throw e;
 * }
 *
 * // Automatic transaction management
 * repository.executeInTransaction(repo -> {
 *     User user = repo.findById("123").orElseThrow();
 *     user.setBalance(user.getBalance() - 100);
 *     repo.save(user);
 * });
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface Transaction extends AutoCloseable {

  /**
   * Returns the unique transaction identifier.
   *
   * @return the transaction ID
   */
  String getId();

  /**
   * Returns the current transaction status.
   *
   * @return the transaction status
   */
  TransactionStatus getStatus();

  /**
   * Returns the transaction isolation level.
   *
   * @return the isolation level
   */
  IsolationLevel getIsolationLevel();

  /**
   * Commits the transaction, making all changes permanent.
   *
   * @throws TransactionException if commit fails
   * @throws IllegalStateException if transaction is not active
   */
  void commit() throws TransactionException;

  /**
   * Rolls back the transaction, discarding all changes.
   *
   * @throws TransactionException if rollback fails
   */
  void rollback() throws TransactionException;

  /**
   * Creates a savepoint within the transaction.
   *
   * <p>Savepoints allow partial rollback to a specific point in the transaction.
   *
   * @param name the savepoint name
   * @return the savepoint
   * @throws TransactionException if savepoint creation fails
   */
  Savepoint createSavepoint(String name) throws TransactionException;

  /**
   * Rolls back to the specified savepoint.
   *
   * @param savepoint the savepoint to roll back to
   * @throws TransactionException if rollback fails
   */
  void rollbackTo(Savepoint savepoint) throws TransactionException;

  /**
   * Releases the specified savepoint.
   *
   * @param savepoint the savepoint to release
   * @throws TransactionException if release fails
   */
  void releaseSavepoint(Savepoint savepoint) throws TransactionException;

  /**
   * Checks if the transaction is active.
   *
   * @return true if active, false otherwise
   */
  boolean isActive();

  /**
   * Checks if the transaction is read-only.
   *
   * @return true if read-only, false otherwise
   */
  boolean isReadOnly();

  /**
   * Sets whether the transaction is read-only.
   *
   * <p>Read-only transactions may be optimized by the underlying data source.
   *
   * @param readOnly true for read-only, false for read-write
   */
  void setReadOnly(boolean readOnly);

  /**
   * Sets the transaction timeout in seconds.
   *
   * @param seconds the timeout in seconds
   */
  void setTimeout(int seconds);

  /**
   * Closes the transaction. If the transaction is still active, it will be rolled back.
   */
  @Override
  void close();

  /**
   * Transaction status enumeration.
   */
  enum TransactionStatus {
    /** Transaction is active and operations can be performed */
    ACTIVE,

    /** Transaction has been committed successfully */
    COMMITTED,

    /** Transaction has been rolled back */
    ROLLED_BACK,

    /** Transaction is in an unknown or error state */
    UNKNOWN
  }

  /**
   * Transaction isolation level enumeration.
   */
  enum IsolationLevel {
    /**
     * Allows dirty reads, non-repeatable reads, and phantom reads.
     * Lowest isolation, highest concurrency.
     */
    READ_UNCOMMITTED,

    /**
     * Prevents dirty reads. Allows non-repeatable reads and phantom reads.
     */
    READ_COMMITTED,

    /**
     * Prevents dirty reads and non-repeatable reads. Allows phantom reads.
     */
    REPEATABLE_READ,

    /**
     * Prevents dirty reads, non-repeatable reads, and phantom reads.
     * Highest isolation, lowest concurrency.
     */
    SERIALIZABLE,

    /**
     * Uses the default isolation level of the underlying data source.
     */
    DEFAULT
  }

  /**
   * Represents a savepoint within a transaction.
   */
  interface Savepoint {
    /**
     * Returns the savepoint name.
     *
     * @return the name
     */
    String getName();

    /**
     * Returns the savepoint ID.
     *
     * @return the ID
     */
    String getId();
  }
}
