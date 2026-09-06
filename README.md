# TP DISEÑO - DONATRACK - GRUPO 7

Trabajo práctico anual de la materia Diseño de Sistemas de Información.

- Eduardo Onishi
- Juan Ignacio Cervini
- Marcos Ifran
- Benjamin Salvatierra
- Santiago Gorbea

## Documentación

- [Modelo del dominio por requerimiento](./docs/README.md) — diagramas de clases por requerimiento de los enunciados.

## Microservicios

El sistema se divide en dos microservicios (ver [enunciado 2](./docs/enunciado/enunciado-2.md)):

| Módulo | Responsabilidad | Paquetes de dominio |
| --- | --- | --- |
| [`donaciones`](./donaciones) | Donaciones, donantes, entidades beneficiarias, necesidades, matchmaking y notificaciones | `donaciones`, `donantes`, `beneficiarios`, `bienes`, `estados`, `notificaciones`, `utilidades` |
| [`logistica`](./logistica) | Flota de camiones, rutas, destinos, monitoreo en tiempo real y planificación | `logistica` (incluye `monitoreo` y `planificacion`) |

Cada módulo es un proyecto Maven independiente; el `pom.xml` raíz solo los agrega (`mvn verify` en la raíz compila y testea ambos).

### Lógica cruzada no migrada (comentada)

La integración REST entre servicios está pendiente; la lógica que requiere el otro microservicio quedó **comentada** dentro de cada módulo con el marcador `LÓGICA FALTANTE`:

- **En `donaciones`** (depende de logística): asignación de camión a la donación (`Donacion`), limpieza de camión al retornar al depósito (`EntregaFallida`) y datos del camión en notificaciones de entrega (`EventoEntregaExitosa`, `ComprobanteEntrega`) y sus tests.
- **En `logistica`** (depende de donaciones): el destino con su entidad beneficiaria y donaciones (`Destino`), la validación de destinos duplicados (`Ruta`), las donaciones del lote (`LoteDonaciones`), el resultado de planificación (`ResultadoPlanificacion`), el servicio de planificación completo (`ServicioPlanificacionRutas`) y sus tests.

Para completar la separación falta lo que el enunciado pide como siguiente paso: exponer los endpoints REST de cada servicio y que se integren por HTTP.
