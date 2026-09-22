package ar.edu.utn.donatrack.notificaciones;

import ar.edu.utn.donatrack.donaciones.DonacionesRepository;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.DonantesRepository;
import ar.edu.utn.donatrack.notificaciones.eventos.EventoAusenciaDonante;
import java.time.LocalDate;
import java.util.List;

public class TareaAusenciaDonantes implements Runnable {

    private final DonantesRepository donantesRepository;
    private final Notificador notificador;

    public TareaAusenciaDonantes(DonantesRepository donantesRepository, Notificador notificador) {
        this.donantesRepository = donantesRepository;
        this.notificador = notificador;
    }

    @Override
    public void run() {
        LocalDate fechaLimite = LocalDate.now().minusDays(20);
        List<Donante> donantesAusentes = donantesRepository.getDonantesInactivos(fechaLimite);

        for (Donante donante : donantesAusentes) {
            EventoAusenciaDonante evento = new EventoAusenciaDonante(donante);
            for (Notificacion n : evento.generarNotificaciones()) {
                n.getDestinatario().aceptar(notificador, n.getMensaje());
                n.marcarEnviada();
            }
        }
    }
}
