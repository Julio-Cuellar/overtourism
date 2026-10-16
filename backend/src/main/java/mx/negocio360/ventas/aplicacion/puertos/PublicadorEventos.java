package mx.negocio360.ventas.aplicacion.puertos;

import mx.negocio360.compartido.eventos.SobreEvento;

/**
 * Puerto de publicacion de eventos. La implementacion real escribe en el outbox dentro de la transaccion de la venta,
 * para que el hecho y el evento se confirmen juntos.
 */
public interface PublicadorEventos {

    /**
     * Publica un evento.
     *
     * @param evento sobre completo
     */
    void publicar(SobreEvento evento);
}
