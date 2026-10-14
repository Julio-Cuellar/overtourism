#!/bin/sh
# Se ejecuta una sola vez, al crear la base por primera vez (docker-entrypoint-initdb.d).
# Crea el rol con el que entra la aplicación: puede iniciar sesión, pero no es superusuario ni puede saltarse la
# seguridad por fila. Los permisos sobre las tablas se los dan las migraciones de Flyway (V001 y siguientes), que
# corren después con el dueño de la base.
set -e

psql -v ON_ERROR_STOP=1 \
     -v app_user="$NEGOCIO360_APP_USER" \
     -v app_password="$NEGOCIO360_APP_PASSWORD" \
     --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<'EOSQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS',
              :'app_user', :'app_password') \gexec
EOSQL
