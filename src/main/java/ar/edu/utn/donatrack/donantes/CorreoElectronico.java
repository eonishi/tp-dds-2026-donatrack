package ar.edu.utn.donatrack.donantes;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import ar.edu.utn.donatrack.notificaciones.Notificador;

@Entity
public class CorreoElectronico extends MedioDeContacto {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  protected CorreoElectronico() {}
  public CorreoElectronico(String contacto) {
    super(contacto);
  }

  @Override
  public void aceptar(Notificador notificador, String mensaje) {
    notificador.enviar(this, mensaje);
  }
}
