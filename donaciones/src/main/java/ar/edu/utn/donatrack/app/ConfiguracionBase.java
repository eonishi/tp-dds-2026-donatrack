package ar.edu.utn.donatrack.app;

import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.flywaydb.core.Flyway;

/**
 * Permite pisar la conexión del persistence.xml con variables de entorno, para que las credenciales
 * (Supabase en docker compose o en el despliegue) no queden en el repo ni en la imagen.
 *
 * <p>{@code DB_URL}, {@code DB_USER} y {@code DB_PASSWORD} reemplazan la conexión; {@code
 * DB_SCHEMA} fija el esquema propio del servicio (flyway lo crea y migra al arrancar). Si {@code
 * DB_URL} no está definida no se toca nada y vale el persistence.xml local de siempre.
 */
public final class ConfiguracionBase {
  private static final Pattern NOMBRE_ESQUEMA = Pattern.compile("[a-z_][a-z0-9_]*");

  private ConfiguracionBase() {}

  public static void desdeEntorno() {
    Map<String, String> entorno = System.getenv();
    Map<String, String> propiedades = propiedades(entorno);
    if (propiedades.isEmpty()) {
      return;
    }
    String esquema = propiedades.get("hibernate.default_schema");
    if (esquema != null) {
      migrar(entorno, esquema);
    }
    WithSimplePersistenceUnit.configure(config -> config.putAll(propiedades));
  }

  /*
  deja el esquema con la última versión de db/migration antes de que arranque hibernate,
  asi flyway arranca el esquema en caso de que no exista, anota los scripts que ya corrió
  y corre solo los nuevos
  */
  private static void migrar(Map<String, String> entorno, String esquema) {
    Flyway.configure()
        .dataSource(entorno.get("DB_URL"), entorno.get("DB_USER"), entorno.get("DB_PASSWORD"))
        .schemas(esquema)
        .load()
        .migrate();
  }

  static Map<String, String> propiedades(Map<String, String> entorno) {
    Map<String, String> propiedades = new HashMap<>();
    String url = entorno.get("DB_URL");
    if (url == null || url.isBlank()) {
      return propiedades;
    }
    propiedades.put("javax.persistence.jdbc.url", url);
    copiar(entorno, "DB_USER", propiedades, "javax.persistence.jdbc.user");
    copiar(entorno, "DB_PASSWORD", propiedades, "javax.persistence.jdbc.password");
    String esquema = entorno.get("DB_SCHEMA");
    if (esquema != null && !esquema.isBlank()) {
      if (!NOMBRE_ESQUEMA.matcher(esquema).matches()) {
        throw new IllegalArgumentException("DB_SCHEMA inválido: " + esquema);
      }
      propiedades.put("hibernate.default_schema", esquema);
    }
    return propiedades;
  }

  private static void copiar(
      Map<String, String> entorno, String variable, Map<String, String> destino, String clave) {
    String valor = entorno.get(variable);
    if (valor != null) {
      destino.put(clave, valor);
    }
  }
}
