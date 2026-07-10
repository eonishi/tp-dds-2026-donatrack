package ar.edu.utn.donatrack.donaciones;

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
