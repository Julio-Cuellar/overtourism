package mx.negocio360.identidad.aplicacion.puertos;

import java.util.List;
import java.util.Optional;
import mx.negocio360.compartido.seguridad.PermisoDeclarado;
import mx.negocio360.identidad.dominio.Giro;
import mx.negocio360.identidad.dominio.Rol;

/** Roles base, plantillas por giro y catalogo de permisos (AM-09): datos de solo lectura para la aplicacion. */
public interface RepositorioRoles {

    Optional<Rol> porCodigo(String codigo);

    /** Los roles que ofrece ese giro, en el orden de su plantilla. */
    List<Rol> plantilla(Giro giro);

    /** Los permisos que declaran los modulos. */
    List<PermisoDeclarado> permisos();
}
