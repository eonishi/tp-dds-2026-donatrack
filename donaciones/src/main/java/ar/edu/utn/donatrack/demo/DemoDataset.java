package ar.edu.utn.donatrack.demo;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.beneficiarios.Necesidad;
import ar.edu.utn.donatrack.beneficiarios.NecesidadRecurrente;
import ar.edu.utn.donatrack.beneficiarios.TipoPeriodo;
import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.bienes.Categoria;
import ar.edu.utn.donatrack.bienes.EstadoUso;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.bienes.SubcategoriasRepository;
import ar.edu.utn.donatrack.bienes.UnidadMedida;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.EstadoDonacion;
import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.Genero;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.PersonajeJuridica;
import ar.edu.utn.donatrack.donantes.TipoDocumento;
import ar.edu.utn.donatrack.donantes.TipoOrganizacion;
import ar.edu.utn.donatrack.notificaciones.AvisoBandeja;
import ar.edu.utn.donatrack.propuestas.PropuestaDeAsignacion;
import ar.edu.utn.donatrack.propuestas.matchmaking.MatchCandidato;
import ar.edu.utn.donatrack.security.AuthService;
import ar.edu.utn.donatrack.security.Role;
import ar.edu.utn.donatrack.security.Usuario;
import ar.edu.utn.donatrack.security.UsuariosRepository;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Opt-in, atomic demo fixtures; never sends notifications or invokes another service. */
public final class DemoDataset implements WithSimplePersistenceUnit {
  public static final String VERSION = "donations-demo-v1";
  public static final String VIEWS_VERSION = "donations-demo-views-v2";
  public static final String PASSWORD = "Demo12345!";
  public static final List<String> EMAILS =
      List.of(
          "demo.admin@example.test", "demo.humana@example.test",
          "demo.juridica@example.test", "demo.entidad@example.test");

  /** Initializes versioned demo records only when explicitly enabled in the environment. */
  public void initialize(Map<String, String> environment, AuthService auth, Clock clock) {
    if (!"true".equalsIgnoreCase(environment.get("DONATRACK_DEMO_DATA"))) {
      return;
    }
    withTransaction(
        () -> {
          if (find(DemoDatasetVersion.class, VERSION) == null) {
            if (EMAILS.stream().anyMatch(auth::existeUsuario)) {
              throw new IllegalStateException(
                  "Demo account collision: existing accounts were not modified.");
            }
            if (createQuery(
                        "select count(d) from Donante d "
                            + "where d.numDocumento in ('99000001', '30999999991')",
                        Long.class)
                    .getSingleResult()
                > 0) {
              throw new IllegalStateException(
                  "Demo document collision: existing donors were not modified.");
            }
            seed(auth, clock);
            persist(new DemoDatasetVersion(VERSION));
          }
          if (find(DemoDatasetVersion.class, VIEWS_VERSION) == null) {
            seedPopulatedViews(clock);
            persist(new DemoDatasetVersion(VIEWS_VERSION));
          }
        });
  }

  private void seed(AuthService auth, Clock clock) {
    LocalDate today = LocalDate.now(clock);
    final LocalDateTime now = LocalDateTime.now(clock);
    final SubCategoria food = category("[DEMO] Arroz", Categoria.ALIMENTO);
    final SubCategoria chairs = category("[DEMO] Sillas", Categoria.MOBILIARIO);
    final SubCategoria clothing = category("[DEMO] Abrigos", Categoria.VESTIMENTA);
    PersonaHumana human =
        new PersonaHumana(
            new CorreoElectronico(EMAILS.get(1)),
            "Ana",
            "Ejemplo",
            "99000001",
            TipoDocumento.DNI,
            Genero.FEMENINO,
            "Av. Medrano 951, Buenos Aires");
    human.setEdad(32);
    PersonajeJuridica legal =
        new PersonajeJuridica(
            new CorreoElectronico(EMAILS.get(2)),
            "30999999991",
            TipoDocumento.CUIT,
            "[DEMO] Cooperativa Solidaria",
            TipoOrganizacion.EMPRESA,
            "Alimentos y equipamiento");
    persist(human);
    persist(legal);
    EntidadBeneficiaria entity =
        new EntidadBeneficiaria(
            "[DEMO] Comedor Encuentro", "Av. Medrano 951, Buenos Aires", "+5491100000000");
    entity.ubicar(-34.5985, -58.4201);
    entity.agregarCorreo(new CorreoElectronico(EMAILS.get(3)));
    Necesidad pending =
        new Necesidad(chairs, 10, UnidadMedida.UNIDADES, "[DEMO] Sillas pendientes");
    Necesidad partial =
        new Necesidad(
            food, 100, UnidadMedida.KILOGRAMOS, "[DEMO] Alimentos parcialmente cubiertos");
    Necesidad satisfied =
        new Necesidad(clothing, 5, UnidadMedida.UNIDADES, "[DEMO] Abrigos cubiertos");
    NecesidadRecurrente weekly = recurring(food, TipoPeriodo.SEMANAL, "[DEMO] Alimentos semanales");
    NecesidadRecurrente monthly =
        recurring(clothing, TipoPeriodo.MENSUAL, "[DEMO] Abrigos mensuales");
    entity.agregarNecesidadExtraordinaria(pending);
    entity.agregarNecesidadExtraordinaria(partial);
    entity.agregarNecesidadExtraordinaria(satisfied);
    entity.agregarNecesidadRecurrente(weekly);
    entity.agregarNecesidadRecurrente(monthly);
    persist(entity);
    auth.agregarUsuario(EMAILS.get(0), PASSWORD, Set.of(Role.ADMIN), null, null);
    auth.agregarUsuario(EMAILS.get(1), PASSWORD, Set.of(Role.DONANTE), human.getId(), null);
    auth.agregarUsuario(EMAILS.get(2), PASSWORD, Set.of(Role.DONANTE), legal.getId(), null);
    auth.agregarUsuario(EMAILS.get(3), PASSWORD, Set.of(Role.BENEFICIARIO), null, entity.getId());
    for (EstadoDonacion state : EstadoDonacion.values()) {
      boolean expired = state == EstadoDonacion.VENCIDA;
      boolean isFood = expired || state == EstadoDonacion.EN_DEPOSITO;
      boolean isClothing = state == EstadoDonacion.ENTREGADA;
      SubCategoria subcategory = isFood ? food : isClothing ? clothing : chairs;
      Bien good =
          new Bien(
              "[DEMO] " + subcategory.getNombre() + " · " + state,
              "",
              isFood ? 25 : 5,
              isFood ? UnidadMedida.KILOGRAMOS : UnidadMedida.UNIDADES,
              isFood ? today.plusDays(expired ? -2 : 30) : null,
              isFood
                  ? EstadoUso.NO_APLICA
                  : state == EstadoDonacion.ENTREGA_FALLIDA ? EstadoUso.USADO : EstadoUso.NUEVO,
              subcategory);
      Donacion donation = new Donacion(isFood ? legal : human, List.of(good));
      persist(donation);
      advance(donation, entity, state);
      if (state == EstadoDonacion.EN_DEPOSITO || state == EstadoDonacion.ASIGNACION_REALIZADA) {
        PropuestaDeAsignacion proposal =
            new PropuestaDeAsignacion(
                donation,
                List.of(
                    List.of(
                        new MatchCandidato(
                            entity, entity.calcularPuntajeCompatibilidad(donation)))));
        if (state == EstadoDonacion.ASIGNACION_REALIZADA) {
          proposal.confirmar(entity);
        }
        persist(proposal);
      }
    }
    // An additional completed delivery gives food needs a partial, domain-derived fulfillment.
    Donacion partialDelivery =
        new Donacion(
            legal,
            List.of(
                new Bien(
                    "[DEMO] Primera entrega de arroz",
                    "",
                    25,
                    UnidadMedida.KILOGRAMOS,
                    today.plusDays(15),
                    EstadoUso.NO_APLICA,
                    food)));
    persist(partialDelivery);
    advance(partialDelivery, entity, EstadoDonacion.ENTREGADA);
    AvisoBandeja unread =
        AvisoBandeja.paraDonante(human.getId(), "[DEMO] Tenés una donación asignada.", now);
    persist(unread);
    AvisoBandeja read =
        AvisoBandeja.paraEntidad(
            entity.getId(), "[DEMO] Recibimos tu confirmación de entrega.", now);
    read.marcarLeido();
    persist(read);
  }

  /** Additive upgrade: existing fixtures and user edits are never reset. */
  private void seedPopulatedViews(Clock clock) {
    UsuariosRepository users = new UsuariosRepository();
    Usuario beneficiary = users.buscarPorEmail(EMAILS.get(3)).orElseThrow();
    EntidadBeneficiaria entity =
        find(EntidadBeneficiaria.class, beneficiary.getEntidadId().orElseThrow());
    if (!beneficiary.hasRole(Role.BENEFICIARIO)
        || entity == null
        || !"[DEMO] Comedor Encuentro".equals(entity.getRazonSocial())) {
      throw new IllegalStateException("Demo beneficiary association changed; upgrade aborted.");
    }
    SubCategoria food = category("[DEMO] Arroz", Categoria.ALIMENTO);
    SubCategoria chairs = category("[DEMO] Sillas", Categoria.MOBILIARIO);
    SubCategoria clothing = category("[DEMO] Abrigos", Categoria.VESTIMENTA);
    // Both donors have a useful history, a current shipment and every donation state.
    for (int index = 1; index <= 2; index++) {
      Usuario user = users.buscarPorEmail(EMAILS.get(index)).orElseThrow();
      Donante donor = find(Donante.class, user.getDonanteId().orElseThrow());
      String document = index == 1 ? "99000001" : "30999999991";
      if (!user.hasRole(Role.DONANTE)
          || donor == null
          || !document.equals(donor.getNumDocumento())) {
        throw new IllegalStateException("Demo donor association changed; upgrade aborted.");
      }
      for (EstadoDonacion state : EstadoDonacion.values()) {
        boolean perishable = state == EstadoDonacion.VENCIDA || state == EstadoDonacion.EN_DEPOSITO;
        SubCategoria subcategory =
            perishable ? food : state == EstadoDonacion.ENTREGADA ? clothing : chairs;
        Bien good =
            new Bien(
                "[DEMO V2] " + user.getUsername() + " · " + state,
                "",
                perishable ? 12 : 3,
                perishable ? UnidadMedida.KILOGRAMOS : UnidadMedida.UNIDADES,
                perishable
                    ? LocalDate.now(clock).plusDays(state == EstadoDonacion.VENCIDA ? -1 : 45)
                    : null,
                perishable
                    ? EstadoUso.NO_APLICA
                    : state == EstadoDonacion.ENTREGA_FALLIDA ? EstadoUso.USADO : EstadoUso.NUEVO,
                subcategory);
        Donacion donation = new Donacion(donor, List.of(good));
        persist(donation);
        advance(donation, entity, state);
      }
      for (int notice = 0; notice < 4; notice++) {
        AvisoBandeja message =
            AvisoBandeja.paraDonante(
                donor.getId(),
                List.of(
                        "[DEMO] Tu aporte está registrado en el depósito.",
                        "[DEMO] Comedor Encuentro recibirá tu próxima donación.",
                        "[DEMO] El camión DEMO-01 está en camino con tu aporte.",
                        "[DEMO] Gracias: una de tus donaciones ya fue entregada.")
                    .get(notice),
                LocalDateTime.now(clock).minusHours(notice));
        if (notice >= 2) {
          message.marcarLeido();
        }
        persist(message);
      }
    }
    entity.agregarNecesidadExtraordinaria(
        new Necesidad(
            chairs, 40, UnidadMedida.UNIDADES, "[DEMO V2] Equipamiento para ampliar el comedor"));
    entity.agregarNecesidadExtraordinaria(
        new Necesidad(
            clothing, 60, UnidadMedida.UNIDADES, "[DEMO V2] Campaña de abrigo pendiente"));
    for (int notice = 0; notice < 4; notice++) {
      AvisoBandeja message =
          AvisoBandeja.paraEntidad(
              entity.getId(),
              List.of(
                      "[DEMO] Hay aportes asignados para tu comedor.",
                      "[DEMO] Tenés entregas en camino para confirmar en Recepciones.",
                      "[DEMO] Una entrega quedó registrada correctamente.",
                      "[DEMO] Una entrega falló: el establecimiento estaba cerrado.")
                  .get(notice),
              LocalDateTime.now(clock).minusHours(notice));
      if (notice >= 2) {
        message.marcarLeido();
      }
      persist(message);
    }
  }

  private SubCategoria category(String name, Categoria category) {
    SubCategoria subcategory =
        new SubcategoriasRepository().obtenerOrCrear(name, category, new java.util.HashMap<>());
    if (subcategory.getCategoria() != category) {
      throw new IllegalStateException("Demo catalog collision: " + name);
    }
    return subcategory;
  }

  private NecesidadRecurrente recurring(
      SubCategoria subcategory, TipoPeriodo period, String description) {
    NecesidadRecurrente need =
        new NecesidadRecurrente(subcategory, 50, UnidadMedida.UNIDADES, description);
    // Food quantities must use the same unit as their goods.
    if (subcategory.getCategoria() == Categoria.ALIMENTO) {
      need.actualizar(subcategory, 50, UnidadMedida.KILOGRAMOS, description);
    }
    need.setPeriodo(period);
    return need;
  }

  private void advance(Donacion donation, EntidadBeneficiaria entity, EstadoDonacion target) {
    if (target == EstadoDonacion.EN_DEPOSITO) {
      return;
    }
    if (target == EstadoDonacion.VENCIDA) {
      donation.marcarVencida();
      return;
    }
    donation.confirmarEntidadBeneficiaria(entity);
    if (target == EstadoDonacion.ASIGNACION_REALIZADA) {
      return;
    }
    donation.planificarRuta();
    if (target == EstadoDonacion.LISTA_PARA_ENTREGAR) {
      return;
    }
    donation.registrarCamion("DEMO-01");
    donation.iniciarTraslado(null);
    if (target == EstadoDonacion.ENTREGADA) {
      donation.confirmarEntrega();
    } else if (target == EstadoDonacion.ENTREGA_FALLIDA) {
      donation.reportarFalla("[DEMO] Entidad cerrada al llegar.");
    }
  }
}
