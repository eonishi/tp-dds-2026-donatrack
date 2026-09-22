# Diseño de Contratos REST e Integración - DonaTrack

Este documento define los contratos de integración y estrategias de comunicación entre los microservicios **Donaciones** y **Logística**.

### 1. Estrategia de Consistencia
Se adopta un enfoque **sincrónico** mediante HTTP/REST. Las actualizaciones de estado en el módulo de Donaciones se confirman en tiempo real tras las acciones físicas de Logística, evitando la complejidad transaccional inicial de un bus de eventos.

### 2. Identificadores que Cruzan la Frontera (Cross-Boundary IDs)
Para mantener el bajo acoplamiento, no se comparten entidades de base de datos. Solo viajan los IDs:
*   **`idDonacion` (UUID):** De Donaciones a Logística (para referenciar el ítem).
*   **`idEntidadBeneficiaria` (Long):** De Donaciones a Logística (para conocer el destino).
*   **`idCamion` (String/Patente) e `idRuta` (UUID):** De Logística a Donaciones (para auditoría e historial).

### 3. Autenticación y Manejo de Errores
*   **Autenticación (S2S):** La comunicación interna entre servicios requerirá una API Key en el header `Authorization`.
*   **Errores (RFC 7807):** 
    *   `400 Bad Request`: Transiciones de estado inválidas.
    *   `404 Not Found`: Cuando un servicio envía un ID inexistente.
    *   `500 Internal Server Error`: Fallos de conexión o base de datos.

---

### Fronteras de Integración y Endpoints

**1. Consulta de donaciones para armar rutas**
* **Situación:** Para generar las rutas del día siguiente, el sistema debe enviar al planificador externo las donaciones listas junto con los camiones disponibles.
* **Cruce:** El módulo de Logística necesita pedirle a Donaciones el listado de todas las donaciones que estén en estado "Asignación Realizada".
* **Contrato API:**
  * **Endpoint:** `GET /api/donaciones?estado=ASIGNACION_REALIZADA`
  * **Payload Request:** N/A
  * **Payload Response (200 OK):**
    ```json
    [
      {
        "idDonacion": "d3b07384-d9a7-47b7-8025-a1c1d9b35b01",
        "idEntidadBeneficiaria": 45,
        "direccionDestino": "Av. Mitre 750, Avellaneda",
        "coordenadas": {
          "lat": -34.662,
          "lng": -58.365
        }
      }
    ]
    ```

**2. Recepción del resultado de planificación (Callback)**
* **Situación:** El componente externo devuelve las rutas armadas a la URL de *callback*.
* **Cruce:** Logística debe notificar a Donaciones para que actualice el estado de las entregas efectivamente asignadas (pasando a "Lista para entregar").
* **Contrato API:**
  * **Endpoint:** `PATCH /api/donaciones/{idDonacion}/estado`
  * **Payload Request:**
    ```json
    {
      "nuevoEstado": "LISTA_PARA_ENTREGAR"
    }
    ```
  * **Payload Response:** `200 OK` | `400 Bad Request`

**3. Inicio de la ruta por parte del chofer**
* **Situación:** El chofer indica en el sistema el inicio de su recorrido.
* **Cruce:** Logística avisa a Donaciones que la ruta comenzó, enviando la patente del camión (`idCamion`).
* **Cruce derivado:** Con este `idCamion`, Donaciones construye la URL única del mapa interactivo (ej: `/monitoreo/{idCamion}`) y envía la notificación a los usuarios.
* **Contrato API:**
  * **Endpoint:** `PATCH /api/donaciones/{idDonacion}/estado`
  * **Payload Request:**
    ```json
    {
      "nuevoEstado": "EN_TRASLADO",
      "idCamion": "AF123ZZ"
    }
    ```
  * **Payload Response:** `200 OK` | `400 Bad Request`

**4. Confirmación de entrega exitosa**
* **Situación:** La entidad beneficiaria confirma la recepción de la donación.
* **Cruce:** Donaciones pasa el estado a "Entregada". El camión que realizó la entrega ya quedó registrado al iniciar la ruta (endpoint 3), por lo que esta confirmación no lo reenvía.
* **Contrato API:**
  * **Endpoint:** `PATCH /api/donaciones/{idDonacion}/estado`
  * **Payload Request:**
    ```json
    {
      "nuevoEstado": "ENTREGADA"
    }
    ```
  * **Payload Response:** `200 OK` | `400 Bad Request`

**5. Notificación de entrega no satisfactoria (en campo)**
* **Situación:** La entrega no puede concretarse por un incidente logístico o problemas en el domicilio.
* **Cruce:** Logística informa el problema a Donaciones para que este cambie el estado a "Entrega fallida", registre la justificación y envíe las notificaciones correspondientes.
* **Contrato API:**
  * **Endpoint:** `PATCH /api/donaciones/{idDonacion}/estado`
  * **Payload Request:**
    ```json
    {
      "nuevoEstado": "ENTREGA_FALLIDA",
      "justificacion": "Tocamos timbre pero nadie respondió"
    }
    ```
  * **Payload Response:** `200 OK` | `400 Bad Request`

**6. Reclamo por entrega no recibida**
* **Situación:** La entidad informa en la plataforma que no recibió la entrega en el día estipulado.
* **Cruce:** El sistema marca la entrega como "No recibida", lo que corresponde al estado `ENTREGA_FALLIDA`; si se determina que vuelve al depósito, la donación pasa al estado `EN_DEPOSITO`. Esto requiere coordinar el cambio de estado entre la ruta de Logística y la donación en Donaciones.
* **Contrato API:**
  * **Endpoint:** `PATCH /api/donaciones/{idDonacion}/estado`
  * **Payload Request:**
    ```json
    {
      "nuevoEstado": "ENTREGA_FALLIDA",
      "justificacion": "Reclamo administrativo: no se recibió en la fecha estipulada"
    }
    ```
  * **Payload Response:** `200 OK` | `400 Bad Request`

**7. Actualización masiva de estados (Múltiples donaciones)**
* **Situación:** Para modificar el estado de múltiples donaciones en una única petición (por ejemplo, al recibir lotes de asignación de rutas del planificador) previniendo múltiples llamadas HTTP.
* **Cruce:** Logística envía un listado de actualizaciones de estado hacia Donaciones.
* **Contrato API:**
  * **Endpoint:** `PATCH /api/donaciones/estado`
  * **Payload Request:**
    ```json
    [
      {
        "idDonacion": "aaa-123-bbb",
        "nuevoEstado": "LISTA_PARA_ENTREGAR"
      },
      {
        "idDonacion": "ccc-567-bbb",
        "nuevoEstado": "LISTA_PARA_ENTREGAR"
      },
      {
        "idDonacion": "ddd-890-ccc",
        "nuevoEstado": "LISTA_PARA_ENTREGAR"
      }
    ]
    ```
  * **Payload Response:** `200 OK` | `400 Bad Request`

---

> **Nota de Diseño: Cancelación manual a estado "Vencida"**
> No se contempló como frontera de integración el caso borde en el que una persona administradora marca una donación como "Vencida" luego de haber sido planificada en una ruta. Se asume por regla de negocio el *camino simple*: la transición a estado "Vencida" solo puede realizarse mientras la donación se encuentra físicamente "En depósito". Al ocurrir estrictamente antes de la planificación, este cambio de estado se resuelve de manera interna en el módulo de Donaciones y no requiere notificar ni coordinar acciones con el microservicio de Logística.