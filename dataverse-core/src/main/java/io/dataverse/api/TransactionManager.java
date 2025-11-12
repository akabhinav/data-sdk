package io.dataverse.api;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Manager for database transactions.
 *
 * <p>TransactionManager provides methods to begin, manage, and execute transactional operations.
 * It supports both manual and automatic transaction management patterns.
 *
 * <p><strong>Example Usage:</strong>
 * <pre>{@code
 * TransactionManager txManager = adapter.getTransactionManager();
 *
 * // Manual transaction management
 * Transaction tx = txManager.begin();
 * try {
 *     // Perform operations
 *     tx.commit();
 * } catch (Exception e) {
 *     tx.rollback();
 * }
 *
 * // Automatic transaction management with consumer
 * txManager.executeInTransaction(repo -> {
 *     User user = repo.findById("123").orElseThrow();
 *     user.setBalance(user.getBalance() + 100);
 *     repo.save(user);
 * });
 *
 * // Automatic transaction management with function (returns result)
 * User updatedUser = txManager.executeInTransaction(repo -> {
 *     User user = repo.findById("123").orElseThrow();
 *     user.setBalance(user.getBalance() + 100);
 *     return repo.save(user);
 * });
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface TransactionManager {

  /**
   * Begins a new transaction with default isolation level.
   *
   * @return the new transaction
   * @throws TransactionException if transaction creation fails
   */
  Transaction begin() throws TransactionException;

  /**
   * Begins a new transaction with the specified isolation level.
   *
   * @param isolationLevel the desired isolation level
   * @return the new transaction
   * @throws TransactionException if transaction creation fails
   */
  Transaction begin(Transaction.IsolationLevel isolationLevel) throws TransactionException;

  /**
   * Returns the current active transaction, if any.
   *
   * @return the current transaction, or null if none active
   */
  Transaction getCurrentTransaction();

  /**
   * Checks if a transaction is currently active.
   *
   * @return true if a transaction is active, false otherwise
   */
  boolean isTransactionActive();

  /**
   * Executes the given operation within a transaction (consumer pattern).
   *
   * <p>The transaction is automatically committed if the operation succeeds,
   * or rolled back if an exception occurs.
   *
   * @param operation the operation to execute
   * @param <T> the entity type
   * @param <ID> the ID type
   * @throws TransactionException if the transaction fails
   */
  <T extends Entity<ID>, ID extends java.io.Serializable> void executeInTransaction(
      Consumer<Repository<T, ID>> operation) throws TransactionException;

  /**
   * Executes the given operation within a transaction (function pattern).
   *
   * <p>The transaction is automatically committed if the operation succeeds,
   * or rolled back if an exception occurs.
   *
   * @param operation the operation to execute
   * @param <T> the entity type
   * @param <ID> the ID type
   * @param <R> the result type
   * @return the result of the operation
   * @throws TransactionException if the transaction fails
   */
  <T extends Entity<ID>, ID extends java.io.Serializable, R> R executeInTransaction(
      Function<Repository<T, ID>, R> operation) throws TransactionException;

  /**
   * Executes a runnable operation within a transaction.
   *
   * @param operation the operation to execute
   * @throws TransactionException if the transaction fails
   */
  void executeInTransaction(Runnable operation) throws TransactionException;

  /**
   * Asynchronously executes an operation within a transaction.
   *
   * @param operation the operation to execute
   * @param <T> the entity type
   * @param <ID> the ID type
   * @return a CompletableFuture that completes when the transaction finishes
   */
  <T extends Entity<ID>, ID extends java.io.Serializable>
      CompletableFuture<Void> executeInTransactionAsync(Consumer<Repository<T, ID>> operation);

  /**
   * Asynchronously executes an operation within a transaction and returns a result.
   *
   * @param operation the operation to execute
   * @param <T> the entity type
   * @param <ID> the ID type
   * @param <R> the result type
   * @return a CompletableFuture that completes with the result
   */
  <T extends Entity<ID>, ID extends java.io.Serializable, R>
      CompletableFuture<R> executeInTransactionAsync(Function<Repository<T, ID>, R> operation);

  /**
   * Suspends the current transaction.
   *
   * <p>The transaction can be resumed later using {@link #resumeTransaction(Transaction)}.
   *
   * @return the suspended transaction
   * @throws TransactionException if suspension fails
   */
  Transaction suspendTransaction() throws TransactionException;

  /**
   * Resumes a previously suspended transaction.
   *
   * @param transaction the transaction to resume
   * @throws TransactionException if resumption fails
   */
  void resumeTransaction(Transaction transaction) throws TransactionException;

  /**
   * Sets the default transaction timeout in seconds.
   *
   * @param seconds the timeout in seconds
   */
  void setDefaultTimeout(int seconds);

  /**
   * Sets the default isolation level for new transactions.
   *
   * @param isolationLevel the default isolation level
   */
  void setDefaultIsolationLevel(Transaction.IsolationLevel isolationLevel);
}
