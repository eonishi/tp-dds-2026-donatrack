package ar.edu.utn.donatrack.beneficiarios;

import static ar.edu.utn.donatrack.testsupport.FabricaDePrueba.nuevaDonacion;
import static ar.edu.utn.donatrack.testsupport.FabricaDePrueba.nuevaEntidad;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EntidadBeneficiariaTest {

  @Test
  void mismoId_SeDeduplicaEnHashSet() {
    Long id = 1L;
    EntidadBeneficiaria a =
        new EntidadBeneficiaria(id, "Comedor Los Pibes", "Av. Siempre Viva 123", "011-4567-8901");
    EntidadBeneficiaria b =
        new EntidadBeneficiaria(id, "Otro Nombre", "Calle Falsa 456", "011-1111-2222");

    Set<EntidadBeneficiaria> set = new HashSet<>();
    set.add(a);
    set.add(b);

    assertEquals(1, set.size());
  }

  @Test
  void distintoId_NoSeDeduplicaEnHashSet() {
    EntidadBeneficiaria a =
        new EntidadBeneficiaria(1L, "Comedor Los Pibes", "Av. Siempre Viva 123", "011-4567-8901");
    EntidadBeneficiaria b =
        new EntidadBeneficiaria(2L, "Comedor Los Pibes", "Calle Falsa 456", "011-1111-2222");

    Set<EntidadBeneficiaria> set = new HashSet<>();
    set.add(a);
    set.add(b);

    assertEquals(2, set.size());
  }

  @Test
  void getId_DevuelveElIdAsignado() {
    Long id = 42L;
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(id, "Test", "Dir", "Tel");

    assertEquals(id, entidad.getId());
  }

  @Test
  void idNull_LanzaNullPointerException() {
    assertThrows(
        NullPointerException.class, () -> new EntidadBeneficiaria(null, "Test", "Dir", "Tel"));
  }

  @Test
  void agregaCorreoElectronico_YLoRecupera() {
    EntidadBeneficiaria eb = nuevaEntidad("Test");
    CorreoElectronico correo = new CorreoElectronico("test@example.com");
    eb.agregarCorreo(correo);

    assertEquals(1, eb.getCorreos().size());
    assertEquals("test@example.com", eb.getCorreos().get(0).getContacto());
  }

  @Test
  void agregaMultiplesCorreos_YLoSRecuperaTodos() {
    EntidadBeneficiaria eb = nuevaEntidad("Test");
    eb.agregarCorreo(new CorreoElectronico("a@example.com"));
    eb.agregarCorreo(new CorreoElectronico("b@example.com"));
    eb.agregarCorreo(new CorreoElectronico("c@example.com"));

    assertEquals(3, eb.getCorreos().size());
    assertEquals("a@example.com", eb.getCorreos().get(0).getContacto());
    assertEquals("b@example.com", eb.getCorreos().get(1).getContacto());
    assertEquals("c@example.com", eb.getCorreos().get(2).getContacto());
  }

  @Test
  void tieneNecesidadCompatible_ConNecesidadExtraordinaria_DevuelveTrue() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    EntidadBeneficiaria entidad = nuevaEntidad("Comedor");
    entidad.agregarNecesidadExtraordinaria(
        new Necesidad(fideos, 10.0, UnidadMedida.KILOGRAMOS, "Fideos"));

    assertTrue(entidad.tieneNecesidadCompatible(donacionDe(fideos)));
  }

  @Test
  void tieneNecesidadCompatible_ConNecesidadRecurrente_DevuelveTrue() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    EntidadBeneficiaria entidad = nuevaEntidad("Comedor");
    entidad.agregarNecesidadRecurrente(
        new NecesidadRecurrente(fideos, 10.0, UnidadMedida.KILOGRAMOS, "Fideos"));

    assertTrue(entidad.tieneNecesidadCompatible(donacionDe(fideos)));
  }

  @Test
  void tieneNecesidadCompatible_SinCoincidencias_DevuelveFalse() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    SubCategoria sillas = new SubCategoria("Sillas", Categoria.MOBILIARIO);
    EntidadBeneficiaria entidad = nuevaEntidad("Comedor");
    entidad.agregarNecesidadExtraordinaria(
        new Necesidad(fideos, 10.0, UnidadMedida.KILOGRAMOS, "Fideos"));

    assertFalse(entidad.tieneNecesidadCompatible(donacionDe(sillas)));
  }

  @Test
  void tieneNecesidadCompatible_DonacionNull_LanzaNullPointerException() {
    EntidadBeneficiaria entidad = nuevaEntidad("Comedor");

    assertThrows(NullPointerException.class, () -> entidad.tieneNecesidadCompatible(null));
  }

  @Test
  void calcularPuntajeCompatibilidad_DevuelveElMaximoDeSusNecesidades() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    EntidadBeneficiaria entidad = nuevaEntidad("Comedor");
    entidad.agregarNecesidadExtraordinaria(
        new Necesidad(fideos, 100.0, UnidadMedida.KILOGRAMOS, "Objetivo lejano"));
    entidad.agregarNecesidadRecurrente(
        new NecesidadRecurrente(fideos, 10.0, UnidadMedida.KILOGRAMOS, "Objetivo alcanzable"));

    assertEquals(
        75,
        entidad.calcularPuntajeCompatibilidad(donacionDe(fideos)),
        "La entidad responde la mejor compatibilidad entre todas sus necesidades");
  }

  @Test
  void calcularPuntajeCompatibilidad_SinNecesidades_DevuelveCero() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    EntidadBeneficiaria entidad = nuevaEntidad("Comedor");

    assertEquals(0, entidad.calcularPuntajeCompatibilidad(donacionDe(fideos)));
  }

  @Test
  void calcularPuntajeCompatibilidad_DonacionNull_LanzaNullPointerException() {
    EntidadBeneficiaria entidad = nuevaEntidad("Comedor");

    assertThrows(NullPointerException.class, () -> entidad.calcularPuntajeCompatibilidad(null));
  }

  @Test
  void aplicarDonacionRecibida_SoloSumaALasNecesidadesCompatibles() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    SubCategoria sillas = new SubCategoria("Sillas", Categoria.MOBILIARIO);
    EntidadBeneficiaria entidad = nuevaEntidad("Comedor");
    Necesidad extraordinaria =
        new Necesidad(fideos, 30.0, UnidadMedida.KILOGRAMOS, "Fideos extraordinarios");
    NecesidadRecurrente recurrente =
        new NecesidadRecurrente(fideos, 30.0, UnidadMedida.KILOGRAMOS, "Fideos recurrentes");
    Necesidad incompatible = new Necesidad(sillas, 5.0, UnidadMedida.UNIDADES, "Sillas");
    entidad.agregarNecesidadExtraordinaria(extraordinaria);
    entidad.agregarNecesidadRecurrente(recurrente);
    entidad.agregarNecesidadExtraordinaria(incompatible);

    entidad.aplicarDonacionRecibida(donacionDe(fideos));

    assertEquals(10.0, extraordinaria.getCantidadRecibida(), 0.001);
    assertEquals(10.0, recurrente.getCantidadRecibida(), 0.001);
    assertEquals(
        0.0,
        incompatible.getCantidadRecibida(),
        0.001,
        "La necesidad de otra subcategoría no suma");
  }

  @Test
  void aplicarDonacionRecibida_DonacionNull_LanzaNullPointerException() {
    EntidadBeneficiaria entidad = nuevaEntidad("Comedor");

    assertThrows(NullPointerException.class, () -> entidad.aplicarDonacionRecibida(null));
  }

  private static Donacion donacionDe(SubCategoria subcategoria) {
    return nuevaDonacion(
        donanteDummy(),
        List.of(
            new Bien(
                "Bien",
                "",
                10.0,
                UnidadMedida.KILOGRAMOS,
                null,
                EstadoUso.NO_APLICA,
                subcategoria)));
  }

  private static Donante donanteDummy() {
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
