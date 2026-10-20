package ar.edu.utn.donatrack.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ConfiguracionBaseTest {

  @Test
  void sinDbUrlNoPisaElPersistenceXml() {
    assertTrue(ConfiguracionBase.propiedades(Map.of("DB_USER", "postgres")).isEmpty());
  }

  @Test
  void conDbUrlPisaLaConexion() {
    Map<String, String> propiedades =
        ConfiguracionBase.propiedades(
            Map.of(
                "DB_URL", "jdbc:postgresql://host:5432/postgres",
                "DB_USER", "usuario",
                "DB_PASSWORD", "secreta"));

    assertEquals(
        "jdbc:postgresql://host:5432/postgres", propiedades.get("javax.persistence.jdbc.url"));
    assertEquals("usuario", propiedades.get("javax.persistence.jdbc.user"));
    assertEquals("secreta", propiedades.get("javax.persistence.jdbc.password"));
    assertFalse(propiedades.containsKey("hibernate.default_schema"));
  }

  @Test
  void elEsquemaDelServicioSeUsaPorDefecto() {
    Map<String, String> propiedades =
        ConfiguracionBase.propiedades(
            Map.of("DB_URL", "jdbc:postgresql://host:5432/postgres", "DB_SCHEMA", "donaciones"));

    assertEquals("donaciones", propiedades.get("hibernate.default_schema"));
  }

  @Test
  void rechazaUnEsquemaQueNoEsUnIdentificadorSimple() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ConfiguracionBase.propiedades(
                Map.of("DB_URL", "jdbc:postgresql://host/postgres", "DB_SCHEMA", "x; drop")));
  }
}
