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
        },
        "volumenM3": 1.5,
        "pesoKg": 25.0
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
* **Cruce:** Logística avisa a Donaciones que la ruta comenzó. Donaciones pasa los estados a "En traslado".
* **Cruce derivado:** Donaciones debe enviar la notificación del evento incluyendo el enlace al mapa interactivo.
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

**4. Monitoreo en tiempo real**
* **Situación:** El sistema debe mostrar la localización de los camiones en tiempo real a las personas donantes y entidades beneficiarias.
* **Cruce:** Donaciones necesita consultar la ubicación actual del camión gestionado por Logística para mostrarla a sus usuarios.
* **Contrato API:**
  * **Endpoint:** `GET /api/logistica/camiones/{idCamion}/posicion`
  * **Payload Request:** N/A
  * **Payload Response (200 OK):**
    ```json
    {
      "idCamion": "AF123ZZ",
      "idRutaActiva": "r9f01234-a1b2",
      "coordenadas": {
        "lat": -34.665,
        "lng": -58.370
      },
      "timestamp": "2026-09-09T18:15:00Z"
    }
    ```

**5. Confirmación de entrega exitosa**
* **Situación:** La entidad beneficiaria confirma la recepción de la donación.
* **Cruce:** Donaciones pasa el estado a "Entregada" y necesita registrar en su sistema qué camión (dato de Logística) realizó la entrega.
* **Contrato API:**
  * **Endpoint:** `PATCH /api/donaciones/{idDonacion}/estado`
  * **Payload Request:**
    ```json
    {
      "nuevoEstado": "ENTREGADA",
      "idCamion": "AF123ZZ"
    }
    ```
  * **Payload Response:** `200 OK` | `400 Bad Request`

**6. Notificación de entrega no satisfactoria (en campo)**
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

**7. Reclamo por entrega no recibida**
* **Situación:** La entidad informa en la plataforma que no recibió la entrega en el día estipulado.
* **Cruce:** El sistema marca la entrega como "No recibida" y, si se determina que vuelve al depósito, la entrega debe volver al estado "Pendiente". Esto requiere coordinar el cambio de estado entre la ruta de Logística y la donación en Donaciones.
* **Contrato API:**
  * **Endpoint:** `PATCH /api/donaciones/{idDonacion}/estado`
  * **Payload Request:**
    ```json
    {
      "nuevoEstado": "PENDIENTE",
      "justificacion": "Reclamo administrativo: no se recibió en la fecha estipulada"
    }
    ```
  * **Payload Response:** `200 OK` | `400 Bad Request`

---

> **Nota de Diseño: Cancelación manual a estado "Vencida"**
> No se contempló como frontera de integración el caso borde en el que una persona administradora marca una donación como "Vencida" luego de haber sido planificada en una ruta. Se asume por regla de negocio el *camino simple*: la transición a estado "Vencida" solo puede realizarse mientras la donación se encuentra físicamente "En depósito". Al ocurrir estrictamente antes de la planificación, este cambio de estado se resuelve de manera interna en el módulo de Donaciones y no requiere notificar ni coordinar acciones con el microservicio de Logística.