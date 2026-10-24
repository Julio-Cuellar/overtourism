-- V040 · Ventas (F6 de docs/planes/integracion-ventas-caja.md): venta, linea_venta, pago_venta, serie_folio y preparacion.
--
-- Seguridad por fila: toda tabla con negocio_id compara contra plataforma.negocio_actual() (el app.negocio_id de la
-- transacción, que fija EjecutorEnNegocio). Aquí solo se ACTIVA; V042 la FUERZA, para que también le aplique al dueño de
-- las tablas. Las llaves foráneas incluyen negocio_id: una línea no puede colgarse de una venta de otro negocio.
--
-- Una venta cobrada no se edita: solo se anula (VT-11). La base lo impone: líneas y pagos son de solo inserción, una
-- venta no se borra y, al actualizarla, solo cambian el estado, los datos de la anulación y la versión.

CREATE SCHEMA IF NOT EXISTS ventas;

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'rol_ventas') THEN
        CREATE ROLE rol_ventas NOLOGIN;
    END IF;
END
$$;

GRANT USAGE ON SCHEMA plataforma TO rol_ventas;
GRANT USAGE ON SCHEMA ventas TO rol_ventas;

-- El rol de la aplicación pertenece a los módulos (un solo pool de conexiones). ${app_user} es el placeholder de Flyway
-- que usa V001.
DO $$
BEGIN
    EXECUTE format('GRANT rol_ventas TO %I', '${app_user}');
END
$$;

-- Consecutivo del folio por negocio y serie, sin huecos (VT-06). El incremento bloquea la fila hasta que termina la
-- transacción de la venta: dos confirmaciones simultáneas no reciben el mismo folio, y si la venta se deshace el folio
-- también (nunca queda un hueco).
CREATE TABLE ventas.serie_folio
(
    negocio_id UUID   NOT NULL,
    serie      TEXT   NOT NULL CONSTRAINT ck_serie_folio_serie CHECK (btrim(serie) <> ''),
    ultimo     BIGINT NOT NULL CONSTRAINT ck_serie_folio_ultimo CHECK (ultimo >= 1),
    PRIMARY KEY (negocio_id, serie)
);

CREATE TABLE ventas.venta
(
    id               UUID PRIMARY KEY,
    negocio_id       UUID                     NOT NULL,
    serie            TEXT                     NOT NULL CONSTRAINT ck_venta_serie CHECK (btrim(serie) <> ''),
    folio            BIGINT                   NOT NULL CONSTRAINT ck_venta_folio CHECK (folio >= 1),
    estado           TEXT                     NOT NULL CONSTRAINT ck_venta_estado CHECK (estado IN ('REGISTRADA', 'ANULADA')),
    -- Referencias opacas a otros módulos (R1): sin llave foránea.
    turno_id         UUID                     NOT NULL,
    cliente_id       UUID,
    cupon_id         UUID,
    subtotal         NUMERIC(14, 2)           NOT NULL CONSTRAINT ck_venta_subtotal CHECK (subtotal >= 0),
    descuento        NUMERIC(14, 2)           NOT NULL CONSTRAINT ck_venta_descuento CHECK (descuento >= 0),
    total            NUMERIC(14, 2)           NOT NULL CONSTRAINT ck_venta_total_positivo CHECK (total >= 0),
    ocurrido_en      TIMESTAMP WITH TIME ZONE NOT NULL,
    registrada_por   TEXT                     NOT NULL,
    anulada_en       TIMESTAMP WITH TIME ZONE,
    anulada_por      TEXT,
    motivo_anulacion TEXT,
    version          INTEGER                  NOT NULL CONSTRAINT ck_venta_version CHECK (version >= 1),
    CONSTRAINT uq_venta_folio UNIQUE (negocio_id, serie, folio),
    CONSTRAINT uq_venta_negocio_id UNIQUE (negocio_id, id),
    -- VT-08: el total es el subtotal menos el descuento.
    CONSTRAINT ck_venta_total CHECK (total = subtotal - descuento),
    CONSTRAINT ck_venta_anulacion CHECK ((estado = 'ANULADA') = (anulada_en IS NOT NULL)
        AND (estado = 'ANULADA' OR (anulada_por IS NULL AND motivo_anulacion IS NULL)))
);
CREATE INDEX ix_venta_turno ON ventas.venta (negocio_id, turno_id, folio);
CREATE INDEX ix_venta_reciente ON ventas.venta (negocio_id, ocurrido_en DESC, folio DESC);

CREATE TABLE ventas.linea_venta
(
    venta_id        UUID           NOT NULL,
    negocio_id      UUID           NOT NULL,
    linea           INTEGER        NOT NULL CONSTRAINT ck_linea_numero CHECK (linea >= 1),
    producto_id     UUID           NOT NULL,
    descripcion     TEXT           NOT NULL,
    cantidad        NUMERIC(18, 4) NOT NULL,
    unidad          TEXT           NOT NULL,
    precio_unitario NUMERIC(18, 4) NOT NULL CONSTRAINT ck_linea_precio CHECK (precio_unitario >= 0),
    importe         NUMERIC(14, 2) NOT NULL CONSTRAINT ck_linea_importe CHECK (importe >= 0),
    serie           TEXT,
    PRIMARY KEY (venta_id, linea),
    CONSTRAINT ck_linea_cantidad CHECK (cantidad > 0),
    CONSTRAINT fk_linea_venta FOREIGN KEY (negocio_id, venta_id) REFERENCES ventas.venta (negocio_id, id)
);

CREATE TABLE ventas.pago_venta
(
    venta_id   UUID           NOT NULL,
    negocio_id UUID           NOT NULL,
    orden      INTEGER        NOT NULL CONSTRAINT ck_pago_orden CHECK (orden >= 1),
    metodo     TEXT           NOT NULL CONSTRAINT ck_pago_metodo CHECK (metodo IN ('EFECTIVO', 'TARJETA', 'TRANSFERENCIA')),
    monto      NUMERIC(14, 2) NOT NULL CONSTRAINT ck_pago_monto CHECK (monto > 0),
    recibido   NUMERIC(14, 2),
    cambio     NUMERIC(14, 2),
    referencia TEXT,
    PRIMARY KEY (venta_id, orden),
    CONSTRAINT fk_pago_venta FOREIGN KEY (negocio_id, venta_id) REFERENCES ventas.venta (negocio_id, id)
);

-- Token de un solo uso de preparar → confirmar (D-23, D-55). Guarda solo el hash del token. El comando se guarda como
-- texto, no como jsonb: el hash del comando se calcula sobre el texto exacto y jsonb lo reordenaría. Se vacía al usarse
-- (SC-10) y la base lo impone: una preparación usada no puede conservar el comando.
CREATE TABLE ventas.preparacion
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
CREATE INDEX ix_preparacion_vigentes ON ventas.preparacion (negocio_id, comando_tipo, ambito) WHERE usado_en IS NULL;
CREATE INDEX ix_preparacion_expira ON ventas.preparacion (expira_en);

-- Seguridad por fila (se activa aquí y se fuerza en V042).
DO $$
DECLARE
    tabla TEXT;
BEGIN
    FOREACH tabla IN ARRAY ARRAY['serie_folio', 'venta', 'linea_venta', 'pago_venta', 'preparacion']
    LOOP
        EXECUTE format('ALTER TABLE ventas.%I ENABLE ROW LEVEL SECURITY', tabla);
        EXECUTE format('CREATE POLICY aislamiento_por_negocio ON ventas.%I '
                       'USING (negocio_id = plataforma.negocio_actual()) '
                       'WITH CHECK (negocio_id = plataforma.negocio_actual())', tabla);
    END LOOP;
END
$$;

-- VT-11: líneas y pagos solo se insertan. El disparador lo impone a todos los roles, incluido el dueño.
CREATE TRIGGER trg_linea_venta_solo_insercion BEFORE UPDATE OR DELETE ON ventas.linea_venta
    FOR EACH ROW EXECUTE FUNCTION plataforma.rechazar_modificacion();
CREATE TRIGGER trg_linea_venta_sin_vaciado BEFORE TRUNCATE ON ventas.linea_venta
    FOR EACH STATEMENT EXECUTE FUNCTION plataforma.rechazar_modificacion();
CREATE TRIGGER trg_pago_venta_solo_insercion BEFORE UPDATE OR DELETE ON ventas.pago_venta
    FOR EACH ROW EXECUTE FUNCTION plataforma.rechazar_modificacion();
CREATE TRIGGER trg_pago_venta_sin_vaciado BEFORE TRUNCATE ON ventas.pago_venta
    FOR EACH STATEMENT EXECUTE FUNCTION plataforma.rechazar_modificacion();
CREATE TRIGGER trg_venta_sin_borrado BEFORE DELETE ON ventas.venta
    FOR EACH ROW EXECUTE FUNCTION plataforma.rechazar_modificacion();
CREATE TRIGGER trg_venta_sin_vaciado BEFORE TRUNCATE ON ventas.venta
    FOR EACH STATEMENT EXECUTE FUNCTION plataforma.rechazar_modificacion();

-- De una venta solo cambian el estado, la anulación y la versión, y una venta anulada no vuelve a registrada.
CREATE FUNCTION ventas.solo_se_anula() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF (NEW.id, NEW.negocio_id, NEW.serie, NEW.folio, NEW.turno_id, NEW.cliente_id, NEW.cupon_id, NEW.subtotal,
        NEW.descuento, NEW.total, NEW.ocurrido_en, NEW.registrada_por)
        IS DISTINCT FROM
       (OLD.id, OLD.negocio_id, OLD.serie, OLD.folio, OLD.turno_id, OLD.cliente_id, OLD.cupon_id, OLD.subtotal,
        OLD.descuento, OLD.total, OLD.ocurrido_en, OLD.registrada_por)
       OR (OLD.estado = 'ANULADA' AND NEW.estado <> 'ANULADA') THEN
        RAISE EXCEPTION 'Una venta cobrada solo se anula: no se edita'
            USING ERRCODE = 'restrict_violation';
    END IF;
    RETURN NEW;
END
$$;
CREATE TRIGGER trg_venta_solo_se_anula BEFORE UPDATE ON ventas.venta
    FOR EACH ROW EXECUTE FUNCTION ventas.solo_se_anula();

-- Permisos mínimos por tabla. Nada se borra salvo lo que el diseño purga (preparaciones vencidas).
GRANT SELECT, INSERT ON ventas.venta, ventas.linea_venta, ventas.pago_venta TO rol_ventas;
GRANT UPDATE (estado, anulada_en, anulada_por, motivo_anulacion, version) ON ventas.venta TO rol_ventas;
GRANT SELECT, INSERT, UPDATE ON ventas.serie_folio TO rol_ventas;
GRANT SELECT, INSERT, UPDATE, DELETE ON ventas.preparacion TO rol_ventas;
