-- V035 · Permisos de Ventas y Caja en el catálogo de Identidad y en los roles base (D-127).
--
-- Los once permisos que declaran PermisosDeVentas y PermisosDeCaja (AZ-02) y la matriz de la sección 8.2 de
-- docs/planes/integracion-ventas-caja.md: cobrar y operar la caja lo hacen cajero, taquillero, gerente, administrador y
-- dueño; anular, retirar, autorizar diferencias y auditar cortes son del gerente para arriba; crear cajas es del
-- administrador y el dueño. Mesero, cocinero y guía no cobran. Ninguno es sensible todavía (SG-19).
-- Los mismos valores que RolesBase: EsquemaEquipoPostgresTest comprueba que coinciden.

INSERT INTO identidad.permiso (codigo, modulo, descripcion, sensible) VALUES
    ('ventas.venta.ver', 'ventas', 'Ver las ventas y el resumen de un turno', false),
    ('ventas.venta.registrar', 'ventas', 'Cobrar una venta y buscar productos para vender', false),
    ('ventas.venta.anular', 'ventas', 'Anular una venta cobrada', false),
    ('caja.caja.gestionar', 'caja', 'Crear cajas', false),
    ('caja.turno.ver', 'caja', 'Ver las cajas, los turnos y su saldo', false),
    ('caja.turno.abrir', 'caja', 'Abrir un turno con su fondo', false),
    ('caja.movimiento.registrar', 'caja', 'Registrar una entrada de efectivo', false),
    ('caja.retiro.registrar', 'caja', 'Registrar un retiro de efectivo', false),
    ('caja.corte.cerrar', 'caja', 'Preparar, descartar y confirmar el corte de caja', false),
    ('caja.corte.autorizar', 'caja', 'Autorizar una diferencia de caja sobre el umbral', false),
    ('caja.corte.ver', 'caja', 'Ver los cortes pasados', false);

INSERT INTO identidad.rol_permiso (rol_codigo, permiso_codigo) VALUES
    ('DUENO', 'ventas.venta.ver'),
    ('DUENO', 'ventas.venta.registrar'),
    ('DUENO', 'caja.turno.ver'),
    ('DUENO', 'caja.turno.abrir'),
    ('DUENO', 'caja.movimiento.registrar'),
    ('DUENO', 'caja.corte.cerrar'),
    ('DUENO', 'ventas.venta.anular'),
    ('DUENO', 'caja.retiro.registrar'),
    ('DUENO', 'caja.corte.autorizar'),
    ('DUENO', 'caja.corte.ver'),
    ('DUENO', 'caja.caja.gestionar'),
    ('ADMINISTRADOR', 'ventas.venta.ver'),
    ('ADMINISTRADOR', 'ventas.venta.registrar'),
    ('ADMINISTRADOR', 'caja.turno.ver'),
    ('ADMINISTRADOR', 'caja.turno.abrir'),
    ('ADMINISTRADOR', 'caja.movimiento.registrar'),
    ('ADMINISTRADOR', 'caja.corte.cerrar'),
    ('ADMINISTRADOR', 'ventas.venta.anular'),
    ('ADMINISTRADOR', 'caja.retiro.registrar'),
    ('ADMINISTRADOR', 'caja.corte.autorizar'),
    ('ADMINISTRADOR', 'caja.corte.ver'),
    ('ADMINISTRADOR', 'caja.caja.gestionar'),
    ('GERENTE', 'ventas.venta.ver'),
    ('GERENTE', 'ventas.venta.registrar'),
    ('GERENTE', 'caja.turno.ver'),
    ('GERENTE', 'caja.turno.abrir'),
    ('GERENTE', 'caja.movimiento.registrar'),
    ('GERENTE', 'caja.corte.cerrar'),
    ('GERENTE', 'ventas.venta.anular'),
    ('GERENTE', 'caja.retiro.registrar'),
    ('GERENTE', 'caja.corte.autorizar'),
    ('GERENTE', 'caja.corte.ver'),
    ('CAJERO', 'ventas.venta.ver'),
    ('CAJERO', 'ventas.venta.registrar'),
    ('CAJERO', 'caja.turno.ver'),
    ('CAJERO', 'caja.turno.abrir'),
    ('CAJERO', 'caja.movimiento.registrar'),
    ('CAJERO', 'caja.corte.cerrar'),
    ('TAQUILLERO', 'ventas.venta.ver'),
    ('TAQUILLERO', 'ventas.venta.registrar'),
    ('TAQUILLERO', 'caja.turno.ver'),
    ('TAQUILLERO', 'caja.turno.abrir'),
    ('TAQUILLERO', 'caja.movimiento.registrar'),
    ('TAQUILLERO', 'caja.corte.cerrar');
