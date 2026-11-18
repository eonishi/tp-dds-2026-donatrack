package ar.edu.utn.donatrack.donaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.bienes.Categoria;
import ar.edu.utn.donatrack.bienes.EstadoUso;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.bienes.UnidadMedida;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.Genero;
import ar.edu.utn.donatrack.donantes.MedioDeContacto;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.TipoDocumento;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CargaDonacionSegmentacionTest implements SimplePersistenceTest {

  @Test
  void cargarDonacion_SinEstadoDeUsoYConNoAplica_ProduceUnaSolaDonacion() {
    Donante donante =
        new PersonaHumana(
            MedioDeContacto.correo("segmentacion@test.com"),
            "Ana",
            "Segmento",
            "20000003",
            TipoDocumento.DNI,
            Genero.FEMENINO,
            "Calle Segmento 100");
    persist(donante);
    SubCategoria fideos = new SubCategoria("Segmentacion-" + UUID.randomUUID(), Categoria.ALIMENTO);
    persist(fideos);
    Bien sinEstado = new Bien("Fideos", "", 1, UnidadMedida.KILOGRAMOS, null, null, fideos);
    Bien noAplica =
        new Bien("Fideos", "", 2, UnidadMedida.KILOGRAMOS, null, EstadoUso.NO_APLICA, fideos);

    List<Donacion> donaciones =
        new CargaDonacion(new DonacionesRepository())
            .cargarDonacion(donante, List.of(sinEstado, noAplica));

    assertEquals(1, donaciones.size());
    assertEquals(List.of(sinEstado, noAplica), donaciones.get(0).getBienes());
  }
}
