-- V072 · Inventario: rastreo del artículo (L-01, D-27) y consumo por venta (K-01 a K-04).
--
-- El consumo es una fila por (negocio, causa, consumidor): esa llave es lo que vuelve idempotentes a registrarConsumo y
-- revertirConsumo. Se escribe en la MISMA transacción que los movimientos y los saldos (FronteraTransaccional), que es lo
-- que hace atómico el consumo (K-02): si algo falla a la mitad, no queda ni el movimiento ni la fila.
-- Aditiva: no toca V070 ni V071, así que una base que ya las aplicó solo corre esta.

-- Como se rastrea el artículo. Hoy solo se crean NINGUNO; LOTE y SERIE ya viajan en la API para que Restaurantes pueda
-- rechazar un artículo de serie como insumo (S-05).
ALTER TABLE inventario.articulo
    ADD COLUMN rastreo TEXT NOT NULL DEFAULT 'NINGUNO'
        CONSTRAINT ck_articulo_rastreo CHECK (rastreo IN ('NINGUNO', 'LOTE', 'SERIE'));

CREATE TABLE inventario.consumo
(
    negocio_id          UUID                     NOT NULL,
    causa_id            UUID                     NOT NULL,
    -- Quien consume ('vinculo', 'restaurantes'): la validez de su valor la decide el servicio, no la base.
    consumidor          TEXT                     NOT NULL CONSTRAINT ck_consumo_consumidor CHECK (btrim(consumidor) <> ''),
    estado              TEXT                     NOT NULL
        CONSTRAINT ck_consumo_estado CHECK (estado IN ('REGISTRADO', 'REVERTIDO', 'ANULADO_PREVIO')),
    -- Lo descontado, con el costo original de cada línea (con él se repone al revertir): arreglo de objetos con
    -- articulo_id, movimiento_id, cantidad_base y costo_unitario como cadenas decimales, para no perder la escala.
    lineas              JSONB                    NOT NULL DEFAULT '[]'::jsonb
        CONSTRAINT ck_consumo_lineas CHECK (jsonb_typeof(lineas) = 'array'),
    costo_total         NUMERIC(18, 4)           NOT NULL CONSTRAINT ck_consumo_costo CHECK (costo_total >= 0),
    -- Ids de los movimientos que repusieron el consumo; vacío mientras no se revierta.
    movimientos_reversa JSONB                    NOT NULL DEFAULT '[]'::jsonb
        CONSTRAINT ck_consumo_reversa CHECK (jsonb_typeof(movimientos_reversa) = 'array'),
    registrado_en       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_consumo PRIMARY KEY (negocio_id, causa_id, consumidor),
    -- K-04: la marca de anulación previa no descontó nada ni repuso nada.
    CONSTRAINT ck_consumo_marca CHECK (estado <> 'ANULADO_PREVIO'
        OR (jsonb_array_length(lineas) = 0 AND jsonb_array_length(movimientos_reversa) = 0)),
    -- Solo un consumo revertido tiene movimientos de reversa.
    CONSTRAINT ck_consumo_solo_revertido_repone CHECK (estado = 'REVERTIDO' OR jsonb_array_length(movimientos_reversa) = 0)
);

-- Seguridad por fila, activada y forzada aquí (como V070 y V071 para el resto de Inventario).
ALTER TABLE inventario.consumo ENABLE ROW LEVEL SECURITY;
CREATE POLICY aislamiento_por_negocio ON inventario.consumo
    USING (negocio_id = plataforma.negocio_actual())
    WITH CHECK (negocio_id = plataforma.negocio_actual());
ALTER TABLE inventario.consumo FORCE ROW LEVEL SECURITY;

-- Un consumo no se borra: se registra, y a lo mucho se revierte una vez (UPDATE).
GRANT SELECT, INSERT, UPDATE ON inventario.consumo TO rol_inventario;
