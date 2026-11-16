-- Migración de MedioDeContacto: herencia JOINED + id IDENTITY  →  SINGLE_TABLE + id SEQUENCE.
-- Correr UNA vez, antes de levantar la versión nueva de donaciones, sobre una base que ya tiene
-- las tablas viejas. Todo va en una transacción: si algo falla, no cambia nada.
-- Uso: psql "<conexión>" -v ON_ERROR_STOP=1 -f migracion-medios-single-table.sql

BEGIN;
SET LOCAL search_path TO donaciones;

-- 1. La tabla única necesita el discriminador (qué subclase es cada fila) y la columna que antes
--    vivía solo en correoelectronico.
ALTER TABLE mediodecontacto ADD COLUMN dtype varchar(31);
ALTER TABLE mediodecontacto ADD COLUMN entidad_id bigint;

UPDATE mediodecontacto m SET dtype = 'CorreoElectronico', entidad_id = c.entidad_id
  FROM correoelectronico c WHERE c.id = m.id;
UPDATE mediodecontacto m SET dtype = 'Sms'      FROM sms s      WHERE s.id = m.id;
UPDATE mediodecontacto m SET dtype = 'Whatsapp' FROM whatsapp w WHERE w.id = m.id;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM mediodecontacto WHERE dtype IS NULL) THEN
    RAISE EXCEPTION 'Hay medios de contacto sin subtipo: no se migra nada';
  END IF;
END $$;

ALTER TABLE mediodecontacto ALTER COLUMN dtype SET NOT NULL;
ALTER TABLE mediodecontacto
  ADD CONSTRAINT fk_mediodecontacto_entidad FOREIGN KEY (entidad_id) REFERENCES entidadbeneficiaria (id);

-- 2. Las tablas de las subclases ya no se usan.
DROP TABLE correoelectronico, sms, whatsapp;

-- 3. El id deja de ser autoincremental: lo asigna Hibernate desde una secuencia que reparte de a 50.
--    Arranca por encima del id más alto para no repetir ninguno.
ALTER TABLE mediodecontacto ALTER COLUMN id DROP DEFAULT;
DROP SEQUENCE mediodecontacto_id_seq;
CREATE SEQUENCE medio_de_contacto_seq INCREMENT BY 50;
SELECT setval('medio_de_contacto_seq', (SELECT coalesce(max(id), 0) FROM mediodecontacto) + 50);

COMMIT;
