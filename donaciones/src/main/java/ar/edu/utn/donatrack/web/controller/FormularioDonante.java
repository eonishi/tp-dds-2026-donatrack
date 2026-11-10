package ar.edu.utn.donatrack.web.controller;

import static ar.edu.utn.donatrack.web.controller.Formularios.enumerado;
import static ar.edu.utn.donatrack.web.controller.Formularios.obligatorio;

import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.DonantesRepository;
import ar.edu.utn.donatrack.donantes.Genero;
import ar.edu.utn.donatrack.donantes.MedioDeContacto;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.PersonajeJuridica;
import ar.edu.utn.donatrack.donantes.Sms;
import ar.edu.utn.donatrack.donantes.TipoDocumento;
import ar.edu.utn.donatrack.donantes.TipoOrganizacion;
import ar.edu.utn.donatrack.donantes.Whatsapp;
import ar.edu.utn.donatrack.web.controller.Formularios.DatoInvalido;
import io.javalin.http.Context;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * El formulario de persona donante (humana o jurídica), campos {@code donante*}. Lo comparten el
 * registro público y el alta y la edición desde la administración, así las tres pantallas validan
 * lo mismo. No persiste nada: eso lo decide quien lo llama.
 */
final class FormularioDonante {
  private FormularioDonante() {}

  /** Una persona donante nueva. Su documento no puede estar ya registrado. */
  static Donante armar(Context ctx, String correo, DonantesRepository donantes) {
    Donante nuevo = construir(ctx, correo);
    exigirDocumentoLibre(nuevo, donantes);
    return nuevo;
  }

  /**
   * Pasa lo cargado en el formulario a una persona donante que ya existe. No se puede cambiar el
   * tipo de persona, y el documento nuevo no puede ser de otra persona.
   */
  static void aplicar(Donante actual, Context ctx, String correo, DonantesRepository donantes) {
    Donante datos = construir(ctx, correo);
    if (datos.getClass() != actual.getClass()) {
      throw new DatoInvalido("No se puede cambiar el tipo de persona.");
    }
    boolean cambiaDocumento =
        datos.getDocumento() != actual.getDocumento()
            || !datos.getNumDocumento().equals(actual.getNumDocumento());
    if (cambiaDocumento) {
      exigirDocumentoLibre(datos, donantes);
    }
    actual.actualizarIdentificacion(datos.getNumDocumento(), datos.getDocumento());
    if (actual instanceof PersonaHumana humana && datos instanceof PersonaHumana nueva) {
      humana.actualizarDatosPersonales(
          nueva.getNombre(), nueva.getApellido(), nueva.getGenero(), nueva.getDireccion());
      humana.setEdad(nueva.getEdad());
    } else if (actual instanceof PersonajeJuridica juridica
        && datos instanceof PersonajeJuridica nueva) {
      // Los representantes no están en el formulario: se conservan los que tenía.
      juridica.actualizarDatosJuridicos(
          nueva.getRazonSocial(),
          nueva.getTipoOrganizacion(),
          nueva.getRubro(),
          List.copyOf(juridica.getRepresentantes()));
    }
    actual.reemplazarMediosDeContacto(
        datos.getMediosDeContacto(),
        datos.getMediosDeContacto().indexOf(datos.getMedioContactoPredeterminado()));
  }

  /**
   * Lo inverso de {@link #aplicar}: los campos del formulario con los datos de una persona donante,
   * para abrir la edición ya completa.
   */
  static Map<String, String> valores(Donante donante) {
    Map<String, String> campos = new HashMap<>();
    donante.extraerEmail().ifPresent(correo -> campos.put("correo", correo));
    if (donante instanceof PersonajeJuridica juridica) {
      campos.put("donanteTipoPersona", "JURIDICA");
      campos.put("donanteCuit", juridica.getNumDocumento());
      campos.put("donanteRazonSocial", juridica.getRazonSocial());
      campos.put("donanteTipoOrganizacion", nombre(juridica.getTipoOrganizacion()));
      campos.put("donanteRubro", juridica.getRubro());
    } else if (donante instanceof PersonaHumana humana) {
      campos.put("donanteTipoPersona", "HUMANA");
      campos.put("donanteNombre", humana.getNombre());
      campos.put("donanteApellido", humana.getApellido());
      campos.put("donanteEdad", humana.getEdad() == null ? "" : humana.getEdad().toString());
      campos.put("donanteTipoDocumento", nombre(humana.getDocumento()));
      campos.put("donanteNumeroDocumento", humana.getNumDocumento());
      campos.put("donanteGenero", nombre(humana.getGenero()));
      campos.put("donanteDireccion", humana.getDireccion());
    }
    MedioDeContacto predeterminado = donante.getMedioContactoPredeterminado();
    campos.put(
        "donanteMedioContacto",
        predeterminado instanceof Whatsapp
            ? "WHATSAPP"
            : predeterminado instanceof Sms ? "SMS" : "CORREO");
    donante.getMediosDeContacto().stream()
        .filter(m -> m instanceof Sms || m instanceof Whatsapp)
        .findFirst()
        .ifPresent(m -> campos.put("donanteTelefono", m.getContacto()));
    return campos;
  }

  private static String nombre(Enum<?> valor) {
    return valor == null ? "" : valor.name();
  }

  private static void exigirDocumentoLibre(Donante donante, DonantesRepository donantes) {
    if (donantes.existeDocumento(donante.getDocumento(), donante.getNumDocumento())) {
      throw new DatoInvalido("Ya hay una persona donante registrada con ese documento.");
    }
  }

  /** Lee y valida el formulario, y arma con él una persona donante sin guardar. */
  private static Donante construir(Context ctx, String correo) {
    String tipoPersona = obligatorio(ctx, "donanteTipoPersona", "el tipo de persona");
    if (!Set.of("HUMANA", "JURIDICA").contains(tipoPersona)) {
      throw new DatoInvalido("Elegí el tipo de persona.");
    }
    boolean juridica = "JURIDICA".equals(tipoPersona);
    TipoDocumento tipoDocumento =
        juridica
            ? TipoDocumento.CUIT
            : enumerado(
                TipoDocumento.class, ctx.formParam("donanteTipoDocumento"), "el tipo de documento");
    String numeroDocumento =
        obligatorio(
                ctx,
                juridica ? "donanteCuit" : "donanteNumeroDocumento",
                juridica ? "el CUIT" : "el número de documento")
            .trim();
    if (juridica) {
      numeroDocumento = numeroDocumento.replace("-", "");
      if (!numeroDocumento.matches("[0-9]{11}")) {
        throw new DatoInvalido("El CUIT debe tener 11 dígitos.");
      }
    }

    Donante persona;
    if (juridica) {
      persona =
          new PersonajeJuridica(
              new CorreoElectronico(correo),
              numeroDocumento,
              TipoDocumento.CUIT,
              obligatorio(ctx, "donanteRazonSocial", "la razón social").trim(),
              enumerado(
                  TipoOrganizacion.class,
                  ctx.formParam("donanteTipoOrganizacion"),
                  "el tipo de organización"),
              obligatorio(ctx, "donanteRubro", "el rubro").trim());
    } else {
      PersonaHumana humana =
          new PersonaHumana(
              new CorreoElectronico(correo),
              obligatorio(ctx, "donanteNombre", "el nombre").trim(),
              obligatorio(ctx, "donanteApellido", "el apellido").trim(),
              numeroDocumento,
              tipoDocumento,
              enumerado(Genero.class, ctx.formParam("donanteGenero"), "el género"),
              obligatorio(ctx, "donanteDireccion", "la dirección").trim());
      try {
        humana.setEdad(Integer.parseInt(obligatorio(ctx, "donanteEdad", "la edad").trim()));
      } catch (IllegalArgumentException e) {
        throw new DatoInvalido("La edad debe ser un número entero entre 0 y 130.");
      }
      persona = humana;
    }
    aplicarMediosDeContacto(persona, ctx, correo);
    return persona;
  }

  // El correo siempre es un medio de contacto; el teléfono, si se cargó, va por SMS o WhatsApp.
  private static void aplicarMediosDeContacto(Donante persona, Context ctx, String correo) {
    String telefono = ctx.formParam("donanteTelefono");
    String preferido = obligatorio(ctx, "donanteMedioContacto", "el medio de contacto preferido");
    if (!Set.of("CORREO", "SMS", "WHATSAPP").contains(preferido)) {
      throw new DatoInvalido("Elegí un medio de contacto válido.");
    }
    List<MedioDeContacto> medios = new ArrayList<>();
    medios.add(new CorreoElectronico(correo));
    boolean conTelefono = telefono != null && !telefono.isBlank();
    if (conTelefono) {
      telefono = telefono.trim();
      if (!telefono.matches("\\+[1-9][0-9]{1,14}")) {
        throw new DatoInvalido(
            "El teléfono debe usar formato internacional E.164, por ejemplo +5491155555555.");
      }
      medios.add("WHATSAPP".equals(preferido) ? new Whatsapp(telefono) : new Sms(telefono));
    } else if ("SMS".equals(preferido) || "WHATSAPP".equals(preferido)) {
      throw new DatoInvalido("Para recibir avisos por SMS o WhatsApp cargá un teléfono.");
    }
    boolean prefiereTelefono = "SMS".equals(preferido) || "WHATSAPP".equals(preferido);
    persona.reemplazarMediosDeContacto(medios, prefiereTelefono ? 1 : 0);
  }
}
