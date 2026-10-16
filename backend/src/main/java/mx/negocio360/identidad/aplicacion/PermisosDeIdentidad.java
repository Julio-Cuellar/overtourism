package mx.negocio360.identidad.aplicacion;

import java.util.List;
import mx.negocio360.compartido.seguridad.CatalogoDePermisos;
import mx.negocio360.compartido.seguridad.PermisoDeclarado;

/**
 * Permisos que declara Identidad (AZ-02, D-70). Invitar, cambiar el rol y revocar son sensibles: ademas del permiso
 * piden una confirmacion reforzada (D-73, IDN-3.2).
 */
public final class PermisosDeIdentidad implements CatalogoDePermisos {

    private static final List<PermisoDeclarado> PERMISOS = List.of(
            new PermisoDeclarado("identidad.equipo.ver", "Ver al equipo del negocio", false),
            new PermisoDeclarado("identidad.miembro.invitar", "Invitar personal", true),
            new PermisoDeclarado("identidad.miembro.cambiar_rol", "Cambiar el rol de un miembro", true),
            new PermisoDeclarado("identidad.miembro.revocar", "Suspender o quitar a un miembro", true),
            new PermisoDeclarado("identidad.auditoria.ver", "Ver la auditoría de accesos", false));

    public static final String EQUIPO_VER = "identidad.equipo.ver";
    public static final String MIEMBRO_CAMBIAR_ROL = "identidad.miembro.cambiar_rol";
    public static final String MIEMBRO_REVOCAR = "identidad.miembro.revocar";

    @Override
    public List<PermisoDeclarado> permisos() {
        return PERMISOS;
    }
}
