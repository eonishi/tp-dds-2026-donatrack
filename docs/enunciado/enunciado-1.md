  
**DonaTrack \- Sistema de Gestión y Trazabilidad de Donaciones**

Trabajo Práctico Anual Integrador  
\-2026-

# **Contexto general**

## *Marco Institucional*

UTN Solidaria es una iniciativa de la Subsecretaría de Asuntos Estudiantiles de nuestra Facultad, orientada a acompañar y asistir a personas en situación de vulnerabilidad a través de distintas acciones solidarias organizadas por estudiantes y voluntarios. Se constituye como un espacio de articulación entre estudiantes, docentes y graduados, con el propósito de canalizar acciones solidarias que generen un impacto positivo y sostenible en la sociedad.

El área busca complementar la formación académica de los futuros profesionales, impulsando valores como la responsabilidad social, la empatía y el trabajo colaborativo. En este marco, UTN Solidaria se posiciona como un puente entre la universidad y la comunidad, promoviendo la participación activa frente a problemáticas sociales y fomentando el desarrollo de soluciones concretas desde una perspectiva interdisciplinaria.

Entre sus principales líneas de acción se destaca el reparto de viandas a personas en situación de vulnerabilidad en los alrededores de la sede Medrano y de donaciones recibidas de alimentos no perecederos, ropa, abrigo, colchones y frazadas, el acompañamiento a instituciones educativas y comunitarias como el Hospital Gandulfo y un hogar de niños, y la generación de espacios de voluntariado que permitan a los participantes involucrarse de manera directa con diversos contextos sociales. Su trabajo se sostiene gracias al compromiso de los voluntarios y a las donaciones de quienes confían y acompañan cada una de las propuestas.

En el marco del presente trabajo práctico de grado, se propone analizar y modelar los distintos procesos que conforman UTN Solidaria, utilizando esta iniciativa como caso de estudio, y llevarla a la práctica mediante el diseño y desarrollo de una solución tecnológica que integre herramientas y enfoques propios de la Ingeniería en Sistemas de Información.

De este modo, se busca articular la formación tecnológica con el compromiso social, contribuyendo a la formación de profesionales no solo altamente capacitados, sino también conscientes de su rol en la sociedad y comprometidos con la transformación de su entorno.

## *Contexto*

Una organización sin fines de lucro dedicada a la recolección y distribución de donaciones de bienes materiales enfrenta dificultades para gestionar y dar trazabilidad a los recursos que recibe. La falta de un sistema unificado provoca desorden, duplicación de registros y poca claridad sobre el destino de las donaciones, perjudicando la transparencia y eficiencia.

## *Nuestro sistema*

A partir de la problemática identificada y presentada en la sección anterior, se propone el diseño y desarrollo de ***DonaTrack***: una solución digital orientada a organizar, registrar y monitorear las donaciones desde su recepción en el depósito de la organización hasta su entrega a entidades beneficiarias que las requieran. El objetivo del sistema será mejorar la distribución de recursos y, al mismo tiempo, fortalecer la confianza de personas donantes y entidades beneficiarias de las donaciones.

# **ENTREGA 1: Arquitectura y Modelado en Objetos \- Parte I**

## *Objetivos de la entrega*

* Entrar en contacto con el dominio y sus principales abstracciones.  
* Incorporar de forma paulatina conceptos y principios de Diseño.  
* Familiarizarse con el entorno de desarrollo y las principales tecnologías a ser aplicadas a lo largo del Trabajo Práctico.  
* Familiarizarse con la arquitectura del sistema. 

## *Unidades del Programa Vinculadas*

* Unidad 2: Herramientas de Concepción y Comunicación del Diseño.  
* Unidad 3: Diseño con Objetos.  
* Unidad 6: Diseño de Arquitectura.  
* Unidad 8: Validación del Diseño.

## *Alcance*

* Donaciones \- Gestión de Donantes y Donaciones.   
* Donaciones \- Importación masiva de donantes mediante archivo CSV.  
* Donaciones \- Gestión de Entidades Beneficiarias y Necesidades.   
* Notificaciones \- Primera iteración.

## *Dominio*

### Donaciones \- Donantes

Las personas donantes son personas humanas o jurídicas que desean aportar a las entidades beneficiarias. Estas podrán registrarse en la plataforma previo a llevar una donación al depósito. A las personas humanas se les solicita nombre, apellido, edad, número de documento, género y dirección y al menos un medio de contacto (en forma obligatoria correo electrónico y en forma opcional teléfono y/o WhatsApp). A su vez, podrá determinar cuál de ellos será su medio de contacto predeterminado para recibir notificaciones del sistema. 

Por otra parte, las personas jurídicas deben indicar razón social, su tipo (Gubernamental, ONG, Empresa, Institución), rubro y al menos un medio de contacto. Cada organización tendrá personas representantes habilitadas a operar en su nombre.

### Donaciones \- Registro de personas donantes

Las personas donantes (o en caso de las jurídicas, un representante) se acercarán al depósito con el objetivo de ingresar una donación. En caso de que no tengan un usuario en la plataforma, una persona administradora les pedirá los datos de registro (*ver sección anterior*) y se les enviará un correo electrónico de bienvenida para que accedan por primera vez.

### Donaciones \- Donaciones y segmentación

Una vez registrada la persona donante, o confirmado que ya cuenta con un usuario, la persona administradora que esté en el depósito en ese momento completará el formulario de la donación en su nombre.

Se debe incluir una descripción general y los bienes que contiene. De cada bien se conoce una descripción y, opcionalmente, se le asocia una foto. Estos pertenecen a una categoría (mobiliario, alimentos, vestimenta, etc.). Para cada categoría existen múltiples subcategorías.

La subcategoría constituye la unidad mínima de asignación dentro del sistema, permitiendo identificar con precisión qué bien se necesita o se dona. Por ejemplo, dentro de la categoría “Alimentos” pueden definirse subcategorías como fideos secos, arroz, legumbres secas, aceite vegetal; y dentro de “Vestimenta” pueden existir subcategorías como camperas de abrigo, remeras, pantalones o ropa infantil.

Existen algunas categorías en las que es necesario conocer si su estado es usado o no, como por ejemplo en bienes mobiliarios o vestimenta. En caso de que los bienes sean perecederos, se debe ingresar la fecha de vencimiento. En todos los casos, se deberá contabilizar la cantidad de un mismo producto en una unidad determinada (kilogramos, unidades o según corresponda).

Se registrará la totalidad de los bienes a donar en una única carga dentro del sistema, a partir de la cual el sistema realizará automáticamente una segmentación interna, generando múltiples **posibles donaciones independientes** agrupadas obligatoriamente por subcategorías. De este modo, cada donación resultante quedará asociada a una única subcategoría, constituyendo la unidad mínima de asignación y garantizando coherencia en el proceso automático de vinculación con las necesidades correspondientes. 

En el caso de bienes perecederos, el sistema podrá generar donaciones separadas cuando existan diferencias en la fecha de vencimiento. Asimismo, para aquellos bienes cuyo estado sea relevante (como mobiliario o vestimenta) deberá consignarse si se trata de artículos nuevos o usados, a fin de asegurar una correcta evaluación y asignación posterior.

Se proveen a continuación ejemplos concretos: 

* La oficina corporativa de Arcos Plateados está en proceso de mudanza y quieren donar un conjunto de muebles usados: seis sillas y una mesa rectangular.   
* Una planta industrial de pastas secas desea donar 100 paquetes de fideos y 50 tetra-packs de tomate que vencen el 01/01/2027. 

### Donaciones \- Entidades beneficiarias y necesidades

Las entidades beneficiarias son organizaciones sin fines de lucro que se registran para recibir donaciones. Pueden ser escuelas rurales, comedores, espacios de tutoría de niños, entre otros. De cada entidad se conoce su razón social, dirección completa, teléfono y correos de las personas representantes designadas. 

Las entidades beneficiarias podrán registrar sus necesidades materiales indicando subcategoría (sillas, ropa de abrigo, arroz, frutas, entre otros) y una breve descripción. La plataforma distinguirá entre necesidades **recurrentes** (consumos habituales) y **extraordinarias** (situaciones puntuales o emergencias).

* **Necesidades extraordinarias:** surgen ante situaciones excepcionales (como mudanzas, inundaciones, incendios o vencimientos próximos de insumos).   
  * Ejemplo: la escuela rural N°10, tras una inundación en un aula, necesita 30 bancos y 30 sillas para reponer la pérdida.   
  * La cantidad solicitada puede cubrirse mediante donaciones parciales. Por ejemplo, una persona dona 2 sillas, otra dona 20, y así sucesivamente hasta alcanzar las 30 sillas requeridas. La necesidad se considera satisfecha cuando **se recibe una cantidad de bienes igualando o superando la cantidad requerida**.  
* **Necesidades recurrentes:** están vinculadas al funcionamiento habitual de la organización e implican bienes de consumo periódico. Se satisfacen dentro del período correspondiente (por ejemplo, mensual), según la cantidad objetivo definida para ese período.  
  * Ejemplo: el comedor infantil “Escobar Sonrisas” requiere 100 paquetes de fideos por semana para su funcionamiento habitual. La necesidad se satisface dentro de cada período (semanal), según la cantidad objetivo establecida.

### Donaciones \- Importación masiva de personas donantes por CSV

Dado que la ONG ya cuenta con un histórico de personas donantes, se debe  permitir la migración de su información en forma masiva importando un archivo .csv. Cada línea representará una persona donante (humana o jurídica) e incluirá los campos mínimos para identificarla y contactarla. En caso de que un registro ya exista (esto quiere decir que el correo electrónico ya se encuentra registrado en el servicio) se deberá actualizar su información; en caso contrario, se lo deberá crear el usuario.

El archivo posee el siguiente formato:

| TipoPersona | TipoDoc | Documento | Nombre/Razón Social | Email | Teléfono |
| :---- | :---- | :---- | :---- | :---- | :---- |
| HUMANA | DNI | 12345678 | Ana Pérez | ana@mail.com | \+54 11 5555-5555 |
| JURIDICA | CUIT | 30-12345678-9 | Arcos Plateados S.A. | contacto@empresa.com | \+54 11 4444-4444 |
| Con estos datos debe ser posible ubicar a la persona donante en el sistema. En caso contrario, se le debe crear un usuario. |  |  | Se puede utilizar este [archivo de prueba](https://drive.google.com/file/d/1UOmqq0CZzBJdQ0SmdofQ2RNzcuCb61rp/view?usp=sharing) de 20.000 filas.  |  |  |

### Donaciones \- Estados de las donaciones

El sistema **deberá garantizar** la trazabilidad y auditoría de los estados de cada donación. Al momento de registrarse, la donación quedará en estado “En depósito”, lo que indica que está disponible para ser asignada a una entidad beneficiaria.

Al día siguiente, cuando se ejecute el algoritmo de asignación (situación que se describe en una posterior entrega), si la donación es asignada a una entidad beneficiaria quedará en estado “Asignación realizada”, en caso contrario permanece en el depósito. 

La donación pasará a estar “Lista para entregar” cuando se haya planificado una ruta que incluya la donación. Pasará a “En traslado” cuando el camión asignado para la entrega haya iniciado el recorrido.  Finalmente, cuando la entidad beneficiaria haya confirmado su entrega, quedará en estado “Entregada”.  

Si no se la pudo entregar el día estipulado, quedará en estado “Entrega fallida” y volverá al depósito. Si esto sucediera, se solicita registrar una justificación de por qué no se realizó la entrega, por ejemplo: “Tocamos timbre pero nadie respondió”. Las personas administradoras encargadas del depósito podrán cambiar el estado de una donación a “Vencida”, en caso de que sea necesario. 

```mermaid
---
title: Estados de una donacion
---

stateDiagram 

[*] -->  EnDeposito: Donación registrada
EnDeposito --> AsignaciónRealizada: Algoritma asgina entidad beneficiaria
AsignaciónRealizada --> ListaParaEntregar : Hay una ruta planificada que incluye la Donación
ListaParaEntregar --> EnTranslado : Camión inicia el recorrido
EnTranslado --> Entregada : Entidad Beneficiaria confirma entrega
Entregada --> [*]
EnTranslado --> EntregaFallida : No se pudo entregar
EntregaFallida --> EnDeposito : Retorna al deposito 
EnDeposito --> Vencida : Se vence la donación 
Vencida --> [*]
```

*Figura 2 \- [Diagrama de Estados de una donación.](https://drive.google.com/file/d/1-A44wwrcuuzcaC-UUhzi1MKrXzK4js-B/view?usp=sharing)* 

### Notificaciones

Para esta entrega, se solicita exponer un componente notificador que, dado un destinatario, un mensaje y un medio de notificación (correo electrónico, SMS y WhatsApp), pueda realizar el envío. En esta iteración, se solicita simular la llamada a los servicios externos y marcar a las notificaciones como completadas. En próximas iteraciones se realizará la integración real. 

## *Requerimientos detallados*

*Requerimientos de dominio*

1. El sistema deberá permitir la gestión de personas donantes.   
2. El sistema deberá permitir la gestión de las donaciones resultantes.  
3. El sistema deberá permitir la gestión de entidades beneficiarias y sus necesidades.   
4. El sistema debe garantizar la trazabilidad de los estados de las donaciones, desde su recepción hasta su entrega.   
5. El sistema deberá permitir la importación masiva de donantes en CSV. 

