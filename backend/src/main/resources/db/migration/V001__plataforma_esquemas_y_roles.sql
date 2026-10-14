-- V001 · Esquemas por módulo, roles de grupo y piezas compartidas de la plataforma.
--
-- R1: cada módulo es dueño de sus datos y los guarda en su propio esquema. Flyway corre como el dueño de la base y
-- crea todo; la aplicación entra con un rol sin privilegios (negocio360_app, que se crea fuera de las migraciones:
-- lo hace docker/postgres/01-roles.sh o, en pruebas, Testcontainers) y recibe permisos solo por pertenecer a los
-- roles de grupo de abajo. Así Identidad y Clientes no ven las tablas del otro (SI-11).

CREATE SCHEMA IF NOT EXISTS plataforma;
CREATE SCHEMA IF NOT EXISTS identidad;
CREATE SCHEMA IF NOT EXISTS clientes;

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'rol_plataforma') THEN
        CREATE ROLE rol_plataforma NOLOGIN;
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'rol_identidad') THEN
        CREATE ROLE rol_identidad NOLOGIN;
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'rol_clientes') THEN
        CREATE ROLE rol_clientes NOLOGIN;
    END IF;
END
$$;

GRANT USAGE ON SCHEMA plataforma TO rol_plataforma, rol_identidad, rol_clientes;
GRANT USAGE ON SCHEMA identidad TO rol_identidad;
GRANT USAGE ON SCHEMA clientes TO rol_clientes;

-- Negocio de la transacción en curso. La aplicación lo fija con set_config('app.negocio_id', ..., true) al abrir
-- cada transacción, siempre a partir del contexto autenticado y nunca de un argumento. Sin valor devuelve NULL, y
-- una comparación con NULL no da verdadero: sin negocio no se ve ni se escribe nada (falla cerrado).
CREATE FUNCTION plataforma.negocio_actual() RETURNS uuid
    LANGUAGE sql STABLE PARALLEL SAFE
AS $$
    SELECT nullif(current_setting('app.negocio_id', true), '')::uuid
$$;

-- Función de los disparadores de las tablas que solo admiten inserciones (historial de consentimiento, bitácora).
CREATE FUNCTION plataforma.rechazar_modificacion() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'La tabla %.% solo admite inserciones', TG_TABLE_SCHEMA, TG_TABLE_NAME
        USING ERRCODE = 'restrict_violation';
END
$$;

-- El rol de la aplicación pertenece a los tres módulos por ahora (un solo pool de conexiones). Separarlos en
-- conexiones distintas es un endurecimiento posterior; los permisos ya están separados por rol de grupo.
-- ${app_user} es el nombre del rol (placeholder de Flyway, NEGOCIO360_APP_USER). Si el rol no existe se detiene la
-- migración: omitir los permisos en silencio dejaría una aplicación que conecta pero recibe «permiso denegado» en todo.
-- Los tiempos máximos acotan una consulta o una transacción colgada, que retendría bloqueos.
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '${app_user}') THEN
        RAISE EXCEPTION 'El rol de la aplicación "${app_user}" no existe: créalo antes de migrar (docker/postgres/01-roles.sh)';
    END IF;
    EXECUTE format('GRANT rol_plataforma, rol_identidad, rol_clientes TO %I', '${app_user}');
    EXECUTE format('ALTER ROLE %I SET search_path = plataforma, public', '${app_user}');
    EXECUTE format('ALTER ROLE %I SET statement_timeout = %L', '${app_user}', '30s');
    EXECUTE format('ALTER ROLE %I SET lock_timeout = %L', '${app_user}', '10s');
    EXECUTE format('ALTER ROLE %I SET idle_in_transaction_session_timeout = %L', '${app_user}', '60s');
END
$$;
