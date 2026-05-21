package ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import ar.edu.utn.donatrack.BienesYCategorias.Bien;
import ar.edu.utn.donatrack.BienesYCategorias.SubCategoria;

public class CargaDonacion {
  LocalDate fechaRegistro = LocalDate.now();
  private static String descripcionGeneral;
  private List<Donacion> donaciones = new ArrayList<>();

  //Constructor
  public CargaDonacion(LocalDate fechaRegistro) {
    this.fechaRegistro = fechaRegistro;
  }

  public void cargarDonacion(List<Bien> bienes){

    bienes.stream()
        .collect(Collectors.groupingBy(Bien::getSubcategoria))
        .forEach((subCategoria, bienesDeEsaSubCategoria) -> {
          Donacion donacion = new Donacion(bienesDeEsaSubCategoria);
          donaciones.add(donacion);
        });


  }

}
