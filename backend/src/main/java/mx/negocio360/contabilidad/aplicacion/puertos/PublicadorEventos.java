package mx.negocio360.contabilidad.aplicacion.puertos;

import mx.negocio360.compartido.eventos.SobreEvento;

/**
 * Puerto de publicacion de los eventos de Contabilidad (E-08). La implementacion real escribe en
 * {@code plataforma.event_publication} dentro de la transaccion de la poliza, para que el hecho y el evento se
 * confirmen juntos. Esa implementacion es de la capa de infraestructura, que todavia no existe.
 */
public interface PublicadorEventos {

    /**
     * Publica un evento.
     *
     * @param evento sobre del evento, sin datos personales
     */
    void publicar(SobreEvento evento);
}
