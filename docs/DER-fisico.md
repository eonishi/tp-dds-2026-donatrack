# Diagrama Entidad-Relación Físico (DER)

Este diagrama representa el modelo relacional. Como que el sistema fue separado en dos microservicios (`Donaciones` y `Logística`), los esquemas pertenecen a bases de datos independientes. Las referencias entre ambos servicios se realizan mediante IDs lógicos a través de la API.

```mermaid
erDiagram
    %% ==========================================
    %% MICROSERVICIO: DONACIONES
    %% ==========================================
    
    %% ESQUEMA DONANTES
    donantes_donantes {
        bigserial id PK
        varchar tipo_documento
        varchar numero_documento
    }
    donantes_personas_humanas {
        bigint id PK, FK
        varchar nombre
        varchar apellido
    }
    donantes_personaje_juridica {
        bigint id PK, FK
        varchar razon_social
    }
    donantes_medio_de_contacto {
        bigserial id PK
        varchar contacto
        varchar tipo
        bigint donante_id FK
    }

    %% ESQUEMA BENEFICIARIOS
    beneficiarios_entidades_beneficiarias {
        bigserial id PK
        varchar razon_social
    }
    beneficiarios_necesidad {
        bigserial id PK
        bigint entidad_id FK
    }

    %% ESQUEMA DONACIONES
    donaciones_donaciones {
        uuid id PK
        bigint donante_id FK
        bigint entidad_id FK
        varchar estado
    }
    donaciones_historial_estado {
        uuid donacion_id PK, FK
        int orden PK
        varchar estado_alcanzado
    }
    donaciones_bien {
        bigserial id PK
        uuid donacion_id FK
        bigint subcategoria_id FK
    }

    %% Relaciones (Microservicio Donaciones)
    donantes_donantes ||--o| donantes_personas_humanas : "es"
    donantes_donantes ||--o| donantes_personaje_juridica : "es"
    donantes_donantes ||--o{ donantes_medio_de_contacto : "tiene"
    
    donantes_donantes ||--o{ donaciones_donaciones : "realiza"
    beneficiarios_entidades_beneficiarias ||--o{ donaciones_donaciones : "recibe"
    beneficiarios_entidades_beneficiarias ||--o{ beneficiarios_necesidad : "declara"
    
    donaciones_donaciones ||--o{ donaciones_bien : "contiene"
    donaciones_donaciones ||--o{ donaciones_historial_estado : "registra"


    %% ==========================================
    %% MICROSERVICIO: LOGÍSTICA
    %% ==========================================
    
    %% ESQUEMA LOGISTICA
    logistica_camiones {
        bigserial id PK
        varchar patente
    }
    logistica_rutas {
        uuid id PK
        boolean en_curso
        bigint camion_id FK
    }
    logistica_destinos {
        bigserial id PK
        uuid ruta_id FK
        bigint entidad_id "Referencia Lógica"
        varchar direccion
    }
    logistica_destino_donaciones {
        bigint destino_id FK
        uuid donacion_id "Referencia Lógica"
    }

    %% Relaciones (Microservicio Logística)
    logistica_camiones ||--o{ logistica_rutas : "asigna"
    logistica_rutas ||--o{ logistica_destinos : "compone"
    logistica_destinos ||--o{ logistica_destino_donaciones : "agrupa"

    %% ==========================================
    %% RELACIONES ENTRE MICROSERVICIOS (APIs)
    %% ==========================================
    %% Nota: No se dibujan conectores relacionales cruzados para reflejar la 
    %% ausencia de restricciones FK reales. La integración se realiza mediante APIs:
    %% - logistica_destinos usa "entidad_id" referenciando a beneficiarios_entidades_beneficiarias
    %% - logistica_destino_donaciones usa "donacion_id" referenciando a donaciones_donaciones
```
