package mx.negocio360.caja.aplicacion.puertos;

import mx.negocio360.compartido.eventos.SobreEvento;

/** Puerto de publicacion de eventos; la implementacion real escribe en el outbox de la misma transaccion. */
public interface PublicadorEventos {

    /**
     * Publica un evento de Caja.
     *
     * @param evento sobre completo
     */
    void publicar(SobreEvento evento);
}
