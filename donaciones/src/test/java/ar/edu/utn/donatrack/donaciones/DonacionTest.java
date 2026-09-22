package ar.edu.utn.donatrack.donaciones;

import static ar.edu.utn.donatrack.testsupport.FabricaDePrueba.nuevaDonacion;
import static ar.edu.utn.donatrack.testsupport.FabricaDePrueba.nuevaEntidad;
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
import ar.edu.utn.donatrack.estados.EnDeposito;
import ar.edu.utn.donatrack.estados.EntregaFallida;
import ar.edu.utn.donatrack.estados.Entregada;
import ar.edu.utn.donatrack.notificaciones.Notificacion;
import ar.edu.utn.donatrack.notificaciones.eventos.EventoEntregaExitosa;
import ar.edu.utn.donatrack.notificaciones.payloads.ComprobanteEntrega;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    Donacion donacion = nuevaDonacion(donante, List.of(bien));
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
    assertThrows(NullPointerException.class, () -> nuevaDonacion(null, List.of(bien)));
  }

  @Test
  void donacionSinId_LanzaNullPointerException() {
    assertThrows(
        NullPointerException.class,
        () -> new Donacion(null, crearDonanteDummy(), List.of(crearBienDummy())));
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
    Donacion donacion = nuevaDonacion(donante, List.of(bien));

    donacion.confirmarEntidadBeneficiaria(nuevaEntidad("Hogar"));
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
  void confirmarEntrega_EnTraslado_SatisfaceNecesidad() {
    SubCategoria subcatFideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Necesidad necesidad = new Necesidad(subcatFideos, 5.0, UnidadMedida.KILOGRAMOS, "Fideos");

    EntidadBeneficiaria entidad = nuevaEntidad("Hogar Test");
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
    Donacion donacion = nuevaDonacion(crearDonanteDummy(), List.of(fideos));

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
    assertInstanceOf(Entregada.class, donacion.getEstado());

    // Assert: aplicarDonacionRecibida fue llamado → cantidadRecibida incrementada
    assertTrue(
        necesidad.getCantidadRecibida() > 0,
        "EntidadBeneficiaria.aplicarDonacionRecibida debe incrementar cantidadRecibida");
    assertEquals(10.0, necesidad.getCantidadRecibida(), 0.001);
    assertTrue(
        necesidad.estaSatisfecha(),
        "Necesidad con objetivo 5kg debería quedar satisfecha con 10kg recibidos");
  }

  @Test
  void confirmarEntrega_DonacionConSubcategoriaDistinta_NoActualizaNecesidad() {
    SubCategoria subcatFideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    SubCategoria subcatSillas = new SubCategoria("Sillas", Categoria.MOBILIARIO);
    Necesidad necesidad = new Necesidad(subcatFideos, 10.0, UnidadMedida.KILOGRAMOS, "Fideos");

    EntidadBeneficiaria entidad = nuevaEntidad("Hogar Test");
    entidad.agregarNecesidadExtraordinaria(necesidad);

    // Donación de sillas — subcategoría NO coincide con la necesidad de fideos
    Bien silla =
        new Bien("Silla", "", 2.0, UnidadMedida.UNIDADES, null, EstadoUso.NO_APLICA, subcatSillas);
    Donacion donacion = nuevaDonacion(crearDonanteDummy(), List.of(silla));

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
  }

  // ── Camión (patente de logística) ─────────────────────────────────

  @Test
  void nuevaDonacion_IdCamionVacio() {
    Donacion donacion = nuevaDonacion(crearDonanteDummy(), List.of(crearBienDummy()));

    assertTrue(donacion.getIdCamion().isEmpty(), "Nueva donación no debe tener camión registrado");
  }

  @Test
  void registrarCamion_GetIdCamionRetornaLaPatente() {
    Donacion donacion = nuevaDonacion(crearDonanteDummy(), List.of(crearBienDummy()));

    donacion.registrarCamion("ABC123");

    assertEquals("ABC123", donacion.getIdCamion().orElseThrow());
  }

  @Test
  void registrarCamionNull_LanzaNullPointerException() {
    Donacion donacion = nuevaDonacion(crearDonanteDummy(), List.of(crearBienDummy()));

    assertThrows(
        NullPointerException.class,
        () -> donacion.registrarCamion(null),
        "registrarCamion(null) debe lanzar NullPointerException");
  }

  @Test
  void limpiarCamion_GetIdCamionVacio() {
    Donacion donacion = nuevaDonacion(crearDonanteDummy(), List.of(crearBienDummy()));
    donacion.registrarCamion("ABC123");

    donacion.limpiarCamion();

    assertTrue(donacion.getIdCamion().isEmpty(), "limpiarCamion() debe dejar el camión vacío");
  }

  @Test
  void retornarAlDeposito_LimpiaCamion() {
    Donacion donacion = nuevaDonacion(crearDonanteDummy(), List.of(crearBienDummy()));
    donacion.confirmarEntidadBeneficiaria(nuevaEntidad("Hogar"));
    donacion.planificarRuta();
    donacion.registrarCamion("XYZ789");
    donacion.iniciarTraslado();
    donacion.reportarFalla("Dirección incorrecta");

    assertInstanceOf(
        EntregaFallida.class,
        donacion.getEstado(),
        "Debe estar en estado EntregaFallida antes de retornar");

    donacion.retornarAlDeposito();

    assertTrue(donacion.getIdCamion().isEmpty(), "retornarAlDeposito() debe limpiar el camión");
    assertInstanceOf(EnDeposito.class, donacion.getEstado(), "Debe volver a EnDeposito");
  }

  @Test
  void confirmarEntrega_GeneraComprobanteConFechaDelHistorialYPatente() {
    Donante donante = crearDonanteDummy();
    EntidadBeneficiaria entidad = nuevaEntidad("Hogar");
    Donacion donacion = nuevaDonacion(donante, List.of(crearBienDummy()));

    donacion.confirmarEntidadBeneficiaria(entidad);
    donacion.planificarRuta();
    donacion.registrarCamion("AF123ZZ");
    donacion.iniciarTraslado();
    donacion.confirmarEntrega();

    LocalDateTime fechaEntrega =
        donacion.getHistorialEstado().stream()
            .filter(h -> h.getEstadoAlcanzado() == NombreEstado.ENTREGADA)
            .findFirst()
            .orElseThrow()
            .getFecha();
    EventoEntregaExitosa evento =
        new EventoEntregaExitosa(
            donante, entidad, fechaEntrega, donacion.getIdCamion().orElseThrow());

    ComprobanteEntrega comprobante =
        evento.generarNotificaciones().stream()
            .map(Notificacion::getPayload)
            .filter(ComprobanteEntrega.class::isInstance)
            .map(ComprobanteEntrega.class::cast)
            .findFirst()
            .orElseThrow();

    assertEquals(fechaEntrega, comprobante.fechaEntrega());
    assertEquals("AF123ZZ", comprobante.idCamion());
  }

  // ── Matchmaking + PropuestaDeAsignacion tests ─────────────────────

  @Test
  void setEntidadBeneficiaria_AsignaEntidad() {
    Donacion donacion = nuevaDonacion(crearDonanteDummy(), List.of(crearBienDummy()));
    EntidadBeneficiaria entidad = nuevaEntidad("Hogar");

    donacion.setEntidadBeneficiaria(entidad);

    assertSame(
        entidad,
        donacion.getEntidadBeneficiaria(),
        "setEntidadBeneficiaria debe asignar la entidad");
  }

  @Test
  void confirmarEntidadBeneficiaria_SinInstanceOf_TransicionaCorrectamente() {
    Donacion donacion = nuevaDonacion(crearDonanteDummy(), List.of(crearBienDummy()));
    EntidadBeneficiaria entidad = nuevaEntidad("Hogar");

    donacion.confirmarEntidadBeneficiaria(entidad);

    assertSame(entidad, donacion.getEntidadBeneficiaria());
    assertInstanceOf(ar.edu.utn.donatrack.estados.AsignacionRealizada.class, donacion.getEstado());
  }

  @Test
  void confirmarEntidadBeneficiaria_EnAsignacionRealizada_LanzaException() {
    Donacion donacion = nuevaDonacion(crearDonanteDummy(), List.of(crearBienDummy()));
    EntidadBeneficiaria entidad = nuevaEntidad("Hogar");

    // Transition to AsignacionRealizada first
    donacion.confirmarEntidadBeneficiaria(entidad);

    // Second call should throw because AsignacionRealizada.asignar() throws
    assertThrows(
        IllegalStateException.class,
        () -> donacion.confirmarEntidadBeneficiaria(nuevaEntidad("Otro")),
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
