package ar.edu.utn.donatrack;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.donatrack.BeneficiariosYNecesidades.*;
import ar.edu.utn.donatrack.BienesYCategorias.*;
import ar.edu.utn.donatrack.DonantesYContacto.*;
import ar.edu.utn.donatrack.EstadoDeDonacion.*;
import ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones.*;
import ar.edu.utn.donatrack.ServiciosExternosYUtilidades.*;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class GestionSistemaTest {
  @Test
  @DisplayName("1. Gestión de personas donantes (Humanas con contactos y Jurídicas)")
  public void testGestionDonantes() {
    // A. Persona Humana (Requisito: Email obligatorio, contactos opcionales y medio predeterminado)
    CorreoElectronico email = new CorreoElectronico("juan@mail.com");
    MedioDeContacto whatsapp = new Whatsapp("1122334455");
    List<MedioDeContacto> opcionales = List.of(whatsapp);

    PersonaHumana donanteHumana = new PersonaHumana(
        email,
        opcionales,
        whatsapp, // Predeterminado: Whatsapp
        "Juan",
        "Pérez",
        35,
        12345678,
        TipoDocumento.DNI,
        "Masculino",
        "Av. Medrano 951"
    );

    assertEquals("Juan", donanteHumana.getNombre());
    assertEquals("Pérez", donanteHumana.getApellido());
    assertEquals(35, donanteHumana.getEdad());
    assertEquals(12345678, donanteHumana.getNumDocumento());
    assertEquals(TipoDocumento.DNI, donanteHumana.getDocumento());
    assertEquals("Masculino", donanteHumana.getGenero());
    assertEquals("Av. Medrano 951", donanteHumana.getDireccion());
    
    // Verificaciones de contacto
    assertEquals("juan@mail.com", donanteHumana.getEmail());
    assertEquals(2, donanteHumana.getMediosDeContacto().size());
    assertEquals(whatsapp, donanteHumana.getMedioContactoPredeterminado());

    // B. Personaje Jurídico (Requisito: TipoOrganizacion corregido y al menos un medio de contacto)
    MedioDeContacto medioJuridica = new CorreoElectronico("contacto@empresa.com");
    PersonajeJuridica donanteJuridica = new PersonajeJuridica(
        medioJuridica,
        301122334L,
        TipoDocumento.CUIT,
        "Ayuda S.A.",
        TipoOrganizacion.EMPRESA, // Enum corregido
        "Alimentos"
    );

    Representante rep = new Representante("Carlos", "Gómez");
    donanteJuridica.agregarRepresentante(rep);

    assertEquals("Ayuda S.A.", donanteJuridica.getRazonSocial());
    assertEquals(301122334L, donanteJuridica.getNumDocumento());
    assertEquals(TipoDocumento.CUIT, donanteJuridica.getDocumento());
    assertEquals(TipoOrganizacion.EMPRESA, donanteJuridica.getTipoOrganizacion());
    assertEquals("Alimentos", donanteJuridica.getRubro());
    assertEquals(1, donanteJuridica.getRepresentantes().size());
    assertEquals("Carlos", donanteJuridica.getRepresentantes().get(0).getNombre());
    assertEquals("Gómez", donanteJuridica.getRepresentantes().get(0).getApellido());
  }

  @Test
  @DisplayName("2. Gestión de donaciones resultantes (Segmentación Avanzada)")
  public void testSegmentacionDonacionesAvanzada() {
    SubCategoria fideos = new SubCategoria("Fideos");
    SubCategoria vestimenta = new SubCategoria("Pantalones");

    // A. Separación por fecha de vencimiento (Perecederos)
    LocalDate vencimientoEnero = LocalDate.of(2027, 1, 1);
    LocalDate vencimientoFebrero = LocalDate.of(2027, 2, 1);

    Bien bienFideos1 = new Bien("Fideos A", "foto1", 10.0, UnidadMedida.UNIDADES, true, vencimientoEnero, EstadoUso.NO_APLICA, fideos);
    Bien bienFideos2 = new Bien("Fideos B", "foto2", 15.0, UnidadMedida.UNIDADES, true, vencimientoEnero, EstadoUso.NO_APLICA, fideos); // misma fecha
    Bien bienFideos3 = new Bien("Fideos C", "foto3", 20.0, UnidadMedida.UNIDADES, true, vencimientoFebrero, EstadoUso.NO_APLICA, fideos); // otra fecha

    // B. Separación por estado de uso (No perecederos pero estado relevante)
    Bien pantalonNuevo = new Bien("Jean Nuevo", "fotoJeanN", 5.0, UnidadMedida.UNIDADES, false, null, EstadoUso.NUEVO, vestimenta);
    Bien pantalonUsado = new Bien("Jean Usado", "fotoJeanU", 3.0, UnidadMedida.UNIDADES, false, null, EstadoUso.USADO, vestimenta);

    List<Bien> bienesRecibidos = List.of(bienFideos1, bienFideos2, bienFideos3, pantalonNuevo, pantalonUsado);

    CargaDonacion carga = new CargaDonacion(LocalDate.now());
    carga.cargarDonacion(bienesRecibidos);

    List<Donacion> donacionesResultantes = carga.getDonaciones();

    // Deberían generarse exactamente 4 donaciones independientes:
    // 1. Fideos que vencen en Enero (contiene bienFideos1 y bienFideos2)
    // 2. Fideos que vencen en Febrero (contiene bienFideos3)
    // 3. Pantalones Nuevos (contiene pantalonNuevo)
    // 4. Pantalones Usados (contiene pantalonUsado)
    assertEquals(4, donacionesResultantes.size());

    long donacionesFideosEnero = donacionesResultantes.stream()
        .filter(d -> d.getSubcategoria().equals(fideos) && d.getBienes().stream().anyMatch(b -> vencimientoEnero.equals(b.getFechaVencimiento())))
        .count();
    assertEquals(1, donacionesFideosEnero);

    Donacion fideosEnero = donacionesResultantes.stream()
        .filter(d -> d.getSubcategoria().equals(fideos) && d.getBienes().stream().anyMatch(b -> vencimientoEnero.equals(b.getFechaVencimiento())))
        .findFirst().orElseThrow();
    assertEquals(2, fideosEnero.getBienes().size()); // agrupa los dos de Enero

    long donacionesFideosFebrero = donacionesResultantes.stream()
        .filter(d -> d.getSubcategoria().equals(fideos) && d.getBienes().stream().anyMatch(b -> vencimientoFebrero.equals(b.getFechaVencimiento())))
        .count();
    assertEquals(1, donacionesFideosFebrero);

    long donacionesPantalonNuevo = donacionesResultantes.stream()
        .filter(d -> d.getSubcategoria().equals(vestimenta) && d.getBienes().stream().allMatch(b -> b.getEstadoDeUso() == EstadoUso.NUEVO))
        .count();
    assertEquals(1, donacionesPantalonNuevo);

    long donacionesPantalonUsado = donacionesResultantes.stream()
        .filter(d -> d.getSubcategoria().equals(vestimenta) && d.getBienes().stream().allMatch(b -> b.getEstadoDeUso() == EstadoUso.USADO))
        .count();
    assertEquals(1, donacionesPantalonUsado);
  }

  @Test
  @DisplayName("3. Gestión de entidades beneficiarias y sus necesidades")
  public void testGestionEntidadesBeneficiariasYNecesidades() {
    EntidadBeneficiaria comedor = new EntidadBeneficiaria("Comedor Los Piletones", "Maza 123", "4321-8765");
    comedor.agregarCorreo("contacto@lospiletones.org");

    assertEquals("Comedor Los Piletones", comedor.getRazonSocial());
    assertEquals("Maza 123", comedor.getDireccion());
    assertEquals("4321-8765", comedor.getTelefono());
    assertTrue(comedor.getCorreos().contains("contacto@lospiletones.org"));

    // A. Necesidad Extraordinaria
    NecesidadExtraordinaria necExtra = new NecesidadExtraordinaria();
    necExtra.setId(101L);
    necExtra.setDescripcion("Colchones para emergencia por inundación");
    necExtra.setCantidadObjetivo(5.0);
    necExtra.setCantidadRecibida(0.0);

    comedor.agregarNecesidadExtraordinaria(necExtra);

    assertFalse(necExtra.estaSatisfecha());

    // Recibe donación parcial
    necExtra.recibirDonacion(2.0);
    assertEquals(2.0, necExtra.getCantidadRecibida());
    assertFalse(necExtra.estaSatisfecha());

    // Recibe donación que completa el objetivo
    necExtra.recibirDonacion(3.0);
    assertEquals(5.0, necExtra.getCantidadRecibida());
    assertTrue(necExtra.estaSatisfecha());

    // B. Necesidad Recurrente
    NecesidadRecurrente necRec = new NecesidadRecurrente();
    necRec.setId(202L);
    necRec.setDescripcion("Litros de leche semanales");
    necRec.setPeriodo(TipoPeriodo.SEMANAL);
    necRec.setCantidadObjetivo(10.0);
    necRec.setCantidadRecibida(0.0);

    comedor.agregarNecesidadRecurrente(necRec);

    assertFalse(necRec.estaSatisfecha());
    necRec.recibirDonacion(10.0);
    assertTrue(necRec.estaSatisfecha());

    // Al reiniciar el período recurrente, se vuelve a 0
    necRec.reiniciarPeriodo();
    assertEquals(0.0, necRec.getCantidadRecibida());
    assertFalse(necRec.estaSatisfecha());
  }

  @Test
  @DisplayName("4. Garantizar la trazabilidad de los estados de las donaciones")
  public void testTrazabilidadEstadosDonacion() {
    SubCategoria subCat = new SubCategoria("Otros");
    Bien bien = new Bien("Mesa", "url", 1.0, UnidadMedida.UNIDADES, false, null, EstadoUso.NUEVO, subCat);
    List<Bien> bienes = List.of(bien);
    Donacion donacion = new Donacion(bienes);

    // 1. Estado inicial
    assertTrue(donacion.getEstado() instanceof EnDeposito);
    assertEquals("Sin comentarios", donacion.getJustificacion());
    assertEquals(0, donacion.getHistorialEstado().size());

    // Excepciones en transiciones inválidas desde EnDeposito
    assertThrows(IllegalStateException.class, () -> donacion.planificarRuta("Intento inválido"));
    assertThrows(IllegalStateException.class, () -> donacion.iniciarTraslado("Intento inválido"));
    assertThrows(IllegalStateException.class, () -> donacion.confirmarEntrega("Intento inválido"));

    // 2. Transición legal: EnDeposito -> AsignacionRealizada
    donacion.asignar("Donación asignada a Comedor Infantil");
    assertTrue(donacion.getEstado() instanceof AsignacionRealizada);
    assertEquals("Donación asignada a Comedor Infantil", donacion.getJustificacion());
    assertEquals(1, donacion.getHistorialEstado().size());
    assertEquals(NombreEstado.ASIGNACION_REALIZADA, donacion.getHistorialEstado().get(0).getEstadoAlcanzado());

    // 3. Transición legal: AsignacionRealizada -> ListaParaEntregar
    donacion.planificarRuta("Ruta logística optimizada");
    assertTrue(donacion.getEstado() instanceof ListaParaEntregar);
    assertEquals("Ruta logística optimizada", donacion.getJustificacion());
    assertEquals(2, donacion.getHistorialEstado().size());
    assertEquals(NombreEstado.LISTA_PARA_ENTREGAR, donacion.getHistorialEstado().get(1).getEstadoAlcanzado());

    // 4. Transición legal: ListaParaEntregar -> EnTraslado
    donacion.iniciarTraslado("Vehículo patente AAA-123 en viaje");
    assertTrue(donacion.getEstado() instanceof EnTraslado);
    assertEquals(3, donacion.getHistorialEstado().size());
    assertEquals(NombreEstado.EN_TRASLADO, donacion.getHistorialEstado().get(2).getEstadoAlcanzado());

    // 5. Transición legal: EnTraslado -> Entregada
    donacion.confirmarEntrega("Entregado con éxito a Directora Marta");
    assertTrue(donacion.getEstado() instanceof Entregada);
    assertEquals(4, donacion.getHistorialEstado().size());
    assertEquals(NombreEstado.ENTREGADA, donacion.getHistorialEstado().get(3).getEstadoAlcanzado());

    // Intentar modificar el estado posterior a la entrega lanza error
    assertThrows(IllegalStateException.class, () -> donacion.marcarVencida("No se puede vencer una donación entregada"));
  }

  @Test
  @DisplayName("5. Importación masiva de donantes en CSV y Lógica Update or Create")
  public void testImportacionMasivaDonantesCSV() {
    DonantesRepository repository = new DonantesRepository();

    // 1. Cargar datos iniciales en el repositorio
    CorreoElectronico emailExistente = new CorreoElectronico("jcarlos@email.com");
    PersonaHumana donanteExistente = new PersonaHumana(
        emailExistente,
        new ArrayList<>(),
        emailExistente,
        "Juan",
        "Viejo", // Apellido viejo
        25,
        99887766,
        TipoDocumento.DNI,
        "Masculino",
        "Direccion Antigua 123"
    );
    repository.registrarOActualizar(donanteExistente);

    // 2. CSV que contiene un donante a actualizar (mismo email) y uno nuevo (nuevo email)
    String csvContent = "TipoPersona,TipoDoc,Documento,Nombre/Razón Social,Email,Teléfono\n"
        + "HUMANA,DNI,99887766,Juan Carlos,jcarlos@email.com,15-9988-7766\n"
        + "JURIDICA,CUIT,30-99998888-7,Fundación Esperanza,esperanza@mail.com,\n";

    StringReader reader = new StringReader(csvContent);

    // 3. Invocar importación masiva y registrar/actualizar
    ImportadorDonantesCSV.importarYRegistrar(reader, repository);

    // 4. Validaciones
    List<Donante> registrados = repository.getDonantes();
    assertEquals(2, registrados.size()); // No se duplicó, se actualizó el existente y se creó el nuevo

    // A. Validar actualización del donante existente
    Donante d1 = repository.buscarPorEmail("jcarlos@email.com").orElseThrow();
    assertTrue(d1 instanceof PersonaHumana);
    PersonaHumana h1 = (PersonaHumana) d1;
    assertEquals("Juan", h1.getNombre());
    assertEquals("Carlos", h1.getApellido()); // Apellido actualizado desde CSV!
    assertEquals(99887766, h1.getNumDocumento());
    assertEquals(2, h1.getMediosDeContacto().size());
    // Se agregó el teléfono opcional de Whatsapp
    assertTrue(h1.getMediosDeContacto().stream().anyMatch(m -> m instanceof Whatsapp && "15-9988-7766".equals(m.getContacto())));

    // B. Validar creación del donante jurídico nuevo
    Donante d2 = repository.buscarPorEmail("esperanza@mail.com").orElseThrow();
    assertTrue(d2 instanceof PersonajeJuridica);
    PersonajeJuridica j2 = (PersonajeJuridica) d2;
    assertEquals("Fundación Esperanza", j2.getRazonSocial());
    assertEquals(30999988887L, j2.getNumDocumento());
    assertEquals(TipoDocumento.CUIT, j2.getDocumento());
    assertEquals(TipoOrganizacion.ONG, j2.getTipoOrganizacion()); // Enum de organización correcto!
  }

  @Test
  @DisplayName("6. Simulación de Notificaciones y marcado como completadas")
  public void testSimulacionNotificaciones() {
    NotificadorSimulado notificador = new NotificadorSimulado();

    CorreoElectronico email = new CorreoElectronico("donante@mail.com");
    PersonaHumana donante = new PersonaHumana(
        email,
        List.of(new Whatsapp("1199998888")),
        email,
        "Maria",
        "Gomez",
        40,
        98765432,
        TipoDocumento.DNI,
        "Femenino",
        "Colon 456"
    );

    // Enviar notificación a través del canal predeterminado del donante (Email)
    String mensaje = "¡Bienvenido a DonaTrack! Gracias por registrarte.";
    notificador.enviarNotificacion(donante.getMedioContactoPredeterminado(), mensaje);

    // Verificar en el log del notificador simulado
    assertEquals(1, notificador.getEnviadas().size());
    NotificacionSimulada notif = notificador.getEnviadas().get(0);
    assertEquals("donante@mail.com", notif.getDestinatario());
    assertEquals("CORREOELECTRONICO", notif.getMedio());
    assertEquals(mensaje, notif.getMensaje());
    assertTrue(notif.isCompletada()); // Marcada como completada!
  }
}
