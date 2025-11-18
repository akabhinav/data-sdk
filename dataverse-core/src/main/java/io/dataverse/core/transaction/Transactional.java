package io.dataverse.core.transaction;

import io.dataverse.core.transaction.TransactionManager.*;

import java.lang.annotation.*;

/**
 * Declarative transaction management annotation.
 *
 * <p>Feature #41: Declarative Transactions - @Transactional annotation support
 * <p>Feature #45: Transaction Callbacks - Before/after commit hooks and rollback handlers
 *
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Transactional {

    /**
     * Transaction propagation behavior.
     */
    PropagationBehavior propagation() default PropagationBehavior.REQUIRED;

    /**
     * Transaction isolation level.
     */
    IsolationLevel isolation() default IsolationLevel.READ_COMMITTED;

    /**
     * Timeout in seconds (0 = no timeout).
     */
    int timeout() default 0;

    /**
     * Whether the transaction is read-only.
     */
    boolean readOnly() default false;

    /**
     * Exception classes that trigger rollback.
     */
    Class<? extends Throwable>[] rollbackFor() default {};

    /**
     * Exception class names that trigger rollback.
     */
    String[] rollbackForClassName() default {};

    /**
     * Exception classes that don't trigger rollback.
     */
    Class<? extends Throwable>[] noRollbackFor() default {};

    /**
     * Exception class names that don't trigger rollback.
     */
    String[] noRollbackForClassName() default {};

    /**
     * Transaction manager bean name.
     */
    String transactionManager() default "";

    /**
     * Label for transaction metrics.
     */
    String label() default "";
}
