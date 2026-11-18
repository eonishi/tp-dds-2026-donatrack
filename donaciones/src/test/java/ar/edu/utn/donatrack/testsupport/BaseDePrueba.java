package ar.edu.utn.donatrack.testsupport;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.beneficiarios.Necesidad;
import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.notificaciones.AvisoBandeja;
import ar.edu.utn.donatrack.propuestas.PropuestaDeAsignacion;
import ar.edu.utn.donatrack.security.Usuario;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.util.List;

/**
 * Vacía la base entre pruebas HTTP. Cada request confirma su propia transacción, así que lo que
 * crea una prueba queda guardado y contaminaría a la siguiente (conteos, documentos o correos
 * repetidos, la restricción única del catálogo).
 *
 * <p>Una sola lista, ordenada de hijo a padre para no chocar con ninguna clave foránea: propuestas,
 * avisos, bienes y necesidades apuntan a donaciones, entidades y subcategorías; las donaciones
 * apuntan a donantes y entidades. Las cuentas fijas del sistema ({@code @donatrack.local}) se
 * conservan porque las pruebas inician sesión con ellas.
 */
public final class BaseDePrueba implements WithSimplePersistenceUnit {
  private static final List<String> EN_ORDEN =
      List.of(
          PropuestaDeAsignacion.class.getSimpleName(),
          AvisoBandeja.class.getSimpleName(),
          Bien.class.getSimpleName(),
          Necesidad.class.getSimpleName(),
          Donacion.class.getSimpleName(),
          EntidadBeneficiaria.class.getSimpleName(),
          Donante.class.getSimpleName(),
          SubCategoria.class.getSimpleName());

  private BaseDePrueba() {}

  public static void vaciar() {
    new BaseDePrueba().borrarTodo();
  }

  private void borrarTodo() {
    entityManager().clear();
    withTransaction(
        () -> {
          // Se borra entidad por entidad (no con DELETE masivo) para que JPA aplique las cascadas
          // a las colecciones de cada una (medios de contacto, historial, fotos).
          EN_ORDEN.forEach(
              entidad ->
                  createQuery("select x from " + entidad + " x", Object.class)
                      .getResultList()
                      .forEach(this::remove));
          createQuery("select u from Usuario u where u.email not like :fijas", Usuario.class)
              .setParameter("fijas", "%@donatrack.local")
              .getResultList()
              .forEach(this::remove);
        });
    entityManager().clear();
  }
}
