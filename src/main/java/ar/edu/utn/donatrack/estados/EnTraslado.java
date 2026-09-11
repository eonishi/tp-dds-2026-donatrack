package ar.edu.utn.donatrack.estados;

import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.NombreEstado;
import java.time.LocalDate;

public class EnTraslado extends EstadoDonacion {
  public EnTraslado() {
    super(NombreEstado.EN_TRASLADO);
  }

  @Override
  public void confirmarEntrega(Donacion donacion) {
    transicionar(donacion, new Entregada(), null);
    donacion.getEntidadBeneficiaria().registrarRecepcion(donacion, LocalDate.now());
  }

  @Override
  public void reportarFalla(Donacion donacion, String justificacion) {
    transicionar(donacion, new EntregaFallida(), justificacion);
  }

  @Override
  public void marcarVencida(Donacion donacion) {
    transicionar(donacion, new Vencida(), null);
  }
}
