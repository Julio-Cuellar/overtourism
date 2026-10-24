package mx.negocio360.restaurantes.web;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.restaurantes.aplicacion.CocinaServicio;
import mx.negocio360.restaurantes.aplicacion.ComandaVista;
import mx.negocio360.restaurantes.aplicacion.CuentaParaCobro;
import mx.negocio360.restaurantes.aplicacion.CuentaVista;
import mx.negocio360.restaurantes.aplicacion.CuentasServicio;
import mx.negocio360.restaurantes.dominio.Estacion;
import mx.negocio360.restaurantes.dominio.EstadoDeComanda;
import mx.negocio360.restaurantes.dominio.MotivoDeAnulacion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Los resolvedores de las cuentas y del tablero de cocina (R5). Solo traducen GraphQL a los servicios: el negocio, el actor
 * y los permisos salen de la sesion (SG-02) y cada servicio exige su permiso (SG-04). Cada mutacion es una sola transaccion
 * para que sus eventos lleguen al outbox junto con el cambio (SR-04).
 */
@Controller
@ConditionalOnProperty(name = "restaurantes.almacen", havingValue = "memoria")
public class CuentasGraphQlController {

    private final CuentasServicio cuentas;
    private final CocinaServicio cocina;
    private final EnTransaccion tx;

    /**
     * Arma el controlador.
     *
     * @param cuentas       las cuentas
     * @param cocina        el tablero de comandas
     * @param transacciones donde cada mutacion es una sola transaccion
     */
    public CuentasGraphQlController(CuentasServicio cuentas, CocinaServicio cocina, TransactionOperations transacciones) {
        this.cuentas = cuentas;
        this.cocina = cocina;
        this.tx = new EnTransaccion(transacciones);
    }

    // ---- consultas ----

    @QueryMapping
    public CuentaVista cuenta(@Argument UUID id, @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return cuentas.cuenta(entrada.exigirContexto(), entrada.autorizacion(), id);
    }

    @QueryMapping
    public List<CuentaVista> cuentasAbiertas(@Argument UUID mesaId, @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return cuentas.cuentasAbiertas(entrada.exigirContexto(), entrada.autorizacion(), mesaId);
    }

    @QueryMapping
    public CuentaParaCobro cuentaParaCobro(@Argument UUID cuentaId, @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return cuentas.cuentaParaCobro(entrada.exigirContexto(), entrada.autorizacion(), cuentaId);
    }

    @QueryMapping
    public List<ComandaVista> comandas(@Argument Estacion estacion, @Argument List<EstadoDeComanda> estados,
                                       @Argument int primeros, @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return cocina.comandas(entrada.exigirContexto(), entrada.autorizacion(), estacion,
                estados == null ? null : Set.copyOf(estados), primeros);
    }

    // ---- mutaciones de cuentas ----

    @MutationMapping
    public CuentaVista abrirCuenta(@Argument AbrirCuentaInput input, @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return tx.ejecutar(() -> cuentas.abrirCuenta(entrada.exigirContexto(), entrada.autorizacion(), input.mesaId(),
                input.comensales(), entrada.llave()));
    }

    @MutationMapping
    public CuentaVista agregarLinea(@Argument AgregarLineaInput input, @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return tx.ejecutar(() -> cuentas.agregarLinea(entrada.exigirContexto(), entrada.autorizacion(), input.cuentaId(),
                input.platilloId(), input.cantidad(), input.nota(), input.marcaAlergia(), entrada.llave()));
    }

    @MutationMapping
    public CuentaVista quitarLineaPendiente(@Argument UUID cuentaId, @Argument UUID lineaId,
                                            @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return tx.ejecutar(() -> cuentas.quitarLineaPendiente(entrada.exigirContexto(), entrada.autorizacion(), cuentaId,
                lineaId));
    }

    @MutationMapping
    public List<ComandaVista> enviarACocina(@Argument UUID cuentaId, @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return tx.ejecutar(() -> cuentas.enviarACocina(entrada.exigirContexto(), entrada.autorizacion(), cuentaId));
    }

    @MutationMapping
    public CuentaVista pedirCuenta(@Argument UUID cuentaId, @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return tx.ejecutar(() -> cuentas.pedirCuenta(entrada.exigirContexto(), entrada.autorizacion(), cuentaId));
    }

    @MutationMapping
    public CuentaVista reabrirCuenta(@Argument UUID cuentaId, @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return tx.ejecutar(() -> cuentas.reabrirCuenta(entrada.exigirContexto(), entrada.autorizacion(), cuentaId));
    }

    @MutationMapping
    public CuentaVista confirmarPresencia(@Argument UUID cuentaId, @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return tx.ejecutar(() -> cuentas.confirmarPresencia(entrada.exigirContexto(), entrada.autorizacion(), cuentaId));
    }

    @MutationMapping
    public CuentaVista anularLinea(@Argument AnularLineaInput input, @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return tx.ejecutar(() -> cuentas.anularLinea(entrada.exigirContexto(), entrada.autorizacion(), input.lineaId(),
                input.motivo(), input.texto()));
    }

    @MutationMapping
    public CuentaVista cancelarCuenta(@Argument UUID cuentaId, @Argument MotivoDeAnulacion motivo, @Argument String texto,
                                      @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return tx.ejecutar(() -> cuentas.cancelarCuenta(entrada.exigirContexto(), entrada.autorizacion(), cuentaId, motivo,
                texto));
    }

    @MutationMapping
    public CuentaVista liberarMesa(@Argument UUID cuentaId, @Argument String motivo,
                                   @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return tx.ejecutar(() -> cuentas.liberarMesa(entrada.exigirContexto(), entrada.autorizacion(), cuentaId, motivo));
    }

    // ---- mutaciones del tablero ----

    @MutationMapping
    public ComandaVista avanzarComanda(@Argument UUID id, @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return tx.ejecutar(() -> cocina.avanzarComanda(entrada.exigirContexto(), entrada.autorizacion(), id));
    }

    @MutationMapping
    public ComandaVista devolverComanda(@Argument UUID id, @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return tx.ejecutar(() -> cocina.devolverComanda(entrada.exigirContexto(), entrada.autorizacion(), id));
    }
}
