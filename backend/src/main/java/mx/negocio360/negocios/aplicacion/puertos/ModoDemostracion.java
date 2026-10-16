package mx.negocio360.negocios.aplicacion.puertos;

import mx.negocio360.negocios.dominio.CodigoError;
import mx.negocio360.negocios.dominio.NegociosException;

/**
 * El modo demostracion (DM-03, D-104): permite reseñar sin cuenta ni sello, sembrar los negocios simulados y restablecer
 * la demo. Esta apagado por omision y la aplicacion no arranca con el perfil de produccion si esta encendido.
 */
public interface ModoDemostracion {

    boolean activo();

    /** @throws NegociosException {@code MODO_DEMOSTRACION_APAGADO} si esta apagado */
    default void exigirActivo() {
        if (!activo()) {
            throw new NegociosException(CodigoError.MODO_DEMOSTRACION_APAGADO);
        }
    }
}
