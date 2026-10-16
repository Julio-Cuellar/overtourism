-- V032 · Seguridad por fila forzada en todas las tablas de Negocios con negocio_id (como en Clientes), tambien en las
-- que alimentan la vista publica.
--
-- V030 las dejo con RLS activada pero no forzada para que la vista, que corre con los privilegios de su dueño, pudiera
-- leerlas. Con FORCE el dueño tambien queda sujeto a las politicas, asi que se le da una politica de solo lectura para
-- que la vista siga funcionando; la aplicacion entra con otro rol y sigue aislada por negocio. El dueño es quien corre las
-- migraciones (current_user aqui) y quien creo la vista.
DO $$
DECLARE
    tabla TEXT;
BEGIN
    FOREACH tabla IN ARRAY ARRAY['negocio', 'negocio_subcategoria', 'negocio_atributo', 'disponibilidad', 'sucursal',
                                  'horario_tramo', 'horario_excepcion']
    LOOP
        EXECUTE format('ALTER TABLE negocios.%I FORCE ROW LEVEL SECURITY', tabla);
        EXECUTE format('CREATE POLICY lectura_de_la_vista_publica ON negocios.%I FOR SELECT TO %I USING (true)',
                       tabla, current_user);
    END LOOP;
END
$$;
