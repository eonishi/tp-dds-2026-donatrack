package ar.edu.utn.donatrack.persistencia;

import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.util.function.Supplier;

/**
 * JPA-backed repository base.
 *
 * <p>{@code TransactionalOps#withTransaction} commits whatever transaction is active, so a
 * repository method that blindly wrapped itself in a transaction would commit the caller's
 * transaction. That breaks the per-test rollback of {@code
 * io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest} and silently detaches entities the
 * caller still intends to use. These helpers join the active transaction when there is one and open
 * their own only when there is none.
 */
public interface JpaRepository extends WithSimplePersistenceUnit {

  /** Runs an action in a transaction, joining the caller's active transaction when there is one. */
  default void inTransaction(Runnable action) {
    if (getTransaction().isActive()) {
      action.run();
      return;
    }
    withTransaction(action);
  }

  /**
   * Runs a supplier in a transaction, joining the caller's active transaction when there is one.
   */
  default <T> T inTransaction(Supplier<T> action) {
    if (getTransaction().isActive()) {
      return action.get();
    }
    return withTransaction(action);
  }
}
