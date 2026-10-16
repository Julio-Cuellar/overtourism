package mx.negocio360.inventario.seguridad;

import java.util.List;
import mx.negocio360.compartido.seguridad.CatalogoDePermisos;
import mx.negocio360.compartido.seguridad.PermisoDeclarado;
import org.springframework.stereotype.Component;

/**
 * Permisos que declara Inventario (AZ-02, D-70, IDN-4.4). Ninguno es sensible por ahora (SG-19): un ajuste por conteo
 * queda protegido por el permiso, la confirmacion en dos pasos y la auditoria del movimiento. Quien tiene cada permiso lo
 * deciden los roles de Identidad, que son datos (migracion V038).
 */
@Component
public final class PermisosDeInventario implements CatalogoDePermisos {

    /** Ver articulos, saldos, costos y el kardex. */
    public static final String ARTICULO_VER = "inventario.articulo.ver";
    /** Crear, editar y dar de baja articulos, y definir sus unidades propias. */
    public static final String ARTICULO_GESTIONAR = "inventario.articulo.gestionar";
    /** Registrar entradas, salidas y mermas. */
    public static final String MOVIMIENTO_REGISTRAR = "inventario.movimiento.registrar";
    /** Ajustar el saldo por un conteo fisico. */
    public static final String CONTEO_AJUSTAR = "inventario.conteo.ajustar";

    private static final List<PermisoDeclarado> PERMISOS = List.of(
            new PermisoDeclarado(ARTICULO_VER, "Ver los articulos, sus saldos, costos y kardex", false),
            new PermisoDeclarado(ARTICULO_GESTIONAR, "Crear, editar y dar de baja articulos y sus unidades", false),
            new PermisoDeclarado(MOVIMIENTO_REGISTRAR, "Registrar entradas, salidas y mermas", false),
            new PermisoDeclarado(CONTEO_AJUSTAR, "Ajustar el saldo por un conteo fisico", false));

    @Override
    public List<PermisoDeclarado> permisos() {
        return PERMISOS;
    }
}
