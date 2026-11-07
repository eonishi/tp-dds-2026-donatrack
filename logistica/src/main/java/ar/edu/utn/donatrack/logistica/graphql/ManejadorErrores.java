package ar.edu.utn.donatrack.logistica.graphql;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Convierte las excepciones de los resolvers en errores GraphQL con un código estable.
 *
 * <p>Las validaciones del dominio ({@link IllegalArgumentException}, {@link IllegalStateException})
 * son datos inválidos; cualquier otra cosa es un error interno y no se expone su detalle.
 */
public class ManejadorErrores implements DataFetcherExceptionHandler {

  @Override
  public CompletableFuture<DataFetcherExceptionHandlerResult> handleException(
      DataFetcherExceptionHandlerParameters parametros) {
    Throwable excepcion = parametros.getException();
    String codigo;
    String mensaje;
    if (excepcion instanceof ErrorLogistica error) {
      codigo = error.getCodigo().name();
      mensaje = error.getMessage();
    } else if (excepcion instanceof IllegalArgumentException
        || excepcion instanceof IllegalStateException) {
      codigo = ErrorLogistica.Codigo.DATOS_INVALIDOS.name();
      mensaje = excepcion.getMessage();
    } else {
      codigo = "ERROR_INTERNO";
      mensaje = "Error interno del servicio de logística.";
    }
    GraphQLError error =
        GraphqlErrorBuilder.newError()
            .message(mensaje)
            .path(parametros.getPath())
            .location(parametros.getSourceLocation())
            .extensions(Map.of("codigo", codigo))
            .build();
    return CompletableFuture.completedFuture(
        DataFetcherExceptionHandlerResult.newResult(error).build());
  }
}
