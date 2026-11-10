package ar.edu.utn.donatrack.importacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.MedioDeContacto;
import ar.edu.utn.donatrack.security.AuthService;
import ar.edu.utn.donatrack.security.Usuario;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** El enunciado pide importar archivos de más de 10.000 filas. */
class ImportadorDonantesTest implements WithSimplePersistenceUnit {

  @AfterEach
  void limpiar() {
    withTransaction(
        () -> {
          createQuery(
                  "select u from Usuario u where u.email not like '%@donatrack.local'",
                  Usuario.class)
              .getResultList()
              .forEach(this::remove);
          createQuery("select d from Donante d", Donante.class)
              .getResultList()
              .forEach(this::remove);
          createQuery("select m from MedioDeContacto m", MedioDeContacto.class)
              .getResultList()
              .forEach(this::remove);
        });
  }

  @Test
  void importaMasDeDiezMilFilasEnLotesYEnPocoTiempo() throws Exception {
    int filas = 10_500;
    StringBuilder csv =
        new StringBuilder("﻿TipoPersona,TipoDoc,Documento,Nombre/Razón Social,Email,Teléfono\n");
    for (int i = 0; i < filas; i++) {
      csv.append("HUMANA,DNI,")
          .append(40_000_000 + i)
          .append(",Persona ")
          .append(i)
          .append(",persona")
          .append(i)
          .append("@example.org,\n");
    }
    ImportadorDonantes importador = new ImportadorDonantes(new AuthService(), Clock.systemUTC());
    Importacion importacion = new Importacion("grande.csv", LocalDateTime.now());

    long inicio = System.nanoTime();
    importador.importar(
        new ByteArrayInputStream(csv.toString().getBytes(StandardCharsets.UTF_8)), importacion);
    Duration duracion = Duration.ofNanos(System.nanoTime() - inicio);

    assertTrue(importacion.isTerminada());
    assertEquals(filas, importacion.getProcesadas());
    assertEquals(filas, importacion.getDonantesNuevos());
    assertEquals(filas, importacion.getCuentasCreadas());
    assertEquals(0, importacion.getFilasConError(), importacion.getErrores().toString());
    assertTrue(duracion.toSeconds() < 60, "tardó " + duracion);
    importador.detener();
  }
}
