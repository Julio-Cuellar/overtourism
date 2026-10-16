package mx.negocio360.negocios.aplicacion;

import java.util.List;
import mx.negocio360.compartido.seguridad.CatalogoDePermisos;
import mx.negocio360.compartido.seguridad.PermisoDeclarado;
import org.springframework.stereotype.Component;

/**
 * Permisos que declara Negocios (AZ-02, D-70, seccion 7.7 del diseno). Cerrar el negocio y dar de baja a un empleado son
 * sensibles: ademas del permiso piden una confirmacion reforzada (D-73). Quien tiene cada permiso lo deciden los roles
 * de Identidad, que son datos: agregarlos a los roles base es un cambio de Identidad (seccion 8.5, cambio 5).
 */
@Component
public final class PermisosDeNegocios implements CatalogoDePermisos {

    public static final String PERFIL_VER = "negocios.perfil.ver";
    public static final String PERFIL_EDITAR = "negocios.perfil.editar";
    public static final String SUCURSAL_GESTIONAR = "negocios.sucursal.gestionar";
    public static final String HORARIO_EDITAR = "negocios.horario.editar";
    public static final String NEGOCIO_PUBLICAR = "negocios.negocio.publicar";
    public static final String DISPONIBILIDAD_EDITAR = "negocios.disponibilidad.editar";
    public static final String NEGOCIO_PAUSAR = "negocios.negocio.pausar";
    public static final String NEGOCIO_CERRAR = "negocios.negocio.cerrar";
    public static final String RESENA_VER = "negocios.resena.ver";
    public static final String RESENA_RESPONDER = "negocios.resena.responder";

    private static final List<PermisoDeclarado> PERMISOS = List.of(
            new PermisoDeclarado(PERFIL_VER, "Ver el perfil, las categorías, la sucursal y el horario", false),
            new PermisoDeclarado(PERFIL_EDITAR,
                    "Editar nombre, descripción, contacto, logo, subcategorías y atributos", false),
            new PermisoDeclarado(SUCURSAL_GESTIONAR, "Editar la ubicación", false),
            new PermisoDeclarado(HORARIO_EDITAR, "Editar el horario y sus excepciones", false),
            new PermisoDeclarado(NEGOCIO_PUBLICAR, "Aparecer u ocultarse en la app del turista", false),
            new PermisoDeclarado(DISPONIBILIDAD_EDITAR, "Declarar la disponibilidad del día", false),
            new PermisoDeclarado(NEGOCIO_PAUSAR, "Pausar y reanudar el negocio", false),
            new PermisoDeclarado(NEGOCIO_CERRAR, "Cerrar el negocio", true),
            new PermisoDeclarado("negocios.empleado.ver", "Ver la lista y la ficha de empleados", false),
            new PermisoDeclarado("negocios.empleado.registrar", "Registrar empleados", false),
            new PermisoDeclarado("negocios.empleado.editar", "Editar fichas y ver el contacto", false),
            new PermisoDeclarado("negocios.empleado.baja", "Suspender, reactivar y dar de baja a un empleado", true),
            new PermisoDeclarado(RESENA_VER, "Ver las reseñas del negocio", false),
            new PermisoDeclarado(RESENA_RESPONDER, "Responder reseñas", false));

    @Override
    public List<PermisoDeclarado> permisos() {
        return PERMISOS;
    }
}
