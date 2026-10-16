package mx.negocio360.ventas.seguridad;

import java.util.List;
import mx.negocio360.compartido.seguridad.CatalogoDePermisos;
import mx.negocio360.compartido.seguridad.PermisoDeclarado;
import org.springframework.stereotype.Component;

/**
 * Permisos que declara Ventas (AZ-02, D-70, D-127). Ninguno es sensible por ahora: si lo fuera quedaria negado por
 * completo hasta que exista la confirmacion reforzada (SG-19), asi que anular una venta se protege con el permiso, el
 * motivo obligatorio (VT-09) y la auditoria. Quien tiene cada permiso lo deciden los roles de Identidad, que son datos
 * (seccion 8.2 de {@code docs/planes/integracion-ventas-caja.md}).
 */
@Component
public final class PermisosDeVentas implements CatalogoDePermisos {

    /** Ver ventas y el resumen del turno. */
    public static final String VENTA_VER = "ventas.venta.ver";
    /** Cobrar, y buscar productos para vender. */
    public static final String VENTA_REGISTRAR = "ventas.venta.registrar";
    /** Anular una venta cobrada. */
    public static final String VENTA_ANULAR = "ventas.venta.anular";

    private static final List<PermisoDeclarado> PERMISOS = List.of(
            new PermisoDeclarado(VENTA_VER, "Ver las ventas y el resumen de un turno", false),
            new PermisoDeclarado(VENTA_REGISTRAR, "Cobrar una venta y buscar productos para vender", false),
            new PermisoDeclarado(VENTA_ANULAR, "Anular una venta cobrada", false));

    @Override
    public List<PermisoDeclarado> permisos() {
        return PERMISOS;
    }
}
