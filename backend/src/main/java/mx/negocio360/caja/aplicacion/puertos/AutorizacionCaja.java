package mx.negocio360.caja.aplicacion.puertos;

import mx.negocio360.compartido.contexto.ContextoNegocio;

/** Puerto hacia los permisos de Identidad. Quien autoriza diferencias esta abierto [?] (CJ-08). */
public interface AutorizacionCaja {

    /**
     * Dice si el actor puede autorizar diferencias de corte sobre el umbral.
     *
     * @param contexto negocio y actor
     * @return verdadero si es encargado o dueño
     */
    boolean esEncargado(ContextoNegocio contexto);
}
