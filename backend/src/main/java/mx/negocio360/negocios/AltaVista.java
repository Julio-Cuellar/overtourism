package mx.negocio360.negocios;

import java.util.UUID;

/**
 * El resultado de una peticion de alta, para la persona que la hizo (AL-04). Sin el nombre ni ningun dato personal.
 *
 * @param negocioId el id que asigno Identidad al pedir el alta
 * @param estado    {@code REGISTRADO} o {@code RECHAZADO}
 * @param motivo    codigo del rechazo, o nulo
 */
public record AltaVista(UUID negocioId, EstadoAlta estado, String motivo) {

    public enum EstadoAlta { REGISTRADO, RECHAZADO }
}
