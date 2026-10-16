-- V022 · Identidad (IDN-2): negocio de referencia, roles, permisos, plantillas por giro y membresías.
--
-- Roles, permisos y plantillas son datos versionados (AM-09): la aplicación solo los lee y los cambia una migración.
-- Los siembra esta migración con los mismos valores que RolesBase y que los catálogos de Identidad y de Clientes (AZ-02);
-- EsquemaEquipoPostgresTest comprueba que coinciden. Los permisos de Inventario se agregarán con IDN-4.4.
--
-- negocio_ref es una copia mínima del negocio (D-78): el dueño del dato es el módulo de negocios.
--
-- membresia es la única tabla con seguridad por fila. Como una persona pertenece a varios negocios y necesita leer las
-- suyas en todos ellos para elegir uno, la política de lectura deja ver las filas del negocio de la transacción
-- (app.negocio_id) o las de la cuenta de la transacción (app.cuenta_id); escribir solo se permite en el negocio de la
-- transacción.
--
-- ATENCIÓN para los repositorios JDBC (IDN-1.1): «siempre queda un dueño activo» (AM-02) es una regla entre filas y el
-- bloqueo optimista de una sola fila no la protege: dos dueños que se quiten a la vez dejarían el negocio sin dueño. Toda
-- operación que quite o degrade a un dueño debe empezar con SELECT ... FROM identidad.negocio_ref WHERE negocio_id = ?
-- FOR UPDATE y repetir el conteo de dueños después.

-- Cuenta de la transacción en curso, igual que negocio_actual(): sin valor devuelve NULL y no coincide con nada.
CREATE FUNCTION plataforma.cuenta_actual() RETURNS uuid
    LANGUAGE sql STABLE PARALLEL SAFE
AS $$
    SELECT nullif(current_setting('app.cuenta_id', true), '')::uuid
$$;

CREATE TABLE identidad.negocio_ref
(
    negocio_id UUID PRIMARY KEY,
    nombre     VARCHAR(80)              NOT NULL CONSTRAINT ck_negocio_nombre CHECK (btrim(nombre) <> ''),
    giro       TEXT                     NOT NULL CONSTRAINT ck_negocio_giro CHECK (giro IN ('RESTAURANTE', 'MISCELANEA', 'MUSEO')),
    estado     TEXT                     NOT NULL DEFAULT 'ACTIVO' CONSTRAINT ck_negocio_estado CHECK (estado IN ('ACTIVO', 'CERRADO')),
    creado_en  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
COMMENT ON TABLE identidad.negocio_ref IS 'Copia mínima del negocio para el selector y la plantilla de roles. El dueño del dato es el módulo de negocios.';

CREATE TABLE identidad.rol
(
    codigo  TEXT PRIMARY KEY CONSTRAINT ck_rol_codigo CHECK (codigo ~ '^[A-Z_]+$'),
    nombre  VARCHAR(40) NOT NULL CONSTRAINT ck_rol_nombre CHECK (btrim(nombre) <> ''),
    version INTEGER     NOT NULL DEFAULT 1 CONSTRAINT ck_rol_version CHECK (version >= 1)
);

CREATE TABLE identidad.permiso
(
    codigo      TEXT PRIMARY KEY CONSTRAINT ck_permiso_codigo CHECK (codigo ~ '^[a-z]+(\.[a-z_]+){1,2}$'),
    modulo      TEXT         NOT NULL,
    descripcion VARCHAR(200) NOT NULL CONSTRAINT ck_permiso_descripcion CHECK (btrim(descripcion) <> ''),
    sensible    BOOLEAN      NOT NULL,
    version     INTEGER      NOT NULL DEFAULT 1 CONSTRAINT ck_permiso_version CHECK (version >= 1),
    CONSTRAINT ck_permiso_modulo CHECK (split_part(codigo, '.', 1) = modulo)
);

CREATE TABLE identidad.rol_permiso
(
    rol_codigo     TEXT NOT NULL REFERENCES identidad.rol (codigo),
    permiso_codigo TEXT NOT NULL REFERENCES identidad.permiso (codigo),
    PRIMARY KEY (rol_codigo, permiso_codigo)
);

CREATE TABLE identidad.plantilla_giro_rol
(
    giro       TEXT    NOT NULL CONSTRAINT ck_plantilla_giro CHECK (giro IN ('RESTAURANTE', 'MISCELANEA', 'MUSEO')),
    rol_codigo TEXT    NOT NULL REFERENCES identidad.rol (codigo),
    orden      INTEGER NOT NULL CONSTRAINT ck_plantilla_orden CHECK (orden >= 1),
    PRIMARY KEY (giro, rol_codigo),
    CONSTRAINT uq_plantilla_orden UNIQUE (giro, orden)
);

CREATE TABLE identidad.membresia
(
    id             UUID PRIMARY KEY,
    cuenta_id      UUID                     NOT NULL REFERENCES identidad.cuenta (id),
    negocio_id     UUID                     NOT NULL REFERENCES identidad.negocio_ref (negocio_id),
    rol_codigo     TEXT                     NOT NULL REFERENCES identidad.rol (codigo),
    estado         TEXT                     NOT NULL DEFAULT 'ACTIVA'
        CONSTRAINT ck_membresia_estado CHECK (estado IN ('ACTIVA', 'SUSPENDIDA', 'REVOCADA')),
    agregada_por   UUID REFERENCES identidad.cuenta (id),
    creada_en      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    actualizada_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    revocada_en    TIMESTAMP WITH TIME ZONE,
    version        BIGINT                   NOT NULL DEFAULT 0,
    -- Una membresía revocada tiene fecha de baja, y solo ella.
    CONSTRAINT ck_membresia_baja CHECK ((estado = 'REVOCADA') = (revocada_en IS NOT NULL))
);
-- AM-01: una cuenta tiene a lo mucho una membresía NO revocada por negocio. Una revocada no vuelve (el disparador de
-- abajo lo impone): para regresar se invita de nuevo y eso crea otra fila, así que la unicidad no cuenta las revocadas.
CREATE UNIQUE INDEX ux_membresia_cuenta_negocio_vigente ON identidad.membresia (cuenta_id, negocio_id)
    WHERE estado <> 'REVOCADA';
CREATE INDEX ix_membresia_cuenta ON identidad.membresia (cuenta_id);
CREATE INDEX ix_membresia_negocio ON identidad.membresia (negocio_id) WHERE estado <> 'REVOCADA';

-- Una membresía no cambia de cuenta ni de negocio, y una revocada no vuelve a ningún otro estado.
CREATE FUNCTION identidad.proteger_membresia() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.cuenta_id <> OLD.cuenta_id OR NEW.negocio_id <> OLD.negocio_id THEN
        RAISE EXCEPTION 'Una membresía no cambia de cuenta ni de negocio' USING ERRCODE = 'restrict_violation';
    END IF;
    IF OLD.estado = 'REVOCADA' AND NEW.estado <> 'REVOCADA' THEN
        RAISE EXCEPTION 'Una membresía revocada no vuelve: hay que invitar de nuevo' USING ERRCODE = 'restrict_violation';
    END IF;
    RETURN NEW;
END
$$;
CREATE TRIGGER trg_membresia_proteger BEFORE UPDATE ON identidad.membresia
    FOR EACH ROW EXECUTE FUNCTION identidad.proteger_membresia();

ALTER TABLE identidad.membresia ENABLE ROW LEVEL SECURITY;
ALTER TABLE identidad.membresia FORCE ROW LEVEL SECURITY;
-- Leer: las del negocio de la transacción o las de la cuenta de la transacción. Escribir: solo en el negocio de la
-- transacción; quien solo tiene la cuenta ve sus membresías pero no puede cambiarlas.
CREATE POLICY ver_por_negocio_o_cuenta ON identidad.membresia FOR SELECT
    USING (negocio_id = plataforma.negocio_actual() OR cuenta_id = plataforma.cuenta_actual());
CREATE POLICY crear_en_el_negocio ON identidad.membresia FOR INSERT
    WITH CHECK (negocio_id = plataforma.negocio_actual());
CREATE POLICY cambiar_en_el_negocio ON identidad.membresia FOR UPDATE
    USING (negocio_id = plataforma.negocio_actual()) WITH CHECK (negocio_id = plataforma.negocio_actual());

-- ---- datos sembrados ----
INSERT INTO identidad.permiso (codigo, modulo, descripcion, sensible) VALUES
    ('identidad.equipo.ver', 'identidad', 'Ver al equipo del negocio', false),
    ('identidad.miembro.invitar', 'identidad', 'Invitar personal', true),
    ('identidad.miembro.cambiar_rol', 'identidad', 'Cambiar el rol de un miembro', true),
    ('identidad.miembro.revocar', 'identidad', 'Suspender o quitar a un miembro', true),
    ('identidad.auditoria.ver', 'identidad', 'Ver la auditoría de accesos', false),
    ('clientes.sugerir', 'clientes', 'Buscar clientes al vender', false),
    ('clientes.lista.ver', 'clientes', 'Ver la lista de clientes', false),
    ('clientes.panorama.ver', 'clientes', 'Ver los conteos de clientes', false),
    ('clientes.ficha.ver', 'clientes', 'Abrir la ficha completa de un cliente', false),
    ('clientes.cliente.registrar', 'clientes', 'Registrar clientes', false),
    ('clientes.cliente.editar', 'clientes', 'Editar clientes', false),
    ('clientes.consentimiento.registrar', 'clientes', 'Registrar o revocar consentimientos', false),
    ('clientes.aviso.publicar', 'clientes', 'Publicar el aviso de privacidad', true),
    ('clientes.cliente.anonimizar', 'clientes', 'Anonimizar a un cliente', true),
    ('clientes.arco.gestionar', 'clientes', 'Atender solicitudes ARCO', true),
    ('clientes.datos.exportar', 'clientes', 'Exportar los datos de un cliente', true);

INSERT INTO identidad.rol (codigo, nombre) VALUES
    ('DUENO', 'Dueño'),
    ('ADMINISTRADOR', 'Administrador'),
    ('GERENTE', 'Gerente'),
    ('CAJERO', 'Cajero'),
    ('MESERO', 'Mesero'),
    ('COCINERO', 'Cocinero'),
    ('TAQUILLERO', 'Taquillero'),
    ('GUIA', 'Guía');

INSERT INTO identidad.rol_permiso (rol_codigo, permiso_codigo) VALUES
    ('DUENO', 'identidad.equipo.ver'),
    ('DUENO', 'identidad.miembro.invitar'),
    ('DUENO', 'identidad.miembro.cambiar_rol'),
    ('DUENO', 'identidad.miembro.revocar'),
    ('DUENO', 'identidad.auditoria.ver'),
    ('DUENO', 'clientes.sugerir'),
    ('DUENO', 'clientes.lista.ver'),
    ('DUENO', 'clientes.panorama.ver'),
    ('DUENO', 'clientes.ficha.ver'),
    ('DUENO', 'clientes.cliente.registrar'),
    ('DUENO', 'clientes.cliente.editar'),
    ('DUENO', 'clientes.consentimiento.registrar'),
    ('DUENO', 'clientes.aviso.publicar'),
    ('DUENO', 'clientes.cliente.anonimizar'),
    ('DUENO', 'clientes.arco.gestionar'),
    ('DUENO', 'clientes.datos.exportar'),
    ('ADMINISTRADOR', 'identidad.equipo.ver'),
    ('ADMINISTRADOR', 'identidad.miembro.invitar'),
    ('ADMINISTRADOR', 'identidad.miembro.cambiar_rol'),
    ('ADMINISTRADOR', 'identidad.miembro.revocar'),
    ('ADMINISTRADOR', 'identidad.auditoria.ver'),
    ('ADMINISTRADOR', 'clientes.sugerir'),
    ('ADMINISTRADOR', 'clientes.lista.ver'),
    ('ADMINISTRADOR', 'clientes.panorama.ver'),
    ('ADMINISTRADOR', 'clientes.ficha.ver'),
    ('ADMINISTRADOR', 'clientes.cliente.registrar'),
    ('ADMINISTRADOR', 'clientes.cliente.editar'),
    ('ADMINISTRADOR', 'clientes.consentimiento.registrar'),
    ('GERENTE', 'identidad.equipo.ver'),
    ('GERENTE', 'clientes.sugerir'),
    ('GERENTE', 'clientes.lista.ver'),
    ('GERENTE', 'clientes.panorama.ver'),
    ('GERENTE', 'clientes.ficha.ver'),
    ('GERENTE', 'clientes.cliente.registrar'),
    ('GERENTE', 'clientes.cliente.editar'),
    ('GERENTE', 'clientes.consentimiento.registrar'),
    ('CAJERO', 'clientes.sugerir'),
    ('CAJERO', 'clientes.cliente.registrar'),
    ('CAJERO', 'clientes.consentimiento.registrar'),
    ('MESERO', 'clientes.sugerir');

INSERT INTO identidad.plantilla_giro_rol (giro, rol_codigo, orden) VALUES
    ('RESTAURANTE', 'DUENO', 1),
    ('RESTAURANTE', 'ADMINISTRADOR', 2),
    ('RESTAURANTE', 'GERENTE', 3),
    ('RESTAURANTE', 'CAJERO', 4),
    ('RESTAURANTE', 'MESERO', 5),
    ('RESTAURANTE', 'COCINERO', 6),
    ('MISCELANEA', 'DUENO', 1),
    ('MISCELANEA', 'ADMINISTRADOR', 2),
    ('MISCELANEA', 'GERENTE', 3),
    ('MISCELANEA', 'CAJERO', 4),
    ('MUSEO', 'DUENO', 1),
    ('MUSEO', 'ADMINISTRADOR', 2),
    ('MUSEO', 'GERENTE', 3),
    ('MUSEO', 'TAQUILLERO', 4),
    ('MUSEO', 'GUIA', 5);

-- La aplicación lee los catálogos y escribe negocios y membresías; roles, permisos y plantillas solo cambian por migración.
GRANT SELECT ON identidad.rol, identidad.permiso, identidad.rol_permiso, identidad.plantilla_giro_rol TO rol_identidad;
-- De negocio_ref la aplicación solo cambia el estado (cerrar un negocio); el nombre y el giro son del módulo de negocios.
GRANT SELECT, INSERT ON identidad.negocio_ref TO rol_identidad;
GRANT UPDATE (estado) ON identidad.negocio_ref TO rol_identidad;
GRANT SELECT, INSERT, UPDATE ON identidad.membresia TO rol_identidad;
