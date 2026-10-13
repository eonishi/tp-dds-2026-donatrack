package ar.edu.utn.donatrack.donaciones;

import static ar.edu.utn.donatrack.testsupport.FabricaDePrueba.nuevaDonacion;
import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.bienes.Categoria;
import ar.edu.utn.donatrack.bienes.EstadoUso;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.bienes.UnidadMedida;
import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.Genero;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.TipoDocumento;
import ar.edu.utn.donatrack.propuestas.EntidadFueraDeRankingException;
import ar.edu.utn.donatrack.propuestas.PropuestaDeAsignacion;
import ar.edu.utn.donatrack.propuestas.matchmaking.MatchCandidato;
import java.util.List;
import org.junit.jupiter.api.Test;

class PropuestaDeAsignacionTest {

  @Test
  void propuestaCreada_ConPendienteTrue_AndReferenciaDonacionYCandidatos() {
    Donacion donacion = crearDonacionDummy();

    PropuestaDeAsignacion propuesta =
        new PropuestaDeAsignacion(donacion, crearListasDeCandidatosDummy());

    assertTrue(propuesta.isPendiente(), "Propuesta recién creada debe estar pendiente");
    assertSame(donacion, propuesta.getDonacion(), "Propuesta debe referenciar la donación");
    assertEquals(
        List.of(1L),
        idsDe(propuesta),
        "Propuesta debe exponer solo la entidad que aparece en todas las listas");
  }

  @Test
  void candidatos_ConInterseccion_ConservaLasCoincidentesConSuPuntajePromedio() {
    EntidadBeneficiaria primera = entidad(1L, "Uno");
    EntidadBeneficiaria coincidente = entidad(2L, "Dos");
    EntidadBeneficiaria ultima = entidad(3L, "Tres");

    PropuestaDeAsignacion propuesta =
        new PropuestaDeAsignacion(
            crearDonacionDummy(),
            List.of(
                List.of(new MatchCandidato(primera, 10), new MatchCandidato(coincidente, 80)),
                List.of(new MatchCandidato(coincidente, 60), new MatchCandidato(ultima, 90))));

    assertEquals(
        List.of(coincidente),
        propuesta.getCandidatos().stream().map(MatchCandidato::getEntidadBeneficiaria).toList(),
        "Solo la entidad presente en todas las listas debe quedar como candidata");
    assertEquals(
        70,
        propuesta.getCandidatos().get(0).getPuntaje(),
        "El puntaje de la coincidencia debe ser el promedio de sus puntajes");
  }

  @Test
  void candidatos_SinInterseccion_ExponeLaUnionEnOrdenDePrimeraAparicion() {
    EntidadBeneficiaria uno = entidad(1L, "Uno");
    EntidadBeneficiaria dos = entidad(2L, "Dos");
    EntidadBeneficiaria tres = entidad(3L, "Tres");
    EntidadBeneficiaria cuatro = entidad(4L, "Cuatro");

    PropuestaDeAsignacion propuesta =
        new PropuestaDeAsignacion(
            crearDonacionDummy(),
            List.of(
                List.of(new MatchCandidato(uno, 100), new MatchCandidato(dos, 31)),
                List.of(new MatchCandidato(tres, 10), new MatchCandidato(cuatro, 50))));

    assertEquals(
        List.of(1L, 2L, 3L, 4L),
        idsDe(propuesta),
        "Sin coincidencias deben aparecer todas las entidades de ambas listas");
    assertEquals(
        List.of(50, 15, 5, 25),
        propuesta.getCandidatos().stream().map(MatchCandidato::getPuntaje).toList(),
        "Cada puntaje se divide por la cantidad de listas y se trunca a entero");
  }

  @Test
  void candidatos_ConUnaSolaLista_ConservaTodosLosCandidatosConSuPuntaje() {
    EntidadBeneficiaria uno = entidad(1L, "Uno");
    EntidadBeneficiaria dos = entidad(2L, "Dos");

    PropuestaDeAsignacion propuesta =
        new PropuestaDeAsignacion(
            crearDonacionDummy(),
            List.of(List.of(new MatchCandidato(uno, 70), new MatchCandidato(dos, 30))));

    assertEquals(
        List.of(1L, 2L), idsDe(propuesta), "Con una sola lista todos sus candidatos coinciden");
    assertEquals(
        List.of(70, 30),
        propuesta.getCandidatos().stream().map(MatchCandidato::getPuntaje).toList(),
        "Con una sola lista el promedio conserva el puntaje original");
  }

  @Test
  void candidatos_SinEjecucionesDeAlgoritmos_ListaVacia() {
    PropuestaDeAsignacion propuesta = new PropuestaDeAsignacion(crearDonacionDummy(), List.of());

    assertTrue(
        propuesta.getCandidatos().isEmpty(), "Sin ejecuciones no hay candidatos que ofrecer");
  }

  @Test
  void confirmar_MarcaLaPropuestaComoConfirmadaSinTocarSuCopiaDeLaDonacion() {
    Donacion donacion = crearDonacionDummy();
    PropuestaDeAsignacion propuesta =
        new PropuestaDeAsignacion(donacion, crearListasDeCandidatosDummy());
    EntidadBeneficiaria entidad = candidata(propuesta);

    propuesta.confirmar(entidad);

    assertFalse(propuesta.isPendiente(), "confirmar() debe setear pendiente=false");
    assertNull(
        donacion.getEntidadBeneficiaria(),
        "La copia de la donación que guarda la propuesta no debe recibir la transición");
    assertEquals(
        EstadoDonacion.EN_DEPOSITO,
        donacion.getEstado(),
        "La transición la aplica el llamador sobre la donación administrada");
  }

  @Test
  void confirmar_EntidadFueraDelRanking_LanzaExcepcionDeDominio() {
    Donacion donacion = crearDonacionDummy();
    PropuestaDeAsignacion propuesta =
        new PropuestaDeAsignacion(donacion, crearListasDeCandidatosDummy());
    EntidadBeneficiaria entidadNoRankeada = new EntidadBeneficiaria("Otra Entidad", "Dir", "Tel");

    EntidadFueraDeRankingException error =
        assertThrows(
            EntidadFueraDeRankingException.class, () -> propuesta.confirmar(entidadNoRankeada));

    assertEquals("La entidad no pertenece al ranking vigente.", error.getMessage());
    assertTrue(propuesta.isPendiente());
    assertNull(donacion.getEntidadBeneficiaria());
  }

  @Test
  void confirmar_DobleConfirmacion_EsNoOp() {
    Donacion donacion = crearDonacionDummy();
    PropuestaDeAsignacion propuesta =
        new PropuestaDeAsignacion(donacion, crearListasDeCandidatosDummy());
    EntidadBeneficiaria entidad = candidata(propuesta);
    EntidadBeneficiaria otraEntidad = new EntidadBeneficiaria("Otro Hogar", "Dir2", "Tel2");

    // First confirm
    propuesta.confirmar(entidad);
    assertFalse(propuesta.isPendiente());

    // Second confirm — silent no-op: the proposal does not even validate the entity again
    assertDoesNotThrow(() -> propuesta.confirmar(otraEntidad));
    assertFalse(propuesta.isPendiente(), "Debe seguir siendo false");
  }

  // ── Helpers ────────────────────────────────────────────────────────

  private static Donante crearDonanteDummy() {
    return new PersonaHumana(
        new CorreoElectronico("test@test.com"),
        "Juan",
        "Perez",
        30,
        "12345678",
        TipoDocumento.DNI,
        Genero.MASCULINO,
        "Calle Falsa 123");
  }

  private static Donacion crearDonacionDummy() {
    Bien bien =
        new Bien(
            "Test",
            "",
            1.0,
            UnidadMedida.UNIDADES,
            null,
            EstadoUso.NO_APLICA,
            new SubCategoria("X", Categoria.ALIMENTO));
    return nuevaDonacion(crearDonanteDummy(), List.of(bien));
  }

  private static EntidadBeneficiaria entidad(long id, String razonSocial) {
    // La entidad lleva un id explícito: la intersección compara por identidad y este test no
    // persiste la entidad para que el proveedor se la asigne.
    return new EntidadBeneficiaria(id, razonSocial, "Dir", "Tel");
  }

  private static List<List<MatchCandidato>> crearListasDeCandidatosDummy() {
    EntidadBeneficiaria entidad = entidad(1L, "Entidad");
    return List.of(
        List.of(new MatchCandidato(entidad, 50)), List.of(new MatchCandidato(entidad, 80)));
  }

  private static List<Long> idsDe(PropuestaDeAsignacion propuesta) {
    return propuesta.getCandidatos().stream()
        .map(candidato -> candidato.getEntidadBeneficiaria().getId())
        .toList();
  }

  private static EntidadBeneficiaria candidata(PropuestaDeAsignacion propuesta) {
    return propuesta.getCandidatos().get(0).getEntidadBeneficiaria();
  }
}
