-- V042 · Seguridad por fila FORZADA en todas las tablas de Ventas y Caja con negocio_id.
--
-- V040 y V041 activan la seguridad por fila y crean la política de aislamiento. Con FORCE le aplica también al dueño de
-- las tablas (quien corre las migraciones); un superusuario siempre se la salta, por eso la aplicación entra con un rol
-- que no lo es. Se separa en su propia migración para que forzarla sea un cambio explícito y revisable.
DO $$
DECLARE
    tabla TEXT;
BEGIN
    FOREACH tabla IN ARRAY ARRAY['serie_folio', 'venta', 'linea_venta', 'pago_venta', 'preparacion']
    LOOP
        EXECUTE format('ALTER TABLE ventas.%I FORCE ROW LEVEL SECURITY', tabla);
    END LOOP;
    FOREACH tabla IN ARRAY ARRAY['caja', 'turno', 'movimiento_caja', 'corte', 'venta_aplicada', 'preparacion']
    LOOP
        EXECUTE format('ALTER TABLE caja.%I FORCE ROW LEVEL SECURITY', tabla);
    END LOOP;
END
$$;
