package ar.edu.utn.donatrack.logistica.graphql;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import io.javalin.http.Context;
import java.util.List;
import java.util.Map;

/**
 * {@code POST /logistica/graphql}: recibe {@code {query, operationName, variables}} y responde el
 * resultado según la especificación de GraphQL ({@code data} y {@code errors}).
 *
 * <p>Los errores de negocio vuelven con 200 dentro de {@code errors}, como indica GraphQL. Las
 * credenciales ya las validó el filtro {@code LogisticaSecurity.autorizar} (401/403).
 */
public class GraphqlController {
  private final GraphQL graphql;

  public GraphqlController(GraphQL graphql) {
    this.graphql = graphql;
  }

  public void ejecutar(Context ctx) {
    Pedido pedido;
    try {
      pedido = ctx.bodyAsClass(Pedido.class);
    } catch (Exception e) {
      pedido = null;
    }
    if (pedido == null || pedido.query == null || pedido.query.isBlank()) {
      ctx.status(400)
          .json(
              Map.of(
                  "errors",
                  List.of(Map.of("message", "El cuerpo debe ser JSON con un campo query."))));
      return;
    }
    ExecutionInput entrada =
        ExecutionInput.newExecutionInput()
            .query(pedido.query)
            .operationName(pedido.operationName)
            .variables(pedido.variables == null ? Map.of() : pedido.variables)
            // Los resolvers leen de acá quién hace el pedido (ver Acceso).
            .graphQLContext(Map.of(Context.class, ctx))
            .build();
    ExecutionResult resultado = graphql.execute(entrada);
    ctx.json(resultado.toSpecification());
  }

  /** Cuerpo estándar de un pedido GraphQL por HTTP. */
  public static class Pedido {
    public String query;
    public String operationName;
    public Map<String, Object> variables;
  }
}
