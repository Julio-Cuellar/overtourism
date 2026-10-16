package mx.negocio360.identidad.infraestructura;

import java.util.List;
import java.util.Optional;
import mx.negocio360.compartido.seguridad.PermisoDeclarado;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioRoles;
import mx.negocio360.identidad.dominio.Giro;
import mx.negocio360.identidad.dominio.Rol;
import mx.negocio360.identidad.dominio.RolesBase;

/**
 * Roles base y plantillas de {@link RolesBase} con el catalogo de permisos que declararon los modulos. Al crearse
 * comprueba que ningun rol cite un permiso sin declarar (AZ-03): si falta alguno, la aplicacion no arranca. Son datos
 * constantes, asi que sirve igual en desarrollo que en produccion mientras no haya roles personalizados (P2).
 */
public final class RepositorioRolesEnMemoria implements RepositorioRoles {

    private final List<PermisoDeclarado> catalogo;

    private RepositorioRolesEnMemoria(List<PermisoDeclarado> catalogo) {
        this.catalogo = List.copyOf(catalogo);
    }

    /** @throws IllegalStateException si un rol base cita un permiso que el catalogo no declara */
    public static RepositorioRolesEnMemoria de(List<PermisoDeclarado> catalogo) {
        RolesBase.verificar(catalogo);
        return new RepositorioRolesEnMemoria(catalogo);
    }

    @Override
    public Optional<Rol> porCodigo(String codigo) {
        return RolesBase.porCodigo(codigo);
    }

    @Override
    public List<Rol> plantilla(Giro giro) {
        return RolesBase.plantilla(giro).stream().map(c -> RolesBase.porCodigo(c).orElseThrow()).toList();
    }

    @Override
    public List<PermisoDeclarado> permisos() {
        return catalogo;
    }
}
