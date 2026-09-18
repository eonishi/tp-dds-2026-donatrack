package ar.edu.utn.donatrack.donaciones;

import java.time.LocalDate;

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
    public void asignar(Donacion donacion) {
      transicionar(donacion, ASIGNACION_REALIZADA, null);
    }

    @Override
    public void marcarVencida(Donacion donacion) {
      transicionar(donacion, VENCIDA, null);
    }
  },
  ASIGNACION_REALIZADA {
    @Override
    public void planificarRuta(Donacion donacion) {
      transicionar(donacion, LISTA_PARA_ENTREGAR, null);
    }

    @Override
    public void marcarVencida(Donacion donacion) {
      transicionar(donacion, VENCIDA, null);
    }
  },
  LISTA_PARA_ENTREGAR {
    @Override
    public void iniciarTraslado(Donacion donacion) {
      transicionar(donacion, EN_TRASLADO, null);
    }

    @Override
    public void marcarVencida(Donacion donacion) {
      transicionar(donacion, VENCIDA, null);
    }
  },
  EN_TRASLADO {
    @Override
    public void confirmarEntrega(Donacion donacion) {
      transicionar(donacion, ENTREGADA, null);
      donacion.getEntidadBeneficiaria().registrarRecepcion(donacion, LocalDate.now());
    }

    @Override
    public void reportarFalla(Donacion donacion, String justificacion) {
      transicionar(donacion, ENTREGA_FALLIDA, justificacion);
    }

    @Override
    public void marcarVencida(Donacion donacion) {
      transicionar(donacion, VENCIDA, null);
    }
  },
  ENTREGADA,
  ENTREGA_FALLIDA {
    @Override
    public void marcarVencida(Donacion donacion) {
      transicionar(donacion, VENCIDA, null);
    }

    @Override
    public void retornarAlDeposito(Donacion donacion) {
      donacion.limpiarEntidadBeneficiaria();
      donacion.limpiarCamion();
      transicionar(donacion, EN_DEPOSITO, null);
    }
  },
  VENCIDA;

  public void asignar(Donacion donacion) {
    noPermitido("asignar");
  }

  public void planificarRuta(Donacion donacion) {
    noPermitido("planificar la ruta");
  }

  public void iniciarTraslado(Donacion donacion) {
    noPermitido("iniciar el traslado");
  }

  public void confirmarEntrega(Donacion donacion) {
    noPermitido("confirmar la entrega");
  }

  public void reportarFalla(Donacion donacion, String justificacion) {
    noPermitido("reportar una falla");
  }

  public void marcarVencida(Donacion donacion) {
    noPermitido("marcar como vencida");
  }

  public void retornarAlDeposito(Donacion donacion) {
    noPermitido("retornar al depósito");
  }

  private static void transicionar(
      Donacion donacion, EstadoDonacion destino, String justificacion) {
    donacion.cambiarEstado(destino, justificacion);
  }

  private void noPermitido(String operacion) {
    throw new IllegalStateException("No se puede " + operacion + " en estado " + this);
  }
}
