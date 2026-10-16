package mx.negocio360.restaurantes.aplicacion;

import mx.negocio360.compartido.eventos.SobreEvento;

/**
 * Puerto de publicacion de eventos. La implementacion real escribe en el outbox dentro de la transaccion del caso de uso
 * (D-103), para que el hecho y el evento se confirmen juntos.
 */
@FunctionalInterface
public interface PublicadorEventos {

    /** Publica un evento. */
    void publicar(SobreEvento evento);
}
