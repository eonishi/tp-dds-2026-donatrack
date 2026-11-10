package ar.edu.utn.donatrack.donaciones;

import static ar.edu.utn.donatrack.testsupport.FabricaDePrueba.notificacionesDescartadas;
import static ar.edu.utn.donatrack.testsupport.FabricaDePrueba.nuevaDonacion;
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
import ar.edu.utn.donatrack.propuestas.matchmaking.AlgoritmoPrioridadSubAtendidos;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class AlgoritmoPrioridadSubAtendidosTest {

  private static final LocalDate FECHA_REFERENCIA = LocalDate.of(2026, 7, 11);

  @Test
  void rankear_ConMasDe10Entidades_RetornaMaximo10Candidatos() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Donacion donacion = donacionDe(fideos, 15.0);

    var entidades =
        IntStream.range(0, 15)
            .mapToObj(i -> entidadQueNecesita(100L + i, "Entidad " + i, fideos))
            .toList();

    var candidatos =
        new AlgoritmoPrioridadSubAtendidos()
            .rankear(donacion, entidades, FECHA_REFERENCIA, List.of());

    assertTrue(
        candidatos.size() <= 10, "Prioridad sub-atendidos debe retornar máximo 10 candidatos");
    assertEquals(
        10, candidatos.size(), "Con 15 entidades sub-atendidas debe retornar exactamente 10");
    // Verify sort order: highest puntaje first
    for (int i = 1; i < candidatos.size(); i++) {
      assertTrue(
          candidatos.get(i - 1).getPuntaje() >= candidatos.get(i).getPuntaje(),
          "Candidatos deben estar ordenados por puntaje descendente");
    }
  }

  @Test
  void rankear_ConMenosDe10Entidades_RetornaTodas() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Donacion donacion = donacionDe(fideos, 15.0);
    EntidadBeneficiaria e1 = entidadQueNecesita(1L, "Comedor A", fideos);
    EntidadBeneficiaria e2 = entidadQueNecesita(2L, "Comedor B", fideos);
    EntidadBeneficiaria e3 = entidadQueNecesita(3L, "Comedor C", fideos);

    var candidatos =
        new AlgoritmoPrioridadSubAtendidos()
            .rankear(donacion, List.of(e1, e2, e3), FECHA_REFERENCIA, List.of());

    assertEquals(3, candidatos.size(), "Con 3 entidades debe retornar las 3");
  }

  @Test
  void rankear_ConEntidadesSinNecesidadCompatible_RetornaVacio() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    SubCategoria sillas = new SubCategoria("Sillas", Categoria.MOBILIARIO);
    Donacion donacion =
        nuevaDonacion(
            crearDonanteDummy(),
            List.of(
                new Bien("Silla", "", 1.0, UnidadMedida.UNIDADES, null, EstadoUso.NUEVO, sillas)));
    EntidadBeneficiaria entidad = entidadQueNecesita(1L, "Comedor", fideos);

    var candidatos =
        new AlgoritmoPrioridadSubAtendidos()
            .rankear(donacion, List.of(entidad), FECHA_REFERENCIA, List.of());

    assertTrue(candidatos.isEmpty(), "Sin entidades con necesidad compatible debe retornar vacío");
  }

  @Test
  void puntaje_EntidadConMuchasEntregas_RetornaPuntajeMenor() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Donacion donacion = donacionDe(fideos, 15.0);
    EntidadBeneficiaria sinEntregas = entidadQueNecesita(1L, "Sin Recibir", fideos);
    EntidadBeneficiaria conMuchas = entidadQueNecesita(2L, "Ya Recibio", fideos);

    // Las entregas viajan como dato hasta el ranking: no consulta ningún repositorio.
    Donante donante = crearDonanteDummy();
    List<Donacion> entregadas =
        IntStream.range(0, 50)
            .mapToObj(i -> crearDonacionEntregada(donante, conMuchas, fideos))
            .toList();

    var candidatos =
        new AlgoritmoPrioridadSubAtendidos()
            .rankear(donacion, List.of(sinEntregas, conMuchas), LocalDate.now(), entregadas);

    assertEquals(2, candidatos.size());
    assertTrue(
        candidatos.get(0).getPuntaje() > candidatos.get(1).getPuntaje(),
        "Entidad sin entregas debe tener mayor puntaje que la que ya recibió mucho");
    assertEquals(
        100, candidatos.get(0).getPuntaje(), "Entidad sin entregas debería tener puntaje 100");
    assertEquals(
        50,
        candidatos.get(1).getPuntaje(),
        "Entidad con 50 entregas debería tener puntaje 100 - 50 = 50");
  }

  @Test
  void rankear_ConEntregasDistintas_UsaElHistorialDeCadaLlamada() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Donacion donacion = donacionDe(fideos, 15.0);
    EntidadBeneficiaria entidad = entidadQueNecesita(2L, "Ya Recibio", fideos);
    Donante donante = crearDonanteDummy();
    List<Donacion> entregadas =
        IntStream.range(0, 3)
            .mapToObj(i -> crearDonacionEntregada(donante, entidad, fideos))
            .toList();
    AlgoritmoPrioridadSubAtendidos algoritmo = new AlgoritmoPrioridadSubAtendidos();

    var sinHistorial = algoritmo.rankear(donacion, List.of(entidad), FECHA_REFERENCIA, List.of());
    var conHistorial = algoritmo.rankear(donacion, List.of(entidad), FECHA_REFERENCIA, entregadas);

    assertEquals(
        100,
        sinHistorial.get(0).getPuntaje(),
        "Sin entregas en la llamada el puntaje debe quedar completo");
    assertEquals(
        97,
        conHistorial.get(0).getPuntaje(),
        "Las entregas de la llamada deben descontarse en ese ranking");
  }

  @Test
  void rankear_ConEntidadesHomonimasEmpatadas_DesempataPorId() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Donacion donacion = donacionDe(fideos, 15.0);
    EntidadBeneficiaria primera = entidadQueNecesita(1L, "Comedor Los Pibes", fideos);
    EntidadBeneficiaria segunda = entidadQueNecesita(2L, "Comedor Los Pibes", fideos);

    var candidatos =
        new AlgoritmoPrioridadSubAtendidos()
            .rankear(donacion, List.of(segunda, primera), FECHA_REFERENCIA, List.of());

    assertEquals(2, candidatos.size());
    assertEquals(
        primera.getId(),
        candidatos.get(0).getEntidadBeneficiaria().getId(),
        "Con puntaje y razón social empatados debe desempatar por id");
  }

  private static Donacion donacionDe(SubCategoria subCategoria, double cantidad) {
    return nuevaDonacion(
        crearDonanteDummy(),
        List.of(
            new Bien(
                "Fideos",
                "",
                cantidad,
                UnidadMedida.KILOGRAMOS,
                null,
                EstadoUso.NO_APLICA,
                subCategoria)));
  }

  private static EntidadBeneficiaria entidadQueNecesita(
      Long id, String razonSocial, SubCategoria subCategoria) {
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(id, razonSocial, "Direccion", "Telefono");
    entidad.agregarNecesidadExtraordinaria(
        new Necesidad(subCategoria, 5.0, UnidadMedida.KILOGRAMOS, "Fideos"));
    return entidad;
  }

  private static Donante crearDonanteDummy() {
    return new PersonaHumana(
        new CorreoElectronico("test@test.com"),
        "Juan",
        "Perez",
        "12345678",
        TipoDocumento.DNI,
        Genero.MASCULINO,
        "Calle Falsa 123");
  }

  private static Donacion crearDonacionEntregada(
      Donante donante, EntidadBeneficiaria entidad, SubCategoria subCategoria) {
    Donacion donacion =
        nuevaDonacion(
            donante,
            List.of(
                new Bien(
                    "Entregada",
                    "",
                    1.0,
                    UnidadMedida.KILOGRAMOS,
                    null,
                    EstadoUso.NO_APLICA,
                    subCategoria)));
    donacion.confirmarEntidadBeneficiaria(entidad, notificacionesDescartadas());
    donacion.cambiarEstado(EstadoDonacion.ENTREGADA, null);
    return donacion;
  }
}
