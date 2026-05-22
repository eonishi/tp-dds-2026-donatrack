package ar.edu.utn.donatrack.ServiciosExternosYUtilidades;

public class NotificacionSimulada {
  private final String destinatario;
  private final String mensaje;
  private final String medio;
  private boolean completada;

  public NotificacionSimulada(String destinatario, String mensaje, String medio, boolean completada) {
    this.destinatario = destinatario;
    this.mensaje = mensaje;
    this.medio = medio;
    this.completada = completada;
  }

  public void completar() {
    this.completada = true;
  }
}