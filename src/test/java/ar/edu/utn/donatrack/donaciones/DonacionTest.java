package ar.edu.utn.donatrack.donaciones;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.beneficiarios.Necesidad;
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
import ar.edu.utn.donatrack.logistica.Camion;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class DonacionTest {

  @Test
  void donacionCreadaConDonante_RetornaElDonante() {
    Donante donante = crearDonanteDummy();
    Bien bien =
        new Bien(
            "Test",
            "",
            1.0,
            UnidadMedida.UNIDADES,
            null,
            EstadoUso.NO_APLICA,
            new SubCategoria("X", Categoria.ALIMENTO));
    Donacion donacion = new Donacion(donante, List.of(bien));
    assertSame(donante, donacion.getDonante());
  }

  @Test
  void donacionSinDonante_LanzaNullPointerException() {
    Bien bien =
        new Bien(
            "Test",
            "",
            1.0,
            UnidadMedida.UNIDADES,
            null,
            EstadoUso.NO_APLICA,
            new SubCategoria("X", Categoria.ALIMENTO));
    assertThrows(NullPointerException.class, () -> new Donacion(null, List.of(bien)));
  }

  @Test
  void donacion_RetornarAlDeposito_NoLimpiaDonante() {
    Donante donante = crearDonanteDummy();
    Bien bien =
        new Bien(
            "Test",
            "",
            1.0,
            UnidadMedida.UNIDADES,
            null,
            EstadoUso.NO_APLICA,
            new SubCategoria("X", Categoria.ALIMENTO));
    Donacion donacion = new Donacion(donante, List.of(bien));

    donacion.confirmarEntidadBeneficiaria(new EntidadBeneficiaria("Hogar", "Dir", "Tel"));
    donacion.planificarRuta();
    donacion.iniciarTraslado();
    donacion.reportarFalla("Dirección incorrecta");
    donacion.retornarAlDeposito();

    assertSame(
        donante,
        donacion.getDonante(),
        "Donante nunca debe limpiarse (diferente de entidadBeneficiaria)");
  }

  @Test
  void confirmarEntrega_EnTraslado_RegistraRecepcionYSatisfaceNecesidad() {
    SubCategoria subcatFideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Necesidad necesidad = new Necesidad(subcatFideos, 5.0, UnidadMedida.KILOGRAMOS, "Fideos");

    EntidadBeneficiaria entidad = new EntidadBeneficiaria("Hogar Test", "Dir", "Tel");
    entidad.agregarNecesidadExtraordinaria(necesidad);

    Bien fideos =
        new Bien(
            "Fideos",
            "",
            10.0,
            UnidadMedida.KILOGRAMOS,
            LocalDate.now().plusYears(1),
            EstadoUso.NO_APLICA,
            subcatFideos);
    Donacion donacion = new Donacion(crearDonanteDummy(), List.of(fideos));

    // Walk through state machine: EnDeposito → Asignada → ListaParaEntregar → EnTraslado
    donacion.confirmarEntidadBeneficiaria(entidad);
    donacion.planificarRuta();
    donacion.iniciarTraslado();

    // Pre-condition: necesidad starts at 0, not satisfied
    assertFalse(necesidad.estaSatisfecha(), "Necesidad aún no debería estar satisfecha");
    assertEquals(0.0, necesidad.getCantidadRecibida(), 0.001);

    // Act
    donacion.confirmarEntrega();

    // Assert: estado cambió
    assertEquals(EstadoDonacion.ENTREGADA, donacion.getEstado());

    // Assert: registrarRecepcion fue llamado → cantidadRecibida incrementada
    assertTrue(
        necesidad.getCantidadRecibida() > 0,
        "EntidadBeneficiaria.registrarRecepcion debe incrementar cantidadRecibida");
    assertEquals(10.0, necesidad.getCantidadRecibida(), 0.001);
    assertTrue(
        necesidad.estaSatisfecha(),
        "Necesidad con objetivo 5kg debería quedar satisfecha con 10kg recibidos");
    assertEquals(1, entidad.getRecepciones().size(), "Debe registrarse exactamente una recepción");
  }

  @Test
  void confirmarEntrega_DonacionConSubcategoriaDistinta_NoActualizaNecesidad() {
    SubCategoria subcatFideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    SubCategoria subcatSillas = new SubCategoria("Sillas", Categoria.MOBILIARIO);
    Necesidad necesidad = new Necesidad(subcatFideos, 10.0, UnidadMedida.KILOGRAMOS, "Fideos");

    EntidadBeneficiaria entidad = new EntidadBeneficiaria("Hogar Test", "Dir", "Tel");
    entidad.agregarNecesidadExtraordinaria(necesidad);

    // Donación de sillas — subcategoría NO coincide con la necesidad de fideos
    Bien silla =
        new Bien("Silla", "", 2.0, UnidadMedida.UNIDADES, null, EstadoUso.NO_APLICA, subcatSillas);
    Donacion donacion = new Donacion(crearDonanteDummy(), List.of(silla));

    donacion.confirmarEntidadBeneficiaria(entidad);
    donacion.planificarRuta();
    donacion.iniciarTraslado();

    // Pre-condition
    assertEquals(0.0, necesidad.getCantidadRecibida(), 0.001);

    // Act
    donacion.confirmarEntrega();

    // Assert: subcategoría distinta → necesidad NO se actualiza
    assertEquals(
        0.0,
        necesidad.getCantidadRecibida(),
        0.001,
        "Necesidad con subcategoría distinta no debe incrementarse");
    assertFalse(necesidad.estaSatisfecha(), "Necesidad no debe quedar satisfecha");
    assertEquals(
        1,
        entidad.getRecepciones().size(),
        "La recepción igual se registra aunque no haya match de necesidades");
  }

  // ── TDD Phase 1: RED — Camion lifecycle tests ─────────────────────

  @Test
  void nuevaDonacion_GetCamionRetornaEmpty() {
    Donacion donacion = new Donacion(crearDonanteDummy(), List.of(crearBienDummy()));
    assertTrue(donacion.getCamion().isEmpty(), "Nueva donación no debe tener camión asignado");
  }

  @Test
  void asignarCamion_GetCamionRetornaElCamion() {
    Donacion donacion = new Donacion(crearDonanteDummy(), List.of(crearBienDummy()));
    Camion camion = new Camion("ABC123", 10.0, 2.5, 1000.0);
    donacion.asignarCamion(camion);
    assertTrue(donacion.getCamion().isPresent(), "getCamion() debe devolver un Optional con valor");
    assertSame(camion, donacion.getCamion().get(), "El camion devuelto debe ser el mismo asignado");
  }

  @Test
  void asignarCamionNull_LanzaNullPointerException() {
    Donacion donacion = new Donacion(crearDonanteDummy(), List.of(crearBienDummy()));
    assertThrows(
        NullPointerException.class,
        () -> donacion.asignarCamion(null),
        "asignarCamion(null) debe lanzar NullPointerException");
  }

  @Test
  void limpiarCamion_GetCamionRetornaEmpty() {
    Donacion donacion = new Donacion(crearDonanteDummy(), List.of(crearBienDummy()));
    Camion camion = new Camion("ABC123", 10.0, 2.5, 1000.0);
    donacion.asignarCamion(camion);
    donacion.limpiarCamion();
    assertTrue(donacion.getCamion().isEmpty(), "limpiarCamion() debe dejar getCamion() vacío");
  }

  @Test
  void retornarAlDeposito_LimpiaCamion() {
    Donacion donacion = new Donacion(crearDonanteDummy(), List.of(crearBienDummy()));
    Camion camion = new Camion("XYZ789", 15.0, 3.0, 2000.0);
    donacion.asignarCamion(camion);

    // Walk through: EnDeposito → Asignada → ListaParaEntregar → EnTraslado → EntregaFallida
    donacion.confirmarEntidadBeneficiaria(new EntidadBeneficiaria("Hogar", "Dir", "Tel"));
    donacion.planificarRuta();
    donacion.iniciarTraslado();
    donacion.reportarFalla("Dirección incorrecta");

    assertEquals(
        EstadoDonacion.ENTREGA_FALLIDA,
        donacion.getEstado(),
        "Debe estar en estado EntregaFallida antes de retornar");

    donacion.retornarAlDeposito();

    assertTrue(donacion.getCamion().isEmpty(), "retornarAlDeposito() debe limpiar el camión");
    assertEquals(EstadoDonacion.EN_DEPOSITO, donacion.getEstado(), "Debe volver a EnDeposito");
  }

  // ── Matchmaking + PropuestaDeAsignacion tests ─────────────────────

  @Test
  void setEntidadBeneficiaria_AsignaEntidad() {
    Donacion donacion = new Donacion(crearDonanteDummy(), List.of(crearBienDummy()));
    EntidadBeneficiaria entidad = new EntidadBeneficiaria("Hogar", "Dir", "Tel");

    donacion.setEntidadBeneficiaria(entidad);

    assertSame(
        entidad,
        donacion.getEntidadBeneficiaria(),
        "setEntidadBeneficiaria debe asignar la entidad");
  }

  @Test
  void confirmarEntidadBeneficiaria_SinInstanceOf_TransicionaCorrectamente() {
    Donacion donacion = new Donacion(crearDonanteDummy(), List.of(crearBienDummy()));
    EntidadBeneficiaria entidad = new EntidadBeneficiaria("Hogar", "Dir", "Tel");

    donacion.confirmarEntidadBeneficiaria(entidad);

    assertSame(entidad, donacion.getEntidadBeneficiaria());
    assertEquals(EstadoDonacion.ASIGNACION_REALIZADA, donacion.getEstado());
  }

  @Test
  void confirmarEntidadBeneficiaria_EnAsignacionRealizada_LanzaException() {
    Donacion donacion = new Donacion(crearDonanteDummy(), List.of(crearBienDummy()));
    EntidadBeneficiaria entidad = new EntidadBeneficiaria("Hogar", "Dir", "Tel");

    // Transition to AsignacionRealizada first
    donacion.confirmarEntidadBeneficiaria(entidad);

    // Second call should throw because AsignacionRealizada.asignar() throws
    assertThrows(
        IllegalStateException.class,
        () ->
            donacion.confirmarEntidadBeneficiaria(new EntidadBeneficiaria("Otro", "Dir2", "Tel2")),
        "Llamar confirmarEntidadBeneficiaria desde AsignacionRealizada debe lanzar excepción");
  }

  private static Bien crearBienDummy() {
    return new Bien(
        "Test",
        "",
        1.0,
        UnidadMedida.UNIDADES,
        null,
        EstadoUso.NO_APLICA,
        new SubCategoria("X", Categoria.ALIMENTO));
  }

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
}
