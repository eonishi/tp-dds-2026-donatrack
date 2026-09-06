# **ENTREGA 3: Persistencia**

## *Objetivos de la entrega*

* Incorporar nociones de persistencia de datos en un medio relacional.  
* Incorporar nociones de la técnica de mapeo objeto – relacional.  
* Incorporar nociones de desnormalizaciones del modelo relacional

## *Unidades del Programa Vinculadas*

* Unidad 2: Herramientas de Concepción y Comunicación del Diseño  
* Unidad 5: Diseño de Datos y Estrategias de Persistencia  
* Unidad 6: Diseño de Arquitectura  
* Unidad 8: Validación del Diseño

## *Alcance*

* Persistencia del modelo de objetos previamente generado  
* Normalización de la información

## *Requerimientos detallados*

1. Se deberán persistir las entidades del modelo planteado. Para ello se debe utilizar un ORM.  
   1. Tener en cuenta que cada servicio tiene que tener su propio esquema de datos, y las entidades no deben ser compartidas.  
2. En caso de no haberse separado anteriormente los servicios, estos deberán estar separados en dos proyectos distintos.

## *Entregables*

1. **Modelo del Dominio**: actualización del modelo de Diagrama de Clases con las funcionalidades previstas en esta entrega.  
2. **Justificaciones de Diseño**: Documento y Diagramas Complementarios.  
3. **Modelo de datos**: diagrama de entidad-relación físico.  
4. **Justificaciones y Consideraciones de Diseño Relacional.**  
5. **Implementación** en código de los requerimientos de la presente entrega, utilizando JPA, el paquete jpa-extras y las bases HSQLDB (para el testing) y una base de datos multiplataforma, cliente servidor, de código abierto (PostgreSQL/MariaDB) para el despliegue local. 

