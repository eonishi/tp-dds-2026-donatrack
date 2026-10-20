# **ENTREGA 4: Presentación y Despliegue**

## *Objetivos de la entrega*

* Incorporar nociones de Diseño UI/UX.  
* Incorporar nociones de maquetado Web a través de los lenguajes HTML y CSS.  
* Incorporar patrones de arquitectura MVC a través de la implementación de un Cliente Liviano.  
* Familiarizarse con técnicas y proveedores de despliegue en la nube.  
* Familiarizarse con diferentes estrategias de integración. 

## *Unidades del Programa Vinculadas*

* Unidad 4: Diseño de Interfaz de Usuario.  
* Unidad 6: Diseño de Arquitectura.  
* Unidad 7: Integración de Sistemas   
* Unidad 8: Validación del Diseño.  
* Unidad 9: Diseño y Seguridad

## *Alcance*

* Diseño y maquetado de interfaces de usuario.  
* Implementación de Presentación Web, Cliente Liviano.  
* Despliegue en la nube utilizando un proveedor IaaS o PaaS.  
* Nuevas estrategias de diseño de APIs Web.

### Implementación de un Cliente Liviano desacoplado

Se deberá implementar la interfaz gráfica de usuario para todas funcionalidades contempladas en entregas anteriores, tanto para roles de usuarie final como de perfiles administrativos. Para la generación de las interfaces de usuario se deberá utilizar un motor de plantillas *(template engine),* basado en la arquitectura [descripta acá](https://docs.google.com/document/d/1jMkmm2fk9FO-h-cxVz_v0r6p-UNl3sew1E1zXnP5WPQ/edit?tab=t.0#heading=h.halhyllz00mo).

A continuación se detallan las pantallas que se deben soportar como mínimo.

**Persona no autenticada**

1. El sistema debe contar con una *landing page* accesible públicamente que permita:  
   * Presentar el propósito y objetivos de DonaTrack.  
   * Incluir enlaces de acceso a:  
     1. Visualización de fotos de donaciones entregadas sin login.  
     2. Registro o inicio de sesión para personas donantes y beneficiarias.  
     3. Información legal y de privacidad.  
2. El sistema debe mostrar las donaciones ya entregadas a entidades beneficiarias en un mapa interactivo y permitir ver sus detalles al hacer clic en cada marcador.  
3. El sistema debe permitir el registro de personas donantes y entidades beneficiarias.   
4. El sistema debe permitir el inicio de sesión de personas donantes, entidades beneficiarias y personas administradoras.

**Persona donante**

5. La persona donante debe poder filtrar sus donaciones realizadas por estado y categoría o subcategoría/s de bienes entregados.  
6. La persona donante debe poder navegar por las entidades beneficiarias registradas.  
7. La persona humana donante debe poder acceder a una sección de Incentivos y consultar misiones a realizar e insignias obtenidas.   
8. La persona donante debe recibir notificaciones cuando una donación realizada por ella es asignada, cuando cumplió una misión, cuando sube de categoría o cuando la entidad beneficiaria asignada recibe la donación realizada.   
9. La persona donante debe poder seguir en un mapa las entregas activas de las donaciones que realizó, visualizando el/los camiones asociados con ubicación y última actualización.

**Entidad beneficiaria**

10. La entidad beneficiaria debe poder registrar necesidades materiales concretas.   
11. La entidad beneficiaria debe poder ver el estado de las donaciones asignadas a ella.  
12. La entidad beneficiaria debe poder confirmar la recepción de una donación, cargando fotos de la misma.   
13. La entidad beneficiaria debe recibir notificaciones en base a la asignación de donaciones y confirmación de recepción de entrega.   
14. La entidad beneficiaria debe poder seguir en un mapa las entregas activas de las donaciones que debe recibir, visualizando el/los camiones asociados con ubicación y última actualización.

**Persona administradora**

15. La persona administradora debe registrar personas donantes y donaciones que haya recibido en el depósito, asociándolas a la persona donante correspondiente.   
16. La persona administradora debe poder actualizar el estado de las donaciones disponibles en el depósito, en caso de que estén vencidas.   
17. La persona administradora debe poder seleccionar la entidad beneficiaria final de una donación pendiente de asignación, a partir del resultado de ejecución de los algoritmos de selección.   
18. La persona administradora debe poder administrar los camiones disponibles.   
19. La persona administradora debe poder importar personas donantes ya registradas para otorgarles un usuario desde archivos CSV con más de 10.000 filas.

### Manejo de sesión y autenticación

Para la autenticación y manejo de sesión se deberán utilizar Cookies HTTP. Opcionalmente, y a criterio de cada equipo se podrá utilizar un servicio de autenticación delegada, como [Logto](https://logto.io/) o [Keycloak](https://www.keycloak.org/)[^1]

## *Requerimientos detallados*

1. Se deberán realizar los bocetos de interfaz de usuario  
2. Se deberán implementar los bocetos de la interfaz de usuario.   
3. Se deberá implementar:  
   1. Opción 1: el protocolo [gRPC](https://grpc.io/) como reemplazo de HTTP para las comunicaciones con el servicios que se haya extraído en la entrega 2\.  
   2. Opción 2: el protocolo [GraphQL](https://graphql.org/) como reemplazo de HTTP para las comunicaciones con el servicios que se haya extraído en la entrega 2\.  
4. Se deberá desplegar el sistema en la nube para que pueda ser accedido por el público general[^2]. Para ello, les recomendamos comparar diversas opciones, tales como:  
   1. Los planes gratuitos de los ecosistemas de [AWS](https://aws.amazon.com/free/), [Oracle](https://www.oracle.com/cloud/free/) o [Azure](https://azure.microsoft.com/en-us/free/students)  
   2. Una combinación de servicios como [Render](https://render.com/) / [Railway](https://railway.com/) y [Supabase](https://supabase.com/database)/ [Neon](https://neon.com/)   
   3. Ejecutar los servicios usando el soporte del [DISILAB](https://docs.google.com/document/d/1NR147FzOqQzNVkL8W5lBfLWuJyHP4lTx2-AYXSbDvjA/edit?usp=drivesdk)  
   4. Ejecutar los servicios de manera local y exponerlos públicamente a través de alguna herramienta como [Cloudflare Tunnels](https://developers.cloudflare.com/tunnel/). Esta opción sólo se aceptará si ya hicieron despliegues cloud en otras materias, **todo el equipo tiene acceso al despliegue** y **lo acuerdan con su ayudante**, pero es  interesante para comparar en términos de [privacidad y soberanía digital](https://docs.google.com/document/d/14HdvHvS33WqYb6Ak0BGa0IeCTbzeCRSDKs-1Ot-qLDw/edit?tab=t.0#heading=h.ukbaukqwnb7x).

	

## *Entregables*

1. **Modelo del Dominio**: actualización del modelo de Diagrama de Clases y de arquitectura actualizado para dar soporte a los requerimientos de esta entrega.  
2. **Justificaciones de Diseño**: Documento y Diagramas Complementarios.  
3. **Maquetado** e implementación en HTML de las interfaces de usuario requeridas, considerando la navegabilidad entre interfaces y aplicando estilos.  
4. **Implementación** del cliente liviano y la integración con el servicio. 

[^1]:  Se sugiere a los grupos que estén cursando la materia Desarrollo de Software y no hayan implementado autenticación delegada en esa materia, en este TPA lo hagan. Por el contrario, si ya lo hicieron en aquella materia, se recomienda que no lo hagan en este TPA. 

[^2]:  Idealmente tanto el servidor principal como el servicio extraído en la entrega 2 deberían ser desplegados en nodos diferentes. Si esto no fuera posible, se aceptará desplegar ambos en el mismo nodo. 