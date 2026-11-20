package ar.edu.utn.donatrack.api.mappers;

import ar.edu.utn.donatrack.api.dto.DonanteDtos.DonanteRequest;
import ar.edu.utn.donatrack.api.dto.DonanteDtos.MedioContactoRequest;
import ar.edu.utn.donatrack.donantes.Canal;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.MedioDeContacto;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.PersonajeJuridica;
import ar.edu.utn.donatrack.donantes.Representante;
import ar.edu.utn.donatrack.donantes.TipoPersona;
import java.util.List;
import java.util.Locale;

/**
 * De un {@link DonanteRequest} a una persona donante: el alta y la edición que comparten la API y
 * los formularios web (que primero pasan sus campos a un request).
 *
 * <p>Acá solo se exige lo que pide el alta (nombre, género, rubro...), que un donante importado por
 * CSV puede no tener. Las reglas que valen siempre (correo obligatorio, forma del documento y del
 * teléfono, edad) son del dominio. Un dato inválido es una {@link IllegalArgumentException} con un
 * mensaje para mostrar.
 *
 * <p>Que el documento no sea de otra persona no se ve acá: necesita la base (ver {@code
 * DonantesRepository.documentoEnUso}).
 */
public final class DonanteMapper {
  private DonanteMapper() {}

  /** Una persona donante nueva, sin guardar. */
  public static Donante nuevo(DonanteRequest request) {
    return armar(request, List.of());
  }

  /** Si el request es del mismo tipo de persona (humana o jurídica) que el donante. */
  public static boolean mismoTipo(Donante actual, DonanteRequest request) {
    return (actual instanceof PersonaHumana) == (request.tipoPersona() == TipoPersona.HUMANA);
  }

  /**
   * Pasa el request a una persona donante que ya existe. No cambia el tipo de persona. Si el
   * request no trae representantes ({@code null}), se conservan los que tenía. Con un dato inválido
   * no cambia nada: primero se arma un donante con los datos nuevos, y solo si sale bien se copian.
   */
  public static void aplicar(Donante actual, DonanteRequest request) {
    List<Representante> representantes =
        actual instanceof PersonajeJuridica juridica ? juridica.getRepresentantes() : List.of();
    actual.actualizarCon(armar(request, representantes));
  }

  /** El tipo de medio tal como lo nombra la API: EMAIL, SMS o WHATSAPP. */
  public static String tipoDeMedio(Canal canal) {
    return canal == Canal.CORREO ? "EMAIL" : canal.name();
  }

  private static Donante armar(DonanteRequest request, List<Representante> siNoVienen) {
    exigirDatosDelAlta(request);
    List<MedioDeContacto> medios = medios(request);
    MedioDeContacto correo =
        medios.stream().filter(MedioDeContacto::esCorreo).findFirst().orElse(null);
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
      PersonajeJuridica juridica =
          new PersonajeJuridica(
              correo,
              request.numeroDocumento(),
              request.tipoDocumento(),
              request.razonSocial(),
              request.tipoOrganizacion(),
              request.rubro());
      representantes(request, siNoVienen).forEach(juridica::agregarRepresentante);
      donante = juridica;
    }
    donante.reemplazarMediosDeContacto(medios, predeterminado(request));
    return donante;
  }

  private static void exigirDatosDelAlta(DonanteRequest request) {
    if (request.tipoPersona() == null) {
      throw new IllegalArgumentException("Falta el tipo de persona.");
    }
    if (request.tipoPersona() == TipoPersona.HUMANA
        && (vacio(request.nombre())
            || vacio(request.apellido())
            || request.genero() == null
            || vacio(request.direccion()))) {
      throw new IllegalArgumentException(
          "Una persona humana requiere nombre, apellido, género y dirección.");
    }
    if (request.tipoPersona() == TipoPersona.JURIDICA
        && (vacio(request.razonSocial())
            || request.tipoOrganizacion() == null
            || vacio(request.rubro()))) {
      throw new IllegalArgumentException(
          "Una persona jurídica requiere razón social, tipo de organización y rubro.");
    }
  }

  private static List<MedioDeContacto> medios(DonanteRequest request) {
    if (request.mediosDeContacto() == null) {
      return List.of();
    }
    return request.mediosDeContacto().stream().map(DonanteMapper::medio).toList();
  }

  private static MedioDeContacto medio(MedioContactoRequest contacto) {
    String tipo = contacto.tipo() == null ? "" : contacto.tipo().toUpperCase(Locale.ROOT);
    return switch (tipo) {
      case "EMAIL" -> MedioDeContacto.correo(contacto.contacto());
      case "SMS" -> MedioDeContacto.sms(contacto.contacto());
      case "WHATSAPP" -> MedioDeContacto.whatsapp(contacto.contacto());
      default -> throw new IllegalArgumentException("Tipo de contacto inválido: " + tipo + ".");
    };
  }

  private static int predeterminado(DonanteRequest request) {
    return request.contactoPredeterminado() == null ? 0 : request.contactoPredeterminado();
  }

  private static List<Representante> representantes(
      DonanteRequest request, List<Representante> siNoVienen) {
    if (request.representantes() == null) {
      return siNoVienen;
    }
    return request.representantes().stream()
        .map(r -> new Representante(r.nombre(), r.apellido()))
        .toList();
  }

  private static boolean vacio(String valor) {
    return valor == null || valor.isBlank();
  }
}
