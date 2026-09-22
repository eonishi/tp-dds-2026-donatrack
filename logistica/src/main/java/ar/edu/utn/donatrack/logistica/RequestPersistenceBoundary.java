package ar.edu.utn.donatrack.logistica;

import ar.edu.utn.donatrack.persistencia.JpaRepository;
import io.javalin.Javalin;

/**
 * Abre una transacción al comenzar cada solicitud HTTP y la cierra cuando la solicitud termina.
 *
 * <p>Vive en la raíz de composición ({@link MainLogistica}) porque es el único lugar que ve el
 * ciclo de vida HTTP completo: un {@code before} que la abre y un {@code after} que la confirma o
 * la revierte. Los repositorios JPA del módulo comparten este mismo contexto de persistencia por
 * hilo, así que sus métodos ({@link JpaRepository#inTransaction}) se unen a la transacción abierta
 * en lugar de confirmarla por su cuenta; esa es exactamente la semántica que necesita una unidad de
 * trabajo por solicitud.
 *
 * <p>El estado de la respuesta decide el cierre: una solicitud contestada con 4xx o 5xx revierte
 * (los mapeos de errores de la API ya corrieron antes de este punto) y cualquier otra confirma,
 * vaciando entonces los cambios acumulados. Después se limpia el contexto de persistencia para que
 * una solicitud no herede instancias administradas de la anterior.
 */
final class RequestPersistenceBoundary implements JpaRepository {

  private RequestPersistenceBoundary() {}

  static void register(Javalin app) {
    RequestPersistenceBoundary boundary = new RequestPersistenceBoundary();
    app.before(ctx -> boundary.beginTransaction());
    app.after(
        ctx -> {
          try {
            if (ctx.statusCode() >= 400) {
              boundary.rollbackTransaction();
            } else {
              boundary.commitTransaction();
            }
          } finally {
            boundary.entityManager().clear();
          }
        });
  }
}
