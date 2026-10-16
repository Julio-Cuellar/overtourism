package mx.negocio360.compartido.seguridad;

import java.util.List;

/**
 * Cada modulo implementa esta interfaz para declarar sus permisos como datos (AZ-02, D-70). Identidad recibe todos los
 * catalogos al arrancar y no importa a ningun modulo (R1 y R4). Un rol base que cite un permiso que ningun modulo
 * declara impide el arranque (AZ-03).
 */
public interface CatalogoDePermisos {

    /** Los permisos de un solo modulo. */
    List<PermisoDeclarado> permisos();
}
