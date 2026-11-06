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

### Integración entre servicios

La integración REST entre los dos microservicios ya está implementada y funcional. Cuando `donaciones` necesita iniciar un traslado, se comunica con `logistica`, y viceversa, permitiendo una planificación distribuida.

### Programas de planificación externa (`schedules`)

Los programas que viven en el paquete `schedules` de un microservicio son puntos de entrada
autónomos: tienen su propio `main`, arman las dependencias que necesitan y terminan al finalizar. No
los ejecuta el proceso HTTP: los invoca un planificador externo (por ejemplo, un cronjob) con la
frecuencia que se configure fuera del código.

| Microservicio | Programa | Qué hace |
| --- | --- | --- |
| `donaciones` | `ar.edu.utn.donatrack.schedules.MatchmakingRunner` | Genera las propuestas de asignación del matchmaking para la fecha actual. |

Ejecución manual (arma el classpath del módulo):

```bash
mvn -q -pl donaciones dependency:build-classpath -Dmdep.outputFile=target/cp.txt
java -cp "donaciones/target/classes:$(cat donaciones/target/cp.txt)" \
  ar.edu.utn.donatrack.schedules.MatchmakingRunner
```

Ejemplo de cronjob diario a las 03:00, hora local:

```cron
0 3 * * * cd /ruta/a/tp-dds-2026-donatrack && java -cp "donaciones/target/classes:$(cat donaciones/target/cp.txt)" ar.edu.utn.donatrack.schedules.MatchmakingRunner
```

El horario, la frecuencia y los reintentos pertenecen al planificador externo: no hay
planificación interna dentro de los microservicios.

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

Las notificaciones se envían de forma inmediata al ocurrir el evento del dominio, sin encolarlas
en memoria ni conservar un atributo `estado` (`PENDIENTE`/`ENVIADA`/`FALLIDA`) o contador de
reintentos en `Notificacion`. Si un proveedor rechaza el envío, el fallo se registra en el log sin
interrumpir el caso de uso principal. Las pruebas usan dobles de gateway en `src/test`, por lo que
no invocan proveedores externos ni requieren dobles en `src/main`.
