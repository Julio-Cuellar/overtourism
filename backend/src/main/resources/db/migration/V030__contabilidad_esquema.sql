-- V030 · Contabilidad: las 13 tablas P1 del diseño (docs/diseno/contabilidad-diagramas.md, sección 5.3).
--
-- NO VERIFICADA: se escribió en una máquina sin Docker y no se ha aplicado contra PostgreSQL. Las pruebas de
-- ContabilidadPostgresTest y AislamientoContabilidadPostgresTest deben correr antes de darla por buena.
--
-- R1: esquema y rol de grupo propios. Toda tabla lleva negocio_id con seguridad por fila FORZADA contra
-- plataforma.negocio_actual() (E-03). Las llaves foráneas incluyen negocio_id para que nada cuelgue de otro negocio.
-- Pólizas y movimientos solo admiten inserciones (A-03, SK-01): sin permiso de UPDATE ni DELETE y con disparador.

CREATE SCHEMA IF NOT EXISTS contabilidad;

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'rol_contabilidad') THEN
        CREATE ROLE rol_contabilidad NOLOGIN;
    END IF;
END
$$;

GRANT USAGE ON SCHEMA plataforma TO rol_contabilidad;
GRANT USAGE ON SCHEMA contabilidad TO rol_contabilidad;
DO $$
BEGIN
    EXECUTE format('GRANT rol_contabilidad TO %I', '${app_user}');
END
$$;

-- Una fila por negocio; su existencia significa que la contabilidad está activa (C-01).
CREATE TABLE contabilidad.configuracion
(
    negocio_id           UUID PRIMARY KEY,
    activada_en          TIMESTAMP WITH TIME ZONE NOT NULL,
    precios_con_iva      BOOLEAN                  NOT NULL,
    tasa_iva_default     NUMERIC(5, 4) CONSTRAINT ck_configuracion_tasa CHECK (tasa_iva_default >= 0 AND tasa_iva_default < 1),
    fuente_tasa          VARCHAR(200)             NOT NULL CONSTRAINT ck_configuracion_fuente CHECK (btrim(fuente_tasa) <> ''),
    propinas_en_total    BOOLEAN                  NOT NULL,
    ejercicio_inicia_mes SMALLINT                 NOT NULL CONSTRAINT ck_configuracion_mes CHECK (ejercicio_inicia_mes BETWEEN 1 AND 12),
    version              BIGINT                   NOT NULL DEFAULT 0
);

CREATE TABLE contabilidad.cuenta
(
    id                   UUID PRIMARY KEY,
    negocio_id           UUID         NOT NULL,
    codigo               VARCHAR(20)  NOT NULL CONSTRAINT ck_cuenta_codigo CHECK (codigo ~ '^[0-9A-Za-z][0-9A-Za-z.\-]{0,19}$'),
    nombre               VARCHAR(120) NOT NULL CONSTRAINT ck_cuenta_nombre CHECK (btrim(nombre) <> ''),
    tipo                 TEXT         NOT NULL
        CONSTRAINT ck_cuenta_tipo CHECK (tipo IN ('ACTIVO', 'PASIVO', 'CAPITAL', 'INGRESO', 'COSTO', 'GASTO')),
    naturaleza           TEXT         NOT NULL CONSTRAINT ck_cuenta_naturaleza CHECK (naturaleza IN ('DEUDORA', 'ACREEDORA')),
    padre_id             UUID,
    afectable            BOOLEAN      NOT NULL,
    de_sistema           BOOLEAN      NOT NULL,
    codigo_agrupador_sat VARCHAR(10),
    estado               TEXT         NOT NULL CONSTRAINT ck_cuenta_estado CHECK (estado IN ('ACTIVA', 'INACTIVA')),
    version              BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_cuenta_codigo UNIQUE (negocio_id, codigo),
    CONSTRAINT uq_cuenta_negocio UNIQUE (negocio_id, id),
    CONSTRAINT fk_cuenta_padre FOREIGN KEY (negocio_id, padre_id) REFERENCES contabilidad.cuenta (negocio_id, id)
);
CREATE INDEX ix_cuenta_padre ON contabilidad.cuenta (negocio_id, padre_id);

CREATE TABLE contabilidad.regla_contable
(
    id                 UUID PRIMARY KEY,
    negocio_id         UUID                     NOT NULL,
    disparador         TEXT                     NOT NULL CONSTRAINT ck_regla_disparador
        CHECK (disparador IN ('VENTA', 'CUPON', 'CORTE_FALTANTE', 'CORTE_SOBRANTE', 'CORTE_RETIRO')),
    condicion          VARCHAR(30),
    cuenta_cargo_id    UUID                     NOT NULL,
    cuenta_abono_id    UUID                     NOT NULL,
    cuenta_impuesto_id UUID,
    tasa_iva           NUMERIC(5, 4) CONSTRAINT ck_regla_tasa CHECK (tasa_iva >= 0 AND tasa_iva < 1),
    fuente             VARCHAR(200)             NOT NULL CONSTRAINT ck_regla_fuente CHECK (btrim(fuente) <> ''),
    vigente_desde      TIMESTAMP WITH TIME ZONE NOT NULL,
    vigente_hasta      TIMESTAMP WITH TIME ZONE,
    version            BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT ck_regla_vigencia CHECK (vigente_hasta IS NULL OR vigente_hasta >= vigente_desde),
    CONSTRAINT ck_regla_impuesto CHECK (tasa_iva IS NULL OR tasa_iva = 0 OR cuenta_impuesto_id IS NOT NULL),
    CONSTRAINT fk_regla_cargo FOREIGN KEY (negocio_id, cuenta_cargo_id) REFERENCES contabilidad.cuenta (negocio_id, id),
    CONSTRAINT fk_regla_abono FOREIGN KEY (negocio_id, cuenta_abono_id) REFERENCES contabilidad.cuenta (negocio_id, id),
    CONSTRAINT fk_regla_impuesto FOREIGN KEY (negocio_id, cuenta_impuesto_id)
        REFERENCES contabilidad.cuenta (negocio_id, id)
);
-- REGLA_SUPERPUESTA: a lo mucho una regla abierta por disparador y condición.
CREATE UNIQUE INDEX ux_regla_abierta ON contabilidad.regla_contable (negocio_id, disparador, coalesce(condicion, ''))
    WHERE vigente_hasta IS NULL;

CREATE TABLE contabilidad.periodo
(
    id          UUID PRIMARY KEY,
    negocio_id  UUID                     NOT NULL,
    anio        SMALLINT                 NOT NULL,
    mes         SMALLINT                 NOT NULL CONSTRAINT ck_periodo_mes CHECK (mes BETWEEN 1 AND 12),
    estado      TEXT                     NOT NULL CONSTRAINT ck_periodo_estado CHECK (estado IN ('ABIERTO', 'CERRADO')),
    abierto_en  TIMESTAMP WITH TIME ZONE NOT NULL,
    cerrado_en  TIMESTAMP WITH TIME ZONE,
    cerrado_por TEXT,
    reaperturas SMALLINT                 NOT NULL DEFAULT 0,
    version     BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT uq_periodo_mes UNIQUE (negocio_id, anio, mes),
    CONSTRAINT uq_periodo_negocio UNIQUE (negocio_id, id),
    CONSTRAINT ck_periodo_cierre CHECK ((estado = 'ABIERTO') = (cerrado_en IS NULL))
);

-- A-05: el folio se reserva bloqueando la fila del periodo (SELECT ... FOR UPDATE) y tomando max(folio) + 1 de la
-- serie; el índice único es la última defensa contra un folio repetido.
CREATE TABLE contabilidad.poliza
(
    id                 UUID PRIMARY KEY,
    negocio_id         UUID                     NOT NULL,
    periodo_id         UUID                     NOT NULL,
    tipo               TEXT                     NOT NULL CONSTRAINT ck_poliza_tipo CHECK (tipo IN ('INGRESO', 'EGRESO', 'DIARIO')),
    folio              INTEGER                  NOT NULL CONSTRAINT ck_poliza_folio CHECK (folio >= 1),
    fecha              DATE                     NOT NULL,
    concepto           VARCHAR(200)             NOT NULL CONSTRAINT ck_poliza_concepto CHECK (btrim(concepto) <> ''),
    origen             TEXT                     NOT NULL CONSTRAINT ck_poliza_origen CHECK (origen IN ('AUTOMATICA', 'MANUAL', 'REVERSA')),
    evento_id          UUID,
    referencia_tipo    TEXT CONSTRAINT ck_poliza_referencia CHECK (referencia_tipo IN ('VENTA', 'CANJE', 'CORTE', 'PENDIENTE')),
    referencia_id      UUID,
    extemporanea       BOOLEAN                  NOT NULL,
    revierte_poliza_id UUID,
    total              NUMERIC(14, 2)           NOT NULL CONSTRAINT ck_poliza_total CHECK (total > 0),
    creado_por         TEXT                     NOT NULL,
    creado_en          TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_poliza_folio UNIQUE (negocio_id, periodo_id, tipo, folio),
    CONSTRAINT uq_poliza_evento UNIQUE (negocio_id, evento_id),
    CONSTRAINT uq_poliza_reversa UNIQUE (revierte_poliza_id),
    CONSTRAINT uq_poliza_negocio UNIQUE (negocio_id, id),
    CONSTRAINT ck_poliza_reversa CHECK ((origen = 'REVERSA') = (revierte_poliza_id IS NOT NULL)),
    CONSTRAINT fk_poliza_periodo FOREIGN KEY (negocio_id, periodo_id) REFERENCES contabilidad.periodo (negocio_id, id),
    CONSTRAINT fk_poliza_revierte FOREIGN KEY (negocio_id, revierte_poliza_id)
        REFERENCES contabilidad.poliza (negocio_id, id)
);
CREATE INDEX ix_poliza_periodo ON contabilidad.poliza (negocio_id, periodo_id, tipo, id);
CREATE INDEX ix_poliza_referencia ON contabilidad.poliza (negocio_id, referencia_tipo, referencia_id);

CREATE TABLE contabilidad.movimiento
(
    id         UUID PRIMARY KEY,
    negocio_id UUID           NOT NULL,
    poliza_id  UUID           NOT NULL,
    renglon    SMALLINT       NOT NULL CONSTRAINT ck_movimiento_renglon CHECK (renglon BETWEEN 1 AND 50),
    cuenta_id  UUID           NOT NULL,
    cargo      NUMERIC(14, 2) NOT NULL CONSTRAINT ck_movimiento_cargo CHECK (cargo >= 0),
    abono      NUMERIC(14, 2) NOT NULL CONSTRAINT ck_movimiento_abono CHECK (abono >= 0),
    concepto   VARCHAR(120),
    -- A-02: exactamente uno de los dos es mayor que cero.
    CONSTRAINT ck_movimiento_cargo_o_abono CHECK ((cargo > 0) <> (abono > 0)),
    CONSTRAINT uq_movimiento_renglon UNIQUE (poliza_id, renglon),
    CONSTRAINT fk_movimiento_poliza FOREIGN KEY (negocio_id, poliza_id) REFERENCES contabilidad.poliza (negocio_id, id),
    CONSTRAINT fk_movimiento_cuenta FOREIGN KEY (negocio_id, cuenta_id) REFERENCES contabilidad.cuenta (negocio_id, id)
);
CREATE INDEX ix_movimiento_cuenta ON contabilidad.movimiento (negocio_id, cuenta_id, poliza_id);

CREATE TABLE contabilidad.saldo_cuenta
(
    negocio_id    UUID           NOT NULL,
    cuenta_id     UUID           NOT NULL,
    periodo_id    UUID           NOT NULL,
    saldo_inicial NUMERIC(14, 2) NOT NULL,
    cargos        NUMERIC(14, 2) NOT NULL,
    abonos        NUMERIC(14, 2) NOT NULL,
    saldo_final   NUMERIC(14, 2) NOT NULL,
    version       BIGINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (negocio_id, cuenta_id, periodo_id),
    CONSTRAINT fk_saldo_cuenta FOREIGN KEY (negocio_id, cuenta_id) REFERENCES contabilidad.cuenta (negocio_id, id),
    CONSTRAINT fk_saldo_periodo FOREIGN KEY (negocio_id, periodo_id) REFERENCES contabilidad.periodo (negocio_id, id)
);

-- Huellas de los eventos de otros módulos: guardan el id del otro módulo sin llave foránea (R1).
CREATE TABLE contabilidad.venta_contable
(
    id             UUID PRIMARY KEY,
    negocio_id     UUID                     NOT NULL,
    venta_id       UUID                     NOT NULL,
    caja_sesion_id UUID,
    medio_pago     VARCHAR(30),
    total          NUMERIC(14, 2)           NOT NULL,
    base           NUMERIC(14, 2)           NOT NULL,
    iva            NUMERIC(14, 2)           NOT NULL,
    ocurrido_en    TIMESTAMP WITH TIME ZONE NOT NULL,
    poliza_id      UUID                     NOT NULL,
    revertida      BOOLEAN                  NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_venta_contable UNIQUE (negocio_id, venta_id),
    CONSTRAINT fk_venta_poliza FOREIGN KEY (negocio_id, poliza_id) REFERENCES contabilidad.poliza (negocio_id, id)
);
CREATE INDEX ix_venta_sesion ON contabilidad.venta_contable (negocio_id, caja_sesion_id) WHERE medio_pago = 'EFECTIVO';

CREATE TABLE contabilidad.canje_contable
(
    id              UUID PRIMARY KEY,
    negocio_id      UUID           NOT NULL,
    cupon_id        UUID           NOT NULL,
    venta_id        UUID,
    monto_descuento NUMERIC(14, 2) NOT NULL,
    base_descuento  NUMERIC(14, 2) NOT NULL,
    financiador     TEXT           NOT NULL CONSTRAINT ck_canje_financiador CHECK (financiador IN ('NEGOCIO', 'PLATAFORMA')),
    estado          TEXT           NOT NULL CONSTRAINT ck_canje_estado CHECK (estado IN ('CONTABILIZADO', 'SIN_EFECTO')),
    motivo          TEXT,
    poliza_id       UUID,
    CONSTRAINT uq_canje_contable UNIQUE (negocio_id, cupon_id),
    CONSTRAINT ck_canje_poliza CHECK ((estado = 'CONTABILIZADO') = (poliza_id IS NOT NULL)),
    CONSTRAINT fk_canje_poliza FOREIGN KEY (negocio_id, poliza_id) REFERENCES contabilidad.poliza (negocio_id, id)
);

CREATE TABLE contabilidad.corte_contable
(
    id                        UUID PRIMARY KEY,
    negocio_id                UUID                     NOT NULL,
    corte_id                  UUID                     NOT NULL,
    caja_sesion_id            UUID                     NOT NULL,
    efectivo_esperado         NUMERIC(14, 2)           NOT NULL,
    efectivo_contado          NUMERIC(14, 2)           NOT NULL,
    diferencia                NUMERIC(14, 2)           NOT NULL,
    ventas_efectivo_caja      NUMERIC(14, 2),
    ventas_efectivo_contables NUMERIC(14, 2)           NOT NULL,
    conciliacion              TEXT                     NOT NULL
        CONSTRAINT ck_corte_conciliacion CHECK (conciliacion IN ('CUADRA', 'DIFERENCIA_CAJA', 'DESCUADRE_VENTAS')),
    descuadre_publicado_en    TIMESTAMP WITH TIME ZONE,
    poliza_id                 UUID,
    cerrada_en                TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_corte_contable UNIQUE (negocio_id, corte_id),
    CONSTRAINT ck_corte_diferencia CHECK (diferencia = efectivo_contado - efectivo_esperado),
    CONSTRAINT fk_corte_poliza FOREIGN KEY (negocio_id, poliza_id) REFERENCES contabilidad.poliza (negocio_id, id)
);
CREATE INDEX ix_corte_sesion ON contabilidad.corte_contable (negocio_id, caja_sesion_id);

-- Candado de idempotencia de los listeners (E-01). A diferencia de Clientes, lleva negocio_id: el usuario pidió
-- negocio_id en toda tabla y la seguridad por fila también le aplica.
CREATE TABLE contabilidad.evento_procesado
(
    negocio_id  UUID                     NOT NULL,
    evento_id   UUID                     NOT NULL,
    tipo        TEXT                     NOT NULL,
    version     SMALLINT                 NOT NULL,
    recibido_en TIMESTAMP WITH TIME ZONE NOT NULL,
    resultado   TEXT                     NOT NULL CONSTRAINT ck_evento_resultado CHECK (resultado IN ('APLICADO', 'OMITIDO', 'PENDIENTE')),
    motivo      TEXT,
    PRIMARY KEY (negocio_id, evento_id)
);

CREATE TABLE contabilidad.pendiente_contable
(
    id                     UUID PRIMARY KEY,
    negocio_id             UUID                     NOT NULL,
    evento_id              UUID                     NOT NULL,
    tipo_evento            TEXT                     NOT NULL,
    datos                  JSONB                    NOT NULL CONSTRAINT ck_pendiente_datos CHECK (jsonb_typeof(datos) = 'object'),
    motivo                 TEXT                     NOT NULL CONSTRAINT ck_pendiente_motivo
        CHECK (motivo IN ('REGLA_FALTANTE', 'CUENTA_INACTIVA', 'DATOS_INVALIDOS', 'VERSION_NO_SOPORTADA')),
    fecha_evento           DATE                     NOT NULL,
    estado                 TEXT                     NOT NULL
        CONSTRAINT ck_pendiente_estado CHECK (estado IN ('ABIERTO', 'CONTABILIZADO', 'DESCARTADO')),
    resuelto_con_poliza_id UUID,
    motivo_resolucion      VARCHAR(200),
    creado_en              TIMESTAMP WITH TIME ZONE NOT NULL,
    resuelto_en            TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_pendiente_evento UNIQUE (negocio_id, evento_id),
    CONSTRAINT ck_pendiente_descarte CHECK (estado <> 'DESCARTADO' OR motivo_resolucion IS NOT NULL),
    CONSTRAINT ck_pendiente_contabilizado CHECK ((estado = 'CONTABILIZADO') = (resuelto_con_poliza_id IS NOT NULL)),
    CONSTRAINT fk_pendiente_poliza FOREIGN KEY (negocio_id, resuelto_con_poliza_id)
        REFERENCES contabilidad.poliza (negocio_id, id)
);
CREATE INDEX ix_pendiente_abierto ON contabilidad.pendiente_contable (negocio_id, creado_en) WHERE estado = 'ABIERTO';

-- Token de preparar → confirmar (D-23, D-55), con la misma forma que clientes.preparacion. El resumen admite nulo
-- porque se vacía junto con el comando.
CREATE TABLE contabilidad.preparacion
(
    token_hash   TEXT PRIMARY KEY CONSTRAINT ck_preparacion_token CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    negocio_id   UUID                     NOT NULL,
    actor        TEXT                     NOT NULL,
    comando_tipo TEXT                     NOT NULL,
    comando_hash TEXT                     NOT NULL,
    comando      JSONB,
    resumen      TEXT,
    expira_en    TIMESTAMP WITH TIME ZONE NOT NULL,
    usado_en     TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_preparacion_usada_sin_comando CHECK (usado_en IS NULL OR comando IS NULL)
);
CREATE INDEX ix_preparacion_expira ON contabilidad.preparacion (expira_en);

-- E-03: seguridad por fila forzada en las 13 tablas.
DO $$
DECLARE
    tabla TEXT;
BEGIN
    FOREACH tabla IN ARRAY ARRAY['configuracion', 'cuenta', 'regla_contable', 'periodo', 'poliza', 'movimiento',
                                 'saldo_cuenta', 'venta_contable', 'canje_contable', 'corte_contable',
                                 'evento_procesado', 'pendiente_contable', 'preparacion']
    LOOP
        EXECUTE format('ALTER TABLE contabilidad.%I ENABLE ROW LEVEL SECURITY', tabla);
        EXECUTE format('ALTER TABLE contabilidad.%I FORCE ROW LEVEL SECURITY', tabla);
        EXECUTE format('CREATE POLICY aislamiento_por_negocio ON contabilidad.%I '
                       'USING (negocio_id = plataforma.negocio_actual()) '
                       'WITH CHECK (negocio_id = plataforma.negocio_actual())', tabla);
    END LOOP;
END
$$;

-- A-03 y SK-01: pólizas y movimientos solo admiten inserciones, aunque alguien otorgue el permiso por error.
CREATE TRIGGER trg_poliza_solo_insercion BEFORE UPDATE OR DELETE ON contabilidad.poliza
    FOR EACH ROW EXECUTE FUNCTION plataforma.rechazar_modificacion();
CREATE TRIGGER trg_poliza_sin_vaciado BEFORE TRUNCATE ON contabilidad.poliza
    FOR EACH STATEMENT EXECUTE FUNCTION plataforma.rechazar_modificacion();
CREATE TRIGGER trg_movimiento_solo_insercion BEFORE UPDATE OR DELETE ON contabilidad.movimiento
    FOR EACH ROW EXECUTE FUNCTION plataforma.rechazar_modificacion();
CREATE TRIGGER trg_movimiento_sin_vaciado BEFORE TRUNCATE ON contabilidad.movimiento
    FOR EACH STATEMENT EXECUTE FUNCTION plataforma.rechazar_modificacion();

-- Permisos mínimos. Pólizas y movimientos: solo leer e insertar (A-03, F-04). Las huellas, saldos y periodos se
-- actualizan pero no se borran. Solo la preparación se borra (purga).
GRANT SELECT, INSERT ON contabilidad.poliza, contabilidad.movimiento, contabilidad.evento_procesado
    TO rol_contabilidad;
GRANT SELECT, INSERT, UPDATE ON contabilidad.configuracion, contabilidad.cuenta, contabilidad.regla_contable,
    contabilidad.periodo, contabilidad.saldo_cuenta, contabilidad.venta_contable, contabilidad.canje_contable,
    contabilidad.corte_contable, contabilidad.pendiente_contable TO rol_contabilidad;
GRANT SELECT, INSERT, UPDATE, DELETE ON contabilidad.preparacion TO rol_contabilidad;
