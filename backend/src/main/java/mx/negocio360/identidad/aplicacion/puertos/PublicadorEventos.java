package mx.negocio360.identidad.aplicacion.puertos;

import mx.negocio360.compartido.eventos.SobreEvento;

/**
 * Puerto de publicacion de eventos. La implementacion real escribe en el outbox dentro de la transaccion del cambio,
 * para que el hecho y el evento se confirmen juntos. Mientras no haya repositorios de Postgres, el de desarrollo los
 * guarda en memoria.
 */
public interface PublicadorEventos {

    void publicar(SobreEvento evento);
}
