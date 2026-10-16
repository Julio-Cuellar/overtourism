package mx.negocio360.restaurantes.infraestructura;

import java.util.Objects;
import java.util.function.Function;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.restaurantes.aplicacion.ConsumidorDeVentas;
import mx.negocio360.restaurantes.aplicacion.ResultadoConsumo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.ApplicationModuleListener;

/**
 * Recibe de Ventas, por el bus, {@code venta.registrada} y {@code venta.anulada} y se los pasa a {@link ConsumidorDeVentas}
 * (CM-01 a CM-06). La entrega es al menos una vez y en su propia transaccion (R3).
 *
 * <p>Si Inventario no respondio ({@link ResultadoConsumo#reintentable()}), <b>lanza</b>: asi Modulith deja la publicacion
 * incompleta y el evento vuelve a llegar (D-122). Lo demas no lanza, porque repetirlo no lo arreglaria. Los mensajes y
 * avisos llevan el tipo y el id del evento, nunca sus datos.
 */
public class EscuchaDeVentas {

    private static final Logger LOG = LoggerFactory.getLogger(EscuchaDeVentas.class);

    private final Function<SobreEvento, ResultadoConsumo> consumidor;

    /** @param consumidor quien aplica el evento; el consumo real o, en pruebas, uno de mentiras */
    public EscuchaDeVentas(Function<SobreEvento, ResultadoConsumo> consumidor) {
        this.consumidor = Objects.requireNonNull(consumidor, "consumidor");
    }

    /** @param consumidor el consumo de insumos por venta */
    public EscuchaDeVentas(ConsumidorDeVentas consumidor) {
        this(Objects.requireNonNull(consumidor, "consumidor")::procesar);
    }

    /**
     * Procesa cada evento publicado; los que no son de venta o no traen platillos de receta se ignoran.
     *
     * @throws IllegalStateException si Inventario no respondio y el evento debe reintentarse
     */
    @ApplicationModuleListener
    public void alRecibir(SobreEvento evento) {
        var resultado = consumidor.apply(evento);
        if (resultado.reintentable()) {
            throw new IllegalStateException("Inventario no respondio para " + evento.tipo() + " " + evento.id()
                    + ": se reintentara");
        }
        if (resultado == ResultadoConsumo.DATOS_INVALIDOS || resultado == ResultadoConsumo.FALLIDA) {
            LOG.warn("Restaurantes no aplico el evento {} {}: {}", evento.tipo(), evento.id(), resultado);
        }
    }
}
