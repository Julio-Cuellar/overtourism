package mx.negocio360.inventario.aplicacion.puertos;

import mx.negocio360.compartido.eventos.SobreEvento;

/**
 * Puerto de publicacion de eventos. La implementacion real escribe en el outbox dentro de la transaccion del
 * cambio, para que el hecho y el evento se confirmen juntos.
 */
public interface PublicadorEventos {

    void publicar(SobreEvento evento);
}
