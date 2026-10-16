package mx.negocio360.clientes.infraestructura;

import java.util.Objects;
import mx.negocio360.clientes.aplicacion.Comando;
import mx.negocio360.clientes.aplicacion.ManejadorComando;
import mx.negocio360.clientes.aplicacion.ResultadoComando;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Pone cada {@code confirmar} de un manejador en una sola transaccion, que incluye sus eventos (outbox): si algo
 * falla, ni el cambio ni los eventos quedan. {@code preparar} no escribe y no abre transaccion. El manejador sigue
 * siendo Java puro; la transaccion vive aqui, en la infraestructura.
 */
public final class ManejadorTransaccional<C extends Comando> implements ManejadorComando<C> {

    private final ManejadorComando<C> manejador;
    private final TransactionOperations transacciones;

    private ManejadorTransaccional(ManejadorComando<C> manejador, TransactionOperations transacciones) {
        this.manejador = Objects.requireNonNull(manejador, "manejador");
        this.transacciones = Objects.requireNonNull(transacciones, "transacciones");
    }

    public static <C extends Comando> ManejadorComando<C> de(ManejadorComando<C> manejador,
                                                             TransactionOperations transacciones) {
        return new ManejadorTransaccional<>(manejador, transacciones);
    }

    @Override
    public String tipo() {
        return manejador.tipo();
    }

    @Override
    public Class<C> claseComando() {
        return manejador.claseComando();
    }

    @Override
    public String preparar(ContextoNegocio contexto, C comando) {
        return manejador.preparar(contexto, comando);
    }

    @Override
    public ResultadoComando confirmar(ContextoNegocio contexto, C comando) {
        return transacciones.execute(estado -> manejador.confirmar(contexto, comando));
    }
}
