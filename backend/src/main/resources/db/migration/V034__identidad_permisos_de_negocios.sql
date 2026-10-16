-- V034 · Permisos de Negocios en el catálogo de Identidad y en los roles base (D-96, cambio 5).
--
-- Los catorce permisos negocios.* que declara PermisosDeNegocios (AZ-02) y la matriz de la sección 7.7 de
-- docs/diseno/negocios.md: todos los roles ven el perfil del negocio; el administrador opera el negocio, su personal y sus
-- reseñas; el gerente edita el horario y la disponibilidad y atiende reseñas; solo el dueño pausa y cierra el negocio.
-- Los mismos valores que RolesBase: EsquemaEquipoPostgresTest comprueba que coinciden.

INSERT INTO identidad.permiso (codigo, modulo, descripcion, sensible) VALUES
    ('negocios.perfil.ver', 'negocios', 'Ver el perfil, las categorías, la sucursal y el horario', false),
    ('negocios.perfil.editar', 'negocios', 'Editar nombre, descripción, contacto, logo, subcategorías y atributos', false),
    ('negocios.sucursal.gestionar', 'negocios', 'Editar la ubicación', false),
    ('negocios.horario.editar', 'negocios', 'Editar el horario y sus excepciones', false),
    ('negocios.negocio.publicar', 'negocios', 'Aparecer u ocultarse en la app del turista', false),
    ('negocios.disponibilidad.editar', 'negocios', 'Declarar la disponibilidad del día', false),
    ('negocios.negocio.pausar', 'negocios', 'Pausar y reanudar el negocio', false),
    ('negocios.negocio.cerrar', 'negocios', 'Cerrar el negocio', true),
    ('negocios.empleado.ver', 'negocios', 'Ver la lista y la ficha de empleados', false),
    ('negocios.empleado.registrar', 'negocios', 'Registrar empleados', false),
    ('negocios.empleado.editar', 'negocios', 'Editar fichas y ver el contacto', false),
    ('negocios.empleado.baja', 'negocios', 'Suspender, reactivar y dar de baja a un empleado', true),
    ('negocios.resena.ver', 'negocios', 'Ver las reseñas del negocio', false),
    ('negocios.resena.responder', 'negocios', 'Responder reseñas', false);

INSERT INTO identidad.rol_permiso (rol_codigo, permiso_codigo) VALUES
    ('DUENO', 'negocios.perfil.ver'),
    ('DUENO', 'negocios.perfil.editar'),
    ('DUENO', 'negocios.sucursal.gestionar'),
    ('DUENO', 'negocios.horario.editar'),
    ('DUENO', 'negocios.negocio.publicar'),
    ('DUENO', 'negocios.disponibilidad.editar'),
    ('DUENO', 'negocios.empleado.ver'),
    ('DUENO', 'negocios.empleado.registrar'),
    ('DUENO', 'negocios.empleado.editar'),
    ('DUENO', 'negocios.empleado.baja'),
    ('DUENO', 'negocios.resena.ver'),
    ('DUENO', 'negocios.resena.responder'),
    ('DUENO', 'negocios.negocio.pausar'),
    ('DUENO', 'negocios.negocio.cerrar'),
    ('ADMINISTRADOR', 'negocios.perfil.ver'),
    ('ADMINISTRADOR', 'negocios.perfil.editar'),
    ('ADMINISTRADOR', 'negocios.sucursal.gestionar'),
    ('ADMINISTRADOR', 'negocios.horario.editar'),
    ('ADMINISTRADOR', 'negocios.negocio.publicar'),
    ('ADMINISTRADOR', 'negocios.disponibilidad.editar'),
    ('ADMINISTRADOR', 'negocios.empleado.ver'),
    ('ADMINISTRADOR', 'negocios.empleado.registrar'),
    ('ADMINISTRADOR', 'negocios.empleado.editar'),
    ('ADMINISTRADOR', 'negocios.empleado.baja'),
    ('ADMINISTRADOR', 'negocios.resena.ver'),
    ('ADMINISTRADOR', 'negocios.resena.responder'),
    ('GERENTE', 'negocios.perfil.ver'),
    ('GERENTE', 'negocios.horario.editar'),
    ('GERENTE', 'negocios.disponibilidad.editar'),
    ('GERENTE', 'negocios.empleado.ver'),
    ('GERENTE', 'negocios.resena.ver'),
    ('GERENTE', 'negocios.resena.responder'),
    ('CAJERO', 'negocios.perfil.ver'),
    ('MESERO', 'negocios.perfil.ver'),
    ('COCINERO', 'negocios.perfil.ver'),
    ('TAQUILLERO', 'negocios.perfil.ver'),
    ('GUIA', 'negocios.perfil.ver');
