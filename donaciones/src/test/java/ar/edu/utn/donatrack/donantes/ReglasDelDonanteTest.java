package ar.edu.utn.donatrack.donantes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Las reglas que valen para todo donante, venga de la API, de un formulario o de un CSV. */
class ReglasDelDonanteTest {
  @Test
  void elTelefonoSeGuardaEnE164AunqueVengaConEspaciosYGuiones() {
    assertEquals("+541151819600", MedioDeContacto.sms("+54 11 5181-9600").getContacto());
    assertEquals("+541151819600", MedioDeContacto.whatsapp("+54 (11) 5181.9600").getContacto());
    assertThrows(IllegalArgumentException.class, () -> MedioDeContacto.sms("11 5181-9600"));
  }

  @Test
  void elCorreoTieneQueTenerFormaDeCorreo() {
    assertEquals("ana@test.com", MedioDeContacto.correo("  ana@test.com ").getContacto());
    assertThrows(IllegalArgumentException.class, () -> MedioDeContacto.correo("ana"));
    assertThrows(IllegalArgumentException.class, () -> MedioDeContacto.correo(null));
  }

  @Test
  void elMismoCuitConOSinGuionesEsElMismoDocumento() {
    assertEquals(
        TipoDocumento.CUIT.normalizar("30712345679"),
        TipoDocumento.CUIT.normalizar("30-71234567-9"));
    assertThrows(IllegalArgumentException.class, () -> TipoDocumento.CUIT.normalizar("30-111"));
    assertThrows(IllegalArgumentException.class, () -> TipoDocumento.DNI.normalizar(" "));
  }

  @Test
  void elDocumentoCoincideAunqueSeEscribaDistinto() {
    PersonajeJuridica coop =
        new PersonajeJuridica(
            MedioDeContacto.correo("coop@test.com"),
            "30-71234567-9",
            TipoDocumento.CUIT,
            "Cooperativa",
            TipoOrganizacion.EMPRESA,
            "Alimentos");

    assertTrue(coop.tieneDocumento("30712345679"));
    assertTrue(coop.tieneDocumento(" 30-71234567-9 "));
    assertFalse(coop.tieneDocumento("30712345670"));
    assertFalse(coop.tieneDocumento("no es un cuit"));
    assertFalse(coop.tieneDocumento(null));
  }

  @Test
  void alReemplazarLosMediosNoPuedeQuedarSinCorreo() {
    PersonaHumana ana = ana();
    var soloTelefono = List.of(MedioDeContacto.sms("+5491155555555"));

    assertThrows(
        IllegalArgumentException.class, () -> ana.reemplazarMediosDeContacto(soloTelefono, 0));
    assertEquals(1, ana.getMediosDeContacto().size(), "Con medios inválidos no cambia nada");
  }

  @Test
  void actualizarConCopiaLosDatosPeroNoCambiaElTipoDePersona() {
    PersonaHumana ana = ana();
    PersonaHumana datos =
        new PersonaHumana(
            MedioDeContacto.correo("ana.nueva@test.com"),
            "Ana María",
            "Gómez",
            "30111223",
            TipoDocumento.DNI,
            Genero.FEMENINO,
            "Calle 2");

    ana.actualizarCon(datos);

    assertEquals("Ana María", ana.getNombre());
    assertEquals("30111223", ana.getNumDocumento());
    assertEquals("ana.nueva@test.com", ana.getMedioContactoPredeterminado().getContacto());
    PersonajeJuridica coop =
        new PersonajeJuridica(
            MedioDeContacto.correo("coop@test.com"),
            "30712345679",
            TipoDocumento.CUIT,
            "Cooperativa",
            TipoOrganizacion.EMPRESA,
            "Alimentos");
    assertThrows(IllegalArgumentException.class, () -> ana.actualizarCon(coop));
  }

  private static PersonaHumana ana() {
    return new PersonaHumana(
        MedioDeContacto.correo("ana@test.com"),
        "Ana",
        "Gómez",
        "30111222",
        TipoDocumento.DNI,
        Genero.FEMENINO,
        "Calle 1");
  }
}
