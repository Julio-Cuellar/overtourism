-- V023 · Identidad: pre-registro y códigos de acceso por correo (D-84, D-85, D-86).
--
-- El registro es en dos pasos: el formulario se guarda como pre-registro (la persona todavía no tiene cuenta) y manda un
-- código al correo; al verificarlo se crea la cuenta y se borra el pre-registro. Vive lo que dura su código (15 minutos) y
-- se borra al verificar, al vencer y al reemplazarse: guarda datos personales solo mientras hace falta. Hay uno por correo.
-- Como cuenta, codigo_acceso y sesion, no lleva negocio_id: se aísla por rol (SI-11), no por seguridad por fila.
-- Del código solo se guarda el hash con sal (AS-01).

CREATE TABLE identidad.preregistro
(
    id                UUID PRIMARY KEY,
    correo            VARCHAR(120)             NOT NULL CONSTRAINT uq_preregistro_correo UNIQUE
        CONSTRAINT ck_preregistro_correo CHECK (correo = lower(correo) AND correo ~ '^[^@[:space:]]+@[^@[:space:]]+$'),
    telefono          VARCHAR(16)              NOT NULL CONSTRAINT ck_preregistro_telefono CHECK (telefono ~ '^\+52[0-9]{10}$'),
    nombre            VARCHAR(80)              NOT NULL CONSTRAINT ck_preregistro_nombre CHECK (btrim(nombre) <> ''),
    apellidos         VARCHAR(80)              NOT NULL CONSTRAINT ck_preregistro_apellidos CHECK (btrim(apellidos) <> ''),
    aviso_version     INTEGER                  NOT NULL CONSTRAINT ck_preregistro_aviso CHECK (aviso_version > 0),
    aviso_aceptado_en TIMESTAMP WITH TIME ZONE NOT NULL,
    negocio_nombre    VARCHAR(80),
    negocio_giro      TEXT CONSTRAINT ck_preregistro_giro CHECK (negocio_giro IN ('RESTAURANTE', 'MISCELANEA', 'MUSEO')),
    codigo_sal        TEXT                     NOT NULL CONSTRAINT ck_preregistro_sal CHECK (codigo_sal ~ '^[0-9a-f]{32}$'),
    codigo_hash       TEXT                     NOT NULL CONSTRAINT ck_preregistro_hash CHECK (codigo_hash ~ '^[0-9a-f]{64}$'),
    intentos          INTEGER                  NOT NULL DEFAULT 0 CONSTRAINT ck_preregistro_intentos CHECK (intentos >= 0),
    expira_en         TIMESTAMP WITH TIME ZONE NOT NULL,
    creado_en         TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_preregistro_vigencia CHECK (expira_en > creado_en),
    CONSTRAINT ck_preregistro_negocio CHECK ((negocio_nombre IS NULL) = (negocio_giro IS NULL))
);
COMMENT ON TABLE identidad.preregistro IS
    'Formulario de registro pendiente de verificar el correo. Un solo pre-registro por correo; se borra al verificar o al vencer (15 minutos).';
-- Sirve para purgar los vencidos.
CREATE INDEX ix_preregistro_expira ON identidad.preregistro (expira_en);

GRANT SELECT, INSERT, UPDATE, DELETE ON identidad.preregistro TO rol_identidad;

-- D-85: el código de acceso llega al correo de la cuenta. La columna deja de ser un teléfono y pasa a ser el destino.
ALTER TABLE identidad.codigo_acceso DROP CONSTRAINT ck_codigo_telefono;
ALTER TABLE identidad.codigo_acceso RENAME COLUMN telefono TO destino;
ALTER TABLE identidad.codigo_acceso ALTER COLUMN destino TYPE VARCHAR(120);
ALTER TABLE identidad.codigo_acceso
    ADD CONSTRAINT ck_codigo_destino CHECK (destino = lower(destino) AND destino ~ '^[^@[:space:]]+@[^@[:space:]]+$');
ALTER INDEX identidad.ix_codigo_acceso_telefono RENAME TO ix_codigo_acceso_destino;
COMMENT ON COLUMN identidad.codigo_acceso.destino IS 'Correo (en minúsculas) al que se envió el código.';
