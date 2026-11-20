package ar.edu.utn.donatrack.web.controller;

import static ar.edu.utn.donatrack.web.controller.Formularios.enumerado;
import static ar.edu.utn.donatrack.web.controller.Formularios.obligatorio;

import ar.edu.utn.donatrack.api.dto.DonanteDtos.DonanteRequest;
import ar.edu.utn.donatrack.api.dto.DonanteDtos.DonanteResponse;
import ar.edu.utn.donatrack.api.dto.DonanteDtos.MedioContactoRequest;
import ar.edu.utn.donatrack.api.dto.DonanteDtos.MedioContactoResponse;
import ar.edu.utn.donatrack.api.mappers.ApiMapper;
import ar.edu.utn.donatrack.api.mappers.DonanteMapper;
import ar.edu.utn.donatrack.donantes.DocumentoEnUso;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.DonantesRepository;
import ar.edu.utn.donatrack.donantes.Genero;
import ar.edu.utn.donatrack.donantes.TipoDocumento;
import ar.edu.utn.donatrack.donantes.TipoOrganizacion;
import ar.edu.utn.donatrack.donantes.TipoPersona;
import ar.edu.utn.donatrack.web.controller.Formularios.DatoInvalido;
import io.javalin.http.Context;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * El formulario de persona donante (humana o jurídica), campos {@code donante*}. Lo comparten el
 * registro público y el alta y la edición desde la administración.
 *
 * <p>Solo traduce: los campos del formulario pasan a un {@link DonanteRequest}, y del alta y la
 * edición se encarga {@link DonanteMapper}, el mismo que usa la API. Para abrir la edición, los
 * datos salen de {@link ApiMapper#donante}. No persiste nada: eso lo decide quien lo llama.
 */
final class FormularioDonante {
  private FormularioDonante() {}

  /** Una persona donante nueva. Su documento no puede estar ya registrado. */
  static Donante armar(Context ctx, String correo, DonantesRepository donantes) {
    DonanteRequest request = leer(ctx, correo);
    Donante nuevo = comoDatoInvalido(() -> DonanteMapper.nuevo(request));
    exigirDocumentoLibre(request, null, donantes);
    return nuevo;
  }

  /**
   * Pasa lo cargado en el formulario a una persona donante que ya existe. No se puede cambiar el
   * tipo de persona, y el documento nuevo no puede ser de otra persona. Los representantes no están
   * en el formulario: se conservan los que tenía.
   */
  static void aplicar(Donante actual, Context ctx, String correo, DonantesRepository donantes) {
    DonanteRequest request = leer(ctx, correo);
    if (!DonanteMapper.mismoTipo(actual, request)) {
      throw new DatoInvalido("No se puede cambiar el tipo de persona.");
    }
    exigirDocumentoLibre(request, actual, donantes);
    comoDatoInvalido(
        () -> {
          DonanteMapper.aplicar(actual, request);
          return actual;
        });
  }

  /**
   * Lo inverso de {@link #leer}: los campos del formulario con los datos de una persona donante,
   * para abrir la edición ya completa.
   */
  static Map<String, String> valores(Donante donante) {
    DonanteResponse datos = ApiMapper.donante(donante);
    Map<String, String> campos = new HashMap<>();
    campos.put("donanteTipoPersona", datos.tipoPersona().name());
    if (datos.tipoPersona() == TipoPersona.JURIDICA) {
      campos.put("donanteCuit", datos.numeroDocumento());
      campos.put("donanteRazonSocial", datos.razonSocial());
      campos.put("donanteTipoOrganizacion", nombre(datos.tipoOrganizacion()));
      campos.put("donanteRubro", datos.rubro());
    } else {
      campos.put("donanteNombre", datos.nombre());
      campos.put("donanteApellido", datos.apellido());
      campos.put("donanteEdad", datos.edad() == null ? "" : datos.edad().toString());
      campos.put("donanteTipoDocumento", nombre(datos.tipoDocumento()));
      campos.put("donanteNumeroDocumento", datos.numeroDocumento());
      campos.put("donanteGenero", nombre(datos.genero()));
      campos.put("donanteDireccion", datos.direccion());
    }
    for (MedioContactoResponse medio : datos.mediosDeContacto()) {
      if ("EMAIL".equals(medio.tipo())) {
        campos.putIfAbsent("correo", medio.contacto());
      } else {
        campos.putIfAbsent("donanteTelefono", medio.contacto());
      }
      if (medio.predeterminado()) {
        campos.put("donanteMedioContacto", "EMAIL".equals(medio.tipo()) ? "CORREO" : medio.tipo());
      }
    }
    return campos;
  }

  /**
   * Lee los campos del formulario y los pasa a un request. Acá solo se valida lo propio del
   * formulario (que los campos estén y tengan forma); las reglas del donante son de {@link
   * DonanteMapper}.
   */
  private static DonanteRequest leer(Context ctx, String correo) {
    String tipoPersona = obligatorio(ctx, "donanteTipoPersona", "el tipo de persona");
    if (!Set.of("HUMANA", "JURIDICA").contains(tipoPersona)) {
      throw new DatoInvalido("Elegí el tipo de persona.");
    }
    List<MedioContactoRequest> medios = new ArrayList<>();
    int predeterminado = leerMediosDeContacto(ctx, correo, medios);

    if ("JURIDICA".equals(tipoPersona)) {
      // El CUIT se puede escribir con guiones (30-12345678-9): el dominio los saca.
      return new DonanteRequest(
          TipoPersona.JURIDICA,
          obligatorio(ctx, "donanteCuit", "el CUIT"),
          TipoDocumento.CUIT,
          medios,
          predeterminado,
          null,
          null,
          null,
          null,
          null,
          obligatorio(ctx, "donanteRazonSocial", "la razón social").trim(),
          enumerado(
              TipoOrganizacion.class,
              ctx.formParam("donanteTipoOrganizacion"),
              "el tipo de organización"),
          obligatorio(ctx, "donanteRubro", "el rubro").trim(),
          null);
    }
    return new DonanteRequest(
        TipoPersona.HUMANA,
        obligatorio(ctx, "donanteNumeroDocumento", "el número de documento"),
        enumerado(
            TipoDocumento.class, ctx.formParam("donanteTipoDocumento"), "el tipo de documento"),
        medios,
        predeterminado,
        obligatorio(ctx, "donanteNombre", "el nombre").trim(),
        obligatorio(ctx, "donanteApellido", "el apellido").trim(),
        edad(ctx),
        enumerado(Genero.class, ctx.formParam("donanteGenero"), "el género"),
        obligatorio(ctx, "donanteDireccion", "la dirección").trim(),
        null,
        null,
        null,
        null);
  }

  /**
   * El correo siempre es un medio de contacto; el teléfono, si se cargó, va por SMS o WhatsApp
   * según el preferido. Devuelve la posición del medio preferido.
   */
  private static int leerMediosDeContacto(
      Context ctx, String correo, List<MedioContactoRequest> medios) {
    String preferido = obligatorio(ctx, "donanteMedioContacto", "el medio de contacto preferido");
    if (!Set.of("CORREO", "SMS", "WHATSAPP").contains(preferido)) {
      throw new DatoInvalido("Elegí un medio de contacto válido.");
    }
    boolean prefiereTelefono = !"CORREO".equals(preferido);
    medios.add(new MedioContactoRequest("EMAIL", correo));
    String telefono = ctx.formParam("donanteTelefono");
    if (telefono != null && !telefono.isBlank()) {
      medios.add(new MedioContactoRequest(prefiereTelefono ? preferido : "SMS", telefono.trim()));
    } else if (prefiereTelefono) {
      throw new DatoInvalido("Para recibir avisos por SMS o WhatsApp cargá un teléfono.");
    }
    return prefiereTelefono ? 1 : 0;
  }

  private static Integer edad(Context ctx) {
    try {
      return Integer.parseInt(obligatorio(ctx, "donanteEdad", "la edad").trim());
    } catch (NumberFormatException e) {
      throw new DatoInvalido("La edad debe ser un número entero entre 0 y 130.");
    }
  }

  private static void exigirDocumentoLibre(
      DonanteRequest request, Donante actual, DonantesRepository donantes) {
    boolean enUso =
        comoDatoInvalido(
            () ->
                donantes.documentoEnUso(
                    request.tipoDocumento(),
                    request.numeroDocumento(),
                    actual == null ? null : actual.getId()));
    if (enUso) {
      throw new DocumentoEnUso();
    }
  }

  /** Las reglas del donante avisan con IllegalArgumentException; la pantalla, con DatoInvalido. */
  private static <T> T comoDatoInvalido(Supplier<T> paso) {
    try {
      return paso.get();
    } catch (IllegalArgumentException e) {
      throw new DatoInvalido(e.getMessage());
    }
  }

  private static String nombre(Enum<?> valor) {
    return valor == null ? "" : valor.name();
  }
}
