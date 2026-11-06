package ar.edu.utn.donatrack.web;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import io.javalin.http.Context;
import io.javalin.rendering.FileRenderer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * Motor de plantillas de las vistas: resuelve {@code ctx.render("index", modelo)} contra {@code
 * resources/templates/index.hbs}.
 *
 * <p>Las páginas heredan de {@code layout.hbs} con los helpers {@code partial}/{@code block} de
 * Handlebars.java: cada página define sus {@code partial} y al final incluye {@code {{> layout}}}.
 */
public final class HandlebarsRenderer implements FileRenderer {
  private final Handlebars handlebars =
      new Handlebars(new ClassPathTemplateLoader("/templates", ".hbs"));

  @Override
  public String render(String vista, Map<String, ?> modelo, Context ctx) {
    try {
      return handlebars.compile(vista).apply(modelo);
    } catch (IOException e) {
      throw new UncheckedIOException("No se pudo renderizar la vista " + vista, e);
    }
  }
}
