package ar.edu.utn.donatrack.notificaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.DonantesRepository;
import ar.edu.utn.donatrack.donantes.Genero;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.TipoDocumento;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TareaAusenciaDonantesTest {
  @Test
  void falloDelProveedorNoMarcaLaNotificacionComoEnviada() {
    DonantesRepository donantes = mock(DonantesRepository.class);
    PersonaHumana donante =
        new PersonaHumana(
            new CorreoElectronico("ausente@example.org"),
            "Ana",
            "Pérez",
            "30111222",
            TipoDocumento.DNI,
            Genero.FEMENINO,
            "Calle 123");
    when(donantes.getDonantesInactivos(any(LocalDate.class))).thenReturn(List.of(donante));
    NotificacionesRepository repository = new NotificacionesRepository();
    Notificador notificador = mock(Notificador.class);
    when(notificador.enviarNotificacion(any(), any()))
        .thenReturn(ResultadoEnvio.fallido("proveedor caído"));
    ServicioNotificaciones servicio = new ServicioNotificaciones(notificador, repository, 1);

    new TareaAusenciaDonantes(donantes, servicio).run();

    assertEquals(1, repository.getNotificaciones().size());
    assertEquals(EstadoNotificacion.FALLIDA, repository.getNotificaciones().get(0).getEstado());
  }
}
