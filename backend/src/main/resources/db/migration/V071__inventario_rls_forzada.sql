-- V071 · Seguridad por fila FORZADA en todas las tablas de Inventario con negocio_id.
--
-- V070 activa la seguridad por fila y crea la política de aislamiento. Con FORCE le aplica también al dueño de las
-- tablas (quien corre las migraciones); un superusuario siempre se la salta, por eso la aplicación entra con un rol que
-- no lo es.
DO $$
DECLARE
    tabla TEXT;
BEGIN
    FOREACH tabla IN ARRAY ARRAY['articulo', 'saldo', 'movimiento', 'preparacion']
    LOOP
        EXECUTE format('ALTER TABLE inventario.%I FORCE ROW LEVEL SECURITY', tabla);
    END LOOP;
END
$$;
