package mx.negocio360.restaurantes.infraestructura;

import java.util.Objects;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.restaurantes.aplicacion.ResultadoSeguimiento;
import mx.negocio360.restaurantes.aplicacion.SeguimientoDeNegocios;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recibe los eventos del bus y se los pasa a {@link SeguimientoDeNegocios}. La entrega es al menos una vez y en su propia
 * transaccion (R3); el seguimiento es idempotente y nunca lanza por datos malos. Lo que no cumple el contrato deja un aviso
 * con el tipo y el id del evento, nunca con sus datos.
 */
public class EscuchaDeNegocios {

    private static final Logger LOG = LoggerFactory.getLogger(EscuchaDeNegocios.class);

    private final SeguimientoDeNegocios seguimiento;

    /** @param seguimiento lo que Restaurantes sabe de su negocio */
    public EscuchaDeNegocios(SeguimientoDeNegocios seguimiento) {
        this.seguimiento = Objects.requireNonNull(seguimiento, "seguimiento");
    }

    /** Procesa cada evento publicado; los que no son de Negocios o no son de un restaurante se ignoran. */
    @EscuchaDeEventos
    public void alRecibir(SobreEvento evento) {
        if (seguimiento.procesar(evento) == ResultadoSeguimiento.DATOS_INVALIDOS) {
            LOG.warn("Restaurantes descarto el evento {} {} porque no cumple el contrato", evento.tipo(), evento.id());
        }
    }
}
