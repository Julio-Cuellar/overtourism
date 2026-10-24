-- V041 · Caja (F6 de docs/planes/integracion-ventas-caja.md): caja, turno, movimiento_caja, corte, venta_aplicada,
-- evento_procesado y preparacion.
--
-- Seguridad por fila como en V040: se activa aquí y V042 la fuerza. Los movimientos y los cortes son de solo inserción
-- (CJ-09): el dinero que ya se contó no se reescribe. Un turno no cerrado por caja es único (CJ-01) y lo impone un
-- índice parcial.

CREATE SCHEMA IF NOT EXISTS caja;

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'rol_caja') THEN
        CREATE ROLE rol_caja NOLOGIN;
    END IF;
END
$$;

GRANT USAGE ON SCHEMA plataforma TO rol_caja;
GRANT USAGE ON SCHEMA caja TO rol_caja;

DO $$
BEGIN
    EXECUTE format('GRANT rol_caja TO %I', '${app_user}');
END
$$;

CREATE TABLE caja.caja
(
    id         UUID PRIMARY KEY,
    negocio_id UUID        NOT NULL,
    nombre     VARCHAR(60) NOT NULL CONSTRAINT ck_caja_nombre CHECK (btrim(nombre) <> ''),
    activa     BOOLEAN     NOT NULL,
    CONSTRAINT uq_caja_negocio_id UNIQUE (negocio_id, id)
);
-- El nombre es único por negocio sin importar mayúsculas.
CREATE UNIQUE INDEX ux_caja_nombre ON caja.caja (negocio_id, lower(nombre));

CREATE TABLE caja.turno
(
    id              UUID PRIMARY KEY,
    negocio_id      UUID                     NOT NULL,
    caja_id         UUID                     NOT NULL,
    cajero          TEXT                     NOT NULL,
    estado          TEXT                     NOT NULL CONSTRAINT ck_turno_estado CHECK (estado IN ('ABIERTO', 'EN_CORTE', 'CERRADO')),
    fondo_inicial   NUMERIC(14, 2)           NOT NULL CONSTRAINT ck_turno_fondo CHECK (fondo_inicial >= 0),
    abierto_en      TIMESTAMP WITH TIME ZONE NOT NULL,
    cerrado_en      TIMESTAMP WITH TIME ZONE,
    corte_vence_en  TIMESTAMP WITH TIME ZONE,
    version         INTEGER                  NOT NULL CONSTRAINT ck_turno_version CHECK (version >= 1),
    CONSTRAINT uq_turno_negocio_id UNIQUE (negocio_id, id),
    CONSTRAINT fk_turno_caja FOREIGN KEY (negocio_id, caja_id) REFERENCES caja.caja (negocio_id, id),
    CONSTRAINT ck_turno_cierre CHECK ((estado = 'CERRADO') = (cerrado_en IS NOT NULL))
);
-- CJ-01: una caja tiene a lo mucho un turno sin cerrar.
CREATE UNIQUE INDEX ux_turno_no_cerrado ON caja.turno (caja_id) WHERE estado <> 'CERRADO';
CREATE INDEX ix_turno_caja ON caja.turno (negocio_id, caja_id, abierto_en DESC);

CREATE TABLE caja.movimiento_caja
(
    id             UUID PRIMARY KEY,
    -- Orden de registro: es el orden en que se muestran los movimientos de un turno.
    secuencia      BIGINT GENERATED ALWAYS AS IDENTITY,
    negocio_id     UUID                     NOT NULL,
    turno_id       UUID                     NOT NULL,
    tipo           TEXT                     NOT NULL
        CONSTRAINT ck_movimiento_tipo CHECK (tipo IN ('FONDO', 'VENTA', 'ANULACION', 'ENTRADA', 'RETIRO', 'AJUSTE')),
    metodo         TEXT                     NOT NULL
        CONSTRAINT ck_movimiento_metodo CHECK (metodo IN ('EFECTIVO', 'TARJETA', 'TRANSFERENCIA')),
    monto          NUMERIC(14, 2)           NOT NULL,
    venta_id       UUID,
    evento_id      UUID,
    nota           TEXT,
    registrado_en  TIMESTAMP WITH TIME ZONE NOT NULL,
    registrado_por TEXT,
    CONSTRAINT fk_movimiento_turno FOREIGN KEY (negocio_id, turno_id) REFERENCES caja.turno (negocio_id, id)
);
CREATE INDEX ix_movimiento_turno ON caja.movimiento_caja (negocio_id, turno_id, secuencia);
CREATE INDEX ix_movimiento_venta ON caja.movimiento_caja (negocio_id, venta_id) WHERE venta_id IS NOT NULL;

CREATE TABLE caja.corte
(
    id                   UUID PRIMARY KEY,
    negocio_id           UUID                     NOT NULL,
    turno_id             UUID                     NOT NULL,
    caja_id              UUID                     NOT NULL,
    -- Monto por método de pago como cadena decimal, por ejemplo {"EFECTIVO": "120.50"}.
    esperado_por_metodo  JSONB                    NOT NULL CONSTRAINT ck_corte_esperado CHECK (jsonb_typeof(esperado_por_metodo) = 'object'),
    contado_por_metodo   JSONB                    NOT NULL CONSTRAINT ck_corte_contado CHECK (jsonb_typeof(contado_por_metodo) = 'object'),
    diferencia_efectivo  NUMERIC(14, 2)           NOT NULL,
    autorizado_por       TEXT,
    abierto_en           TIMESTAMP WITH TIME ZONE NOT NULL,
    confirmado_en        TIMESTAMP WITH TIME ZONE NOT NULL,
    -- CJ-09: un turno se cierra con un solo corte.
    CONSTRAINT uq_corte_turno UNIQUE (negocio_id, turno_id),
    CONSTRAINT fk_corte_turno FOREIGN KEY (negocio_id, turno_id) REFERENCES caja.turno (negocio_id, id)
);
CREATE INDEX ix_corte_reciente ON caja.corte (negocio_id, confirmado_en DESC);

-- Lo que Caja hizo con cada venta: idempotencia por venta y marca de anulación previa (CJ-04). Sin llave foránea al
-- turno: una marca de anulación previa puede llegar antes que el turno se conozca.
CREATE TABLE caja.venta_aplicada
(
    negocio_id UUID NOT NULL,
    venta_id   UUID NOT NULL,
    turno_id   UUID,
    estado     TEXT NOT NULL CONSTRAINT ck_venta_aplicada_estado CHECK (estado IN ('APLICADA', 'REVERTIDA', 'ANULADO_PREVIO')),
    PRIMARY KEY (negocio_id, venta_id)
);
CREATE INDEX ix_venta_aplicada_turno ON caja.venta_aplicada (negocio_id, turno_id, estado);

-- Candado de idempotencia de los listeners (R3). No lleva negocio_id: el candado es del evento.
CREATE TABLE caja.evento_procesado
(
    consumidor   TEXT                     NOT NULL,
    evento_id    UUID                     NOT NULL,
    procesado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY (consumidor, evento_id)
);

CREATE TABLE caja.preparacion
(
    token_hash   TEXT PRIMARY KEY CONSTRAINT ck_preparacion_token CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    negocio_id   UUID                     NOT NULL,
    actor        TEXT                     NOT NULL,
    comando_tipo TEXT                     NOT NULL,
    comando_hash TEXT                     NOT NULL,
    comando      TEXT,
    resumen      TEXT,
    ambito       TEXT,
    expira_en    TIMESTAMP WITH TIME ZONE NOT NULL,
    usado_en     TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_preparacion_usada_sin_comando CHECK (usado_en IS NULL OR (comando IS NULL AND resumen IS NULL))
);
CREATE INDEX ix_preparacion_vigentes ON caja.preparacion (negocio_id, comando_tipo, ambito) WHERE usado_en IS NULL;
CREATE INDEX ix_preparacion_expira ON caja.preparacion (expira_en);

DO $$
DECLARE
    tabla TEXT;
BEGIN
    FOREACH tabla IN ARRAY ARRAY['caja', 'turno', 'movimiento_caja', 'corte', 'venta_aplicada', 'preparacion']
    LOOP
        EXECUTE format('ALTER TABLE caja.%I ENABLE ROW LEVEL SECURITY', tabla);
        EXECUTE format('CREATE POLICY aislamiento_por_negocio ON caja.%I '
                       'USING (negocio_id = plataforma.negocio_actual()) '
                       'WITH CHECK (negocio_id = plataforma.negocio_actual())', tabla);
    END LOOP;
END
$$;

-- CJ-09: movimientos y cortes solo se insertan; ni el dueño de la base los reescribe.
CREATE TRIGGER trg_movimiento_solo_insercion BEFORE UPDATE OR DELETE ON caja.movimiento_caja
    FOR EACH ROW EXECUTE FUNCTION plataforma.rechazar_modificacion();
CREATE TRIGGER trg_movimiento_sin_vaciado BEFORE TRUNCATE ON caja.movimiento_caja
    FOR EACH STATEMENT EXECUTE FUNCTION plataforma.rechazar_modificacion();
CREATE TRIGGER trg_corte_solo_insercion BEFORE UPDATE OR DELETE ON caja.corte
    FOR EACH ROW EXECUTE FUNCTION plataforma.rechazar_modificacion();
CREATE TRIGGER trg_corte_sin_vaciado BEFORE TRUNCATE ON caja.corte
    FOR EACH STATEMENT EXECUTE FUNCTION plataforma.rechazar_modificacion();

-- Permisos mínimos por tabla. Un turno y una caja no se borran; los candados de idempotencia y las preparaciones sí se
-- purgan.
GRANT SELECT, INSERT ON caja.caja, caja.movimiento_caja, caja.corte TO rol_caja;
GRANT SELECT, INSERT, UPDATE ON caja.turno, caja.venta_aplicada TO rol_caja;
GRANT SELECT, INSERT, DELETE ON caja.evento_procesado TO rol_caja;
GRANT SELECT, INSERT, UPDATE, DELETE ON caja.preparacion TO rol_caja;
