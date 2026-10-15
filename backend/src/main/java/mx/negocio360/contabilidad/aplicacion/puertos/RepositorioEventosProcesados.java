package mx.negocio360.contabilidad.aplicacion.puertos;

import java.util.Optional;
import java.util.UUID;
import mx.negocio360.contabilidad.dominio.EventoProcesado;

/**
 * Puerto de {@code contabilidad.evento_procesado} (E-01). La llave es (negocio, evento). Se escribe en la misma
 * transaccion que el efecto del evento, asi que un fallo no deja el evento marcado.
 */
public interface RepositorioEventosProcesados {

    /**
     * Fila de un evento ya procesado.
     *
     * @param negocioId negocio
     * @param eventoId  evento
     * @return la fila, o vacio si el evento no se ha procesado
     */
    Optional<EventoProcesado> de(UUID negocioId, UUID eventoId);

    /**
     * Inserta la fila si no existe.
     *
     * @param evento fila a insertar
     * @return falso si otra entrega del mismo evento ya la habia insertado
     */
    boolean insertar(EventoProcesado evento);
}
