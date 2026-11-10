package ar.edu.utn.donatrack.utilidades;

import java.util.Optional;

/** El texto que se escribe en un buscador, pasado a patrón LIKE para las consultas JPQL. */
public final class Busqueda {
  private Busqueda() {}

  /**
   * " Sofía " → "%sofía%". Vacío si no hay nada que buscar. Se quitan los comodines de LIKE ({@code
   * %} y {@code _}) para que quien busca no pueda armar patrones propios.
   */
  public static Optional<String> patron(String texto) {
    if (texto == null || texto.isBlank()) {
      return Optional.empty();
    }
    String limpio = texto.trim().toLowerCase().replace("%", "").replace("_", "");
    return limpio.isEmpty() ? Optional.empty() : Optional.of("%" + limpio + "%");
  }
}
