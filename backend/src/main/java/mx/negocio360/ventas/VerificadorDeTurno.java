package mx.negocio360.ventas;

import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;

/**
 * Pregunta que Ventas hace antes de preparar y de confirmar una venta: ¿el turno sigue abierto en este negocio?
 * (VT-01). Ventas define la pregunta y Caja la contesta con su propio adaptador, asi Ventas no depende de Caja (D-76).
 */
public interface VerificadorDeTurno {

    /**
     * Dice si el turno existe en el negocio del contexto y su estado efectivo es ABIERTO.
     *
     * @param contexto negocio y actor
     * @param turnoId  turno de la venta; nulo cuenta como no abierto
     * @return verdadero solo si se puede vender en ese turno
     */
    boolean abierto(ContextoNegocio contexto, UUID turnoId);
}
