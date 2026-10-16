package mx.negocio360.restaurantes.aplicacion;

import java.util.List;
import java.util.UUID;

/** Almacen de los consumos fallidos y de los eventos de Ventas que ya se procesaron (R3). */
public interface RepositorioConsumos {

    /**
     * Anota que este consumidor proceso este evento.
     *
     * @return verdadero si es la primera vez; falso si ya estaba anotado
     */
    boolean marcarProcesado(String consumidor, UUID eventoId);

    /** Verdadero si ese consumidor ya proceso ese evento. */
    boolean yaProcesado(String consumidor, UUID eventoId);

    /** Guarda un consumo fallido. */
    void guardarFallido(ConsumoFallido fallido);

    /** Los fallos del negocio, del mas nuevo al mas viejo. */
    List<ConsumoFallido> fallidos(UUID negocioId);
}
