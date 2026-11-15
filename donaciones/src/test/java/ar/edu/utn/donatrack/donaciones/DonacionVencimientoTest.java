package ar.edu.utn.donatrack.donaciones;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class DonacionVencimientoTest {

  private static final LocalDate HOY = LocalDate.of(2026, 11, 15);

  private static Donacion donacionQueVence(LocalDate fechaVencimiento) {
    Bien bien =
        new Bien(
            "Leche",
            "",
            1.0,
            UnidadMedida.UNIDADES,
            fechaVencimiento,
            EstadoUso.NO_APLICA,
            new SubCategoria("Leche", Categoria.ALIMENTO));
    Donante donante =
        new PersonaHumana(
            new CorreoElectronico("test@test.com"),
            "Juan",
            "Perez",
            "12345678",
            TipoDocumento.DNI,
            Genero.MASCULINO,
            "Calle Falsa 123");
    return new Donacion(donante, List.of(bien));
  }

  @Test
  void estaVencidaSiSuFechaDeVencimientoYaPaso() {
    assertTrue(donacionQueVence(HOY.minusDays(1)).estaVencida(HOY));
  }

  @Test
  void noEstaVencidaSiVenceHoy() {
    assertFalse(donacionQueVence(HOY).estaVencida(HOY));
  }

  @Test
  void noEstaVencidaSiVenceEnElFuturo() {
    assertFalse(donacionQueVence(HOY.plusDays(1)).estaVencida(HOY));
  }

  @Test
  void noEstaVencidaSiNoEsPerecedera() {
    assertFalse(donacionQueVence(null).estaVencida(HOY));
  }
}
