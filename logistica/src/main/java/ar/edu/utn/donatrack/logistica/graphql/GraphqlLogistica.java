package ar.edu.utn.donatrack.logistica.graphql;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** Arma el motor GraphQL de logística a partir del esquema y los resolvers. */
public final class GraphqlLogistica {
  private static final String ESQUEMA = "/graphql/logistica.graphqls";

  private GraphqlLogistica() {}

  public static GraphQL crear(ResolversLogistica r) {
    TypeDefinitionRegistry tipos = new SchemaParser().parse(leerEsquema());
    RuntimeWiring wiring =
        RuntimeWiring.newRuntimeWiring()
            .type(
                "Query",
                t ->
                    t.dataFetcher("camiones", r.camiones())
                        .dataFetcher("camion", r.camion())
                        .dataFetcher("rutas", r.rutas())
                        .dataFetcher("ruta", r.ruta())
                        .dataFetcher("posicion", r.posicion())
                        .dataFetcher("repartidores", r.repartidores()))
            .type(
                "Mutation",
                t ->
                    t.dataFetcher("crearCamion", r.crearCamion())
                        .dataFetcher("actualizarCamion", r.actualizarCamion())
                        .dataFetcher("eliminarCamion", r.eliminarCamion())
                        .dataFetcher("crearRuta", r.crearRuta())
                        .dataFetcher("eliminarRuta", r.eliminarRuta())
                        .dataFetcher("asignarRepartidor", r.asignarRepartidor())
                        .dataFetcher("iniciarRuta", r.iniciarRuta())
                        .dataFetcher("finalizarRuta", r.finalizarRuta())
                        .dataFetcher("reportarPosicion", r.reportarPosicion()))
            .type(
                "Destino",
                t ->
                    t.dataFetcher("entidadId", ResolversLogistica::entidadId)
                        .dataFetcher("direccion", ResolversLogistica::direccion)
                        .dataFetcher("donaciones", ResolversLogistica::donaciones))
            .build();
    GraphQLSchema esquema = new SchemaGenerator().makeExecutableSchema(tipos, wiring);
    return GraphQL.newGraphQL(esquema)
        .defaultDataFetcherExceptionHandler(new ManejadorErrores())
        .build();
  }

  private static String leerEsquema() {
    try (InputStream in = GraphqlLogistica.class.getResourceAsStream(ESQUEMA)) {
      if (in == null) {
        throw new IllegalStateException("No se encontró el esquema " + ESQUEMA);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
