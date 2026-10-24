package mx.negocio360.caja.infraestructura;

import java.util.Objects;
import mx.negocio360.caja.aplicacion.ConsumidorVentas;
import mx.negocio360.caja.aplicacion.ResultadoAplicacion;
import mx.negocio360.compartido.eventos.SobreEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.ApplicationModuleListener;

/**
 * Escucha de {@code venta.registrada} y {@code venta.anulada} con el outbox de Spring Modulith (F5, CF-20 y CF-21):
 * recibe el evento solo despues de que la transaccion de Ventas se confirmo, en otro hilo y en una transaccion propia,
 * al menos una vez (R3). Esa transaccion incluye lo que {@link ConsumidorVentas} escribe en Caja y el candado del
 * evento, asi que si algo falla no queda nada a medias y el evento se reintenta.
 *
 * <p>Si Caja todavia no puede aplicar el evento ({@link ResultadoAplicacion#SIN_TURNO_ABIERTO}: el turno no existe o ya
 * cerro, CJ-11 sigue pendiente), lanza para que Spring Modulith lo deje <b>pendiente</b> en el outbox y se vuelva a
 * entregar al reiniciar, en vez de perderlo. Un evento que no cumple el contrato ({@code DATOS_INVALIDOS}) solo se
 * reporta: reintentarlo no lo arreglaria. Los avisos llevan el tipo y el id del evento, nunca sus datos.
 */
public class EscuchaDeVentasEnOutbox {

    private static final Logger LOG = LoggerFactory.getLogger(EscuchaDeVentasEnOutbox.class);

    private final ConsumidorVentas consumidor;

    /**
     * Arma la escucha.
     *
     * @param consumidor consumidor de Caja
     */
    public EscuchaDeVentasEnOutbox(ConsumidorVentas consumidor) {
        this.consumidor = Objects.requireNonNull(consumidor, "consumidor");
    }

    /**
     * Procesa cada evento publicado; los que no son de venta se ignoran.
     *
     * @param evento sobre publicado
     * @throws IllegalStateException si Caja todavia no pudo aplicarlo y debe quedar pendiente
     */
    @ApplicationModuleListener
    public void alRecibir(SobreEvento evento) {
        var resultado = consumidor.procesar(evento);
        if (resultado == ResultadoAplicacion.SIN_TURNO_ABIERTO) {
            throw new IllegalStateException("Caja no pudo aplicar " + evento.tipo() + " " + evento.id()
                    + ": no hay un turno abierto. Queda pendiente en el outbox");
        }
        if (!resultado.completado()) {
            LOG.warn("Caja descarto el evento {} {}: {}", evento.tipo(), evento.id(), resultado);
        }
    }
}
