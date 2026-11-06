package ar.edu.utn.donatrack.donaciones;

import ar.edu.utn.donatrack.notificaciones.Notificacion;
import ar.edu.utn.donatrack.notificaciones.eventos.EventoEntregaExitosa;
import ar.edu.utn.donatrack.notificaciones.eventos.EventoInicioRuta;
import java.util.List;

/**
 * Estados de una donación y las transiciones válidas desde cada uno.
 *
 * <p>Reemplaza la jerarquía del patrón State: una clase abstracta y siete subclases concretas. Cada
 * constante declara el comportamiento que le corresponde, y las operaciones no soportadas heredan
 * el comportamiento por defecto, que lanza {@link IllegalStateException}.
 *
 * <p>El estado se persiste por nombre y no por índice, así que el orden de declaración no es un
 * contrato de almacenamiento: reordenar las constantes no afecta a las filas ya guardadas. Lo que
 * sí las afecta es renombrar una constante, porque deja a esas filas sin constante a la que mapear.
 *
 * @see HistorialEstado#getEstadoAlcanzado()
 */
public enum EstadoDonacion {
  EN_DEPOSITO {
    @Override
    public List<Notificacion> asignar(Donacion donacion) {
      transicionar(donacion, ASIGNACION_REALIZADA, null);
      if (donacion.getDonante() != null && donacion.getEntidadBeneficiaria() != null) {
        return new ar.edu.utn.donatrack.notificaciones.eventos.EventoAsignacion(
                donacion.getDonante(), donacion.getEntidadBeneficiaria())
            .generarNotificaciones();
      }
      return List.of();
    }

    @Override
    public List<Notificacion> marcarVencida(Donacion donacion) {
      transicionar(donacion, VENCIDA, null);
      return List.of();
    }
  },
  ASIGNACION_REALIZADA {
    @Override
    public List<Notificacion> planificarRuta(Donacion donacion) {
      transicionar(donacion, LISTA_PARA_ENTREGAR, null);
      return List.of();
    }

    @Override
    public List<Notificacion> marcarVencida(Donacion donacion) {
      transicionar(donacion, VENCIDA, null);
      return List.of();
    }
  },
  LISTA_PARA_ENTREGAR {
    @Override
    public List<Notificacion> iniciarTraslado(Donacion donacion, String urlMapa) {
      transicionar(donacion, EN_TRASLADO, null);
      if (donacion.getDonante() != null
          && donacion.getEntidadBeneficiaria() != null
          && urlMapa != null) {
        return new EventoInicioRuta(
                List.of(donacion.getDonante(), donacion.getEntidadBeneficiaria()), urlMapa)
            .generarNotificaciones();
      }
      return List.of();
    }

    @Override
    public List<Notificacion> marcarVencida(Donacion donacion) {
      transicionar(donacion, VENCIDA, null);
      return List.of();
    }
  },
  EN_TRASLADO {
    @Override
    public List<Notificacion> confirmarEntrega(Donacion donacion) {
      if (donacion.getEntidadBeneficiaria() == null) {
        throw new IllegalStateException(
            "No se puede confirmar la entrega sin una entidad beneficiaria asignada.");
      }
      transicionar(donacion, ENTREGADA, null);
      donacion.getEntidadBeneficiaria().aplicarDonacionRecibida(donacion);
      if (donacion.getDonante() != null
          && donacion.getEntidadBeneficiaria() != null
          && donacion.getIdCamion().isPresent()) {
        return new EventoEntregaExitosa(
                donacion.getDonante(),
                donacion.getEntidadBeneficiaria(),
                java.time.LocalDateTime.now(),
                donacion.getIdCamion().get())
            .generarNotificaciones();
      }
      return List.of();
    }

    @Override
    public List<Notificacion> reportarFalla(Donacion donacion, String justificacion) {
      transicionar(donacion, ENTREGA_FALLIDA, justificacion);
      if (donacion.getDonante() != null && donacion.getEntidadBeneficiaria() != null) {
        return new ar.edu.utn.donatrack.notificaciones.eventos.EventoEntregaNoSatisfactoria(
                donacion.getDonante(), donacion.getEntidadBeneficiaria(), justificacion)
            .generarNotificaciones();
      }
      return List.of();
    }

    @Override
    public List<Notificacion> marcarVencida(Donacion donacion) {
      transicionar(donacion, VENCIDA, null);
      return List.of();
    }
  },
  ENTREGADA,
  ENTREGA_FALLIDA {
    @Override
    public List<Notificacion> marcarVencida(Donacion donacion) {
      transicionar(donacion, VENCIDA, null);
      return List.of();
    }

    @Override
    public List<Notificacion> retornarAlDeposito(Donacion donacion) {
      donacion.limpiarEntidadBeneficiaria();
      donacion.limpiarCamion();
      transicionar(donacion, EN_DEPOSITO, null);
      return List.of();
    }
  },
  VENCIDA;

  public List<Notificacion> asignar(Donacion donacion) {
    return noPermitido("asignar");
  }

  public List<Notificacion> planificarRuta(Donacion donacion) {
    return noPermitido("planificar la ruta");
  }

  public List<Notificacion> iniciarTraslado(Donacion donacion, String urlMapa) {
    return noPermitido("iniciar el traslado");
  }

  public List<Notificacion> confirmarEntrega(Donacion donacion) {
    return noPermitido("confirmar la entrega");
  }

  public List<Notificacion> reportarFalla(Donacion donacion, String justificacion) {
    return noPermitido("reportar una falla");
  }

  public List<Notificacion> marcarVencida(Donacion donacion) {
    return noPermitido("marcar como vencida");
  }

  public List<Notificacion> retornarAlDeposito(Donacion donacion) {
    return noPermitido("retornar al depósito");
  }

  /**
   * Registra la transición en el historial de la donación.
   *
   * <p>La donación arma la entrada: acá solo se declara el estado destino y la justificación.
   *
   * @param donacion donación que transiciona
   * @param destino estado al que transiciona
   * @param justificacion motivo de la transición, o {@code null} si no requiere justificación
   */
  private static void transicionar(
      Donacion donacion, EstadoDonacion destino, String justificacion) {
    donacion.cambiarEstado(destino, justificacion);
  }

  /**
   * Rechaza una operación que el estado actual no admite.
   *
   * @param operacion descripción de la operación rechazada, tal como se lee en el mensaje
   * @throws IllegalStateException siempre
   */
  private List<Notificacion> noPermitido(String operacion) {
    throw new IllegalStateException("No se puede " + operacion + " en estado " + this);
  }
}
