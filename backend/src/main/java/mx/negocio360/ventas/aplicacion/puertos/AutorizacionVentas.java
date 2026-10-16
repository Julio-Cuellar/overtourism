package mx.negocio360.ventas.aplicacion.puertos;

import mx.negocio360.compartido.contexto.ContextoNegocio;

/** Puerto hacia los permisos de Identidad. Quien puede anular esta abierto [?] en el diseno (VT-09). */
public interface AutorizacionVentas {

    /**
     * Dice si el actor del contexto puede anular ventas en su negocio.
     *
     * @param contexto negocio y actor
     * @return verdadero si tiene el permiso de anular
     */
    boolean puedeAnular(ContextoNegocio contexto);
}
