package mx.negocio360.canal.demo;

import mx.negocio360.caja.aplicacion.puertos.AutorizacionCaja;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.ventas.aplicacion.puertos.AutorizacionVentas;

/**
 * Permisos <b>simulados</b> para la demo: los roles de Identidad aun no existen, asi que toda cuenta del negocio puede
 * anular ventas (VT-09) y autorizar diferencias del corte (CJ-08). Es una simplificacion de demo, no una regla: con los
 * permisos reales, solo el encargado podra hacerlo.
 */
public final class PermisosSimulados implements AutorizacionVentas, AutorizacionCaja {

    @Override
    public boolean puedeAnular(ContextoNegocio contexto) {
        return true;
    }

    @Override
    public boolean esEncargado(ContextoNegocio contexto) {
        return true;
    }
}
