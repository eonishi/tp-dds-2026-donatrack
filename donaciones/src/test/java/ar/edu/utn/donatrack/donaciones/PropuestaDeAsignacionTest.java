package ar.edu.utn.donatrack.donaciones;

import static ar.edu.utn.donatrack.testsupport.FabricaDePrueba.nuevaDonacion;
import static ar.edu.utn.donatrack.testsupport.FabricaDePrueba.nuevaEntidad;
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
import ar.edu.utn.donatrack.estados.AsignacionRealizada;
import java.util.List;
import org.junit.jupiter.api.Test;

class PropuestaDeAsignacionTest {

  @Test
  void propuestaCreada_ConPendienteTrue_AndReferenciaDonacionYRankings() {
    Donacion donacion = crearDonacionDummy();
    List<RankingMatchmaking> rankings = crearRankingsDummy();

    PropuestaDeAsignacion propuesta = new PropuestaDeAsignacion(donacion, rankings);

    assertTrue(propuesta.isPendiente(), "Propuesta recién creada debe estar pendiente");
    assertSame(donacion, propuesta.getDonacion(), "Propuesta debe referenciar la donación");
    assertEquals(2, propuesta.getRankings().size(), "Propuesta debe contener los rankings");
  }

  @Test
  void confirmar_TransicionaPendienteYAsignaDonacion() {
    Donacion donacion = crearDonacionDummy();
    PropuestaDeAsignacion propuesta = new PropuestaDeAsignacion(donacion, crearRankingsDummy());
    EntidadBeneficiaria entidad = entidadRankeada(propuesta);

    propuesta.confirmar(entidad);

    assertFalse(propuesta.isPendiente(), "confirmar() debe setear pendiente=false");
    assertSame(
        entidad,
        donacion.getEntidadBeneficiaria(),
        "Donacion debe tener la entidad beneficiaria asignada");
    assertInstanceOf(
        AsignacionRealizada.class,
        donacion.getEstado(),
        "Donacion debe transicionar a AsignacionRealizada");
  }

  @Test
  void confirmar_EntidadFueraDelRanking_LanzaExcepcionDeDominio() {
    Donacion donacion = crearDonacionDummy();
    PropuestaDeAsignacion propuesta = new PropuestaDeAsignacion(donacion, crearRankingsDummy());
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
    PropuestaDeAsignacion propuesta = new PropuestaDeAsignacion(donacion, crearRankingsDummy());
    EntidadBeneficiaria entidad = entidadRankeada(propuesta);
    EntidadBeneficiaria otraEntidad = new EntidadBeneficiaria("Otro Hogar", "Dir2", "Tel2");

    // First confirm
    propuesta.confirmar(entidad);
    assertFalse(propuesta.isPendiente());
    assertSame(entidad, donacion.getEntidadBeneficiaria());

    // Second confirm — silent no-op, donacion already in AsignacionRealizada
    assertDoesNotThrow(() -> propuesta.confirmar(otraEntidad));
    assertFalse(propuesta.isPendiente(), "Debe seguir siendo false");
    assertSame(
        entidad,
        donacion.getEntidadBeneficiaria(),
        "La entidad no debe cambiar en doble confirmación");
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

  private static List<RankingMatchmaking> crearRankingsDummy() {
    EntidadBeneficiaria entidad = nuevaEntidad("Entidad");
    RankingMatchmaking ranking1 =
        new RankingMatchmaking("algoritmo-1", List.of(new MatchCandidato(entidad, 50)));
    RankingMatchmaking ranking2 =
        new RankingMatchmaking("algoritmo-2", List.of(new MatchCandidato(entidad, 80)));
    return List.of(ranking1, ranking2);
  }

  private static EntidadBeneficiaria entidadRankeada(PropuestaDeAsignacion propuesta) {
    return propuesta.getRankings().get(0).getCandidatos().get(0).getEntidadBeneficiaria();
  }
}
