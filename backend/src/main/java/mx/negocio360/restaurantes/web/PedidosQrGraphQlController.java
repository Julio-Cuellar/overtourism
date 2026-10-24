package mx.negocio360.restaurantes.web;

import java.util.List;
import java.util.Optional;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.restaurantes.aplicacion.EstadoDePedidoQrVista;
import mx.negocio360.restaurantes.aplicacion.MesaPublicaVista;
import mx.negocio360.restaurantes.aplicacion.PedidosQrServicio;
import mx.negocio360.restaurantes.aplicacion.ResultadoPedidoQrVista;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Las operaciones publicas del comensal por QR (QR-03, QR-05, QR-11, QR-13): sin sesion, de mesa en mesa por el token
 * opaco del QR. Son las unicas de Restaurantes que escriben sin sesion (D-210), asi que no toman el negocio de ningun
 * argumento: sale de la mesa que resuelve el token. El pedido lleva la {@code Idempotency-Key} que dejo el filtro de entrada
 * (CT-14). Cada escritura es una sola transaccion para que sus eventos lleguen al outbox junto con el cambio.
 */
@Controller
@ConditionalOnProperty(name = "restaurantes.almacen", havingValue = "memoria")
public class PedidosQrGraphQlController {

    private final PedidosQrServicio pedidos;
    private final EnTransaccion tx;

    /**
     * Arma el controlador.
     *
     * @param pedidos       el pedido por QR
     * @param transacciones donde cada escritura es una sola transaccion
     */
    public PedidosQrGraphQlController(PedidosQrServicio pedidos, TransactionOperations transacciones) {
        this.pedidos = pedidos;
        this.tx = new EnTransaccion(transacciones);
    }

    /** Nulo si el token no sirve (QR-03). */
    @QueryMapping
    public MesaPublicaVista mesaPorQr(@Argument String token) {
        return pedidos.mesaPorQr(token).orElse(null);
    }

    /** Nulo si el token no sirve; las lineas son solo las de ese dispositivo (QR-11). */
    @QueryMapping
    public EstadoDePedidoQrVista estadoDePedidoQr(@Argument String token, @Argument String dispositivoId) {
        return pedidos.estadoDePedidoQr(token, dispositivoId).orElse(null);
    }

    @MutationMapping
    public ResultadoPedidoQrVista pedirPorQr(@Argument PedirPorQrInput input,
                                             @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        var lineas = Optional.ofNullable(input.lineas()).orElse(List.of()).stream().map(PedidoPorQrLineaInput::aLinea).toList();
        return tx.ejecutar(() -> pedidos.pedirPorQr(input.token(), input.dispositivoId(), lineas, entrada.llave()));
    }

    @MutationMapping
    public EstadoDePedidoQrVista pedirCuentaPorQr(@Argument String token, @Argument String dispositivoId) {
        return tx.ejecutar(() -> pedidos.pedirCuentaPorQr(token, dispositivoId));
    }
}
