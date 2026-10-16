-- V020 · Clientes (HTC-21, HTC-22 y HTC-23): las nueve tablas P1 del diseño (docs/diseno/clientes.md, sección 5).
--
-- Seguridad por fila: toda tabla con negocio_id queda con RLS activada y FORZADA, y su política compara contra
-- plataforma.negocio_actual() (el app.negocio_id de la transacción). FORCE hace que también le aplique al dueño de
-- la tabla; un superusuario siempre se la salta, por eso la aplicación entra con un rol que no lo es.
-- Las llaves foráneas incluyen negocio_id: un registro no puede colgarse de un cliente o un aviso de otro negocio.

CREATE EXTENSION IF NOT EXISTS unaccent WITH SCHEMA public;

-- unaccent no es IMMUTABLE y una columna generada lo exige: este envoltorio fija el diccionario y lo declara así.
CREATE FUNCTION clientes.f_unaccent(texto TEXT) RETURNS TEXT
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
AS $$
    SELECT public.unaccent('public.unaccent'::regdictionary, texto)
$$;

-- Aviso de privacidad de cada negocio, por versión (L-03). El contenido no se edita: un cambio crea otra versión y
-- cierra la anterior con vigente_hasta.
CREATE TABLE clientes.aviso_privacidad
(
    id                    UUID PRIMARY KEY,
    negocio_id            UUID                     NOT NULL,
    version               INTEGER                  NOT NULL CONSTRAINT ck_aviso_version CHECK (version >= 1),
    responsable_nombre    VARCHAR(120)             NOT NULL CONSTRAINT ck_aviso_responsable CHECK (btrim(responsable_nombre) <> ''),
    responsable_domicilio VARCHAR(200)             NOT NULL CONSTRAINT ck_aviso_domicilio CHECK (btrim(responsable_domicilio) <> ''),
    finalidades           JSONB                    NOT NULL CONSTRAINT ck_aviso_finalidades CHECK (jsonb_typeof(finalidades) = 'array'),
    texto_simplificado    TEXT                     NOT NULL,
    texto_integral        TEXT                     NOT NULL,
    contacto_arco         VARCHAR(200)             NOT NULL,
    vigente_desde         TIMESTAMP WITH TIME ZONE NOT NULL,
    vigente_hasta         TIMESTAMP WITH TIME ZONE,
    publicado_por         TEXT                     NOT NULL,
    CONSTRAINT uq_aviso_version UNIQUE (negocio_id, version),
    CONSTRAINT ck_aviso_vigencia CHECK (vigente_hasta IS NULL OR vigente_hasta > vigente_desde)
);
-- Un negocio tiene a lo mucho un aviso vigente a la vez.
CREATE UNIQUE INDEX ux_aviso_vigente ON clientes.aviso_privacidad (negocio_id) WHERE vigente_hasta IS NULL;

CREATE TABLE clientes.cliente
(
    id                 UUID PRIMARY KEY,
    negocio_id         UUID                     NOT NULL,
    nombre             VARCHAR(80)              NOT NULL CONSTRAINT ck_cliente_nombre CHECK (btrim(nombre) <> ''),
    apellidos          VARCHAR(80),
    -- Nombre y apellidos en minúsculas y sin acentos, para buscar (I-01).
    nombre_busqueda    TEXT GENERATED ALWAYS AS (
        lower(clientes.f_unaccent(btrim(nombre || ' ' || coalesce(apellidos, ''))))) STORED,
    telefono           VARCHAR(16) CONSTRAINT ck_cliente_telefono CHECK (telefono ~ '^\+52[0-9]{10}$'),
    correo             VARCHAR(120),
    fecha_nacimiento   DATE,
    notas              VARCHAR(500),
    origen             TEXT                     NOT NULL CONSTRAINT ck_cliente_origen CHECK (origen IN ('PANEL', 'VISITANTE')),
    estado             TEXT                     NOT NULL DEFAULT 'ACTIVO'
        CONSTRAINT ck_cliente_estado CHECK (estado IN ('ACTIVO', 'ANONIMIZADO')),
    aviso_version      INTEGER                  NOT NULL,
    aviso_entregado_en TIMESTAMP WITH TIME ZONE NOT NULL,
    anonimizado_en     TIMESTAMP WITH TIME ZONE,
    version            BIGINT                   NOT NULL DEFAULT 0,
    creado_en          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    actualizado_en     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    creado_por         TEXT                     NOT NULL,
    CONSTRAINT uq_cliente_negocio_id UNIQUE (negocio_id, id),
    CONSTRAINT fk_cliente_aviso FOREIGN KEY (negocio_id, aviso_version)
        REFERENCES clientes.aviso_privacidad (negocio_id, version),
    CONSTRAINT ck_cliente_anonimizado CHECK ((estado = 'ANONIMIZADO') = (anonimizado_en IS NOT NULL)),
    -- B-01: anonimizar borra los datos de contacto y personales; un cliente anonimizado no los conserva.
    CONSTRAINT ck_cliente_anonimizado_sin_datos CHECK (estado = 'ACTIVO' OR (apellidos IS NULL AND telefono IS NULL
        AND correo IS NULL AND fecha_nacimiento IS NULL AND notas IS NULL))
);
-- I-03: el teléfono de un cliente activo es único por negocio. Uno anonimizado ya no tiene teléfono y deja libre el número.
CREATE UNIQUE INDEX ux_cliente_telefono_activo ON clientes.cliente (negocio_id, telefono)
    WHERE telefono IS NOT NULL AND estado = 'ACTIVO';
CREATE INDEX ix_cliente_estado ON clientes.cliente (negocio_id, estado);
CREATE INDEX ix_cliente_creado ON clientes.cliente (negocio_id, creado_en);
-- Cursor de la búsqueda por nombre. No hay índice de trigramas: bajo seguridad por fila sus operadores (LIKE, similarity)
-- no son leakproof y el planificador no los usa; la búsqueda queda acotada a los clientes de un negocio.
CREATE INDEX ix_cliente_nombre_cursor ON clientes.cliente (negocio_id, nombre_busqueda, id);

-- B-02: la anonimización es irreversible. Ni siquiera el dueño de la base puede reactivar a un cliente anonimizado.
CREATE FUNCTION clientes.rechazar_reactivacion() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'Un cliente anonimizado no puede volver a ser un cliente activo'
        USING ERRCODE = 'restrict_violation';
END
$$;
CREATE TRIGGER trg_cliente_anonimizado_irreversible BEFORE UPDATE OF estado ON clientes.cliente
    FOR EACH ROW WHEN (OLD.estado = 'ANONIMIZADO' AND NEW.estado <> 'ANONIMIZADO')
    EXECUTE FUNCTION clientes.rechazar_reactivacion();

-- Historial de permisos de contacto: solo se inserta; el vigente es el más reciente por finalidad (P-03).
CREATE TABLE clientes.consentimiento
(
    id            UUID PRIMARY KEY,
    negocio_id    UUID                     NOT NULL,
    cliente_id    UUID                     NOT NULL,
    finalidad     TEXT                     NOT NULL
        CONSTRAINT ck_consentimiento_finalidad CHECK (finalidad IN ('CONTACTO_MENSAJERIA', 'PROMOCIONES')),
    estado        TEXT                     NOT NULL
        CONSTRAINT ck_consentimiento_estado CHECK (estado IN ('OTORGADO', 'REVOCADO')),
    forma         TEXT                     NOT NULL CONSTRAINT ck_consentimiento_forma CHECK (forma IN ('EXPRESO', 'TACITO')),
    medio         TEXT                     NOT NULL
        CONSTRAINT ck_consentimiento_medio CHECK (medio IN ('VERBAL', 'PANEL', 'APP_TURISTA')),
    aviso_version INTEGER                  NOT NULL,
    registrado_por TEXT                    NOT NULL,
    registrado_en TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_consentimiento_cliente FOREIGN KEY (negocio_id, cliente_id)
        REFERENCES clientes.cliente (negocio_id, id),
    CONSTRAINT fk_consentimiento_aviso FOREIGN KEY (negocio_id, aviso_version)
        REFERENCES clientes.aviso_privacidad (negocio_id, version)
);
CREATE INDEX ix_consentimiento_vigente ON clientes.consentimiento (negocio_id, cliente_id, finalidad, registrado_en DESC);

-- Solicitudes de acceso, rectificación, cancelación u oposición, con plazos en días hábiles (L-05 a L-08).
CREATE TABLE clientes.solicitud_arco
(
    id                      UUID PRIMARY KEY,
    negocio_id              UUID    NOT NULL,
    cliente_id              UUID,
    derecho                 TEXT    NOT NULL
        CONSTRAINT ck_arco_derecho CHECK (derecho IN ('ACCESO', 'RECTIFICACION', 'CANCELACION', 'OPOSICION')),
    recibida_en             DATE    NOT NULL,
    medio_recepcion         TEXT    NOT NULL CONSTRAINT ck_arco_medio CHECK (medio_recepcion IN ('PRESENCIAL', 'CORREO', 'OTRO')),
    identidad_verificada_en TIMESTAMP WITH TIME ZONE,
    responder_antes_de      DATE    NOT NULL,
    ampliada                BOOLEAN NOT NULL DEFAULT FALSE,
    comunicada_en           DATE,
    ejecutar_antes_de       DATE,
    ejecutada_en            DATE,
    resultado               TEXT CONSTRAINT ck_arco_resultado CHECK (resultado IN ('PROCEDENTE', 'PARCIAL', 'IMPROCEDENTE')),
    motivo                  TEXT,
    estado                  TEXT    NOT NULL
        CONSTRAINT ck_arco_estado CHECK (estado IN ('RECIBIDA', 'COMUNICADA', 'EJECUTADA', 'CERRADA', 'VENCIDA')),
    creado_por              TEXT    NOT NULL,
    CONSTRAINT fk_arco_cliente FOREIGN KEY (negocio_id, cliente_id) REFERENCES clientes.cliente (negocio_id, id)
);
CREATE INDEX ix_arco_plazo ON clientes.solicitud_arco (negocio_id, estado, responder_antes_de);
CREATE INDEX ix_arco_cliente ON clientes.solicitud_arco (negocio_id, cliente_id);

-- Totales de compra: los calcula el código a partir de los eventos de Ventas; nadie los edita (H-04).
CREATE TABLE clientes.resumen_cliente
(
    cliente_id         UUID PRIMARY KEY,
    negocio_id         UUID                     NOT NULL,
    compras_n          INTEGER                  NOT NULL DEFAULT 0 CONSTRAINT ck_resumen_compras CHECK (compras_n >= 0),
    total_comprado     NUMERIC(14, 2)           NOT NULL DEFAULT 0 CONSTRAINT ck_resumen_total CHECK (total_comprado >= 0),
    primera_compra_en  TIMESTAMP WITH TIME ZONE,
    ultima_compra_en   TIMESTAMP WITH TIME ZONE,
    aviso_inactivo_en  TIMESTAMP WITH TIME ZONE,
    version            BIGINT                   NOT NULL DEFAULT 0,
    actualizado_en     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_resumen_cliente FOREIGN KEY (negocio_id, cliente_id) REFERENCES clientes.cliente (negocio_id, id)
);
-- Panorama (clientes frecuentes, inactivos): cuentas por negocio sin pasar por la llave primaria de cliente.
CREATE INDEX ix_resumen_ultima_compra ON clientes.resumen_cliente (negocio_id, ultima_compra_en);

-- Cada venta aplicada a un cliente: da la idempotencia, la reversión y la marca de anulación previa (H-02, H-03).
CREATE TABLE clientes.compra_aplicada
(
    id          UUID PRIMARY KEY,
    negocio_id  UUID                     NOT NULL,
    venta_id    UUID                     NOT NULL,
    cliente_id  UUID,
    monto       NUMERIC(14, 2),
    ocurrido_en TIMESTAMP WITH TIME ZONE,
    estado      TEXT                     NOT NULL
        CONSTRAINT ck_compra_estado CHECK (estado IN ('APLICADA', 'REVERTIDA', 'ANULADO_PREVIO', 'OMITIDA')),
    motivo      TEXT CONSTRAINT ck_compra_motivo CHECK (motivo IN ('CLIENTE_DESCONOCIDO', 'CLIENTE_ANONIMIZADO')),
    creado_en   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_compra_venta UNIQUE (negocio_id, venta_id),
    CONSTRAINT fk_compra_cliente FOREIGN KEY (negocio_id, cliente_id) REFERENCES clientes.cliente (negocio_id, id)
);
CREATE INDEX ix_compra_cliente ON clientes.compra_aplicada (negocio_id, cliente_id, ocurrido_en DESC);
CREATE INDEX ix_compra_negocio_fecha ON clientes.compra_aplicada (negocio_id, ocurrido_en) WHERE estado = 'APLICADA';

-- Quién abrió la ficha completa o exportó los datos de un cliente. Solo se inserta. No guarda lo consultado.
CREATE TABLE clientes.acceso_cliente
(
    id          UUID PRIMARY KEY,
    negocio_id  UUID                     NOT NULL,
    cliente_id  UUID                     NOT NULL,
    actor       TEXT                     NOT NULL,
    operacion   TEXT                     NOT NULL CONSTRAINT ck_acceso_operacion CHECK (operacion IN ('DETALLE', 'EXPORTACION')),
    ocurrido_en TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_acceso_cliente FOREIGN KEY (negocio_id, cliente_id) REFERENCES clientes.cliente (negocio_id, id)
);
CREATE INDEX ix_acceso_cliente ON clientes.acceso_cliente (negocio_id, cliente_id, ocurrido_en DESC);

-- Token de un solo uso de preparar → confirmar (D-23, D-55). Guarda solo el hash del token. El comando lleva datos
-- personales: se vacía al confirmar y la fila se purga a las 24 horas (SC-10). La base impone el vaciado: una
-- preparación con usado_en no puede conservar el comando.
CREATE TABLE clientes.preparacion
(
    token_hash   TEXT PRIMARY KEY CONSTRAINT ck_preparacion_token CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    negocio_id   UUID                     NOT NULL,
    actor        TEXT                     NOT NULL,
    comando_tipo TEXT                     NOT NULL,
    comando_hash TEXT                     NOT NULL,
    comando      JSONB,
    resumen      JSONB                    NOT NULL,
    expira_en    TIMESTAMP WITH TIME ZONE NOT NULL,
    usado_en     TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_preparacion_usada_sin_comando CHECK (usado_en IS NULL OR comando IS NULL),
    CONSTRAINT ck_preparacion_vigente_con_comando CHECK (usado_en IS NOT NULL OR comando IS NOT NULL)
);
CREATE INDEX ix_preparacion_expira ON clientes.preparacion (expira_en);

-- Candado de idempotencia de los listeners (R3). No lleva negocio_id: el candado es del evento.
CREATE TABLE clientes.evento_procesado
(
    consumidor   TEXT                     NOT NULL,
    evento_id    UUID                     NOT NULL,
    procesado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY (consumidor, evento_id)
);

-- Seguridad por fila en las ocho tablas con negocio_id.
DO $$
DECLARE
    tabla TEXT;
BEGIN
    FOREACH tabla IN ARRAY ARRAY['aviso_privacidad', 'cliente', 'consentimiento', 'solicitud_arco',
                                 'resumen_cliente', 'compra_aplicada', 'acceso_cliente', 'preparacion']
    LOOP
        EXECUTE format('ALTER TABLE clientes.%I ENABLE ROW LEVEL SECURITY', tabla);
        EXECUTE format('ALTER TABLE clientes.%I FORCE ROW LEVEL SECURITY', tabla);
        EXECUTE format('CREATE POLICY aislamiento_por_negocio ON clientes.%I '
                       'USING (negocio_id = plataforma.negocio_actual()) '
                       'WITH CHECK (negocio_id = plataforma.negocio_actual())', tabla);
    END LOOP;
END
$$;

-- P-03: consentimiento y bitácora de accesos solo admiten inserciones. El disparador lo impone a todos los roles,
-- incluido el dueño; además no se les otorga UPDATE ni DELETE.
CREATE TRIGGER trg_consentimiento_solo_insercion BEFORE UPDATE OR DELETE ON clientes.consentimiento
    FOR EACH ROW EXECUTE FUNCTION plataforma.rechazar_modificacion();
CREATE TRIGGER trg_consentimiento_sin_vaciado BEFORE TRUNCATE ON clientes.consentimiento
    FOR EACH STATEMENT EXECUTE FUNCTION plataforma.rechazar_modificacion();
CREATE TRIGGER trg_acceso_cliente_solo_insercion BEFORE UPDATE OR DELETE ON clientes.acceso_cliente
    FOR EACH ROW EXECUTE FUNCTION plataforma.rechazar_modificacion();
CREATE TRIGGER trg_acceso_cliente_sin_vaciado BEFORE TRUNCATE ON clientes.acceso_cliente
    FOR EACH STATEMENT EXECUTE FUNCTION plataforma.rechazar_modificacion();

-- De compra_aplicada solo se borran las marcas ANULADO_PREVIO (H-03); el historial de compras aplicadas o revertidas
-- no. La política es RESTRICTIVE: se suma (AND) a la de aislamiento por negocio.
CREATE POLICY solo_borra_marcas_previas ON clientes.compra_aplicada AS RESTRICTIVE FOR DELETE
    USING (estado = 'ANULADO_PREVIO');

-- Permisos mínimos por tabla. Nada se borra salvo lo que el diseño purga (marcas ANULADO_PREVIO, preparaciones y
-- candados de idempotencia); un cliente no se borra, se anonimiza.
GRANT SELECT, INSERT, UPDATE ON clientes.cliente, clientes.solicitud_arco, clientes.resumen_cliente TO rol_clientes;
-- L-03: el contenido de un aviso publicado no se edita; solo se cierra su vigencia. Un cambio crea otra versión.
GRANT SELECT, INSERT ON clientes.aviso_privacidad TO rol_clientes;
GRANT UPDATE (vigente_hasta) ON clientes.aviso_privacidad TO rol_clientes;
GRANT SELECT, INSERT ON clientes.consentimiento, clientes.acceso_cliente TO rol_clientes;
GRANT SELECT, INSERT, UPDATE, DELETE ON clientes.compra_aplicada, clientes.preparacion TO rol_clientes;
GRANT SELECT, INSERT, DELETE ON clientes.evento_procesado TO rol_clientes;
