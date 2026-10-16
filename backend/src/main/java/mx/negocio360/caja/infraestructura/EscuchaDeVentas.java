package mx.negocio360.caja.infraestructura;

import java.util.Objects;
import mx.negocio360.caja.aplicacion.ConsumidorVentas;
import mx.negocio360.compartido.eventos.SobreEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

/**
 * Recibe del bus de Spring los eventos de Ventas y se los pasa a {@link ConsumidorVentas} (CF-20, CF-21). Si el
 * consumidor no los pudo aplicar ({@code completado()} falso), deja un aviso con el tipo y el id del evento, nunca sus
 * datos. Pendiente F5: con el outbox, un evento no completado se queda pendiente y se reintenta.
 */
public final class EscuchaDeVentas {

    private static final Logger LOG = LoggerFactory.getLogger(EscuchaDeVentas.class);

    private final ConsumidorVentas consumidor;

    /**
     * Arma la escucha.
     *
     * @param consumidor consumidor de Caja
     */
    public EscuchaDeVentas(ConsumidorVentas consumidor) {
        this.consumidor = Objects.requireNonNull(consumidor, "consumidor");
    }

    /**
     * Procesa cada evento publicado en el proceso; los que no son de venta se ignoran.
     *
     * @param evento sobre publicado
     */
    @EventListener
    public void alPublicarse(SobreEvento evento) {
        var resultado = consumidor.procesar(evento);
        if (!resultado.completado()) {
            LOG.warn("Caja no aplico el evento {} {}: {}", evento.tipo(), evento.id(), resultado);
        }
    }
}
