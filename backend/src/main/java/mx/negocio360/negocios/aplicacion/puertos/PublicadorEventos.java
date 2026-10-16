package mx.negocio360.negocios.aplicacion.puertos;

import mx.negocio360.compartido.eventos.SobreEvento;

/** Puerto de publicacion de eventos: la implementacion real escribe en el outbox dentro de la transaccion del cambio. */
public interface PublicadorEventos {

    void publicar(SobreEvento evento);
}
