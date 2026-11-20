package ar.edu.utn.donatrack.api.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.utn.donatrack.api.dto.DonanteDtos.DonanteRequest;
import ar.edu.utn.donatrack.api.dto.DonanteDtos.MedioContactoRequest;
import ar.edu.utn.donatrack.api.dto.DonanteDtos.RepresentanteRequest;
import ar.edu.utn.donatrack.donantes.Canal;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.Genero;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.PersonajeJuridica;
import ar.edu.utn.donatrack.donantes.TipoDocumento;
import ar.edu.utn.donatrack.donantes.TipoOrganizacion;
import ar.edu.utn.donatrack.donantes.TipoPersona;
import java.util.List;
import org.junit.jupiter.api.Test;

class DonanteMapperTest {
  private static final List<MedioContactoRequest> CORREO_Y_WHATSAPP =
      List.of(
          new MedioContactoRequest("EMAIL", "ana@test.com"),
          new MedioContactoRequest("WHATSAPP", "+5491155555555"));

  @Test
  void armaUnaPersonaHumanaConSusMediosYElPreferido() {
    Donante donante = DonanteMapper.nuevo(humana("30111222", 35, CORREO_Y_WHATSAPP, 1));

    PersonaHumana ana = assertInstanceOf(PersonaHumana.class, donante);
    assertEquals(35, ana.getEdad());
    assertEquals(2, ana.getMediosDeContacto().size());
    assertEquals(Canal.WHATSAPP, ana.getMedioContactoPredeterminado().getCanal());
  }

  @Test
  void exigeUnCorreoEntreLosMedios() {
    var soloTelefono = List.of(new MedioContactoRequest("SMS", "+5491155555555"));
    var error =
        assertThrows(
            IllegalArgumentException.class,
            () -> DonanteMapper.nuevo(humana("30111222", 35, soloTelefono, 0)));
    assertEquals("Debe incluir un correo electrónico.", error.getMessage());
  }

  @Test
  void elTelefonoTieneQueEstarEnFormatoInternacional() {
    var telefonoLocal =
        List.of(
            new MedioContactoRequest("EMAIL", "ana@test.com"),
            new MedioContactoRequest("SMS", "1155555555"));
    assertThrows(
        IllegalArgumentException.class,
        () -> DonanteMapper.nuevo(humana("30111222", 35, telefonoLocal, 0)));
  }

  @Test
  void elCuitTieneOnceDigitosYSeGuardaSinGuiones() {
    assertEquals(
        "30712345679", DonanteMapper.nuevo(juridica("30-71234567-9", null)).getNumDocumento());
    assertThrows(
        IllegalArgumentException.class, () -> DonanteMapper.nuevo(juridica("30-7123456-9", null)));
  }

  @Test
  void alEditarNoCambiaElTipoDePersonaNiTocaNadaSiAlgoEsInvalido() {
    Donante ana = DonanteMapper.nuevo(humana("30111222", 35, CORREO_Y_WHATSAPP, 0));

    assertFalse(DonanteMapper.mismoTipo(ana, juridica("30712345679", null)));
    assertThrows(
        IllegalArgumentException.class,
        () -> DonanteMapper.aplicar(ana, juridica("30712345679", null)));
    assertThrows(
        IllegalArgumentException.class,
        () -> DonanteMapper.aplicar(ana, humana("40999888", 200, CORREO_Y_WHATSAPP, 0)));
    assertEquals("30111222", ana.getNumDocumento(), "Con un dato inválido no se aplica nada");
  }

  @Test
  void siLaEdicionNoTraeRepresentantesSeConservanLosQueTenia() {
    var conRepresentante = List.of(new RepresentanteRequest("Luis", "Paz"));
    PersonajeJuridica coop =
        (PersonajeJuridica) DonanteMapper.nuevo(juridica("30712345679", conRepresentante));

    DonanteMapper.aplicar(coop, juridica("30712345679", null));

    assertEquals(1, coop.getRepresentantes().size());
  }

  private static DonanteRequest humana(
      String documento, Integer edad, List<MedioContactoRequest> medios, int preferido) {
    return new DonanteRequest(
        TipoPersona.HUMANA,
        documento,
        TipoDocumento.DNI,
        medios,
        preferido,
        "Ana",
        "Gomez",
        edad,
        Genero.FEMENINO,
        "Calle 1",
        null,
        null,
        null,
        null);
  }

  private static DonanteRequest juridica(String cuit, List<RepresentanteRequest> representantes) {
    return new DonanteRequest(
        TipoPersona.JURIDICA,
        cuit,
        TipoDocumento.CUIT,
        List.of(new MedioContactoRequest("EMAIL", "coop@test.com")),
        0,
        null,
        null,
        null,
        null,
        null,
        "Cooperativa",
        TipoOrganizacion.EMPRESA,
        "Alimentos",
        representantes);
  }
}
