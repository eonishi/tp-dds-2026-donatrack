package ar.edu.utn.donatrack.logistica;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.bienes.Categoria;
import ar.edu.utn.donatrack.bienes.EstadoUso;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.bienes.UnidadMedida;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.Genero;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.TipoDocumento;
import java.util.List;
import org.junit.jupiter.api.Test;

class DestinoTest {

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

  private static Bien crearBienDummy() {
    return new Bien(
        "Arroz",
        "",
        10.0,
        UnidadMedida.KILOGRAMOS,
        null,
        EstadoUso.NO_APLICA,
        new SubCategoria("Alimentos", Categoria.ALIMENTO));
  }

  private static Donacion crearDonacion(EntidadBeneficiaria entidad) {
    Donacion donacion = new Donacion(crearDonanteDummy(), List.of(crearBienDummy()));
    donacion.confirmarEntidadBeneficiaria(entidad);
    return donacion;
  }

  // ── Cycle 1: Creación  ─────────────────────────────

  @Test
  void crearDestino_ConEntidadNull_DebeLanzarNullPointerException() {
    assertThrows(NullPointerException.class, () -> new Destino(null));
  }

  @Test
  void crearDestino_ConEntidadValida_DebeTenerAtributosCorrectos() {
    EntidadBeneficiaria entidad =
        new EntidadBeneficiaria("Fundación Ayuda", "Calle 123", "555-0100");

    Destino destino = new Destino(entidad);

    assertSame(entidad, destino.getEntidadBeneficiaria());
    assertTrue(destino.getDonaciones().isEmpty());
  }

  // ── Cycle 2: Agregación  ───────────────

  @Test
  void agregarDonacion_ConDonacionNull_DebeLanzarNullPointerException() {
    Destino destino =
        new Destino(new EntidadBeneficiaria("Fundación Ayuda", "Calle 123", "555-0100"));

    assertThrows(NullPointerException.class, () -> destino.agregarDonacion(null));
  }

  @Test
  void agregarDonacion_ConDistintaEntidad_DebeLanzarIllegalArgumentException() {
    EntidadBeneficiaria entidadDestino =
        new EntidadBeneficiaria("Fundación A", "Calle 1", "555-0001");
    EntidadBeneficiaria entidadDonacion =
        new EntidadBeneficiaria("Fundación B", "Calle 2", "555-0002");
    Destino destino = new Destino(entidadDestino);
    Donacion donacion = crearDonacion(entidadDonacion);

    assertThrows(IllegalArgumentException.class, () -> destino.agregarDonacion(donacion));
  }

  @Test
  void agregarDonacion_ConMismaEntidad_DebeIncluirDonacion() {
    EntidadBeneficiaria entidad =
        new EntidadBeneficiaria("Fundación Ayuda", "Calle 123", "555-0100");
    Destino destino = new Destino(entidad);
    Donacion donacion = crearDonacion(entidad);

    destino.agregarDonacion(donacion);

    assertEquals(1, destino.getDonaciones().size());
    assertSame(donacion, destino.getDonaciones().get(0));
  }

  @Test
  void agregarDonacion_ConMultiplesDonaciones_DevuelveTodas() {
    EntidadBeneficiaria entidad =
        new EntidadBeneficiaria("Fundación Ayuda", "Calle 123", "555-0100");
    Destino destino = new Destino(entidad);
    Donacion d1 = crearDonacion(entidad);
    Donacion d2 = crearDonacion(entidad);
    Donacion d3 = crearDonacion(entidad);

    destino.agregarDonacion(d1);
    destino.agregarDonacion(d2);
    destino.agregarDonacion(d3);

    assertEquals(3, destino.getDonaciones().size());
    assertSame(d1, destino.getDonaciones().get(0));
    assertSame(d2, destino.getDonaciones().get(1));
    assertSame(d3, destino.getDonaciones().get(2));
  }

  @Test
  void agregarDonacion_ConDonacionDuplicada_DebePermitir() {
    EntidadBeneficiaria entidad =
        new EntidadBeneficiaria("Fundación Ayuda", "Calle 123", "555-0100");
    Destino destino = new Destino(entidad);
    Donacion donacion = crearDonacion(entidad);

    destino.agregarDonacion(donacion);
    destino.agregarDonacion(donacion);

    assertEquals(2, destino.getDonaciones().size());
    assertSame(donacion, destino.getDonaciones().get(0));
    assertSame(donacion, destino.getDonaciones().get(1));
  }

  // ── Cycle 3: Query  ─────────────────────────────────────

  @Test
  void getDonaciones_AlModificarLista_DebeLanzarUnsupportedOperationException() {
    EntidadBeneficiaria entidad =
        new EntidadBeneficiaria("Fundación Ayuda", "Calle 123", "555-0100");
    Destino destino = new Destino(entidad);
    destino.agregarDonacion(crearDonacion(entidad));

    assertThrows(UnsupportedOperationException.class, () -> destino.getDonaciones().add(null));
  }

  @Test
  void getEntidadBeneficiaria_DebeSerMismaReferencia() {
    EntidadBeneficiaria entidad =
        new EntidadBeneficiaria("Fundación Ayuda", "Calle 123", "555-0100");
    Destino destino = new Destino(entidad);

    assertSame(entidad, destino.getEntidadBeneficiaria());
  }

  // ── Phase 1: equals/hashCode  ────────────────────────

  @Test
  void mismaEntidadBeneficiaria_EqualsDevuelveTrue() {
    EntidadBeneficiaria entidad =
        new EntidadBeneficiaria("Fundación Ayuda", "Calle 123", "555-0100");
    Destino a = new Destino(entidad);
    Destino b = new Destino(entidad);

    assertEquals(a, b);
  }

  @Test
  void mismaEntidadBeneficiaria_HashCodeEsIgual() {
    EntidadBeneficiaria entidad =
        new EntidadBeneficiaria("Fundación Ayuda", "Calle 123", "555-0100");
    Destino a = new Destino(entidad);
    Destino b = new Destino(entidad);

    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void distintaEntidadBeneficiaria_EqualsDevuelveFalse() {
    EntidadBeneficiaria entidadA = new EntidadBeneficiaria("Fundación A", "Calle 1", "555-0001");
    EntidadBeneficiaria entidadB = new EntidadBeneficiaria("Fundación B", "Calle 2", "555-0002");
    Destino a = new Destino(entidadA);
    Destino b = new Destino(entidadB);

    assertNotEquals(a, b);
  }
}
