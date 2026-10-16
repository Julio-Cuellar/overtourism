package mx.negocio360.restaurantes.aplicacion;

import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.seguridad.PermisoDenegadoException;

/**
 * Puerto de permisos para lo que no recibe la sesion como argumento, como los manejadores de comandos y los listeners de
 * eventos (AZ-04). El adaptador consulta la sesion de la peticion; los permisos son los de {@code PermisosDeRestaurantes}.
 */
public interface AutorizacionDeRestaurantes {

    /** Verdadero si la persona de la peticion tiene ese permiso en el negocio del contexto. */
    boolean puede(ContextoNegocio contexto, String permiso);

    /** @throws PermisoDenegadoException si no lo tiene */
    default void exigir(ContextoNegocio contexto, String permiso) {
        if (!puede(contexto, permiso)) {
            throw new PermisoDenegadoException(permiso);
        }
    }
}
