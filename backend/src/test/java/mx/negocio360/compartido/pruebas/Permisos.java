package mx.negocio360.compartido.pruebas;

import java.util.Set;
import mx.negocio360.compartido.seguridad.Autorizacion;

/** Solo pruebas: las autorizaciones que usan las pruebas de los modulos. */
public final class Permisos {

    /** La persona puede todo: para las pruebas que no son de permisos. */
    public static final Autorizacion TODOS = permiso -> true;
    /** La persona no puede nada. */
    public static final Autorizacion NINGUNO = permiso -> false;

    /** Lo que hace un cajero (o taquillero): cobra y opera su caja, sin anular, retirar ni autorizar. */
    public static final Autorizacion CAJERO = solo("ventas.venta.ver", "ventas.venta.registrar", "caja.turno.ver",
            "caja.turno.abrir", "caja.movimiento.registrar", "caja.corte.cerrar");

    private Permisos() {
    }

    /**
     * Permisos de prueba segun el actor del contexto: un actor que empieza con {@code encargado} (gerente para
     * arriba) puede todo; cualquier otro es un cajero. Asi las pruebas conservan quien es «encargado» sin armar
     * roles.
     */
    public static Autorizacion de(mx.negocio360.compartido.contexto.ContextoNegocio contexto) {
        return contexto.actor().startsWith("encargado") ? TODOS : CAJERO;
    }

    /** Autorizacion con exactamente esos permisos. */
    public static Autorizacion solo(String... permisos) {
        return Autorizacion.de(Set.of(permisos));
    }

    /** Autorizacion con todo menos esos permisos. */
    public static Autorizacion todosMenos(String... permisos) {
        var negados = Set.of(permisos);
        return permiso -> !negados.contains(permiso);
    }
}
