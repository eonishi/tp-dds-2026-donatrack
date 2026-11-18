package ar.edu.utn.donatrack.donantes;

import ar.edu.utn.donatrack.notificaciones.Notificable;
import ar.edu.utn.donatrack.notificaciones.NotificadorPorCanal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OrderColumn;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Donante implements Notificable {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  protected String numDocumento;

  @Enumerated(EnumType.STRING)
  protected TipoDocumento documento;

  @ElementCollection
  @CollectionTable(
      name = "donante_medio_de_contacto",
      joinColumns = @JoinColumn(name = "donante_id"))
  @OrderColumn(name = "orden")
  protected List<MedioDeContacto> mediosDeContacto = new ArrayList<>();

  // Es un valor, no una referencia a una fila de la lista: se guarda la copia. Sigue siendo "uno de
  // los medios" porque los medios se comparan por canal y contacto.
  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "canal", column = @Column(name = "predeterminado_canal")),
    @AttributeOverride(name = "contacto", column = @Column(name = "predeterminado_contacto"))
  })
  protected MedioDeContacto contactoPredeterminado;

  protected Donante() {}

  public Donante(MedioDeContacto medioDeContacto, String numDocumento, TipoDocumento documento) {
    Objects.requireNonNull(medioDeContacto);
    this.numDocumento = numDocumento;
    this.documento = documento;
    this.mediosDeContacto.add(medioDeContacto);
    this.contactoPredeterminado = medioDeContacto;
    // Aca agregaria el enviar el mail de bienvenida
  }

  protected Donante(
      UUID id, MedioDeContacto medioDeContacto, String numDocumento, TipoDocumento documento) {
    this(medioDeContacto, numDocumento, documento);
    this.id = Objects.requireNonNull(id, "id no puede ser null");
  }

  public UUID getId() {
    return id;
  }

  public void addMedioDeContacto(MedioDeContacto medioDeContacto) {
    if (medioDeContacto != null && !mediosDeContacto.contains(medioDeContacto)) {
      this.mediosDeContacto.add(medioDeContacto);
    }
  }

  public void setContactoPredeterminado(MedioDeContacto medioDeContacto) {
    if (medioDeContacto != null && mediosDeContacto.contains(medioDeContacto)) {
      this.contactoPredeterminado = medioDeContacto;
    }
  }

  public MedioDeContacto getMedioContactoPredeterminado() {
    return this.contactoPredeterminado;
  }

  /** El donante se notifica solo por su medio de contacto predeterminado. */
  @Override
  public void notificar(String mensaje, NotificadorPorCanal canales) {
    contactoPredeterminado.enviar(mensaje, canales);
  }

  public List<MedioDeContacto> getMediosDeContacto() {
    return Collections.unmodifiableList(mediosDeContacto);
  }

  public String getNumDocumento() {
    return numDocumento;
  }

  public TipoDocumento getDocumento() {
    return documento;
  }

  public Optional<String> extraerEmail() {
    return mediosDeContacto.stream()
        .filter(MedioDeContacto::esCorreo)
        .map(MedioDeContacto::getContacto)
        .findFirst();
  }

  public void actualizarIdentificacion(String numDocumento, TipoDocumento documento) {
    this.numDocumento = numDocumento;
    this.documento = documento;
  }

  public void reemplazarMediosDeContacto(
      List<MedioDeContacto> nuevosMedios, int indicePredeterminado) {
    if (nuevosMedios == null || nuevosMedios.isEmpty()) {
      throw new IllegalArgumentException("Debe informar al menos un medio de contacto.");
    }
    if (indicePredeterminado < 0 || indicePredeterminado >= nuevosMedios.size()) {
      throw new IllegalArgumentException("El contacto predeterminado no existe.");
    }
    this.mediosDeContacto = new ArrayList<>(nuevosMedios);
    this.contactoPredeterminado = this.mediosDeContacto.get(indicePredeterminado);
  }

  void merge(Donante entrante) {
    this.numDocumento = entrante.numDocumento;
    this.documento = entrante.documento;
    for (MedioDeContacto medio : entrante.mediosDeContacto) {
      addMedioDeContacto(medio);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Donante donante)) {
      return false;
    }
    return Objects.equals(numDocumento, donante.numDocumento) && documento == donante.documento;
  }

  @Override
  public int hashCode() {
    return Objects.hash(numDocumento, documento);
  }
}
