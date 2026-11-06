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

### 1.3 Protocolo del servicio de logística: GraphQL en lugar de HTTP/REST (entrega 4)
**Requerimiento:** la entrega 4 pide reemplazar HTTP por gRPC (opción 1) **o** por GraphQL (opción 2) en las comunicaciones con el servicio extraído en la entrega 2, que es logística. Se elige una sola opción.

**Quién consume a logística:**
1. El panel web de la administración (navegador), para la flota y las rutas.
2. La aplicación del repartidor, para sus rutas y para reportar la posición del camión.
3. Donaciones (servicio a servicio), para mostrarle a la donante y a la entidad dónde está el camión.

**Comparación:**

| Criterio | gRPC | GraphQL |
| --- | --- | --- |
| Esfuerzo | Contrato `.proto`, generación de código con `protoc` en Maven y un servidor gRPC aparte de Javalin. | Un esquema `.graphqls` y un endpoint en el mismo Javalin con `graphql-java`. |
| Tooling | Plugin de `protoc` y stubs generados en los dos servicios. | Una dependencia (`graphql-java`); el cliente es un `POST` con JSON. |
| Cliente liviano (navegador) | No funciona directo: necesita gRPC-Web y un proxy (por ejemplo Envoy) que traduzca a HTTP/2. | `fetch` común, con la misma cookie de sesión y el mismo CORS que ya existían. |
| Despliegue | Un contenedor más (el proxy) y HTTP/2. La instancia tiene 1 GB de RAM compartido por las dos JVM. | Sin cambios: mismo contenedor, mismo puerto (9001) y HTTP/1.1. |
| Seguridad | Hay que rehacer la autenticación con interceptores de gRPC. | El filtro existente (`LogisticaSecurity`) sigue validando las credenciales; los permisos por operación pasan a los resolvers. |
| Rendimiento entre servicios | Mejor (binario y tipado). | JSON, igual que antes. |

**Decisión:** GraphQL. Logística tiene clientes en el navegador y en una aplicación, y con gRPC todos necesitarían un proxy extra, que complica el despliegue en una instancia que ya está justa de memoria. La ventaja de rendimiento de gRPC solo pesa en la llamada entre servicios, que es una consulta puntual (la posición del camión).

**Alcance:**
* El esquema `logistica/src/main/resources/graphql/logistica.graphqls` es el contrato de logística y la única fuente de verdad: camiones, rutas, repartidores y posiciones.
* Se retiraron los endpoints REST equivalentes (`/logistica/camiones`, `/logistica/rutas`, `/logistica/repartidores` y `/logistica/camiones/{patente}/posicion`): no quedan dos contratos vivos.
* Siguen por HTTP, de forma explícita:
  * `POST /logistica/login` y `/logistica/logout`: la sesión del repartidor no es una operación del dominio y su respuesta (el token) la consume la aplicación antes de poder usar GraphQL.
  * `POST /logistica/planificacion/callback`: su contrato lo define el planificador externo, no nosotros.
* Las llamadas de logística **hacia** donaciones (`DonacionesRemotasImpl`) siguen en REST: el requerimiento habla de las comunicaciones con el servicio extraído (logística), y donaciones no lo es.

**Consecuencias:**
* GraphQL usa un solo endpoint, así que el filtro HTTP ya no puede decidir permisos por URL: solo valida que haya credenciales, y cada resolver decide quién puede usar su operación (ver `logistica.graphql.Acceso`).
* Los errores de negocio vuelven con código 200 dentro de `errors`, con `extensions.codigo` (`NO_ENCONTRADO`, `DATOS_INVALIDOS`, `PROHIBIDO`). Las credenciales faltantes o inválidas siguen siendo 401 y 403.

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

**Justificación:** Los `UUID` previenen la colisión de claves generadas al integrar datos de múltiples bases de datos. Los identificadores autoincrementales (`bigserial` o genéricos `GenerationType.IDENTITY`) se dejaron únicamente para tablas fuertemente internas y locales a un microservicio (como `Camiones`), donde no se exportan como referencias foráneas en otros servicios.

`HistorialEstado` no lleva identificador: es un value object sin identidad ni ciclo de vida propios, mapeado con `@ElementCollection`. Su tabla cuelga de la donación con clave compuesta `(donacion_id, orden)`, y `orden` conserva la secuencia de las transiciones.

### 2.4 Control de Transacciones y Open-Session-In-View
**Decisión:** La gestión de transacciones HTTP (por request) y la inyección del `EntityManager` se manejan mediante el patrón `RequestPersistenceBoundary`.
**Justificación:** En lugar de abrir la transacción en el repositorio, se abre al iniciar el request HTTP y se commitea al finalizar. Esto resuelve problemas clásicos de JPA como *LazyInitializationException* cuando la capa de presentación intenta renderizar o serializar en JSON objetos proxy que aún no fueron fetcheados de la base de datos.
