package ar.edu.utn.donatrack.notificaciones;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donantes.Donante;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Bandeja web persistida: un aviso por titular y mensaje, aunque haya salido por varios medios. */
public class BandejaNotificacionesRepository
    implements BandejaNotificaciones, WithSimplePersistenceUnit {
  private static final int LIMITE = 100;
  private final Clock clock;

  public BandejaNotificacionesRepository(Clock clock) {
    this.clock = clock;
  }

  @Override
  public void registrar(List<Notificacion> notificaciones) {
    Map<String, AvisoBandeja> avisos = new LinkedHashMap<>();
    LocalDateTime ahora = LocalDateTime.now(clock);
    for (Notificacion notificacion : notificaciones) {
      notificacion
          .getTitular()
          .ifPresent(
              titular -> {
                if (titular instanceof Donante donante && donante.getId() != null) {
                  avisos.putIfAbsent(
                      "d" + donante.getId() + notificacion.getMensaje(),
                      AvisoBandeja.paraDonante(donante.getId(), notificacion.getMensaje(), ahora));
                } else if (titular instanceof EntidadBeneficiaria entidad
                    && entidad.getId() != null) {
                  avisos.putIfAbsent(
                      "e" + entidad.getId() + notificacion.getMensaje(),
                      AvisoBandeja.paraEntidad(entidad.getId(), notificacion.getMensaje(), ahora));
                }
              });
    }
    if (avisos.isEmpty()) {
      return;
    }
    // Se llama después de la transacción del caso de uso, pero algunos llamadores (tareas
    // programadas) pueden traer una abierta: en ese caso se suma a ella.
    if (entityManager().getTransaction().isActive()) {
      avisos.values().forEach(this::persist);
    } else {
      withTransaction(() -> avisos.values().forEach(this::persist));
    }
  }

  /** Los avisos más recientes de una persona donante. */
  public List<AvisoBandeja> deDonante(UUID donanteId) {
    return createQuery(
            "select a from AvisoBandeja a where a.donanteId = :id order by a.fecha desc",
            AvisoBandeja.class)
        .setParameter("id", donanteId)
        .setMaxResults(LIMITE)
        .getResultList();
  }

  /** Los avisos más recientes de una entidad beneficiaria. */
  public List<AvisoBandeja> deEntidad(Long entidadId) {
    return createQuery(
            "select a from AvisoBandeja a where a.entidadId = :id order by a.fecha desc",
            AvisoBandeja.class)
        .setParameter("id", entidadId)
        .setMaxResults(LIMITE)
        .getResultList();
  }

  /** Dentro de la transacción del llamador, borra todos los avisos de una persona donante. */
  public void borrarDeDonante(UUID donanteId) {
    createQuery("delete from AvisoBandeja a where a.donanteId = :id")
        .setParameter("id", donanteId)
        .executeUpdate();
  }

  public void marcarLeidos(List<AvisoBandeja> avisos) {
    if (avisos.stream().allMatch(AvisoBandeja::isLeido)) {
      return;
    }
    withTransaction(() -> avisos.forEach(a -> merge(a).marcarLeido()));
  }

  public long sinLeerDeDonante(UUID donanteId) {
    return createQuery(
            "select count(a) from AvisoBandeja a where a.donanteId = :id and a.leido = false",
            Long.class)
        .setParameter("id", donanteId)
        .getSingleResult();
  }

  public long sinLeerDeEntidad(Long entidadId) {
    return createQuery(
            "select count(a) from AvisoBandeja a where a.entidadId = :id and a.leido = false",
            Long.class)
        .setParameter("id", entidadId)
        .getSingleResult();
  }
}
