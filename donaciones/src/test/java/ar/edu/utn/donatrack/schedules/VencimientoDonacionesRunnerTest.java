package ar.edu.utn.donatrack.schedules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.bienes.Categoria;
import ar.edu.utn.donatrack.bienes.EstadoUso;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.bienes.UnidadMedida;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.EstadoDonacion;
import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.Genero;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.TipoDocumento;
import ar.edu.utn.donatrack.propuestas.PropuestaDeAsignacion;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Corre el runner contra la base en memoria. No usa {@code SimplePersistenceTest} porque su
 * transacción abierta chocaría con el {@code withTransaction} del runner: la siembra confirma sus
 * datos y el {@code @AfterEach} los borra.
 */
class VencimientoDonacionesRunnerTest implements WithSimplePersistenceUnit {

  private Donante donante;
  private SubCategoria subcategoria;

  @Test
  void marcaVencidaLaDonacionEnDepositoVencidaYBorraSuPropuesta() {
    sembrarDonanteYSubcategoria();
    Donacion vencida = donacion(LocalDate.now().minusDays(1));
    Donacion vigente = donacion(LocalDate.now());
    withTransaction(
        () -> {
          persist(vencida);
          persist(vigente);
          persist(new PropuestaDeAsignacion(vencida, List.of()));
          persist(new PropuestaDeAsignacion(vigente, List.of()));
        });

    VencimientoDonacionesRunner.main(new String[0]);

    entityManager().clear();
    assertEquals(EstadoDonacion.VENCIDA, find(Donacion.class, vencida.getId()).getEstado());
    assertEquals(EstadoDonacion.EN_DEPOSITO, find(Donacion.class, vigente.getId()).getEstado());
    assertTrue(propuestasDe(vencida).isEmpty(), "la propuesta de la vencida se borra");
    assertEquals(1, propuestasDe(vigente).size(), "la propuesta de la vigente se mantiene");
  }

  private List<PropuestaDeAsignacion> propuestasDe(Donacion donacion) {
    return createQuery(
            "select p from PropuestaDeAsignacion p where p.donacion.id = :id",
            PropuestaDeAsignacion.class)
        .setParameter("id", donacion.getId())
        .getResultList();
  }

  private void sembrarDonanteYSubcategoria() {
    donante =
        new PersonaHumana(
            new CorreoElectronico("vencimiento@test.com"),
            "Vera",
            "Vencida",
            "10000002",
            TipoDocumento.DNI,
            Genero.FEMENINO,
            "Calle 1");
    subcategoria = new SubCategoria("Vencimiento-Leche", Categoria.ALIMENTO);
    withTransaction(
        () -> {
          persist(donante);
          persist(subcategoria);
        });
  }

  private Donacion donacion(LocalDate fechaVencimiento) {
    Bien bien =
        new Bien(
            "Leche",
            "",
            1.0,
            UnidadMedida.UNIDADES,
            fechaVencimiento,
            EstadoUso.NO_APLICA,
            subcategoria);
    return new Donacion(donante, List.of(bien));
  }

  /** Borra lo que el test confirmó para no contaminar el HSQLDB en memoria compartido. */
  @AfterEach
  void limpiar() {
    if (donante == null) {
      return;
    }
    entityManager().clear();
    withTransaction(
        () -> {
          createQuery(
                  "select p from PropuestaDeAsignacion p where p.donacion.donante.id = :id",
                  PropuestaDeAsignacion.class)
              .setParameter("id", donante.getId())
              .getResultList()
              .forEach(this::remove);
          createQuery("select d from Donacion d where d.donante.id = :id", Donacion.class)
              .setParameter("id", donante.getId())
              .getResultList()
              .forEach(this::remove);
          createQuery("select s from SubCategoria s where s.nombre = :nombre", SubCategoria.class)
              .setParameter("nombre", subcategoria.getNombre())
              .getResultList()
              .forEach(this::remove);
          remove(find(Donante.class, donante.getId()));
        });
  }
}
