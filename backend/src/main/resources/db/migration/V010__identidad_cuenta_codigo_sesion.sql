-- V010 · Identidad (IDN-1.1): cuenta, código de acceso y sesión.
--
-- Estas tablas no llevan negocio_id (una persona puede pertenecer a varios negocios), así que la seguridad por fila
-- no sirve para aislarlas. Se aíslan por rol (SI-11): solo rol_identidad las toca. Las reglas de formato y vigencia
-- viven aquí como restricciones, para que ningún error de la aplicación pueda guardar un estado imposible.

CREATE TABLE identidad.cuenta
(
    id               UUID PRIMARY KEY,
    nombre           VARCHAR(80)              NOT NULL CONSTRAINT ck_cuenta_nombre CHECK (btrim(nombre) <> ''),
    telefono         VARCHAR(16)              NOT NULL CONSTRAINT uq_cuenta_telefono UNIQUE
        CONSTRAINT ck_cuenta_telefono CHECK (telefono ~ '^\+52[0-9]{10}$'),
    estado           TEXT                     NOT NULL DEFAULT 'ACTIVA'
        CONSTRAINT ck_cuenta_estado CHECK (estado IN ('ACTIVA', 'SUSPENDIDA', 'ANONIMIZADA')),
    creada_en        TIMESTAMP WITH TIME ZONE NOT NULL,
    ultimo_acceso_en TIMESTAMP WITH TIME ZONE
);
COMMENT ON TABLE identidad.cuenta IS 'La persona que usa el panel. Se identifica por su teléfono E.164 verificado (AC-01).';

-- Del código solo se guarda el hash con sal (AS-01): el código en claro no se puede recuperar.
CREATE TABLE identidad.codigo_acceso
(
    id        UUID PRIMARY KEY,
    telefono  VARCHAR(16)              NOT NULL CONSTRAINT ck_codigo_telefono CHECK (telefono ~ '^\+52[0-9]{10}$'),
    sal       TEXT                     NOT NULL CONSTRAINT ck_codigo_sal CHECK (sal ~ '^[0-9a-f]{32}$'),
    hash      TEXT                     NOT NULL CONSTRAINT ck_codigo_hash CHECK (hash ~ '^[0-9a-f]{64}$'),
    intentos  INTEGER                  NOT NULL DEFAULT 0 CONSTRAINT ck_codigo_intentos CHECK (intentos >= 0),
    expira_en TIMESTAMP WITH TIME ZONE NOT NULL,
    usado_en  TIMESTAMP WITH TIME ZONE,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_codigo_vigencia CHECK (expira_en > creado_en)
);
CREATE INDEX ix_codigo_acceso_telefono ON identidad.codigo_acceso (telefono, creado_en DESC);
-- Los códigos guardan el teléfono: este índice sirve para purgar los vencidos.
CREATE INDEX ix_codigo_expira ON identidad.codigo_acceso (expira_en);

-- Del token solo se guarda su hash SHA-256 (AS-05). negocio_id es el negocio activo y no lleva llave foránea porque
-- el negocio es de otro módulo.
CREATE TABLE identidad.sesion
(
    token_hash        TEXT PRIMARY KEY CONSTRAINT ck_sesion_token CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    cuenta_id         UUID                     NOT NULL REFERENCES identidad.cuenta (id),
    negocio_id        UUID,
    dispositivo       VARCHAR(80),
    ip_truncada       VARCHAR(45),
    creada_en         TIMESTAMP WITH TIME ZONE NOT NULL,
    ultimo_uso_en     TIMESTAMP WITH TIME ZONE NOT NULL,
    expira_en         TIMESTAMP WITH TIME ZONE NOT NULL,
    expira_absoluta_en TIMESTAMP WITH TIME ZONE NOT NULL,
    revocada_en       TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_sesion_vigencia CHECK (expira_en <= expira_absoluta_en)
);
CREATE INDEX ix_sesion_cuenta_vigentes ON identidad.sesion (cuenta_id) WHERE revocada_en IS NULL;
CREATE INDEX ix_sesion_expira ON identidad.sesion (expira_absoluta_en);

GRANT SELECT, INSERT, UPDATE ON identidad.cuenta TO rol_identidad;
GRANT SELECT, INSERT, UPDATE, DELETE ON identidad.codigo_acceso TO rol_identidad;
GRANT SELECT, INSERT, UPDATE, DELETE ON identidad.sesion TO rol_identidad;
