-- V033 · Negocios N6: reseñas (en modo demostración, sin cuenta ni sello), su resumen, la vista publica de reseñas, la
-- calificacion en la vista de negocios y el soporte de la demo.
--
-- El autor de una reseña en P1 es un id de dispositivo (RS-11): un dato no confiable que no identifica a nadie. La tabla
-- de preparaciones de reseña guarda un comentario a medias (puede traer datos personales, SN-13): solo el hash del token,
-- y el comentario se vacia al confirmar. Todas las tablas con negocio_id llevan RLS forzada como las demas.

CREATE TABLE negocios.resena
(
    id              UUID PRIMARY KEY,
    negocio_id      UUID                     NOT NULL REFERENCES negocios.negocio (id),
    visitante_id    UUID                     NOT NULL,
    calificacion    SMALLINT                 NOT NULL CONSTRAINT ck_resena_calificacion CHECK (calificacion BETWEEN 1 AND 5),
    comentario      VARCHAR(1000),
    estado          TEXT                     NOT NULL DEFAULT 'PUBLICADA'
        CONSTRAINT ck_resena_estado CHECK (estado IN ('PUBLICADA', 'OCULTA')),
    origen          TEXT                     NOT NULL CONSTRAINT ck_resena_origen CHECK (origen IN ('DEMOSTRACION', 'VISITANTE')),
    respuesta       VARCHAR(500),
    respondida_por  UUID,
    respondida_en   TIMESTAMP WITH TIME ZONE,
    creada_en       TIMESTAMP WITH TIME ZONE NOT NULL,
    editada_en      TIMESTAMP WITH TIME ZONE,
    version         BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT uq_resena_autor UNIQUE (negocio_id, visitante_id),
    CONSTRAINT ck_resena_respuesta CHECK ((respuesta IS NULL) = (respondida_en IS NULL))
);
CREATE INDEX ix_resena_negocio ON negocios.resena (negocio_id, creada_en DESC, id);

CREATE TABLE negocios.resumen_resenas
(
    negocio_id     UUID PRIMARY KEY REFERENCES negocios.negocio (id),
    total          INTEGER                  NOT NULL CHECK (total >= 0),
    suma           INTEGER                  NOT NULL CHECK (suma >= 0),
    c1             INTEGER                  NOT NULL CHECK (c1 >= 0),
    c2             INTEGER                  NOT NULL CHECK (c2 >= 0),
    c3             INTEGER                  NOT NULL CHECK (c3 >= 0),
    c4             INTEGER                  NOT NULL CHECK (c4 >= 0),
    c5             INTEGER                  NOT NULL CHECK (c5 >= 0),
    actualizado_en TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_resumen_suma CHECK (total = c1 + c2 + c3 + c4 + c5 AND suma = c1 + 2 * c2 + 3 * c3 + 4 * c4 + 5 * c5)
);

-- Una reseña preparada y aun no confirmada (RS-09, D-23). Sin RLS por negocio: se busca por el hash del token, que es de
-- 256 bits. El token solo lo confirma el mismo dispositivo que lo pidio.
CREATE TABLE negocios.preparacion_resena
(
    token_hash   CHAR(64) PRIMARY KEY,
    negocio_id   UUID                     NOT NULL,
    visitante_id UUID                     NOT NULL,
    calificacion SMALLINT                 NOT NULL CHECK (calificacion BETWEEN 1 AND 5),
    comentario   VARCHAR(1000),
    creada_en    TIMESTAMP WITH TIME ZONE NOT NULL,
    expira_en    TIMESTAMP WITH TIME ZONE NOT NULL,
    usada_en     TIMESTAMP WITH TIME ZONE,
    -- SN-09: una preparacion usada ya no conserva el comentario.
    CONSTRAINT ck_preparacion_resena_usada CHECK (usada_en IS NULL OR comentario IS NULL)
);
CREATE INDEX ix_preparacion_resena_dispositivo ON negocios.preparacion_resena (visitante_id, creada_en DESC);

-- Los negocios de la demo, para poder restablecerlos sin leer otros negocios (DM-04). Solo ids.
CREATE TABLE negocios.demostracion_negocio
(
    negocio_id UUID PRIMARY KEY REFERENCES negocios.negocio (id)
);

DO $$
DECLARE
    tabla TEXT;
BEGIN
    FOREACH tabla IN ARRAY ARRAY['resena', 'resumen_resenas']
    LOOP
        EXECUTE format('ALTER TABLE negocios.%I ENABLE ROW LEVEL SECURITY', tabla);
        EXECUTE format('ALTER TABLE negocios.%I FORCE ROW LEVEL SECURITY', tabla);
        EXECUTE format('CREATE POLICY aislamiento_por_negocio ON negocios.%I USING (negocio_id = plataforma.negocio_actual())', tabla);
        EXECUTE format('CREATE POLICY lectura_de_la_vista_publica ON negocios.%I FOR SELECT TO %I USING (true)', tabla, current_user);
    END LOOP;
END
$$;

-- Vista publica de reseñas (RS-08, PU-03): sin el autor, ni el dispositivo, ni quien respondio. Solo reseñas publicadas de
-- negocios publicos y activos.
CREATE VIEW negocios.v_resena_publica AS
SELECT r.id,
       r.negocio_id,
       r.calificacion,
       r.comentario,
       r.creada_en,
       r.respuesta,
       r.respondida_en,
       (r.origen = 'DEMOSTRACION') AS demostracion
FROM negocios.resena r
         JOIN negocios.negocio n ON n.id = r.negocio_id AND n.estado = 'ACTIVO' AND n.publicado
WHERE r.estado = 'PUBLICADA';

-- La calificacion de cada negocio entra a la vista de negocios, al final de la lista de columnas.
CREATE OR REPLACE VIEW negocios.v_negocio_publico AS
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
       d.vence_en                                                                     AS disponibilidad_vence_en,
       coalesce(rr.total, 0)                                                          AS calificacion_total,
       coalesce(rr.suma, 0)                                                           AS calificacion_suma,
       coalesce(rr.c1, 0)                                                             AS calificacion_c1,
       coalesce(rr.c2, 0)                                                             AS calificacion_c2,
       coalesce(rr.c3, 0)                                                             AS calificacion_c3,
       coalesce(rr.c4, 0)                                                             AS calificacion_c4,
       coalesce(rr.c5, 0)                                                             AS calificacion_c5
FROM negocios.negocio n
         JOIN negocios.giro g ON g.codigo = n.giro
         JOIN negocios.sucursal s ON s.negocio_id = n.id AND s.es_principal AND s.activa
         LEFT JOIN negocios.disponibilidad d ON d.negocio_id = n.id
         LEFT JOIN negocios.resumen_resenas rr ON rr.negocio_id = n.id
WHERE n.estado = 'ACTIVO'
  AND n.publicado;

GRANT SELECT, INSERT, UPDATE, DELETE ON negocios.resena, negocios.resumen_resenas TO rol_negocios;
GRANT SELECT, INSERT, UPDATE, DELETE ON negocios.preparacion_resena TO rol_negocios;
GRANT SELECT, INSERT, DELETE ON negocios.demostracion_negocio TO rol_negocios;
GRANT DELETE ON negocios.disponibilidad TO rol_negocios;
GRANT SELECT ON negocios.v_resena_publica TO rol_negocios_publico;
