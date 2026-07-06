package ar.edu.utn.donatrack.beneficiarios;

import ar.edu.utn.donatrack.donaciones.Donacion;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EntidadBeneficiaria {
  private String razonSocial;
  private String direccion;
  private String telefono;

  private final List<Necesidad> necesidadesExtraordinarias = new ArrayList<>();
  private final List<NecesidadRecurrente> necesidadesRecurrentes = new ArrayList<>();
  private final List<String> correos = new ArrayList<>();
  private final List<RecepcionDonacion> recepciones = new ArrayList<>();

  public EntidadBeneficiaria(String razonSocial, String direccion, String telefono) {
    this.razonSocial = razonSocial;
    this.direccion = direccion;
    this.telefono = telefono;
  }

  public String getRazonSocial() {
    return razonSocial;
  }

  public String getDireccion() {
    return direccion;
  }

  public String getTelefono() {
    return telefono;
  }

  public List<Necesidad> getNecesidadesExtraordinarias() {
    return Collections.unmodifiableList(necesidadesExtraordinarias);
  }

  public List<NecesidadRecurrente> getNecesidadesRecurrentes() {
    return Collections.unmodifiableList(necesidadesRecurrentes);
  }

  public List<String> getCorreos() {
    return Collections.unmodifiableList(correos);
  }

  public List<RecepcionDonacion> getRecepciones() {
    return Collections.unmodifiableList(recepciones);
  }

  public void agregarNecesidadExtraordinaria(Necesidad n) {
    this.necesidadesExtraordinarias.add(n);
  }

  public void agregarNecesidadRecurrente(NecesidadRecurrente n) {
    this.necesidadesRecurrentes.add(n);
  }

  public void agregarCorreo(String correo) {
    this.correos.add(correo);
  }

  public void registrarRecepcion(Donacion donacion, LocalDate fechaRecepcion) {
    this.recepciones.add(new RecepcionDonacion(donacion, fechaRecepcion));
    this.actualizarNecesidadesCon(donacion);
  }

  public long cantidadDonacionesRecibidasDesde(LocalDate fechaDesde) {
    return recepciones.stream().filter(recepcion -> !recepcion.getFechaRecepcion().isBefore(fechaDesde)).count();
  }

  private void actualizarNecesidadesCon(Donacion donacion) {
    necesidadesExtraordinarias.forEach(necesidad -> aplicarSiCoincide(necesidad, donacion));
    necesidadesRecurrentes.forEach(necesidad -> aplicarSiCoincide(necesidad, donacion));
  }

  private void aplicarSiCoincide(Necesidad necesidad, Donacion donacion) {
    if (necesidad.getBienObjetivo().getSubcategoria().equals(donacion.getSubcategoria())) {
      necesidad.recibirDonacion(donacion.getCantidadTotal());
    }
  }
}
