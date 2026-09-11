package ar.edu.utn.donatrack.beneficiarios;

import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.MedioDeContacto;
import ar.edu.utn.donatrack.notificaciones.Notificable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class EntidadBeneficiaria implements Notificable {
  private final UUID id;
  private String razonSocial;
  private String direccion;
  private String telefono;

  private final List<Necesidad> necesidadesExtraordinarias = new ArrayList<>();
  private final List<NecesidadRecurrente> necesidadesRecurrentes = new ArrayList<>();
  private final List<CorreoElectronico> correos = new ArrayList<>();

  public EntidadBeneficiaria(String razonSocial, String direccion, String telefono) {
    this(UUID.randomUUID(), razonSocial, direccion, telefono);
  }

  public EntidadBeneficiaria(
      UUID id, String razonSocial, String direccion, String telefono) {
    this.id = Objects.requireNonNull(id, "id no puede ser null");
    this.razonSocial = razonSocial;
    this.direccion = direccion;
    this.telefono = telefono;
  }

  public UUID getId() {
    return id;
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

  public List<CorreoElectronico> getCorreos() {
    return Collections.unmodifiableList(correos);
  }

  @Override
  public List<MedioDeContacto> getMediosDeContacto() {
    return Collections.unmodifiableList(new ArrayList<>(correos));
  }

  public void agregarNecesidadExtraordinaria(Necesidad n) {
    this.necesidadesExtraordinarias.add(n);
  }

  public void agregarNecesidadRecurrente(NecesidadRecurrente n) {
    this.necesidadesRecurrentes.add(n);
  }

  public Optional<Necesidad> buscarNecesidad(UUID necesidadId) {
    return necesidades().stream().filter(n -> n.getId().equals(necesidadId)).findFirst();
  }

  public List<Necesidad> necesidades() {
    List<Necesidad> todas = new ArrayList<>(necesidadesExtraordinarias);
    todas.addAll(necesidadesRecurrentes);
    return Collections.unmodifiableList(todas);
  }

  public void eliminarNecesidad(UUID necesidadId) {
    necesidadesExtraordinarias.removeIf(n -> n.getId().equals(necesidadId));
    necesidadesRecurrentes.removeIf(n -> n.getId().equals(necesidadId));
  }

  public void agregarCorreo(CorreoElectronico correo) {
    this.correos.add(correo);
  }

  public void actualizarDatos(
      String razonSocial, String direccion, String telefono, List<CorreoElectronico> correos) {
    this.razonSocial = razonSocial;
    this.direccion = direccion;
    this.telefono = telefono;
    this.correos.clear();
    if (correos != null) {
      correos.forEach(this::agregarCorreo);
    }
  }


  public void aplicarDonacionRecibida(Donacion donacion) {
    Objects.requireNonNull(donacion, "donacion no puede ser null");
    necesidadesExtraordinarias.forEach(necesidad -> aplicarSiCoincide(necesidad, donacion));
    necesidadesRecurrentes.forEach(necesidad -> aplicarSiCoincide(necesidad, donacion));
  }

  private void aplicarSiCoincide(Necesidad necesidad, Donacion donacion) {
    if (necesidad.getSubcategoria().equals(donacion.getSubcategoria())) {
      necesidad.recibirDonacion(donacion.getCantidadTotal());
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EntidadBeneficiaria that)) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
