package ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CargaDonacion {
  LocalDate fechaRegistro = LocalDate.now();
  private static String descripcionGeneral;
  List<Donacion> donaciones = new ArrayList<>();

  //Constructor
  public CargaDonacion(LocalDate fechaRegistro) {
    this.fechaRegistro = fechaRegistro;
  }

  public void segmentarDonaciones(){

    donaciones.forEach(donacion => );

  }
}
