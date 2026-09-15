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

## Notificaciones reales para la demo

El microservicio `donaciones` usa Resend para correo y Twilio Trial para SMS y WhatsApp. Las
credenciales se leen exclusivamente desde variables de entorno:

```bash
export RESEND_API_KEY="re_..."
export RESEND_FROM="DonaTrack <onboarding@resend.dev>"
export TWILIO_ACCOUNT_SID="AC..."
export TWILIO_AUTH_TOKEN="..."
export TWILIO_SMS_FROM="+1..."
export TWILIO_WHATSAPP_FROM="whatsapp:+14155238886"
```

En Twilio Trial, cada destinatario telefónico debe estar verificado y el trial dura 30 días. Para
WhatsApp, las personas destinatarias primero deben unirse al sandbox de Twilio. Los números se
guardan en formato E.164, por ejemplo `+541112345678`.

Si falta la configuración de un canal, sus notificaciones quedan registradas como `FALLIDA` y se
reintentan hasta tres veces. Las pruebas usan dobles de gateway, por lo que no invocan proveedores
externos.
