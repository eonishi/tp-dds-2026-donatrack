package ar.edu.utn.donatrack.donaciones;

import ar.edu.utn.donatrack.estados.EnDeposito;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DonacionesRepository {
  private final List<Donacion> donaciones = new ArrayList<>();

  public List<Donacion> getDonaciones() {
    return Collections.unmodifiableList(donaciones);
  }

  public List<Donacion> getDonacionesEnDeposito() {
    return donaciones.stream()
        .filter(donacion -> donacion.getEstado() instanceof EnDeposito)
        .toList();
  }

  public void addDonacion(Donacion donacion) {
    this.donaciones.add(donacion);
  }
}
