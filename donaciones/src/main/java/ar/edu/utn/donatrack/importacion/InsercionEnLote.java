package ar.edu.utn.donatrack.importacion;

import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.MedioDeContacto;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.PersonajeJuridica;
import ar.edu.utn.donatrack.security.Role;
import ar.edu.utn.donatrack.security.Usuario;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.type.Type;

/**
 * Guarda un lote de donantes nuevos, sus medios de contacto y sus cuentas con JDBC en lote: un
 * viaje a la base por tabla, no uno por fila.
 *
 * <p><b>Por qué no alcanza con {@code persist()}.</b> {@link Donante} usa herencia JOINED: cada
 * persona humana son dos INSERT, uno en la tabla de {@code Donante} y otro en la de {@code
 * PersonaHumana}. Hibernate los manda alternados y cada cambio de tabla corta el lote de JDBC, así
 * que con 20.000 filas eran ~40.000 viajes. Contra una base en otra región (la app en us-east-2,
 * Supabase en us-east-1; desde una casa en Argentina, ~145 ms por viaje) eso son minutos u horas.
 * Acá se arma cada tabla completa y se manda de una vez.
 *
 * <p><b>Por qué no es SQL escrito a mano.</b> Las tablas (con su esquema), las columnas, los
 * generadores de id y los tipos con los que se pasa cada valor salen de la metadata de Hibernate,
 * la misma que usa para el resto de la app. Si cambia el mapeo de una columna, esto lo sigue. Lo
 * que sí hay que mantener a mano es <i>qué</i> propiedades se insertan: si una entidad suma un
 * campo que el CSV llena, hay que agregarlo acá (el test del importador lo verifica).
 *
 * <p>Corre dentro de la transacción del llamador y deja el contexto de persistencia sin tocar.
 */
final class InsercionEnLote {
  // La misma sesión vista de dos formas: Session para doWork, la interna para los generadores y tipos.
  private final Session session;
  private final SharedSessionContractImplementor sesion;
  private final AbstractEntityPersister donantes;
  private final AbstractEntityPersister humanas;
  private final AbstractEntityPersister juridicas;
  private final AbstractEntityPersister medios;
  private final AbstractCollectionPersister mediosDelDonante;
  private final AbstractEntityPersister usuarios;
  private final AbstractCollectionPersister roles;

  InsercionEnLote(EntityManager entityManager) {
    this.session = entityManager.unwrap(Session.class);
    this.sesion = entityManager.unwrap(SharedSessionContractImplementor.class);
    MetamodelImplementor modelo =
        entityManager
            .getEntityManagerFactory()
            .unwrap(SessionFactoryImplementor.class)
            .getMetamodel();
    this.donantes = (AbstractEntityPersister) modelo.entityPersister(Donante.class);
    this.humanas = (AbstractEntityPersister) modelo.entityPersister(PersonaHumana.class);
    this.juridicas = (AbstractEntityPersister) modelo.entityPersister(PersonajeJuridica.class);
    this.medios = (AbstractEntityPersister) modelo.entityPersister(MedioDeContacto.class);
    this.mediosDelDonante =
        (AbstractCollectionPersister)
            modelo.collectionPersister(Donante.class.getName() + ".mediosDeContacto");
    this.usuarios = (AbstractEntityPersister) modelo.entityPersister(Usuario.class);
    this.roles =
        (AbstractCollectionPersister) modelo.collectionPersister(Usuario.class.getName() + ".roles");
  }

  /** Un id nuevo para el donante, del mismo generador que usaría {@code persist()}. */
  UUID idPara(Donante donante) {
    return (UUID) donantes.getIdentifierGenerator().generate(sesion, donante);
  }

  /** Un id nuevo para la cuenta, del mismo generador que usaría {@code persist()}. */
  UUID idPara(Usuario usuario) {
    return (UUID) usuarios.getIdentifierGenerator().generate(sesion, usuario);
  }

  /**
   * Inserta los donantes (ya con su id de {@link #idPara(Donante)}) y las cuentas.
   *
   * @param nuevos cada donante con el id que se le reservó
   * @param cuentas cada cuenta con el id que se le reservó
   */
  void insertar(Map<UUID, Donante> nuevos, Map<UUID, Usuario> cuentas) {
    if (nuevos.isEmpty() && cuentas.isEmpty()) {
      return;
    }
    // La tabla de Donante y la de cada subclase. El contacto predeterminado apunta a un medio que
    // todavía no existe, así que va vacío y se completa al final (la FK no lo dejaría antes).
    Insercion donante =
        new Insercion(
            donantes.getTableName(),
            columna(donantes.getIdentifierColumnNames(), donantes.getIdentifierType()),
            columna(donantes, "documento"),
            columna(donantes, "numDocumento"));
    Insercion humana =
        new Insercion(
            humanas.getPropertyTableName("nombre"),
            columna(humanas.getIdentifierColumnNames(), humanas.getIdentifierType()),
            columna(humanas, "nombre"),
            columna(humanas, "apellido"),
            columna(humanas, "edad"),
            columna(humanas, "genero"),
            columna(humanas, "direccion"));
    Insercion juridica =
        new Insercion(
            juridicas.getPropertyTableName("razonSocial"),
            columna(juridicas.getIdentifierColumnNames(), juridicas.getIdentifierType()),
            columna(juridicas, "razonSocial"),
            columna(juridicas, "tipoOrganizacion"),
            columna(juridicas, "rubro"));
    // MedioDeContacto es SINGLE_TABLE: una tabla con el discriminador que dice la subclase.
    Insercion medio =
        new Insercion(
            medios.getTableName(),
            columna(medios.getIdentifierColumnNames(), medios.getIdentifierType()),
            new Columna(medios.getDiscriminatorColumnName(), medios.getDiscriminatorType()),
            columna(medios, "contacto"),
            columna(mediosDelDonante.getKeyColumnNames(), mediosDelDonante.getKeyType()));
    Actualizacion contactoPredeterminado =
        new Actualizacion(
            donantes.getTableName(),
            columna(donantes.getPropertyColumnNames("contactoPredeterminado"),
                medios.getIdentifierType()),
            columna(donantes.getIdentifierColumnNames(), donantes.getIdentifierType()));
    Insercion usuario =
        new Insercion(
            usuarios.getTableName(),
            columna(usuarios.getIdentifierColumnNames(), usuarios.getIdentifierType()),
            columna(usuarios, "email"),
            columna(usuarios, "passwordHash"),
            columna(usuarios, "donanteId"));
    Insercion rol =
        new Insercion(
            roles.getTableName(),
            columna(roles.getKeyColumnNames(), roles.getKeyType()),
            columna(roles.getElementColumnNames(), roles.getElementType()));

    nuevos.forEach(
        (id, d) -> {
          donante.fila(id, d.getDocumento(), d.getNumDocumento());
          if (d instanceof PersonaHumana h) {
            humana.fila(id, h.getNombre(), h.getApellido(), h.getEdad(), h.getGenero(),
                h.getDireccion());
          } else if (d instanceof PersonajeJuridica j) {
            juridica.fila(id, j.getRazonSocial(), j.getTipoOrganizacion(), j.getRubro());
          }
          Map<MedioDeContacto, Serializable> ids = new IdentityHashMap<>();
          for (MedioDeContacto m : d.getMediosDeContacto()) {
            Serializable idMedio = medios.getIdentifierGenerator().generate(sesion, m);
            ids.put(m, idMedio);
            medio.fila(idMedio, discriminador(m), m.getContacto(), id);
          }
          MedioDeContacto predeterminado = d.getMedioContactoPredeterminado();
          if (predeterminado != null && ids.containsKey(predeterminado)) {
            contactoPredeterminado.fila(ids.get(predeterminado), id);
          }
        });
    cuentas.forEach(
        (id, u) -> {
          usuario.fila(id, u.getEmail(), u.getPasswordHash(), u.getDonanteId().orElse(null));
          for (Role r : u.getRoles()) {
            rol.fila(id, r);
          }
        });

    // El orden respeta las claves foráneas: primero a quien se apunta, después quien apunta.
    session.doWork(
            conexion -> {
              donante.ejecutar(conexion);
              humana.ejecutar(conexion);
              juridica.ejecutar(conexion);
              medio.ejecutar(conexion);
              contactoPredeterminado.ejecutar(conexion);
              usuario.ejecutar(conexion);
              rol.ejecutar(conexion);
            });
  }

  private Object discriminador(MedioDeContacto medio) {
    return ((AbstractEntityPersister) sesion.getEntityPersister(null, medio))
        .getDiscriminatorValue();
  }

  private static Columna columna(AbstractEntityPersister persister, String propiedad) {
    return columna(persister.getPropertyColumnNames(propiedad), persister.getPropertyType(propiedad));
  }

  private static Columna columna(String[] nombres, Type tipo) {
    if (nombres.length != 1) {
      throw new IllegalStateException("Se esperaba una sola columna y hay " + nombres.length);
    }
    return new Columna(nombres[0], tipo);
  }

  private record Columna(String nombre, Type tipo) {}

  /** Una sentencia con sus filas, que se manda en un solo lote de JDBC. */
  private abstract class Sentencia {
    private final String sql;
    private final List<Columna> columnas;
    private final List<Object[]> filas = new ArrayList<>();

    Sentencia(String sql, List<Columna> columnas) {
      this.sql = sql;
      this.columnas = columnas;
    }

    void fila(Object... valores) {
      filas.add(valores);
    }

    void ejecutar(Connection conexion) throws SQLException {
      if (filas.isEmpty()) {
        return;
      }
      try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
        for (Object[] valores : filas) {
          for (int i = 0; i < columnas.size(); i++) {
            // El tipo de Hibernate sabe pasar cada valor: un UUID es uuid en Postgres y binario en
            // HSQLDB, un enum va como texto por @Enumerated(STRING), etc.
            columnas.get(i).tipo().nullSafeSet(sentencia, valores[i], i + 1, sesion);
          }
          sentencia.addBatch();
        }
        sentencia.executeBatch();
      }
    }
  }

  private final class Insercion extends Sentencia {
    Insercion(String tabla, Columna... columnas) {
      super(
          "insert into " + tabla + " ("
              + List.of(columnas).stream().map(Columna::nombre).collect(Collectors.joining(", "))
              + ") values ("
              + List.of(columnas).stream().map(c -> "?").collect(Collectors.joining(", "))
              + ")",
          List.of(columnas));
    }
  }

  private final class Actualizacion extends Sentencia {
    Actualizacion(String tabla, Columna asignada, Columna clave) {
      super(
          "update " + tabla + " set " + asignada.nombre() + " = ? where " + clave.nombre() + " = ?",
          List.of(asignada, clave));
    }
  }
}
