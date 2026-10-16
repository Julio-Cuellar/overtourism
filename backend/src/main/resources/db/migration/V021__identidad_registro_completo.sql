-- V021 · Identidad (D-76): registro completo de la persona.
--
-- La cuenta pasa de «nombre y teléfono» a nombre, apellidos, correo y teléfono, más la versión del aviso de privacidad
-- que aceptó. El id, el correo y el teléfono identifican a la persona y los tres son únicos. El correo se guarda en
-- minúsculas, con la misma lista de caracteres que la aplicación (SI-16), y queda sin verificar hasta que exista un
-- canal de correo (correo_verificado_en nulo).
--
-- V010 ya está en un commit: el cambio va aquí y no editándola. Las columnas nuevas son obligatorias, así que la
-- migración falla (a propósito) si hubiera cuentas sin ellas; hasta hoy la aplicación no escribe en esta tabla.

ALTER TABLE identidad.cuenta
    ADD COLUMN apellidos            VARCHAR(80),
    ADD COLUMN correo               VARCHAR(120),
    ADD COLUMN correo_verificado_en TIMESTAMP WITH TIME ZONE,
    ADD COLUMN aviso_version        INTEGER,
    ADD COLUMN aviso_aceptado_en    TIMESTAMP WITH TIME ZONE;

ALTER TABLE identidad.cuenta
    ALTER COLUMN apellidos SET NOT NULL,
    ALTER COLUMN correo SET NOT NULL,
    ALTER COLUMN aviso_version SET NOT NULL,
    ALTER COLUMN aviso_aceptado_en SET NOT NULL,
    ADD CONSTRAINT ck_cuenta_apellidos CHECK (btrim(apellidos) <> ''),
    ADD CONSTRAINT ck_cuenta_correo CHECK (
        correo ~ '^[a-z0-9._%+-]+@[a-z0-9]([a-z0-9-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$'),
    ADD CONSTRAINT ck_cuenta_aviso_version CHECK (aviso_version >= 1),
    ADD CONSTRAINT uq_cuenta_correo UNIQUE (correo);

COMMENT ON COLUMN identidad.cuenta.correo IS 'Identifica a la persona junto con el id y el teléfono (D-76). Minúsculas, único.';
COMMENT ON COLUMN identidad.cuenta.correo_verificado_en IS 'Nulo mientras no exista un canal de correo para verificarlo.';
COMMENT ON COLUMN identidad.cuenta.aviso_version IS 'Versión del aviso de privacidad del personal que aceptó al registrarse (LFPDPPP).';
