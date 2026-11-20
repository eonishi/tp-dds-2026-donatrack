package ar.edu.utn.donatrack.api.controller;

import static ar.edu.utn.donatrack.api.mappers.ApiMapper.donante;
import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.put;

import ar.edu.utn.donatrack.api.dto.DonanteDtos.DonanteRequest;
import ar.edu.utn.donatrack.api.dto.DonanteDtos.DonanteResponse;
import ar.edu.utn.donatrack.api.error.ApiException;
import ar.edu.utn.donatrack.api.mappers.ApiMapper;
import ar.edu.utn.donatrack.api.mappers.DonanteMapper;
import ar.edu.utn.donatrack.api.validation.DtoValidator;
import ar.edu.utn.donatrack.donaciones.DonacionesRepository;
import ar.edu.utn.donatrack.donantes.DocumentoEnUso;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.DonantesRepository;
import ar.edu.utn.donatrack.notificaciones.PublicadorDiferido;
import ar.edu.utn.donatrack.notificaciones.ServicioNotificaciones;
import ar.edu.utn.donatrack.notificaciones.eventos.EventoBienvenida;
import ar.edu.utn.donatrack.security.Role;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.http.Context;
import io.javalin.security.RouteRole;
import java.util.UUID;

public final class DonanteController implements WithSimplePersistenceUnit {
  private final DonantesRepository donantes;
  private final DonacionesRepository donaciones;
  private final ServicioNotificaciones notificaciones;

  public DonanteController(
      DonantesRepository donantes,
      DonacionesRepository donaciones,
      ServicioNotificaciones notificaciones) {
    this.donantes = donantes;
    this.donaciones = donaciones;
    this.notificaciones = notificaciones;
  }

  private static final RouteRole[] ADMIN_O_DONANTE = {Role.ADMIN, Role.DONANTE};

  public void routes() {
    path(
        "/donantes",
        () -> {
          get(this::listar, Role.ADMIN);
          post(this::crear, ADMIN_O_DONANTE);
          path(
              "/{id}",
              () -> {
                get(this::obtener, ADMIN_O_DONANTE);
                put(this::actualizar, ADMIN_O_DONANTE);
                delete(this::eliminar, ADMIN_O_DONANTE);
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
    Donante nuevo = DonanteMapper.nuevo(request);
    exigirDocumentoLibre(request, null);
    PublicadorDiferido publicador = notificaciones.publicadorDiferido();
    DonanteResponse creado =
        withTransaction(
            () -> {
              Donante donante = donantes.save(nuevo);
              publicador.publicar(new EventoBienvenida(donante));
              return donante(donante);
            });
    publicador.enviarPendientes();
    ctx.status(201).json(creado);
  }

  private void actualizar(Context ctx) {
    UUID id = HttpSupport.id(ctx, "id");
    Donante actual = buscar(id);
    DonanteRequest request = DtoValidator.validate(ctx.bodyAsClass(DonanteRequest.class));
    if (!DonanteMapper.mismoTipo(actual, request)) {
      throw ApiException.conflict("No se puede cambiar el tipo de persona del donante.");
    }
    exigirDocumentoLibre(request, id);
    DonanteMapper.aplicar(actual, request);
    DonanteResponse actualizado = withTransaction(() -> donante(donantes.save(actual)));
    ctx.json(actualizado);
  }

  private void eliminar(Context ctx) {
    UUID id = HttpSupport.id(ctx, "id");
    buscar(id);
    if (donaciones.tieneDonacionesDe(id)) {
      throw ApiException.conflict("No se puede eliminar un donante con donaciones asociadas.");
    }
    withTransaction(() -> donantes.deleteById(id));
    ctx.status(204);
  }

  private Donante buscar(UUID id) {
    return donantes.findById(id).orElseThrow(() -> ApiException.notFound("Donante", id));
  }

  private void exigirDocumentoLibre(DonanteRequest request, UUID salvo) {
    if (donantes.documentoEnUso(request.tipoDocumento(), request.numeroDocumento(), salvo)) {
      throw new DocumentoEnUso();
    }
  }
}
