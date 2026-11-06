package ar.edu.utn.donatrack.notificaciones;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.DonantesRepository;
import ar.edu.utn.donatrack.donantes.Genero;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.TipoDocumento;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class TareaAusenciaDonantesTest {
  @Test
  void falloDelProveedorNoInterrumpeLaTareaDeAusencia() {
    DonantesRepository donantes = mock(DonantesRepository.class);
    PersonaHumana donante =
        new PersonaHumana(
            new CorreoElectronico("ausente@example.org"),
            "Ana",
            "Pérez",
            30,
            "30111222",
            TipoDocumento.DNI,
            Genero.FEMENINO,
            "Calle 123");
    when(donantes.getDonantesInactivos(any(LocalDate.class))).thenReturn(List.of(donante));
    AtomicInteger intentos = new AtomicInteger();
    NotificadorPorCanal notificador =
        new NotificadorPorCanal(
            (destinatario, asunto, mensaje) -> {
              intentos.incrementAndGet();
              return ResultadoEnvio.fallido("proveedor caído");
            },
            (destinatario, mensaje) -> ResultadoEnvio.fallido("no corresponde"),
            (destinatario, mensaje) -> ResultadoEnvio.fallido("no corresponde"));
    ServicioNotificaciones servicio = new ServicioNotificaciones(notificador);

    assertDoesNotThrow(() -> new TareaAusenciaDonantes(donantes, servicio).run());

    verify(donantes).getDonantesInactivos(any(LocalDate.class));
    assertEquals(1, intentos.get());
  }
}
