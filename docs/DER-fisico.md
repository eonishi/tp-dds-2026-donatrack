# Diagrama Entidad-Relación Físico (DER)

Este diagrama representa el modelo relacional físico en la base de datos, reflejando la decisión arquitectónica de aislar las tablas en esquemas separados (`donantes`, `beneficiarios`, `donaciones`, `logistica`).

```mermaid
erDiagram
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
        bigserial id PK
        bigint donante_id FK
        bigint entidad_id FK
        varchar estado
    }
    donaciones_historial_estado {
        bigserial id PK
        bigint donacion_id FK
        varchar estado_alcanzado
    }
    donaciones_bien {
        bigserial id PK
        bigint donacion_id FK
        bigint subcategoria_id FK
    }

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
        bigint ruta_id FK
        bigint entidad_beneficiaria_id FK
    }

    %% RELACIONES
    donantes_donantes ||--o| donantes_personas_humanas : "es"
    donantes_donantes ||--o| donantes_personaje_juridica : "es"
    donantes_donantes ||--o{ donantes_medio_de_contacto : "tiene"
    
    donantes_donantes ||--o{ donaciones_donaciones : "realiza"
    beneficiarios_entidades_beneficiarias ||--o{ donaciones_donaciones : "recibe"
    beneficiarios_entidades_beneficiarias ||--o{ beneficiarios_necesidad : "declara"
    
    donaciones_donaciones ||--o{ donaciones_bien : "contiene"
    donaciones_donaciones ||--o{ donaciones_historial_estado : "registra"
    
    logistica_camiones ||--o{ logistica_rutas : "asigna"
    logistica_rutas ||--o{ logistica_destinos : "compone"
    beneficiarios_entidades_beneficiarias ||--o{ logistica_destinos : "visita"
    logistica_destinos ||--o{ donaciones_donaciones : "entrega"
```
