package ar.edu.utn.donatrack.api.mappers;

import ar.edu.utn.donatrack.api.dto.DonanteDtos.DonanteRequest;
import ar.edu.utn.donatrack.api.dto.DonanteDtos.MedioContactoRequest;
import ar.edu.utn.donatrack.donantes.Canal;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.MedioDeContacto;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.PersonajeJuridica;
import ar.edu.utn.donatrack.donantes.Representante;
import ar.edu.utn.donatrack.donantes.TipoDocumento;
import ar.edu.utn.donatrack.donantes.TipoPersona;
import java.util.List;
import java.util.Locale;

/**
 * De un {@link DonanteRequest} a una persona donante: la única lógica de alta y edición, la usan la
 * API y los formularios web (que primero pasan sus campos a un request). Valida todo antes de tocar
 * nada; un dato inválido es una {@link IllegalArgumentException} con un mensaje para mostrar.
 *
 * <p>Que el documento no sea de otra persona no se ve acá: necesita la base (ver {@code
 * DonantesRepository.documentoEnUso}).
 */
public final class DonanteMapper {
  private static final String TELEFONO_E164 = "\\+[1-9][0-9]{1,14}";

  private DonanteMapper() {}

  /** Una persona donante nueva, sin guardar. */
  public static Donante nuevo(DonanteRequest request) {
    validar(request);
    List<MedioDeContacto> medios = medios(request);
    MedioDeContacto correo = medios.stream().filter(MedioDeContacto::esCorreo).findFirst().get();
    Donante donante;
    if (request.tipoPersona() == TipoPersona.HUMANA) {
      PersonaHumana humana =
          new PersonaHumana(
              correo,
              request.nombre(),
              request.apellido(),
              request.numeroDocumento(),
              request.tipoDocumento(),
              request.genero(),
              request.direccion());
      if (request.edad() != null) {
        humana.setEdad(request.edad());
      }
      donante = humana;
    } else {
      donante =
          new PersonajeJuridica(
              correo,
              request.numeroDocumento(),
              request.tipoDocumento(),
              request.razonSocial(),
              request.tipoOrganizacion(),
              request.rubro());
      aplicarDatosJuridicos((PersonajeJuridica) donante, request);
    }
    donante.reemplazarMediosDeContacto(medios, predeterminado(request));
    return donante;
  }

  /** Si el request es del mismo tipo de persona (humana o jurídica) que el donante. */
  public static boolean mismoTipo(Donante actual, DonanteRequest request) {
    return (actual instanceof PersonaHumana) == (request.tipoPersona() == TipoPersona.HUMANA);
  }

  /**
   * Pasa el request a una persona donante que ya existe. No cambia el tipo de persona. Si el
   * request no trae representantes ({@code null}), se conservan los que tenía.
   */
  public static void aplicar(Donante actual, DonanteRequest request) {
    validar(request);
    if (!mismoTipo(actual, request)) {
      throw new IllegalArgumentException("No se puede cambiar el tipo de persona.");
    }
    List<MedioDeContacto> medios = medios(request);
    actual.actualizarIdentificacion(request.numeroDocumento(), request.tipoDocumento());
    if (actual instanceof PersonaHumana humana) {
      humana.actualizarDatosPersonales(
          request.nombre(), request.apellido(), request.genero(), request.direccion());
      if (request.edad() != null) {
        humana.setEdad(request.edad());
      }
    } else {
      aplicarDatosJuridicos((PersonajeJuridica) actual, request);
    }
    actual.reemplazarMediosDeContacto(medios, predeterminado(request));
  }

  /** El tipo de medio tal como lo nombra la API: EMAIL, SMS o WHATSAPP. */
  public static String tipoDeMedio(Canal canal) {
    return canal == Canal.CORREO ? "EMAIL" : canal.name();
  }

  private static void validar(DonanteRequest request) {
    if (request.tipoPersona() == null || request.tipoDocumento() == null) {
      throw new IllegalArgumentException("Faltan el tipo de persona o el tipo de documento.");
    }
    if (vacio(request.numeroDocumento())) {
      throw new IllegalArgumentException("Falta el número de documento.");
    }
    if (request.tipoDocumento() == TipoDocumento.CUIT
        && !request.numeroDocumento().matches("[0-9]{11}")) {
      throw new IllegalArgumentException("El CUIT debe tener 11 dígitos.");
    }
    if (request.tipoPersona() == TipoPersona.HUMANA
        && (vacio(request.nombre())
            || vacio(request.apellido())
            || request.genero() == null
            || vacio(request.direccion()))) {
      throw new IllegalArgumentException(
          "Una persona humana requiere nombre, apellido, género y dirección.");
    }
    if (request.edad() != null && (request.edad() < 0 || request.edad() > 130)) {
      throw new IllegalArgumentException("La edad debe ser un número entero entre 0 y 130.");
    }
    if (request.tipoPersona() == TipoPersona.JURIDICA
        && (vacio(request.razonSocial())
            || request.tipoOrganizacion() == null
            || vacio(request.rubro()))) {
      throw new IllegalArgumentException(
          "Una persona jurídica requiere razón social, tipo de organización y rubro.");
    }
    if (request.mediosDeContacto() == null || request.mediosDeContacto().isEmpty()) {
      throw new IllegalArgumentException("Falta al menos un medio de contacto.");
    }
    List<MedioDeContacto> medios = medios(request);
    if (medios.stream().noneMatch(MedioDeContacto::esCorreo)) {
      throw new IllegalArgumentException("Debe incluir un correo electrónico (EMAIL).");
    }
    if (medios.stream().anyMatch(m -> m.esTelefono() && !m.getContacto().matches(TELEFONO_E164))) {
      throw new IllegalArgumentException(
          "El teléfono debe usar formato internacional E.164, por ejemplo +5491155555555.");
    }
    int predeterminado = predeterminado(request);
    if (predeterminado < 0 || predeterminado >= medios.size()) {
      throw new IllegalArgumentException(
          "El contacto predeterminado debe ser uno de los medios de contacto.");
    }
  }

  private static List<MedioDeContacto> medios(DonanteRequest request) {
    return request.mediosDeContacto().stream().map(DonanteMapper::medio).toList();
  }

  private static MedioDeContacto medio(MedioContactoRequest contacto) {
    String valor = contacto.contacto() == null ? "" : contacto.contacto().trim();
    String tipo = contacto.tipo() == null ? "" : contacto.tipo().toUpperCase(Locale.ROOT);
    return switch (tipo) {
      case "EMAIL" -> MedioDeContacto.correo(valor);
      case "SMS" -> MedioDeContacto.sms(valor);
      case "WHATSAPP" -> MedioDeContacto.whatsapp(valor);
      default -> throw new IllegalArgumentException("Tipo de contacto inválido: " + tipo + ".");
    };
  }

  private static int predeterminado(DonanteRequest request) {
    return request.contactoPredeterminado() == null ? 0 : request.contactoPredeterminado();
  }

  private static void aplicarDatosJuridicos(PersonajeJuridica juridica, DonanteRequest request) {
    List<Representante> representantes =
        request.representantes() == null
            ? List.copyOf(juridica.getRepresentantes())
            : request.representantes().stream()
                .map(r -> new Representante(r.nombre(), r.apellido()))
                .toList();
    juridica.actualizarDatosJuridicos(
        request.razonSocial(), request.tipoOrganizacion(), request.rubro(), representantes);
  }

  private static boolean vacio(String valor) {
    return valor == null || valor.isBlank();
  }
}
