# **ENTREGA 2: Arquitectura y Modelado en Objetos \- Parte II**

## *Objetivos de la entrega*

* Diseñar e implementar, de manera incremental, las nuevas funcionalidades.  
* Incorporar nociones de ejecuciones de tareas asincrónicas y/o calendarizadas.  
* Familiarizarse con el concepto de APIs como mecanismo de integración y su consumo  
* Exponer un servicio a través de un protocolo de red.  
* Incorporar flujos de trabajo asincrónicos.

## *Unidades del Programa Vinculadas*

* Unidad 2: Herramientas de Concepción y Comunicación del Diseño.   
* Unidad 3: Diseño con Objetos.   
* Unidad 6: Diseño de Arquitectura.   
* Unidad 7: Integración de Sistemas.  
* Unidad 8: Validación del Diseño. 

## *Alcance*

* Donaciones \- Trazabilidad de las donaciones.   
* Notificaciones \- Integración concreta con distintos medios de notificación.  
* Donaciones \- Asignación de Necesidades Materiales.  
* Logística \- Entrega y planificación de rutas.  
* Logística \- Monitoreo de camiones en tiempo real.  
* Exposición REST de los servicios. 

## *Dominio*

### Donaciones \- Asignación de las donaciones

Por cada una de las donaciones cuyo estado sea **“En Depósito”**, el sistema debe ejecutar un proceso de *matchmaking* con el objetivo de asignar el destino de las mismas, es decir, a qué entidad beneficiaria será entregada. 

Se cuenta con dos algoritmos (con la posibilidad de incorporar nuevos a medida que evolucione el sistema) que aplican un criterio distinto para evaluar a las entidades beneficiarias y genera un ranking en función de qué tanto corresponde que reciban la donación, de acuerdo con la lógica seleccionada. Estos algoritmos proponen **hasta diez** entidades beneficiarias que podrían recibir la donación. Hasta la actualidad estos son:

* **Algoritmo de Compatibilidad Semántica**: analiza la correspondencia entre las características del bien o bienes donado(s) y las necesidades declaradas por cada entidad beneficiaria. Favorece a aquellas organizaciones cuya demanda coincida de forma más precisa con la donación recibida.

* **Algoritmo de Prioridad a sub-atendidos**: asigna prioridad a organizaciones que hayan recibido menos donaciones en el último trimestre. 

A partir de los resultados, este componente puede filtrar automáticamente las entidades beneficiarias que hayan aparecido en la ejecución de ambos algoritmos para que una persona administradora confirme el destino final. Si no hubo coincidencias, entonces mostrará ambas ejecuciones. 

Para asegurarse de que la ejecución de los algoritmos no degrade el desempeño del sistema, se solicita que el mismo se realice en horarios de baja carga. 

### Eventos e integración con medios de notificación

El sistema deberá enviar notificaciones a personas donantes y entidades beneficiarias cuando ocurra un evento considerado relevante. Para esta entrega, se requiere notificar en los siguientes casos:

* **Ausencia de la plataforma:** A una persona donante, cuando no registre interacción con la plataforma durante más de 20 días, con el objetivo de incentivar a realizar una nueva donación  
* **Donación asignada (beneficiario):** A una entidad beneficiaria, cuando se le asigna una donación en base a sus necesidades recurrentes o extraordinarias  
* **Donación asignada (donante):** A una persona donante, cuando haya realizado una donación que acaba de ser asignada a una entidad beneficiaria  
* **Inicio de ruta:** Se notificará a todas las entidades beneficiarias y a los donantes cuyas entregas formen parte de la ruta iniciada por el chofer. La notificación deberá incluir un enlace al mapa interactivo, permitiendo el seguimiento de la entrega en tiempo real.  
* **Entrega realizada con éxito:** Cuando la entidad beneficiaria confirme la recepción satisfactoria de la donación, se notificará tanto a la entidad como al donante correspondiente. La notificación deberá incluir un comprobante de entrega, indicando fecha, hora y camión responsable.  
* **Entrega no satisfactoria:** En caso de que la entrega no pueda concretarse por cualquier motivo (por ejemplo: vencimiento de los bienes previo a la entrega, imposibilidad de recepción por parte de la entidad, incidentes logísticos, etc.), se notificará a la entidad beneficiaria, a la persona donante y a personas administradoras del sistema. Si la entrega pudiera ser replanificada, se dejará constancia del estado correspondiente en el sistema y podrá generarse una nueva asignación de ruta para la donación en cuestión.

En esta iteración, además, se solicita que se envíen notificaciones reales a las personas usuarias involucradas, a través de correo electrónico, SMS y/o WhatsApp, basándose en la simulación realizada en la [entrega anterior](./Donatrack - entrega-1.md). 

### Logística \- Entrega y planificación de rutas

Para realizar las entregas, la organización cuenta con una flota de camiones. De cada uno se conoce la patente, la capacidad en volumen (m³), la altura (m) y la capacidad de carga (kg). Todos los camiones pueden transportar cualquier tipo de bien y parten siempre desde el depósito para realizar las entregas del día. 

La plataforma contará con la integración de un componente externo encargado de generar las rutas de reparto del día siguiente para la flota. Este componente recibirá, por ejecución, un conjunto de donaciones en estado *Asignación Realizada* junto con la información de los camiones disponibles. Devolverá, por cada camión, una lista ordenada de destinos (direcciones de las entidades beneficiarias) con las entregas que debe realizar en cada una. Al completar la planificación, las rutas asignadas quedan disponibles para los choferes en su aplicación.  

### Logística \- Trazabilidad de las entregas de las donaciones

Antes de iniciar el recorrido, el chofer deberá indicar en el sistema que dará por inicio su ruta. Automáticamente, las entregas asignadas pasarán al estado **“En traslado”**. Cuando el camión entregue la donación en la sede de la entidad beneficiaria, esta deberá confirmar la recepción en el sistema. La entrega pasará al estado **“Entregada”** y quedará registrado qué camión realizó la entrega. Luego, la entidad deberá cargar fotos de la donación recibida en la plataforma.  
Si la entidad informa que no recibió la entrega en el día correspondiente, la misma se marcará como **“No recibida”**. El caso será revisado por las personas administradoras. Si la donación regresa al depósito, la entrega volverá al estado **“Pendiente”**.

### Logística \- Monitoreo de camiones en tiempo real

Para permitir el seguimiento en tiempo real de las entregas por todos los interesados, el sistema deberá mostrar en un *dashboard* administrativo la posición actual de los camiones y su avance sobre la ruta asignada en tiempo real. El equipo encargado de los dispositivos y herramientas de campo ha propuesto **dos alternativas** para obtener la posición del camión en tiempo real:

1. **Dispositivo GPS configurable instalado en el camión**, que enviará periódicamente la ubicación y velocidad.  
2. **Aplicación móvil utilizada por el conductor**, que reportará la geolocalización mientras la ruta esté activa.

Ambas opciones permiten cumplir con el objetivo funcional de monitoreo en tiempo real. Se deberá **elegir una de las dos alternativas** y continuar el desarrollo en función de la decisión adoptada, definiendo el contrato de integración correspondiente.

La configuración de los dispositivos físicos o de la aplicación móvil será responsabilidad del equipo externo. La plataforma deberá recibir, validar y procesar la información enviada para reflejar correctamente el estado del recorrido en el dashboard. 

### Integración entre Servicios

Debido a que se prevee que el tamaño del equipo de desarrollo crecerá pronto, se desea organizarlo en dos equipos. Por ello, se solicita que se divida al sistema en, inicialmente, dos microservicios; sus responsabilidades y las interacciones entre ellos formarán parte de las decisiones de diseño que deberá tomar cada equipo[^1].

### Exposición REST de los servicios

La integración entre los servicios identificados en el punto anterior deberá ser realizada empleando el protocolo HTTP y siguiendo las convenciones REST. A 

**Donaciones \- Gestión de Personas Donantes y Donaciones:**

* Operaciones CRUD sobre las donaciones.   
* Operaciones CRUD sobre las personas donantes (jurídicas y humanas).  
* Cambios de estado de una donación, garantizando trazabilidad y auditoría.

**Donaciones \- Gestión de Entidades Beneficiarias y Necesidades:**

* Operaciones CRUD sobre las entidades beneficiarias.  
* Operaciones CRUD sobre las necesidad materiales (recurrentes y extraordinarias)  
* Obtención del ranking generado por los algoritmos de asignación y selección de entidad beneficiaria final.   
* Ejecución a demanda de los algoritmos de asignación de necesidades.

**Logística:**

* Gestión de flota de camiones.  
* Operaciones CRUD sobre las rutas y entregas.

## *Requerimientos detallados*

*Requerimientos de dominio:*

1. El sistema debe garantizar la trazabilidad de los estados de las donaciones, desde su recepción hasta su entrega.   
2. El sistema debe garantizar el envío de notificaciones por distintos medios ante registro de inactividad por parte de una persona donante,  asignación de donaciones a la entidad beneficiaria, inicio de rutas de las entregas y estado de las entregas.  
3. El sistema debe garantizar la ejecución asincrónica de los algoritmos de asignación de donaciones presentes en el depósito.  
4. El sistema debe permitir la realización de operaciones a través de los endpoints solicitados, mediante el desarrollo de una API REST.  
5. El sistema debe, en horarios de baja carga, generar los planes de rutas para los camiones durante la siguiente jornada operativa, integrándose con el componente externo provisto.  
6. El sistema debe permitir a los choferes de los camiones informar del comienzo de su ruta.  
7. El sistema debe mostrar en tiempo real tanto a la persona donante como a la entidad beneficiaria de la donación la localización de los camiones para poder estar al tanto de su llegada.  
8. El sistema debe gestionar la recepción de la entrega por parte de las entidades beneficiarias en todos los casos.

*Requerimientos de implementación:*

1. Para que la integración con el servicio externo de planificación de rutas funcione, el sistema deberá exponer una **URL de callback** donde el componente externo podrá notificar el resultado de la planificación. La llamada de retorno permitirá que la plataforma registre las rutas generadas y actualice el estado de las entregas que hayan sido efectivamente asignadas.  
2. El sistema deberá realizar las solicitudes al proveedor en lotes puesto que, por restricciones del proveedor, cada ejecución procesa como máximo 100 donaciones a entregar.   
3. Es posible que el planificador no pueda asignar todas las donaciones en una única ruta. En ese caso, devolverá las donaciones sin asignar en un campo aparte, y será responsabilidad de nuestro sistema volver a planificarlas. 


[^1]:  Sugerencia: buscar que las particiones del dominio se definan maximizando la cohesión interna y minimizando el acoplamiento entre servicios
