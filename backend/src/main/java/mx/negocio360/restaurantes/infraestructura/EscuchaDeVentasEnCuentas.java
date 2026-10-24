package mx.negocio360.restaurantes.infraestructura;

import java.util.Objects;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.restaurantes.aplicacion.VinculoDeCuentasConVentas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recibe de Ventas, por el bus, {@code venta.registrada} y {@code venta.anulada} y se los pasa a
 * {@link VinculoDeCuentasConVentas} para que la cuenta cobrada quede pagada y la mesa libre (CT-08 a CT-10). La entrega es
 * al menos una vez y en su propia transaccion (R3). Solo lanza si otra escritura de la misma cuenta se adelanto varias veces:
 * asi el evento vuelve a llegar. Los registros llevan el tipo, el id del evento y el resultado, nunca sus datos.
 */
public class EscuchaDeVentasEnCuentas {

    private static final Logger LOG = LoggerFactory.getLogger(EscuchaDeVentasEnCuentas.class);

    private final VinculoDeCuentasConVentas vinculo;

    /** @param vinculo quien aplica el evento a las cuentas */
    public EscuchaDeVentasEnCuentas(VinculoDeCuentasConVentas vinculo) {
        this.vinculo = Objects.requireNonNull(vinculo, "vinculo");
    }

    /** Procesa cada evento publicado; los que no son de venta o no nombran una cuenta de Restaurantes se ignoran. */
    @EscuchaDeEventos
    public void alRecibir(SobreEvento evento) {
        var resultado = vinculo.procesar(evento);
        switch (resultado) {
            case YA_PAGADA, CUENTA_CANCELADA, SIN_CUENTA, DATOS_INVALIDOS ->
                    LOG.warn("Restaurantes no vinculo la venta del evento {} {}: {}", evento.tipo(), evento.id(), resultado);
            default -> {
            }
        }
    }
}
