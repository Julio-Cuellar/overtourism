-- V031 · El horario semanal de una sucursal tiene su propia version (NG-10): editar la direccion no debe chocar con
-- editar el horario, ni al reves.
ALTER TABLE negocios.sucursal ADD COLUMN horario_version BIGINT NOT NULL DEFAULT 0;
