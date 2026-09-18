-- Migración: el estado pasa de índice a nombre de constante. Se corre UNA VEZ, a mano, sobre una
-- base del esquema anterior; con una base vacía alcanza con recrear el esquema:
--   podman exec -i donatrack-postgres psql -U postgres -d postgres < db/migrations/2026-09-19-estado-de-donacion-a-nombre.sql
-- Identificadores sin comillas porque Hibernate los creó sin comillar. Los dos ALTER son de Postgres;
-- los dos UPDATE son SQL estándar y el test que los acompaña los ejecuta tal como se entregan.
BEGIN;

-- 1. El historial guardaba el índice de la constante.
ALTER TABLE HistorialEstado
  ALTER COLUMN estadoAlcanzado TYPE varchar(255)
  USING estadoAlcanzado::text;

-- Un valor inesperado queda en NULL y la restricción de abajo lo rechaza.
UPDATE HistorialEstado
   SET estadoAlcanzado = CASE estadoAlcanzado
        WHEN '0' THEN 'EN_DEPOSITO'
        WHEN '1' THEN 'ASIGNACION_REALIZADA'
        WHEN '2' THEN 'LISTA_PARA_ENTREGAR'
        WHEN '3' THEN 'EN_TRASLADO'
        WHEN '4' THEN 'ENTREGADA'
        WHEN '5' THEN 'ENTREGA_FALLIDA'
        WHEN '6' THEN 'VENCIDA'
        ELSE NULL
      END;
ALTER TABLE HistorialEstado ALTER COLUMN estadoAlcanzado SET NOT NULL;

-- 2. La donación no tenía columna de estado: se agrega y se rellena con la última transición.
ALTER TABLE Donacion ADD COLUMN IF NOT EXISTS estado varchar(255);
UPDATE Donacion
   SET estado = (
     SELECT h.estadoAlcanzado FROM HistorialEstado h
      WHERE h.donacion_id = Donacion.id
      ORDER BY h.id DESC
      LIMIT 1)
 WHERE estado IS NULL;
ALTER TABLE Donacion ALTER COLUMN estado SET NOT NULL;
COMMIT;
