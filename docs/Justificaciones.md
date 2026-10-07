# Documento de Justificaciones de Diseño y Consideraciones Relacionales

## 1. Justificaciones de Diseño de Arquitectura (Microservicios)

### 1.1 División Estratégica del Dominio (Bounded Contexts)
El requerimiento establecía que el tamaño del equipo crecerá y se debía dividir el sistema en dos microservicios inicialmente.
**Decisión:** Se particionó el monolito en:
- **Microservicio de Donaciones:** Gestiona Donantes, Beneficiarios, Bienes, Necesidades y el ciclo de vida de la Donación (Estados, Matchmaking).
- **Microservicio de Logística:** Gestiona Camiones, Rutas, Destinos, Posiciones GPS e integración con Planificadores de ruteo.

**Justificación:**
Esta separación maximiza la **cohesión interna** y minimiza el **acoplamiento**. La logística cambia por razones operativas (ej. integrar nuevos GPS, cambiar algoritmos de ruteo), mientras que las donaciones cambian por reglas de negocio (ej. nuevos algoritmos de matchmaking, tipos de necesidades). Mantenerlos separados permite que distintos equipos trabajen de forma separa en cada microservicio.

### 1.2 Comunicación e Integración
**Decisión:** Integración sincrónica vía HTTP REST (`DonacionesRemotasImpl`).
**Justificación:** Cuando el chofer inicia la ruta, se hace un llamado REST a Donaciones para cambiar el estado. Se eligió REST sincrónico para tener una respuesta inmediata de si el lote fue actualizado correctamente o hubo un error de negocio. Si fuera asincrónico (ej. Kafka/RabbitMQ), requeriría manejar complejidad extra de eventual consistency y sagas para un flujo que, por ahora, es secuencial e imperativo.

## 2. Consideraciones de Diseño Relacional (ORM y JPA)

### 2.1 Aislamiento de Bases de Datos (Schemas Independientes)
**Decisión:** Las entidades no comparten esquema. En el modelo relacional físico (`DER-fisico.md`), existen `schema="donaciones"` y `schema="logistica"`.

**Justificación:** En una arquitectura orientada a microservicios, el patrón *Database-per-service* es necesario. Evita la "integración por base de datos". Para lograrlo en JPA, se reemplazaron las asociaciones de objetos físicas (`@OneToMany`, `@ManyToOne`) que cruzaban dominios por referencias lógicas (IDs).

### 2.2 Herencia de Entidades (Inheritance Mapping)
El sistema cuenta con jerarquías como `Donante` (PersonaHumana, PersonajeJuridica) y `EstadoDonacion`.

**Decisión:**
- Para `Donante`: Se utiliza `JOINED` para normalizar los datos en tablas hijas sin generar nulos innecesario en la tabla padre.
- Para `EstadoDonacion`: En el modelo relacional, no se persisten las subclases del State Pattern como tablas separadas. En su lugar, se persiste una columna `estado` (String) en la tabla `donaciones_donaciones` y se utilizan fábricas o el patrón State en memoria (Rehidratación del estado).

**Justificación:** Mapear el patrón State estrictamente en JPA (con tablas para cada estado) llevaría el uso de muchos `JOINs` al consultar donaciones. Mantener un campo `VARCHAR` para el estado actual optimiza las lecturas, mientras que el `HistorialEstado` mantiene la auditoría en otra tabla (`donaciones_historial_estado`).

### 2.3 Tipos de Datos y Claves Primarias
**Decisión:** Se migró el uso de identificadores a `UUID` para entidades distribuidas (como `Donacion` y `Ruta`).

**Justificación:** Los `UUID` previenen la colisión de claves generadas al integrar datos de múltiples bases de datos. Los identificadores autoincrementales (`bigserial` o genéricos `GenerationType.IDENTITY`) se dejaron únicamente para tablas fuertemente internas y locales a un microservicio (como `Camiones` o `HistorialEstado`), donde no se exportan como referencias foráneas en otros servicios.

### 2.4 Control de Transacciones y Open-Session-In-View
**Decisión:** La gestión de transacciones HTTP (por request) y la inyección del `EntityManager` se manejan mediante el patrón `RequestPersistenceBoundary`.
**Justificación:** En lugar de abrir la transacción en el repositorio, se abre al iniciar el request HTTP y se commitea al finalizar. Esto resuelve problemas clásicos de JPA como *LazyInitializationException* cuando la capa de presentación intenta renderizar o serializar en JSON objetos proxy que aún no fueron fetcheados de la base de datos.
