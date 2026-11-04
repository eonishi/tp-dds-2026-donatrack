# DonaTrack — Modelo del dominio por requerimiento

Diagramas de clases (Mermaid) del dominio de DonaTrack, uno por requerimiento de los enunciados [`enunciado-1.md`](./enunciado/enunciado-1.md) y [`enunciado-2.md`](./enunciado/enunciado-2.md).

Cada diagrama es el **fragmento mínimo del dominio** que cumple ese requerimiento; el diagrama completo está en [`diagrama-clases-dominio.mermaid`](./diagrama-clases-dominio.mermaid).

> Piezas fuera del dominio (repositorios, servicios, schedulers) se etiquetan `<<infraestructura>>` y solo se incluyen cuando el requerimiento las necesita.


## Entrega 1 — Requerimientos de dominio


### R1 — Gestión de personas donantes

**Decisión:** `Donante` es una clase abstracta con dos especializaciones (`PersonaHumana` y `PersonajeJuridica`) porque cada tipo registra datos distintos. Los medios de contacto forman una jerarquía (`MedioDeContacto` con email, WhatsApp y SMS) que permite sumar canales sin tocar `Donante`.

![R1 — Gestión de personas donantes](assets/e1-req1-gestion-donantes.svg)


### R2 — Gestión de las donaciones resultantes

**Decisión:** La carga única (`CargaDonacion`) genera varias donaciones separando automáticamente por `SegmentacionKey` (subcategoría + vencimiento + estado de uso). Cada `Donacion` queda ligada a una única subcategoría, la unidad mínima de asignación.

![R2 — Gestión de las donaciones resultantes](assets/e1-req2-gestion-donaciones.svg)


### R3 — Gestión de entidades beneficiarias y sus necesidades

**Decisión:** `EntidadBeneficiaria` mantiene dos colecciones separadas (`Necesidad` extraordinaria y `NecesidadRecurrente`). `Necesidad` modela la satisfacción parcial por cantidades; la recurrente agrega el período (`TipoPeriodo`). `RecepcionDonacion` registra qué donaciones la cubren.

![R3 — Gestión de entidades beneficiarias y sus necesidades](assets/e1-req3-entidades-beneficiarias.svg)


### R4 — Trazabilidad de los estados de las donaciones

**Decisión:** Patrón **State**: `Donacion` delega las transiciones en `EstadoDonacion` (abstracto) con 7 estados concretos. Cada cambio queda registrado en `HistorialEstado` (fecha, estado, justificación) para auditoría.

![R4 — Trazabilidad de los estados de las donaciones](assets/e1-req4-trazabilidad-estados.svg)


### R5 — Importación masiva de donantes en CSV

**Decisión:** `ImportadorDonantesCsv` parsea a `DonanteCsv` y mapea a donante humano o jurídico. El upsert por email (`DonantesRepository.upsert`) resuelve la regla del enunciado: "si existe se actualiza con `merge`, si no se crea".

![R5 — Importación masiva de donantes en CSV](assets/e1-req5-importacion-csv.svg)


## Entrega 1 — Alcance


### Notificaciones — Primera iteración

**Decisión:** Doble dispatch: `MedioDeContacto.aceptar(Notificador)` delega el envío en el `Notificador` (interfaz), simulado por `NotificadorSimulado`. `Notificacion` guarda destinatario, mensaje y estado (PENDIENTE/ENVIADA/FALLIDA).

![Notificaciones — Primera iteración](assets/e1-notificaciones-iteracion1.svg)


## Entrega 2 — Requerimientos de dominio


### R1 — Trazabilidad de los estados de las donaciones

**Decisión:** Mismo fragmento que el R4 de la Entrega 1 (patrón State + `HistorialEstado`).

![R1 — Trazabilidad de los estados de las donaciones](assets/e2-req1-trazabilidad-estados.svg)


### R2 — Notificaciones por distintos medios ante eventos

**Decisión:** Template Method: cada evento (`EventoNotificable` + 6 subclases) genera sus `Notificacion` en `generarNotificaciones()`. Los payloads (`ComprobanteEntrega`, `InfoMapa`, `SinPayload`) forman una interfaz con los diferentes tipos de payloads posibles. `Donante` y `EntidadBeneficiaria` implementan `Notificable`.

![R2 — Notificaciones por distintos medios ante eventos](assets/e2-req2-notificaciones-eventos.svg)


### R3 — Ejecución asíncrona de los algoritmos de asignación

**Decisión:** Strategy: `AlgoritmoMatchmaking` con dos implementaciones intercambiables. Un planificador externo (cronjob) invoca `MatchmakingRunner` una vez por día (baja carga); el runner ejecuta `ServicioMatchmakingDonaciones`, que genera `PropuestaDeAsignacion` con la intersección de las ejecuciones —o su unión cuando no hay coincidencias— para confirmación administrativa (reificación).

![R3 — Ejecución asíncrona de los algoritmos de asignación](assets/e2-req3-matchmaking-asincrono.svg)


### R4 — Operaciones vía API REST

**Decisión:** El código actual es dominio puro (sin capa REST todavía). El fragmento muestra los agregados que la API debe exponer y las operaciones del enunciado; *los controladores quedan pendientes*.

![R4 — Operaciones vía API REST](assets/e2-req4-api-rest.svg)


### R5 — Planes de rutas en horarios de baja carga (componente externo)

**Decisión:** Port hacia el componente externo: `PlanificadorRutas` recibe `LoteDonaciones` y devuelve `LoteId`. El resultado (`ResultadoPlanificacion`) se materializa en el agregado `Ruta` (camión + destinos). `ServicioPlanificacionRutas` orquesta.

![R5 — Planes de rutas en horarios de baja carga (componente externo)](assets/e2-req5-planificacion-rutas.svg)


### R6 — Choferes informan el comienzo de su ruta

**Decisión:** `Ruta.iniciar()` marca la ruta en curso y las donaciones de sus destinos pasan a "En traslado" (`Donacion.iniciarTraslado`).

![R6 — Choferes informan el comienzo de su ruta](assets/e2-req6-inicio-ruta.svg)


### R7 — Monitoreo en tiempo real de los camiones

**Decisión:** `MonitorRutas` procesa `LecturaPosicion` (camión, coordenada, velocidad, timestamp), descarta lecturas futuras y correlaciona con la ruta activa. El inicio de ruta notifica con enlace al mapa (`InfoMapa`).

![R7 — Monitoreo en tiempo real de los camiones](assets/e2-req7-monitoreo-tiempo-real.svg)


### R8 — Gestión de la recepción de la entrega

**Decisión:** `EntidadBeneficiaria.registrarRecepcion` crea `RecepcionDonacion` y actualiza las necesidades. La confirmación deja la donación "Entregada" (con camión responsable) o "Entrega fallida" con justificación y retorno al depósito.

![R8 — Gestión de la recepción de la entrega](assets/e2-req8-recepcion-entrega.svg)


## Entrega 2 — Requerimientos de implementación


### Impl 1 — URL de callback del planificador

**Decisión:** El componente externo llama de vuelta a `ServicioPlanificacionRutas.procesarResultado`, que crea y persiste las `Ruta` y pasa las donaciones asignadas a "Lista para entregar".

![Impl 1 — URL de callback del planificador](assets/e2-impl1-callback-planificacion.svg)


### Impl 2 — Solicitudes al proveedor en lotes (máx. 100)

**Decisión:** `ServicioPlanificacionRutas.partirEnLotes` arma lotes de hasta 100 donaciones y `LoteDonaciones` valida el tope como invariante en su constructor (record).

![Impl 2 — Solicitudes al proveedor en lotes (máx. 100)](assets/e2-impl2-lotes-100.svg)


### Impl 3 — Replanificación de donaciones sin asignar

**Decisión:** Las donaciones que el planificador no asigna permanecen en "Asignación realizada" y entran a la siguiente ejecución de planificación.

![Impl 3 — Replanificación de donaciones sin asignar](assets/e2-impl3-replanificacion.svg)

