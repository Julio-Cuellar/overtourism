package mx.negocio360.clientes.aplicacion.puertos;

import mx.negocio360.compartido.eventos.SobreEvento;

/**
 * Puerto de publicacion de eventos. La implementacion real escribe en el outbox dentro de la transaccion del
 * cambio, para que el hecho y el evento se confirmen juntos (B-05, seccion 8.5).
 */
public interface PublicadorEventos {

    void publicar(SobreEvento evento);
}
