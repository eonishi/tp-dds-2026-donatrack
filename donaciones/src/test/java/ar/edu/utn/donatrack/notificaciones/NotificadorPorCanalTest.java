package ar.edu.utn.donatrack.notificaciones;

import static ar.edu.utn.donatrack.testsupport.FabricaDePrueba.nuevaEntidad;
import static org.junit.jupiter.api.Assertions.assertEquals;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.Genero;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.Sms;
import ar.edu.utn.donatrack.donantes.TipoDocumento;
import ar.edu.utn.donatrack.notificaciones.eventos.EventoBienvenida;
import ar.edu.utn.donatrack.notificaciones.payloads.SinPayload;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificadorPorCanalTest {
  private final List<String> invocaciones = new ArrayList<>();
  private final NotificadorPorCanal notificador =
      new NotificadorPorCanal(
          (destinatario, asunto, mensaje) -> registrar("correo", destinatario),
          (destinatario, mensaje) -> registrar("sms", destinatario),
          (destinatario, mensaje) -> registrar("whatsapp", destinatario));

  @Test
  void usaSolamenteElGatewayDelMedioPredeterminado() {
    PersonaHumana donante =
        new PersonaHumana(
            new CorreoElectronico("donante@example.org"),
            "Ana",
            "Pérez",
            30,
            "30111222",
            TipoDocumento.DNI,
            Genero.FEMENINO,
            "Calle 123");
    Sms predeterminado = new Sms("+541112345678");
    donante.addMedioDeContacto(predeterminado);
    donante.setContactoPredeterminado(predeterminado);

    List<Notificacion> notificaciones = new EventoBienvenida(donante).generarNotificaciones();
    notificaciones.forEach(n -> n.enviarCon(notificador));

    assertEquals(List.of("sms|+541112345678"), invocaciones);
    assertEquals(
        List.of(new Envio(Canal.SMS, ResultadoEnvio.aceptado())),
        notificaciones.get(0).getEnvios());
  }

  @Test
  void entidadConVariosCorreos_recibeUnaNotificacionEnviadaATodosSusCorreos() {
    EntidadBeneficiaria entidad = nuevaEntidad("Fundación Test");
    entidad.agregarCorreo(new CorreoElectronico("uno@test.com"));
    entidad.agregarCorreo(new CorreoElectronico("dos@test.com"));
    Notificacion notificacion = new Notificacion(entidad, "Hola", SinPayload.INSTANCE);

    notificacion.enviarCon(notificador);

    assertEquals(List.of("correo|uno@test.com", "correo|dos@test.com"), invocaciones);
    assertEquals(
        List.of(
            new Envio(Canal.CORREO, ResultadoEnvio.aceptado()),
            new Envio(Canal.CORREO, ResultadoEnvio.aceptado())),
        notificacion.getEnvios());
  }

  @Test
  void gatewayQueLanzaExcepcion_registraEnvioFallidoYSigueConElSiguienteCorreo() {
    NotificadorPorCanal conFalla =
        new NotificadorPorCanal(
            (destinatario, asunto, mensaje) -> {
              if (destinatario.startsWith("uno")) {
                throw new IllegalStateException("Proveedor caído.");
              }
              return registrar("correo", destinatario);
            },
            (destinatario, mensaje) -> ResultadoEnvio.fallido("No corresponde"),
            (destinatario, mensaje) -> ResultadoEnvio.fallido("No corresponde"));
    EntidadBeneficiaria entidad = nuevaEntidad("Fundación Test");
    entidad.agregarCorreo(new CorreoElectronico("uno@test.com"));
    entidad.agregarCorreo(new CorreoElectronico("dos@test.com"));
    Notificacion notificacion = new Notificacion(entidad, "Hola", SinPayload.INSTANCE);

    notificacion.enviarCon(conFalla);

    assertEquals(List.of("correo|dos@test.com"), invocaciones);
    assertEquals(
        List.of(
            new Envio(Canal.CORREO, ResultadoEnvio.fallido("Proveedor caído.")),
            new Envio(Canal.CORREO, ResultadoEnvio.aceptado())),
        notificacion.getEnvios());
  }

  private ResultadoEnvio registrar(String canal, String destinatario) {
    invocaciones.add(canal + "|" + destinatario);
    return ResultadoEnvio.aceptado();
  }
}
