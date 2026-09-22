package ar.edu.utn.donatrack.persistencia;

import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.util.function.Supplier;

/**
 * Base para repositorios respaldados por JPA.
 *
 * <p>{@code TransactionalOps#withTransaction} confirma la transacción que esté activa, así que un
 * método de repositorio que se envolviera ciegamente en una transacción confirmaría la transacción
 * del llamador. Eso rompe el rollback por test de {@code
 * io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest} y desasocia silenciosamente
 * entidades que el llamador todavía pretende usar. Estos métodos auxiliares se unen a la
 * transacción activa cuando existe y abren la propia solo cuando no hay ninguna.
 */
public interface JpaRepository extends WithSimplePersistenceUnit {

  /**
   * Ejecuta una acción en una transacción, uniéndose a la transacción activa del llamador cuando
   * existe.
   */
  default void inTransaction(Runnable action) {
    if (getTransaction().isActive()) {
      action.run();
      return;
    }
    withTransaction(action);
  }

  /**
   * Ejecuta un proveedor en una transacción, uniéndose a la transacción activa del llamador cuando
   * existe.
   */
  default <T> T inTransaction(Supplier<T> action) {
    if (getTransaction().isActive()) {
      return action.get();
    }
    return withTransaction(action);
  }
}
