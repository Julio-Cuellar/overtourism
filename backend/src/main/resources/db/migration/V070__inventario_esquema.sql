-- V070 · Inventario (F6 de docs/planes/integracion-ventas-caja.md): articulo, saldo, movimiento y preparacion.
--
-- Seguridad por fila como en Clientes y Ventas: se activa aquí y V071 la fuerza. El movimiento es de solo inserción (el
-- kardex no se reescribe: una corrección es otro movimiento) y el saldo es la foto que el código recalcula con cada uno.
-- Las llaves foráneas incluyen negocio_id: un saldo o un movimiento no se cuelga de un artículo de otro negocio.

CREATE SCHEMA IF NOT EXISTS inventario;

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'rol_inventario') THEN
        CREATE ROLE rol_inventario NOLOGIN;
    END IF;
END
$$;

GRANT USAGE ON SCHEMA plataforma TO rol_inventario;
GRANT USAGE ON SCHEMA inventario TO rol_inventario;

DO $$
BEGIN
    EXECUTE format('GRANT rol_inventario TO %I', '${app_user}');
END
$$;

CREATE TABLE inventario.articulo
(
    id                UUID PRIMARY KEY,
    negocio_id        UUID                     NOT NULL,
    nombre            VARCHAR(80)              NOT NULL CONSTRAINT ck_articulo_nombre CHECK (btrim(nombre) <> ''),
    categoria         VARCHAR(40),
    unidad_base       TEXT                     NOT NULL CONSTRAINT ck_articulo_unidad CHECK (unidad_base IN ('G', 'KG', 'ML', 'L', 'PIEZA')),
    -- Unidades propias del negocio: nombre → factor a la unidad base, como cadena decimal ({"costal": "50.00000000"}).
    unidades_propias  JSONB                    NOT NULL DEFAULT '{}'::jsonb
        CONSTRAINT ck_articulo_unidades CHECK (jsonb_typeof(unidades_propias) = 'object'),
    minimo_base       NUMERIC(18, 4)           NOT NULL CONSTRAINT ck_articulo_minimo CHECK (minimo_base >= 0),
    activo            BOOLEAN                  NOT NULL,
    version           BIGINT                   NOT NULL CONSTRAINT ck_articulo_version CHECK (version >= 1),
    creado_en         TIMESTAMP WITH TIME ZONE NOT NULL,
    actualizado_en    TIMESTAMP WITH TIME ZONE NOT NULL,
    creado_por        TEXT                     NOT NULL,
    CONSTRAINT uq_articulo_negocio_id UNIQUE (negocio_id, id)
);
-- U-02: entre los artículos activos de un negocio el nombre no se repite, sin importar mayúsculas.
CREATE UNIQUE INDEX ux_articulo_nombre_activo ON inventario.articulo (negocio_id, lower(nombre)) WHERE activo;
CREATE INDEX ix_articulo_busqueda ON inventario.articulo (negocio_id, id) WHERE activo;

-- Cantidad y costo promedio ponderado de cada artículo. Puede quedar negativo (una salida sin existencia suficiente se
-- permite y queda en alerta), por eso cantidad_base no tiene mínimo.
CREATE TABLE inventario.saldo
(
    articulo_id          UUID PRIMARY KEY,
    negocio_id           UUID           NOT NULL,
    cantidad_base        NUMERIC(18, 4) NOT NULL,
    cpp                  NUMERIC(18, 4) NOT NULL,
    ultimo_movimiento_id UUID,
    version              BIGINT         NOT NULL CONSTRAINT ck_saldo_version CHECK (version >= 1),
    CONSTRAINT fk_saldo_articulo FOREIGN KEY (negocio_id, articulo_id) REFERENCES inventario.articulo (negocio_id, id)
);

CREATE TABLE inventario.movimiento
(
    id                 UUID PRIMARY KEY,
    negocio_id         UUID                     NOT NULL,
    articulo_id        UUID                     NOT NULL,
    tipo               TEXT                     NOT NULL
        CONSTRAINT ck_movimiento_tipo CHECK (tipo IN ('ENTRADA', 'SALIDA', 'MERMA', 'AJUSTE_POS', 'AJUSTE_NEG')),
    cantidad_capturada NUMERIC(18, 4)           NOT NULL,
    unidad_capturada   TEXT                     NOT NULL,
    factor_aplicado    NUMERIC(18, 8)           NOT NULL CONSTRAINT ck_movimiento_factor CHECK (factor_aplicado > 0),
    -- Con signo: positiva si entra, negativa si sale.
    cantidad_base      NUMERIC(18, 4)           NOT NULL CONSTRAINT ck_movimiento_cantidad CHECK (cantidad_base <> 0),
    costo_unitario     NUMERIC(18, 4)           NOT NULL,
    costo_total        NUMERIC(14, 2)           NOT NULL,
    cpp_resultante     NUMERIC(18, 4)           NOT NULL,
    saldo_resultante   NUMERIC(18, 4)           NOT NULL,
    causa_tipo         TEXT                     NOT NULL
        CONSTRAINT ck_movimiento_causa CHECK (causa_tipo IN ('VENTA', 'COMPRA', 'CONTEO', 'MERMA', 'MANUAL', 'REVERSION')),
    causa_id           UUID,
    motivo             TEXT CONSTRAINT ck_movimiento_motivo CHECK (motivo IN ('CADUCIDAD', 'DANO', 'ROBO', 'OTRO')),
    ocurrido_en        TIMESTAMP WITH TIME ZONE NOT NULL,
    registrado_en      TIMESTAMP WITH TIME ZONE NOT NULL,
    registrado_por     TEXT                     NOT NULL,
    -- El signo de la cantidad corresponde al tipo.
    CONSTRAINT ck_movimiento_signo CHECK ((tipo IN ('ENTRADA', 'AJUSTE_POS')) = (cantidad_base > 0)),
    CONSTRAINT fk_movimiento_articulo FOREIGN KEY (negocio_id, articulo_id) REFERENCES inventario.articulo (negocio_id, id)
);
-- El kardex de un artículo se recorre por id (UUIDv7: orden de registro).
CREATE INDEX ix_movimiento_kardex ON inventario.movimiento (negocio_id, articulo_id, id);

-- Token de un solo uso de preparar → confirmar (D-23, D-55). Solo el hash del token. El comando de Inventario no lleva
-- datos personales, así que se conserva (a diferencia de Clientes, Ventas y Caja). Se purga a las 24 horas.
CREATE TABLE inventario.preparacion
(
    token_hash   TEXT PRIMARY KEY CONSTRAINT ck_preparacion_token CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    negocio_id   UUID                     NOT NULL,
    actor        TEXT                     NOT NULL,
    comando_tipo TEXT                     NOT NULL,
    comando_hash TEXT                     NOT NULL,
    comando      TEXT                     NOT NULL,
    resumen      TEXT                     NOT NULL,
    expira_en    TIMESTAMP WITH TIME ZONE NOT NULL,
    usado_en     TIMESTAMP WITH TIME ZONE
);
CREATE INDEX ix_preparacion_expira ON inventario.preparacion (expira_en);

DO $$
DECLARE
    tabla TEXT;
BEGIN
    FOREACH tabla IN ARRAY ARRAY['articulo', 'saldo', 'movimiento', 'preparacion']
    LOOP
        EXECUTE format('ALTER TABLE inventario.%I ENABLE ROW LEVEL SECURITY', tabla);
        EXECUTE format('CREATE POLICY aislamiento_por_negocio ON inventario.%I '
                       'USING (negocio_id = plataforma.negocio_actual()) '
                       'WITH CHECK (negocio_id = plataforma.negocio_actual())', tabla);
    END LOOP;
END
$$;

-- El kardex solo se inserta; ni el dueño de la base lo reescribe.
CREATE TRIGGER trg_movimiento_solo_insercion BEFORE UPDATE OR DELETE ON inventario.movimiento
    FOR EACH ROW EXECUTE FUNCTION plataforma.rechazar_modificacion();
CREATE TRIGGER trg_movimiento_sin_vaciado BEFORE TRUNCATE ON inventario.movimiento
    FOR EACH STATEMENT EXECUTE FUNCTION plataforma.rechazar_modificacion();

-- Permisos mínimos por tabla. Un artículo no se borra, se da de baja.
GRANT SELECT, INSERT, UPDATE ON inventario.articulo, inventario.saldo TO rol_inventario;
GRANT SELECT, INSERT ON inventario.movimiento TO rol_inventario;
GRANT SELECT, INSERT, UPDATE, DELETE ON inventario.preparacion TO rol_inventario;
