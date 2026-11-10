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

### 1.2.1 Interfaz web: MVC con template engine (cliente liviano, entrega 4)
**Decisión:** las pantallas se renderizan del lado del servidor, dentro del servicio de donaciones, con Javalin y plantillas Handlebars (patrón MVC).

| Capa | Dónde |
| --- | --- |
| Model | El dominio y los repositorios de `donaciones`. |
| View | Plantillas en `donaciones/src/main/resources/templates/`: `layout.hbs` con la navegación común y una plantilla por pantalla, incluida `error.hbs`. |
| Controller | `donaciones/src/main/java/ar/edu/utn/donatrack/web/controller/`: un controller por grupo de pantallas (`PaginasPublicasController`, `RegistroController`, `ActivacionController`, `RecepcionesController`, `DonanteVistasController`, `EntidadVistasController`, `AdminVistasController`, `PaginasDeError`) y `security/AuthController` (login y paneles). Están separados de los controladores de la API (`api/controller`), aunque compartan la misma aplicación Javalin. |

**Por qué el cliente es liviano:** el navegador recibe HTML ya armado y estilos estáticos; no hay una aplicación de una sola página ni un paso de compilación del frontend. Los formularios hacen `POST` al servidor, que valida, aplica el caso de uso y redirige (patrón *Post/Redirect/Get*); si hay un error, vuelve a renderizar la misma vista con el mensaje y lo que la persona había cargado. El JavaScript queda acotado a lo que no se puede hacer sin él:
* `mapas.js`: los mapas con Leaflet. Pide los puntos por JSON a la misma aplicación y, en el seguimiento, los vuelve a pedir cada 30 segundos.
* `panel.js`: las tres secciones de administración heredadas de la entrega 3 (registro de donaciones, rutas y catálogo), que llaman a `/api/v1` y al GraphQL de logística.

Así la interfaz usa el mismo dominio que la API, sin duplicar reglas, y la sesión es la cookie HTTP que pide el enunciado.

**Errores:** las pantallas web muestran páginas propias dentro del layout (400, 401, 403, 404, 405, 409 y 500, en `PaginasDeError`), con un enlace para volver al inicio o al panel y sin exponer el detalle técnico. La API sigue respondiendo sus errores en JSON.

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

### 1.4 Envío Inmediato de Notificaciones (Sin Estado ni Cola en Memoria)
**Decisión:** Se eliminó el atributo `estado` (`EstadoNotificacion`: `PENDIENTE`, `ENVIADA`, `FALLIDA`), el contador de reintentos y el repositorio en memoria (`NotificacionesRepository`), así como el doble `NotificadorSimulado` del árbol de producción (`src/main`) y la interfaz intermedia `Notificador`.
**Justificación:** Las notificaciones se despachan en el acto al ocurrir el evento mediante `ServicioNotificaciones` y `NotificadorPorCanal` hacia los gateways reales (Resend y Twilio). Mantener una lista en memoria y un estado de ciclo de vida en `Notificacion` respondía a un procesamiento diferido o en lote no requerido que ningún caso de uso leía; los dobles de prueba pertenecen exclusivamente a `src/test`.

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

## 4. Decisiones de las vistas de la entrega 4

Las vistas del enunciado 4 (issues #43 a #60) agregaron necesidades que el modelo de las entregas
anteriores no cubría. Esta sección explica cada decisión y la alternativa que se descartó.

### 4.1 Bandeja de avisos persistida (#49 y #54)
**Necesidad:** la persona donante y la entidad tienen que ver sus notificaciones en la web. Hasta la
entrega 3, una `Notificacion` solo conocía el medio de contacto al que se envió (un correo o un
teléfono), no a quién pertenecía, y quedaba en una lista en memoria.

**Decisión:** cada `Notificacion` conoce a su **titular** (el `Notificable`: donante o entidad). Al
enviarlas, `ServicioNotificaciones` las registra en una `BandejaNotificaciones`, que guarda un
`AvisoBandeja` por titular y mensaje en la tabla `aviso_bandeja`.

**Por qué un aviso por titular y no por envío:** una entidad con tres correos recibe tres envíos del
mismo evento. En la web importa qué pasó, no por qué canal llegó.

**Por qué no buscar el titular por el medio de contacto:** el mismo correo puede estar en una
persona donante y en una entidad. El evento sabe a quién le habla, así que se lo pasa a la
notificación.

**Consecuencia:** si guardar en la bandeja falla, el envío por correo o SMS igual se hace y la
falla queda en el log. La bandeja es un registro para la web, no parte de la entrega del aviso.

### 4.2 Fotos de recepción en Supabase Storage (#53)
**Decisión:** las fotos se suben a un bucket público de Supabase Storage y en `Donacion` se guarda
la URL pública, que se sirve desde su CDN. `AlmacenFotos` es un puerto con dos implementaciones:
`AlmacenFotosSupabase` para el despliegue y `AlmacenFotosLocal` para desarrollo y pruebas, que se
elige sola si faltan las credenciales.

**Alternativas descartadas:**
* Guardarlas en el disco del contenedor: se pierden en cada redeploy.
* Guardarlas en la base: agranda los backups y hace que la aplicación sirva imágenes.
* Pegar URLs a mano: peor experiencia y sin control de qué se publica.

**Seguridad:** el tipo de archivo se decide por los primeros bytes (JPG, PNG o WebP), no por la
extensión ni por lo que declara el navegador. El nombre lo genera el sistema. Hay un límite de 5
fotos de 5 MB cada una, y la clave de servicio de Supabase nunca llega al navegador.

### 4.3 Ubicación de las entidades por geocodificación (#43, #50 y #55)
**Necesidad:** los mapas necesitan coordenadas, y las entidades solo tenían la dirección escrita.

**Decisión:** `EntidadBeneficiaria` guarda `latitud` y `longitud`. Las completa `UbicadorEntidades`,
una tarea en segundo plano que convierte la dirección con Nominatim (OpenStreetMap). Corre cada
minuto y hace a lo sumo un pedido por segundo.

**Por qué en segundo plano y no al registrarse:** registrarse no tiene que depender de un servicio
externo, y Nominatim no admite más de un pedido por segundo. Una entidad nueva aparece en el mapa en
la pasada siguiente.

**Casos borde:**
* Si la dirección no se encuentra, la entidad se marca como intentada para no reintentarla en cada
  pasada.
* Si Nominatim no responde, se reintenta en la próxima pasada.
* Si la entidad cambia la dirección, se vuelve a ubicar.

**Alternativa descartada:** que la entidad marque su ubicación en un mapa al registrarse. Es más
preciso, pero suma un paso al registro.

### 4.4 Seguimiento de camiones: consulta periódica (#50 y #55)
**Decisión:** la vista de seguimiento pide las posiciones por JSON cada 30 segundos. Donaciones las
obtiene de logística por GraphQL (consulta `posicion`) con la clave interna. Si varias donaciones
viajan en el mismo camión, lo consulta una sola vez.

**Alternativa descartada:** WebSockets o *server-sent events*. Darían tiempo real estricto, pero
logística no publica eventos de posición, y el GPS informa cada pocos segundos, así que 30
segundos alcanzan para saber cuándo llega el camión. Una consulta periódica es un `fetch` y
respeta la idea de cliente liviano.

**Privacidad:** el JSON de seguimiento y el del mapa público no incluyen datos de la persona
donante. El público solo muestra la entidad, la fecha, los bienes y las fotos.

### 4.5 Importación de más de 10.000 donantes (#60)
**Decisión:** la importación corre en segundo plano y la vista muestra el progreso. El archivo se lee
fila por fila y se guarda en lotes de 500 por transacción, vaciando el contexto de persistencia
después de cada lote. Las filas inválidas no frenan la importación: se cuentan y se listan las
primeras 50 con su número de fila.

**Cuentas pendientes de activación:** cada persona importada recibe un usuario **sin contraseña**.
La activa en `/activar` probando quién es con su correo y su número de documento, y ahí elige la
contraseña. Así no hay que hashear 10.000 contraseñas con BCrypt (unos 10 minutos) ni mandar 10.000
contraseñas temporales por correo. Si el correo o el documento no coinciden, la respuesta es la
misma en los dos casos, para no revelar qué correos tienen cuenta.

**Medición:** `ImportadorDonantesTest` importa 10.500 filas en unos 8 segundos.

### 4.6 Camiones administrados desde donaciones (#59)
**Decisión:** la pantalla de camiones vive en donaciones, como todas las vistas (ver
[MVC en 1.2.1](#121-interfaz-web-mvc-con-template-engine-cliente-liviano-entrega-4)), y opera la
flota por el GraphQL de logística con la **sesión de la persona administradora**. Donaciones
reenvía su cookie, así que logística decide si tiene permiso, igual que si la llamara el navegador.
No se usa la clave interna, que daría permisos de servicio a cualquier usuario de la web.

### 4.7 Planificación de rutas en horario de baja carga (#34)
**Decisión:** la planificación la corre `PlanificacionRutasRunner` (paquete `schedules` de
logística), un programa autónomo que invoca un planificador externo (cronjob) en horario de baja
carga. El horario, la zona horaria y los reintentos se configuran en el planificador externo, no en
el código. Lo mismo vale para el aviso por ausencia de donantes (`AusenciaDonantesRunner`).

### 4.8 Sin edad en la persona humana
El enunciado 1 pide la edad de la persona humana. El equipo decidió no pedirla: no la usa ningún
requerimiento (ni el matchmaking, ni las notificaciones, ni los incentivos) y es un dato personal
más para resguardar. Se quitó del dominio, de la API, del registro y de los diagramas.
