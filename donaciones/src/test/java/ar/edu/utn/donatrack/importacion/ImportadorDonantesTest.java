package ar.edu.utn.donatrack.importacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.MedioDeContacto;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.PersonajeJuridica;
import ar.edu.utn.donatrack.donantes.Sms;
import ar.edu.utn.donatrack.donantes.TipoDocumento;
import ar.edu.utn.donatrack.security.Role;
import ar.edu.utn.donatrack.security.AuthService;
import ar.edu.utn.donatrack.security.Usuario;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
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

  @Test
  void importaUnCsvGuardadoEnLatin1SinRomperLosAcentos() throws Exception {
    String csv =
        "TipoPersona,TipoDoc,Documento,Nombre/Razón Social,Email,Teléfono\n"
            + "HUMANA,DNI,41000001,Benjamín Núñez,benjamin.nunez@example.org,\n";
    ImportadorDonantes importador = new ImportadorDonantes(new AuthService(), Clock.systemUTC());
    Importacion importacion = new Importacion("excel.csv", LocalDateTime.now());

    importador.importar(
        new ByteArrayInputStream(csv.getBytes(Charset.forName("windows-1252"))), importacion);

    assertEquals(0, importacion.getFilasConError(), importacion.getErrores().toString());
    assertEquals(1, importacion.getDonantesNuevos());
    PersonaHumana donante =
        createQuery("select h from PersonaHumana h where h.numDocumento = '41000001'",
                PersonaHumana.class)
            .getSingleResult();
    assertEquals("Benjamín", donante.getNombre());
    assertEquals("Núñez", donante.getApellido());
    importador.detener();
  }

  @Test
  void loQueSeGuardaEnLoteSeLeeIgualQueSiLoHubieraGuardadoHibernate() throws Exception {
    String csv =
        "TipoPersona,TipoDoc,Documento,Nombre/Razón Social,Email,Teléfono\n"
            + "HUMANA,DNI,42000001,Ana Lote,ana.lote@example.org,+54 11 5555-0001\n"
            + "JURIDICA,CUIT,30-42000002-1,Cooperativa Lote,coop.lote@example.org,\n"
            + "HUMANA,DNI,42000001,Ana Repetida,ana.lote@example.org,\n"
            + "MARCIANA,DNI,42000003,Nadie,nadie@example.org,\n";
    ImportadorDonantes importador = new ImportadorDonantes(new AuthService(), Clock.systemUTC());
    Importacion importacion = new Importacion("mixto.csv", LocalDateTime.now());

    importador.importar(
        new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), importacion);
    entityManager().clear();

    assertEquals(4, importacion.getProcesadas());
    assertEquals(2, importacion.getDonantesNuevos());
    assertEquals(1, importacion.getDonantesExistentes());
    assertEquals(2, importacion.getCuentasCreadas());
    assertEquals(1, importacion.getFilasConError(), importacion.getErrores().toString());

    PersonaHumana ana =
        createQuery(
                "select h from PersonaHumana h where h.numDocumento = '42000001'",
                PersonaHumana.class)
            .getSingleResult();
    assertEquals("Ana", ana.getNombre());
    assertEquals(TipoDocumento.DNI, ana.getDocumento());
    assertEquals(2, ana.getMediosDeContacto().size());
    assertTrue(ana.getMediosDeContacto().stream().anyMatch(Sms.class::isInstance));
    assertEquals("ana.lote@example.org", ana.getMedioContactoPredeterminado().getContacto());
    assertTrue(ana.getMedioContactoPredeterminado() instanceof CorreoElectronico);

    PersonajeJuridica coop =
        createQuery(
                "select j from PersonajeJuridica j where j.razonSocial = 'Cooperativa Lote'",
                PersonajeJuridica.class)
            .getSingleResult();
    assertEquals(TipoDocumento.CUIT, coop.getDocumento());

    Usuario cuenta =
        createQuery("select u from Usuario u where u.email = 'ana.lote@example.org'", Usuario.class)
            .getSingleResult();
    assertEquals(Set.of(Role.DONANTE), cuenta.getRoles());
    assertEquals(Optional.of(ana.getId()), cuenta.getDonanteId());
    importador.detener();
  }
}
