package ar.edu.utn.donatrack.donaciones;

import java.time.LocalDateTime;

/**
 * Estados de una donación y las transiciones válidas desde cada uno.
 *
 * <p>Reemplaza la jerarquía del patrón State: una clase abstracta y siete subclases concretas. Cada
 * constante declara el comportamiento que le corresponde, con los mismos cuerpos y los mismos
 * mensajes que las clases que reemplaza.
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
    public void planificarRuta(Donacion donacion) {
      throw new IllegalStateException(
          "No se puede planificar la ruta de una donación en depósito sin asignar.");
    }

    @Override
    public void iniciarTraslado(Donacion donacion) {
      throw new IllegalStateException(
          "No se puede iniciar el traslado de una donación en depósito.");
    }

    @Override
    public void confirmarEntrega(Donacion donacion) {
      throw new IllegalStateException(
          "No se puede confirmar la entrega de una donación en depósito.");
    }

    @Override
    public void reportarFalla(Donacion donacion, String justificacion) {
      throw new IllegalStateException(
          "No se puede reportar falla de entrega para una donación en depósito.");
    }

    @Override
    public void marcarVencida(Donacion donacion) {
      transicionar(donacion, VENCIDA, null);
    }

    @Override
    public void retornarAlDeposito(Donacion donacion) {
      throw new IllegalStateException("La donación ya se encuentra en depósito.");
    }
  },
  ASIGNACION_REALIZADA {
    @Override
    public void asignar(Donacion donacion) {
      throw new IllegalStateException("La donación ya se encuentra asignada.");
    }

    @Override
    public void planificarRuta(Donacion donacion) {
      transicionar(donacion, LISTA_PARA_ENTREGAR, null);
    }

    @Override
    public void iniciarTraslado(Donacion donacion) {
      throw new IllegalStateException(
          "No se puede iniciar el traslado sin haber planificado la ruta previamente.");
    }

    @Override
    public void confirmarEntrega(Donacion donacion) {
      throw new IllegalStateException(
          "No se puede confirmar la entrega de una donación que no está en traslado.");
    }

    @Override
    public void reportarFalla(Donacion donacion, String justificacion) {
      throw new IllegalStateException(
          "No se puede reportar falla para una donación que no está en traslado.");
    }

    @Override
    public void marcarVencida(Donacion donacion) {
      transicionar(donacion, VENCIDA, null);
    }

    @Override
    public void retornarAlDeposito(Donacion donacion) {
      throw new IllegalStateException(
          "No se puede retornar al depósito: la donación ya tiene una asignación.");
    }
  },
  LISTA_PARA_ENTREGAR {
    @Override
    public void asignar(Donacion donacion) {
      throw new IllegalStateException("La donación ya se encuentra asignada.");
    }

    @Override
    public void planificarRuta(Donacion donacion) {
      throw new IllegalStateException("La ruta para esta donación ya ha sido planificada.");
    }

    @Override
    public void iniciarTraslado(Donacion donacion) {
      transicionar(donacion, EN_TRASLADO, null);
    }

    @Override
    public void confirmarEntrega(Donacion donacion) {
      throw new IllegalStateException(
          "No se puede confirmar la entrega sin haber iniciado el traslado.");
    }

    @Override
    public void reportarFalla(Donacion donacion, String justificacion) {
      throw new IllegalStateException(
          "No se puede reportar falla para una donación que no está en traslado.");
    }

    @Override
    public void marcarVencida(Donacion donacion) {
      transicionar(donacion, VENCIDA, null);
    }

    @Override
    public void retornarAlDeposito(Donacion donacion) {
      throw new IllegalStateException(
          "No se puede retornar al depósito: la donación está lista para entregar.");
    }
  },
  EN_TRASLADO {
    @Override
    public void asignar(Donacion donacion) {
      throw new IllegalStateException("La donación ya se encuentra asignada.");
    }

    @Override
    public void planificarRuta(Donacion donacion) {
      throw new IllegalStateException(
          "No se puede planificar la ruta de una donación que ya está en traslado.");
    }

    @Override
    public void iniciarTraslado(Donacion donacion) {
      throw new IllegalStateException("El traslado ya se encuentra en curso.");
    }

    @Override
    public void confirmarEntrega(Donacion donacion) {
      if (donacion.getEntidadBeneficiaria() == null) {
        throw new IllegalStateException(
            "No se puede confirmar la entrega sin una entidad beneficiaria asignada.");
      }
      transicionar(donacion, ENTREGADA, null);
      donacion.getEntidadBeneficiaria().aplicarDonacionRecibida(donacion);
    }

    @Override
    public void reportarFalla(Donacion donacion, String justificacion) {
      transicionar(donacion, ENTREGA_FALLIDA, justificacion);
    }

    @Override
    public void marcarVencida(Donacion donacion) {
      transicionar(donacion, VENCIDA, null);
    }

    @Override
    public void retornarAlDeposito(Donacion donacion) {
      throw new IllegalStateException(
          "No se puede retornar al depósito: la donación está en traslado.");
    }
  },
  ENTREGADA {
    @Override
    public void asignar(Donacion donacion) {
      throw new IllegalStateException(
          "La donación ya ha sido entregada. No admite cambios de estado.");
    }

    @Override
    public void planificarRuta(Donacion donacion) {
      throw new IllegalStateException(
          "La donación ya ha sido entregada. No admite cambios de estado.");
    }

    @Override
    public void iniciarTraslado(Donacion donacion) {
      throw new IllegalStateException(
          "La donación ya ha sido entregada. No admite cambios de estado.");
    }

    @Override
    public void confirmarEntrega(Donacion donacion) {
      throw new IllegalStateException(
          "La donación ya ha sido entregada. No admite cambios de estado.");
    }

    @Override
    public void reportarFalla(Donacion donacion, String justificacion) {
      throw new IllegalStateException(
          "La donación ya ha sido entregada. No admite cambios de estado.");
    }

    @Override
    public void marcarVencida(Donacion donacion) {
      throw new IllegalStateException(
          "La donación ya ha sido entregada. No admite cambios de estado.");
    }

    @Override
    public void retornarAlDeposito(Donacion donacion) {
      throw new IllegalStateException(
          "La donación ya ha sido entregada. No admite cambios de estado.");
    }
  },
  ENTREGA_FALLIDA {
    @Override
    public void asignar(Donacion donacion) {
      throw new IllegalStateException("La donación ya se encuentra asignada.");
    }

    @Override
    public void planificarRuta(Donacion donacion) {
      throw new IllegalStateException(
          "No se puede planificar ruta desde una entrega fallida. Use retornarAlDeposito primero.");
    }

    @Override
    public void iniciarTraslado(Donacion donacion) {
      throw new IllegalStateException("Debe planificarse la ruta nuevamente.");
    }

    @Override
    public void confirmarEntrega(Donacion donacion) {
      throw new IllegalStateException(
          "No se puede confirmar la entrega de una donación con entrega fallida.");
    }

    @Override
    public void reportarFalla(Donacion donacion, String justificacion) {
      throw new IllegalStateException(
          "La donación ya se encuentra registrada con entrega fallida.");
    }

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
  VENCIDA {
    @Override
    public void asignar(Donacion donacion) {
      throw new IllegalStateException(
          "La donación se encuentra vencida. No admite cambios de estado.");
    }

    @Override
    public void planificarRuta(Donacion donacion) {
      throw new IllegalStateException(
          "La donación se encuentra vencida. No admite cambios de estado.");
    }

    @Override
    public void iniciarTraslado(Donacion donacion) {
      throw new IllegalStateException(
          "La donación se encuentra vencida. No admite cambios de estado.");
    }

    @Override
    public void confirmarEntrega(Donacion donacion) {
      throw new IllegalStateException(
          "La donación se encuentra vencida. No admite cambios de estado.");
    }

    @Override
    public void reportarFalla(Donacion donacion, String justificacion) {
      throw new IllegalStateException(
          "La donación se encuentra vencida. No admite cambios de estado.");
    }

    @Override
    public void marcarVencida(Donacion donacion) {
      throw new IllegalStateException("La donación ya se encuentra vencida.");
    }

    @Override
    public void retornarAlDeposito(Donacion donacion) {
      throw new IllegalStateException(
          "La donación se encuentra vencida. No admite cambios de estado.");
    }
  };

  public abstract void asignar(Donacion donacion);

  public abstract void planificarRuta(Donacion donacion);

  public abstract void iniciarTraslado(Donacion donacion);

  public abstract void confirmarEntrega(Donacion donacion);

  public abstract void reportarFalla(Donacion donacion, String justificacion);

  public abstract void marcarVencida(Donacion donacion);

  public abstract void retornarAlDeposito(Donacion donacion);

  private static void transicionar(
      Donacion donacion, EstadoDonacion destino, String justificacion) {
    donacion.cambiarEstado(
        new HistorialEstado(LocalDateTime.now(), destino, justificacion), destino);
  }
}
