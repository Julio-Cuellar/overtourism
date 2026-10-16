-- V030 · Negocios (N1 a N3): las tablas de las etapas autorizadas del diseño (docs/diseno/negocios.md, sección 5).
-- Equipo, archivos y reseñas llegan con sus etapas.
--
-- Seguridad por fila: cada tabla con negocio_id compara contra plataforma.negocio_actual() (el app.negocio_id de la
-- transacción). alta_negocio se aísla por CUENTA (plataforma.cuenta_actual()), porque cuando un alta se rechaza no hay
-- negocio. Las tablas que alimentan la vista pública tienen RLS activada pero NO forzada: la vista corre con los
-- privilegios de su dueño y puede leer todos los negocios publicados, mientras que el rol de la aplicación (que no es
-- el dueño) sigue sujeto a la política. La ruta pública entra con el rol rol_negocios_publico, que solo tiene SELECT
-- sobre la vista y ningún permiso sobre las tablas base (SN-14).

CREATE SCHEMA IF NOT EXISTS negocios;

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'rol_negocios') THEN
        CREATE ROLE rol_negocios NOLOGIN;
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'rol_negocios_publico') THEN
        CREATE ROLE rol_negocios_publico NOLOGIN;
    END IF;
END
$$;

GRANT USAGE ON SCHEMA plataforma TO rol_negocios;
GRANT USAGE ON SCHEMA negocios TO rol_negocios, rol_negocios_publico;

-- El rol de la aplicación pertenece a los módulos (un solo pool de conexiones). La ruta pública hace SET LOCAL ROLE
-- rol_negocios_publico dentro de su transacción. ${app_user} es el placeholder de Flyway que usa V001.
DO $$
BEGIN
    EXECUTE format('GRANT rol_negocios, rol_negocios_publico TO %I', '${app_user}');
END
$$;

CREATE FUNCTION negocios.f_unaccent(texto TEXT) RETURNS TEXT
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
AS $$
    SELECT public.unaccent('public.unaccent'::regdictionary, texto)
$$;

-- ---------------------------------------------------------------------------------------------------------------
-- Catálogos globales (solo lectura para la aplicación). Agregar un giro, una subcategoría o un atributo es una
-- migración, no un cambio de código (D-94, CA-05).
-- ---------------------------------------------------------------------------------------------------------------
CREATE TABLE negocios.giro
(
    codigo VARCHAR(30) PRIMARY KEY,
    nombre VARCHAR(60) NOT NULL,
    activo BOOLEAN     NOT NULL DEFAULT TRUE,
    orden  INTEGER     NOT NULL
);

CREATE TABLE negocios.subcategoria
(
    codigo VARCHAR(40) PRIMARY KEY,
    giro   VARCHAR(30) NOT NULL REFERENCES negocios.giro (codigo),
    nombre VARCHAR(60) NOT NULL,
    orden  INTEGER     NOT NULL,
    activa BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE TABLE negocios.catalogo_atributo
(
    tipo   VARCHAR(20) NOT NULL CONSTRAINT ck_atributo_tipo CHECK (tipo IN ('DIETA', 'ACCESIBILIDAD')),
    codigo VARCHAR(40) NOT NULL,
    nombre VARCHAR(60) NOT NULL,
    orden  INTEGER     NOT NULL,
    activa BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (tipo, codigo)
);

INSERT INTO negocios.giro (codigo, nombre, orden)
VALUES ('RESTAURANTE', 'Restaurante', 1),
       ('MUSEO', 'Museo', 2),
       ('MISCELANEA', 'Miscelánea', 3);

INSERT INTO negocios.subcategoria (codigo, giro, nombre, orden)
VALUES ('GASTRONOMIA_TIPICA', 'RESTAURANTE', 'Gastronomía típica', 1),
       ('COMIDA_CORRIDA', 'RESTAURANTE', 'Comida corrida', 2),
       ('SIN_GLUTEN', 'RESTAURANTE', 'Sin gluten', 3),
       ('VERANO', 'RESTAURANTE', 'Verano', 4),
       ('MIXTO', 'RESTAURANTE', 'Mixto', 5),
       ('EXTRANJERA', 'RESTAURANTE', 'Extranjera', 6),
       ('HISTORIA', 'MUSEO', 'Historia', 1),
       ('CULTURA', 'MUSEO', 'Cultura', 2),
       ('ARTE', 'MUSEO', 'Arte', 3),
       ('ABARROTES', 'MISCELANEA', 'Abarrotes y bebidas', 1),
       ('DULCES_TIPICOS', 'MISCELANEA', 'Dulces típicos', 2),
       ('ARTESANIAS', 'MISCELANEA', 'Artesanías', 3),
       ('TALAVERA', 'MISCELANEA', 'Talavera y cerámica', 4),
       ('TEXTILES', 'MISCELANEA', 'Textiles y bordados', 5),
       ('JOYERIA_PLATA', 'MISCELANEA', 'Joyería, plata y bisutería', 6),
       ('ARTE_GALERIA', 'MISCELANEA', 'Arte y galería', 7),
       ('ANTIGUEDADES', 'MISCELANEA', 'Antigüedades y juguetes tradicionales', 8),
       ('PAPELERIA_REGALOS', 'MISCELANEA', 'Papelería y regalos', 9),
       ('ROPA_ACCESORIOS', 'MISCELANEA', 'Ropa, calzado y accesorios', 10);

INSERT INTO negocios.catalogo_atributo (tipo, codigo, nombre, orden)
VALUES ('DIETA', 'VEGETARIANA', 'Vegetariana', 1),
       ('DIETA', 'VEGANA', 'Vegana', 2),
       ('DIETA', 'SIN_GLUTEN', 'Sin gluten', 3),
       ('DIETA', 'SIN_LACTOSA', 'Sin lactosa', 4),
       ('DIETA', 'SIN_FRUTOS_SECOS', 'Sin frutos secos', 5),
       ('ACCESIBILIDAD', 'ACCESO_SIN_ESCALONES', 'Acceso sin escalones', 1),
       ('ACCESIBILIDAD', 'BANO_ACCESIBLE', 'Baño accesible', 2),
       ('ACCESIBILIDAD', 'ESTACIONAMIENTO_ACCESIBLE', 'Estacionamiento accesible', 3),
       ('ACCESIBILIDAD', 'SENALIZACION_BRAILLE', 'Señalización en braille', 4),
       ('ACCESIBILIDAD', 'PERSONAL_LSM', 'Personal en lengua de señas mexicana', 5),
       ('ACCESIBILIDAD', 'ACEPTA_PERROS_GUIA', 'Acepta perros guía', 6);

-- ---------------------------------------------------------------------------------------------------------------
-- El negocio. Su id lo asigna Identidad y viaja en el sobre del evento de alta (D-89).
-- ---------------------------------------------------------------------------------------------------------------
CREATE TABLE negocios.negocio
(
    id                  UUID PRIMARY KEY,
    nombre              VARCHAR(80)              NOT NULL CONSTRAINT ck_negocio_nombre CHECK (char_length(btrim(nombre)) >= 2),
    nombre_busqueda     TEXT GENERATED ALWAYS AS (lower(negocios.f_unaccent(nombre))) STORED,
    giro                VARCHAR(30)              NOT NULL REFERENCES negocios.giro (codigo),
    estado              TEXT                     NOT NULL DEFAULT 'ACTIVO'
        CONSTRAINT ck_negocio_estado CHECK (estado IN ('ACTIVO', 'SUSPENDIDO', 'CERRADO')),
    suspendido_por      TEXT CONSTRAINT ck_negocio_suspendido_por CHECK (suspendido_por IN ('DUENO', 'PLATAFORMA')),
    publicado           BOOLEAN                  NOT NULL DEFAULT FALSE,
    descripcion         VARCHAR(280),
    telefono_contacto   VARCHAR(16) CONSTRAINT ck_negocio_telefono CHECK (telefono_contacto ~ '^\+52[0-9]{10}$'),
    correo_contacto     VARCHAR(120),
    contacto_publico    BOOLEAN                  NOT NULL DEFAULT FALSE,
    zona_horaria        VARCHAR(40)              NOT NULL DEFAULT 'America/Mexico_City',
    logo_archivo_id     UUID,
    solicitado_por      UUID                     NOT NULL,
    duracion_visita_min SMALLINT CONSTRAINT ck_negocio_duracion CHECK (duracion_visita_min BETWEEN 5 AND 480),
    costo_medio_mxn     INTEGER CONSTRAINT ck_negocio_costo CHECK (costo_medio_mxn BETWEEN 0 AND 100000),
    es_simulado         BOOLEAN                  NOT NULL DEFAULT FALSE,
    motivo_estado       VARCHAR(80),
    creado_en           TIMESTAMP WITH TIME ZONE NOT NULL,
    actualizado_en      TIMESTAMP WITH TIME ZONE NOT NULL,
    cerrado_en          TIMESTAMP WITH TIME ZONE,
    version             BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT ck_negocio_suspension CHECK ((estado = 'SUSPENDIDO') = (suspendido_por IS NOT NULL)),
    CONSTRAINT ck_negocio_cierre CHECK ((estado = 'CERRADO') = (cerrado_en IS NOT NULL))
);
CREATE INDEX ix_negocio_publico ON negocios.negocio (giro, nombre_busqueda) WHERE estado = 'ACTIVO' AND publicado;

CREATE TABLE negocios.negocio_subcategoria
(
    negocio_id          UUID                     NOT NULL REFERENCES negocios.negocio (id),
    subcategoria_codigo VARCHAR(40)              NOT NULL REFERENCES negocios.subcategoria (codigo),
    creada_en           TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (negocio_id, subcategoria_codigo)
);

CREATE TABLE negocios.negocio_atributo
(
    negocio_id UUID                     NOT NULL REFERENCES negocios.negocio (id),
    tipo       VARCHAR(20)              NOT NULL,
    codigo     VARCHAR(40)              NOT NULL,
    creada_en  TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (negocio_id, tipo, codigo),
    FOREIGN KEY (tipo, codigo) REFERENCES negocios.catalogo_atributo (tipo, codigo)
);

CREATE TABLE negocios.disponibilidad
(
    negocio_id         UUID PRIMARY KEY REFERENCES negocios.negocio (id),
    nivel              TEXT                     NOT NULL CONSTRAINT ck_disp_nivel CHECK (nivel IN ('LIBRE', 'MODERADA', 'LLENA')),
    nota               VARCHAR(80),
    fuente             TEXT                     NOT NULL CONSTRAINT ck_disp_fuente CHECK (fuente IN ('MANUAL', 'VERTICAL')),
    declarada_por      UUID,
    declarada_en       TIMESTAMP WITH TIME ZONE NOT NULL,
    vence_en           TIMESTAMP WITH TIME ZONE NOT NULL,
    ocurrido_en_origen TIMESTAMP WITH TIME ZONE
);

-- El resultado de cada petición de alta, para poder responderle a la persona aunque el negocio no haya nacido.
-- Sin llave foránea al negocio: un alta rechazada no tiene negocio. Aislada por cuenta, no por negocio.
CREATE TABLE negocios.alta_negocio
(
    negocio_id     UUID PRIMARY KEY,
    solicitado_por UUID                     NOT NULL,
    nombre         VARCHAR(80)              NOT NULL,
    giro           VARCHAR(40)              NOT NULL,
    estado         TEXT                     NOT NULL CONSTRAINT ck_alta_estado CHECK (estado IN ('REGISTRADO', 'RECHAZADO')),
    motivo         VARCHAR(40),
    evento_id      UUID                     NOT NULL,
    creada_en      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_alta_motivo CHECK ((estado = 'RECHAZADO') = (motivo IS NOT NULL))
);
CREATE INDEX ix_alta_cuenta ON negocios.alta_negocio (solicitado_por, creada_en DESC);

CREATE TABLE negocios.sucursal
(
    id                UUID PRIMARY KEY,
    negocio_id        UUID                     NOT NULL REFERENCES negocios.negocio (id),
    nombre            VARCHAR(60)              NOT NULL DEFAULT 'Principal',
    es_principal      BOOLEAN                  NOT NULL,
    activa            BOOLEAN                  NOT NULL DEFAULT TRUE,
    calle             VARCHAR(80),
    numero            VARCHAR(20),
    colonia           VARCHAR(80),
    municipio         VARCHAR(80),
    entidad_federativa VARCHAR(40),
    codigo_postal     CHAR(5) CONSTRAINT ck_sucursal_cp CHECK (codigo_postal ~ '^[0-9]{5}$'),
    referencias       VARCHAR(200),
    latitud           NUMERIC(9, 6) CONSTRAINT ck_sucursal_latitud CHECK (latitud BETWEEN -90 AND 90),
    longitud          NUMERIC(9, 6) CONSTRAINT ck_sucursal_longitud CHECK (longitud BETWEEN -180 AND 180),
    telefono          VARCHAR(16) CONSTRAINT ck_sucursal_telefono CHECK (telefono ~ '^\+52[0-9]{10}$'),
    creada_en         TIMESTAMP WITH TIME ZONE NOT NULL,
    actualizada_en    TIMESTAMP WITH TIME ZONE NOT NULL,
    version           BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT uq_sucursal_negocio_id UNIQUE (negocio_id, id),
    CONSTRAINT ck_sucursal_coordenadas CHECK ((latitud IS NULL) = (longitud IS NULL))
);
-- SU-01: entre las activas hay exactamente una principal (a lo mucho una aquí; el alta la crea).
CREATE UNIQUE INDEX ux_sucursal_principal ON negocios.sucursal (negocio_id) WHERE es_principal AND activa;

CREATE TABLE negocios.horario_tramo
(
    id          UUID PRIMARY KEY,
    negocio_id  UUID     NOT NULL,
    sucursal_id UUID     NOT NULL,
    dia         SMALLINT NOT NULL CONSTRAINT ck_tramo_dia CHECK (dia BETWEEN 1 AND 7),
    abre        TIME     NOT NULL,
    cierra      TIME     NOT NULL,
    CONSTRAINT ck_tramo_distintos CHECK (abre <> cierra),
    CONSTRAINT fk_tramo_sucursal FOREIGN KEY (negocio_id, sucursal_id) REFERENCES negocios.sucursal (negocio_id, id)
);
CREATE INDEX ix_tramo_sucursal ON negocios.horario_tramo (sucursal_id, dia, abre);

CREATE TABLE negocios.horario_excepcion
(
    id          UUID PRIMARY KEY,
    negocio_id  UUID    NOT NULL,
    sucursal_id UUID    NOT NULL,
    fecha       DATE    NOT NULL,
    cerrado     BOOLEAN NOT NULL,
    abre        TIME,
    cierra      TIME,
    motivo      VARCHAR(80),
    CONSTRAINT uq_excepcion_fecha UNIQUE (sucursal_id, fecha),
    CONSTRAINT ck_excepcion_horas CHECK ((cerrado AND abre IS NULL AND cierra IS NULL)
        OR (NOT cerrado AND abre IS NOT NULL AND cierra IS NOT NULL AND abre <> cierra)),
    CONSTRAINT fk_excepcion_sucursal FOREIGN KEY (negocio_id, sucursal_id) REFERENCES negocios.sucursal (negocio_id, id)
);

-- Quién cambió qué. Solo inserción. Guarda los NOMBRES de los campos cambiados, nunca sus valores (SN-05).
CREATE TABLE negocios.historial_negocio
(
    id          UUID PRIMARY KEY,
    negocio_id  UUID                     NOT NULL,
    accion      VARCHAR(40)              NOT NULL,
    campos      TEXT[]                   NOT NULL DEFAULT '{}',
    actor       TEXT                     NOT NULL,
    ocurrido_en TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_historial_negocio ON negocios.historial_negocio (negocio_id, ocurrido_en DESC);
CREATE TRIGGER trg_historial_solo_inserciones BEFORE UPDATE OR DELETE ON negocios.historial_negocio
    FOR EACH ROW EXECUTE FUNCTION plataforma.rechazar_modificacion();

-- Eventos que un listener ya procesó (EV-02). Soporte global, sin datos de nadie.
CREATE TABLE negocios.evento_procesado
(
    evento_id    UUID                     NOT NULL,
    listener     VARCHAR(60)              NOT NULL,
    procesado_en TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (evento_id, listener)
);

-- ---------------------------------------------------------------------------------------------------------------
-- Seguridad por fila
-- ---------------------------------------------------------------------------------------------------------------
-- Con FORCE: tablas que no alimentan la vista pública.
DO $$
DECLARE
    tabla TEXT;
BEGIN
    FOREACH tabla IN ARRAY ARRAY['historial_negocio']
    LOOP
        EXECUTE format('ALTER TABLE negocios.%I ENABLE ROW LEVEL SECURITY', tabla);
        EXECUTE format('ALTER TABLE negocios.%I FORCE ROW LEVEL SECURITY', tabla);
        EXECUTE format('CREATE POLICY aislamiento_por_negocio ON negocios.%I USING (negocio_id = plataforma.negocio_actual())', tabla);
    END LOOP;
END
$$;

ALTER TABLE negocios.alta_negocio ENABLE ROW LEVEL SECURITY;
ALTER TABLE negocios.alta_negocio FORCE ROW LEVEL SECURITY;
CREATE POLICY aislamiento_por_cuenta ON negocios.alta_negocio USING (solicitado_por = plataforma.cuenta_actual());

-- Sin FORCE: alimentan v_negocio_publico, que corre como su dueño.
DO $$
DECLARE
    tabla TEXT;
BEGIN
    FOREACH tabla IN ARRAY ARRAY['negocio_subcategoria', 'negocio_atributo', 'disponibilidad', 'sucursal',
                                  'horario_tramo', 'horario_excepcion']
    LOOP
        EXECUTE format('ALTER TABLE negocios.%I ENABLE ROW LEVEL SECURITY', tabla);
        EXECUTE format('CREATE POLICY aislamiento_por_negocio ON negocios.%I USING (negocio_id = plataforma.negocio_actual())', tabla);
    END LOOP;
END
$$;
ALTER TABLE negocios.negocio ENABLE ROW LEVEL SECURITY;
CREATE POLICY aislamiento_por_negocio ON negocios.negocio USING (id = plataforma.negocio_actual());

-- ---------------------------------------------------------------------------------------------------------------
-- Vista pública (PU-03, SN-14): solo columnas permitidas y solo negocios publicados y activos. Todo lo que se pueda
-- calcular se calcula en el servidor (abierto ahora, vigencia de la disponibilidad, distancia).
-- ---------------------------------------------------------------------------------------------------------------
CREATE VIEW negocios.v_negocio_publico AS
SELECT n.id,
       n.nombre,
       n.nombre_busqueda,
       n.giro,
       g.nombre                                                                       AS giro_nombre,
       n.descripcion,
       CASE WHEN n.contacto_publico THEN n.telefono_contacto END                      AS telefono,
       CASE WHEN n.contacto_publico THEN n.correo_contacto END                        AS correo,
       n.zona_horaria,
       n.es_simulado                                                                  AS simulado,
       n.duracion_visita_min,
       n.costo_medio_mxn,
       s.calle, s.numero, s.colonia, s.municipio, s.entidad_federativa, s.codigo_postal, s.referencias,
       s.latitud, s.longitud,
       coalesce((SELECT jsonb_agg(jsonb_build_object('codigo', sc.codigo, 'nombre', sc.nombre, 'giro', sc.giro)
                                  ORDER BY sc.orden)
                 FROM negocios.negocio_subcategoria ns
                          JOIN negocios.subcategoria sc ON sc.codigo = ns.subcategoria_codigo
                 WHERE ns.negocio_id = n.id), '[]'::jsonb)                           AS subcategorias,
       coalesce((SELECT jsonb_agg(jsonb_build_object('codigo', ca.codigo, 'nombre', ca.nombre) ORDER BY ca.orden)
                 FROM negocios.negocio_atributo na
                          JOIN negocios.catalogo_atributo ca ON ca.tipo = na.tipo AND ca.codigo = na.codigo
                 WHERE na.negocio_id = n.id AND na.tipo = 'DIETA'), '[]'::jsonb)     AS dieta,
       coalesce((SELECT jsonb_agg(jsonb_build_object('codigo', ca.codigo, 'nombre', ca.nombre) ORDER BY ca.orden)
                 FROM negocios.negocio_atributo na
                          JOIN negocios.catalogo_atributo ca ON ca.tipo = na.tipo AND ca.codigo = na.codigo
                 WHERE na.negocio_id = n.id AND na.tipo = 'ACCESIBILIDAD'), '[]'::jsonb) AS accesibilidad,
       coalesce((SELECT jsonb_agg(jsonb_build_object('dia', t.dia, 'abre', to_char(t.abre, 'HH24:MI'),
                                                     'cierra', to_char(t.cierra, 'HH24:MI')) ORDER BY t.dia, t.abre)
                 FROM negocios.horario_tramo t WHERE t.sucursal_id = s.id), '[]'::jsonb) AS horario,
       coalesce((SELECT jsonb_agg(jsonb_build_object('fecha', e.fecha, 'cerrado', e.cerrado,
                                                     'abre', to_char(e.abre, 'HH24:MI'),
                                                     'cierra', to_char(e.cierra, 'HH24:MI'), 'motivo', e.motivo)
                                  ORDER BY e.fecha)
                 FROM negocios.horario_excepcion e
                 WHERE e.sucursal_id = s.id AND e.fecha >= current_date - 1), '[]'::jsonb) AS excepciones,
       d.nivel                                                                        AS disponibilidad_nivel,
       d.nota                                                                         AS disponibilidad_nota,
       d.declarada_en                                                                 AS disponibilidad_declarada_en,
       d.vence_en                                                                     AS disponibilidad_vence_en
FROM negocios.negocio n
         JOIN negocios.giro g ON g.codigo = n.giro
         JOIN negocios.sucursal s ON s.negocio_id = n.id AND s.es_principal AND s.activa
         LEFT JOIN negocios.disponibilidad d ON d.negocio_id = n.id
WHERE n.estado = 'ACTIVO'
  AND n.publicado;

-- ---------------------------------------------------------------------------------------------------------------
-- Permisos mínimos (SN-14)
-- ---------------------------------------------------------------------------------------------------------------
GRANT SELECT ON negocios.giro, negocios.subcategoria, negocios.catalogo_atributo TO rol_negocios;
GRANT SELECT, INSERT, UPDATE ON negocios.negocio, negocios.sucursal TO rol_negocios;
GRANT SELECT, INSERT, DELETE ON negocios.negocio_subcategoria, negocios.negocio_atributo,
    negocios.horario_tramo TO rol_negocios;
GRANT SELECT, INSERT, UPDATE, DELETE ON negocios.horario_excepcion TO rol_negocios;
GRANT SELECT, INSERT, UPDATE ON negocios.disponibilidad TO rol_negocios;
GRANT SELECT, INSERT ON negocios.alta_negocio, negocios.historial_negocio TO rol_negocios;
GRANT SELECT, INSERT, DELETE ON negocios.evento_procesado TO rol_negocios;
-- La ruta pública no tiene permiso sobre ninguna tabla base: solo sobre la vista.
GRANT SELECT ON negocios.v_negocio_publico TO rol_negocios_publico;
