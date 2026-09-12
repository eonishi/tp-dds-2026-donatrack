package ar.edu.utn.donatrack.beneficiarios;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import ar.edu.utn.donatrack.donaciones.Donacion;
import java.time.LocalDate;

@Entity
public class RecepcionDonacion {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  protected RecepcionDonacion() {}
  @ManyToOne
  @JoinColumn(name = "donacion_id")
  private Donacion donacion;
  private LocalDate fechaRecepcion;

  public RecepcionDonacion(Donacion donacion, LocalDate fechaRecepcion) {
    this.donacion = donacion;
    this.fechaRecepcion = fechaRecepcion;
  }

  public Donacion getDonacion() {
    return donacion;
  }

  public LocalDate getFechaRecepcion() {
    return fechaRecepcion;
  }
}
