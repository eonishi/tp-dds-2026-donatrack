package ar.edu.utn.donatrack.api.controller;

import static ar.edu.utn.donatrack.api.mappers.ApiMapper.donante;
import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.put;

import ar.edu.utn.donatrack.api.dto.DonanteDtos.DonanteRequest;
import ar.edu.utn.donatrack.api.dto.DonanteDtos.MedioContactoRequest;
import ar.edu.utn.donatrack.api.error.ApiException;
import ar.edu.utn.donatrack.api.mappers.ApiMapper;
import ar.edu.utn.donatrack.api.validation.DtoValidator;
import ar.edu.utn.donatrack.donaciones.DonacionesRepository;
import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.DonantesRepository;
import ar.edu.utn.donatrack.donantes.MedioDeContacto;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.PersonajeJuridica;
import ar.edu.utn.donatrack.donantes.Representante;
import ar.edu.utn.donatrack.donantes.Sms;
import ar.edu.utn.donatrack.donantes.Whatsapp;
import ar.edu.utn.donatrack.utilidades.TipoPersona;
import io.javalin.http.Context;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class DonanteController {
  private final DonantesRepository donantes;
  private final DonacionesRepository donaciones;

  public DonanteController(DonantesRepository donantes, DonacionesRepository donaciones) {
    this.donantes = donantes;
    this.donaciones = donaciones;
  }

  public void routes() {
    path(
        "/donantes",
        () -> {
          get(this::listar);
          post(this::crear);
          path(
              "/{id}",
              () -> {
                get(this::obtener);
                put(this::actualizar);
                delete(this::eliminar);
              });
        });
  }

  private void listar(Context ctx) {
    ctx.json(donantes.getDonantes().stream().map(ApiMapper::donante).toList());
  }

  private void obtener(Context ctx) {
    ctx.json(donante(buscar(HttpSupport.id(ctx, "id"))));
  }

  private void crear(Context ctx) {
    DonanteRequest request = DtoValidator.validate(ctx.bodyAsClass(DonanteRequest.class));
    validar(request, null);
    ctx.status(201).json(donante(donantes.save(construirDonante(request))));
  }

  private void actualizar(Context ctx) {
    UUID id = HttpSupport.id(ctx, "id");
    Donante actual = buscar(id);
    DonanteRequest request = DtoValidator.validate(ctx.bodyAsClass(DonanteRequest.class));
    validar(request, id);
    if ((actual instanceof PersonaHumana) != (request.tipoPersona() == TipoPersona.HUMANA)) {
      throw ApiException.conflict("No se puede cambiar el tipo de persona del donante.");
    }
    aplicarCambios(actual, request);
    ctx.json(donante(donantes.save(actual)));
  }

  private void eliminar(Context ctx) {
    UUID id = HttpSupport.id(ctx, "id");
    buscar(id);
    if (donaciones.getDonaciones().stream()
        .anyMatch(donacion -> donacion.getDonante().getId().equals(id))) {
      throw ApiException.conflict("No se puede eliminar un donante con donaciones asociadas.");
    }
    donantes.deleteById(id);
    ctx.status(204);
  }

  private Donante buscar(UUID id) {
    return donantes.findById(id).orElseThrow(() -> ApiException.notFound("Donante", id));
  }

  private Donante construirDonante(DonanteRequest request) {
    CorreoElectronico email =
        medios(request).stream()
            .filter(CorreoElectronico.class::isInstance)
            .map(CorreoElectronico.class::cast)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Debe incluir un medio EMAIL."));
    if (request.tipoPersona() == TipoPersona.HUMANA) {
      PersonaHumana persona =
          new PersonaHumana(
              email,
              request.nombre(),
              request.apellido(),
              request.edad(),
              request.numeroDocumento(),
              request.tipoDocumento(),
              request.genero(),
              request.direccion());
      actualizarMediosDeContacto(persona, request);
      return persona;
    }
    PersonajeJuridica persona =
        new PersonajeJuridica(
            email,
            request.numeroDocumento(),
            request.tipoDocumento(),
            request.razonSocial(),
            request.tipoOrganizacion(),
            request.rubro());
    persona.actualizarDatosJuridicos(
        request.razonSocial(),
        request.tipoOrganizacion(),
        request.rubro(),
        representantes(request));
    actualizarMediosDeContacto(persona, request);
    return persona;
  }

  private void aplicarCambios(Donante donante, DonanteRequest request) {
    donante.actualizarIdentificacion(request.numeroDocumento(), request.tipoDocumento());
    actualizarMediosDeContacto(donante, request);
    if (donante instanceof PersonaHumana persona) {
      persona.actualizarDatosPersonales(
          request.nombre(),
          request.apellido(),
          request.edad(),
          request.genero(),
          request.direccion());
    } else {
      ((PersonajeJuridica) donante)
          .actualizarDatosJuridicos(
              request.razonSocial(),
              request.tipoOrganizacion(),
              request.rubro(),
              representantes(request));
    }
  }

  private void validar(DonanteRequest request, UUID actual) {
    boolean existe =
        donantes.getDonantes().stream()
            .anyMatch(
                d ->
                    !d.getId().equals(actual)
                        && d.getDocumento() == request.tipoDocumento()
                        && d.getNumDocumento().equals(request.numeroDocumento()));
    if (existe) {
      throw ApiException.conflict("Ya existe un donante con ese documento.");
    }
    if (request.contactoPredeterminado() != null
        && (request.contactoPredeterminado() < 0
            || request.contactoPredeterminado() >= request.mediosDeContacto().size())) {
      throw new IllegalArgumentException(
          "contactoPredeterminado debe apuntar a un contacto existente.");
    }
    if (request.tipoPersona() == TipoPersona.HUMANA
        && (request.nombre() == null
            || request.nombre().isBlank()
            || request.apellido() == null
            || request.apellido().isBlank()
            || request.edad() == null
            || request.genero() == null
            || request.direccion() == null
            || request.direccion().isBlank())) {
      throw new IllegalArgumentException(
          "Una persona humana requiere nombre, apellido, edad, género y dirección.");
    }
    if (request.tipoPersona() == TipoPersona.JURIDICA
        && (request.razonSocial() == null
            || request.razonSocial().isBlank()
            || request.tipoOrganizacion() == null
            || request.rubro() == null
            || request.rubro().isBlank())) {
      throw new IllegalArgumentException(
          "Una persona jurídica requiere razón social, tipo de organización y rubro.");
    }
  }

  private void actualizarMediosDeContacto(Donante donante, DonanteRequest request) {
    donante.reemplazarMediosDeContacto(
        medios(request),
        request.contactoPredeterminado() == null ? 0 : request.contactoPredeterminado());
  }

  private List<MedioDeContacto> medios(DonanteRequest request) {
    return request.mediosDeContacto().stream().map(this::medio).toList();
  }

  private MedioDeContacto medio(MedioContactoRequest contacto) {
    return switch (contacto.tipo().toUpperCase(Locale.ROOT)) {
      case "EMAIL" -> new CorreoElectronico(contacto.contacto());
      case "WHATSAPP" -> new Whatsapp(contacto.contacto());
      case "SMS" -> new Sms(contacto.contacto());
      default ->
          throw new IllegalArgumentException("tipo de contacto inválido: " + contacto.tipo());
    };
  }

  private List<Representante> representantes(DonanteRequest request) {
    return request.representantes() == null
        ? List.of()
        : request.representantes().stream()
            .map(r -> new Representante(r.nombre(), r.apellido()))
            .toList();
  }
}
