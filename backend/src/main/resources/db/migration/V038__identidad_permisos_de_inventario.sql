-- V038 · Permisos de Inventario en el catálogo de Identidad y en los roles base (IDN-4.4).
--
-- Los cuatro permisos que declara PermisosDeInventario (AZ-02): el dueño, el administrador y el gerente manejan todo el
-- inventario; el cocinero ve existencias y registra salidas y mermas; los demás roles no lo tocan. Ninguno es sensible
-- todavía (SG-19). Los mismos valores que RolesBase: EsquemaEquipoPostgresTest comprueba que coinciden.

INSERT INTO identidad.permiso (codigo, modulo, descripcion, sensible) VALUES
    ('inventario.articulo.ver', 'inventario', 'Ver los articulos, sus saldos, costos y kardex', false),
    ('inventario.articulo.gestionar', 'inventario', 'Crear, editar y dar de baja articulos y sus unidades', false),
    ('inventario.movimiento.registrar', 'inventario', 'Registrar entradas, salidas y mermas', false),
    ('inventario.conteo.ajustar', 'inventario', 'Ajustar el saldo por un conteo fisico', false);

INSERT INTO identidad.rol_permiso (rol_codigo, permiso_codigo) VALUES
    ('DUENO', 'inventario.articulo.ver'),
    ('DUENO', 'inventario.articulo.gestionar'),
    ('DUENO', 'inventario.movimiento.registrar'),
    ('DUENO', 'inventario.conteo.ajustar'),
    ('ADMINISTRADOR', 'inventario.articulo.ver'),
    ('ADMINISTRADOR', 'inventario.articulo.gestionar'),
    ('ADMINISTRADOR', 'inventario.movimiento.registrar'),
    ('ADMINISTRADOR', 'inventario.conteo.ajustar'),
    ('GERENTE', 'inventario.articulo.ver'),
    ('GERENTE', 'inventario.articulo.gestionar'),
    ('GERENTE', 'inventario.movimiento.registrar'),
    ('GERENTE', 'inventario.conteo.ajustar'),
    ('COCINERO', 'inventario.articulo.ver'),
    ('COCINERO', 'inventario.movimiento.registrar');
