package mx.negocio360.canal.web;

import graphql.schema.DataFetchingEnvironment;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.caja.CajaApi;
import mx.negocio360.caja.SaldoTurno;
import mx.negocio360.caja.dominio.Caja;
import mx.negocio360.caja.dominio.Corte;
import mx.negocio360.caja.dominio.MetodoPago;
import mx.negocio360.caja.dominio.Turno;
import mx.negocio360.canal.demo.CatalogoSimulado;
import mx.negocio360.canal.demo.CuponesSimulados;
import mx.negocio360.ventas.VentasApi;
import mx.negocio360.ventas.dominio.Venta;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * Consultas del panel (CF-01 a CF-04, VF-01, VF-02) y los datos simulados de la demo. Todas filtran por el negocio de
 * la sesion; un id de otro negocio responde vacio, igual que uno que no existe (R1).
 */
@Controller
@Profile("!produccion")
@ConditionalOnProperty(name = "negocio360.demo.habilitada", havingValue = "true")
public class ConsultasController {

    /**
     * Un monto por metodo de pago.
     *
     * @param metodo metodo
     * @param monto  escala 2
     */
    public record MontoVista(MetodoPago metodo, BigDecimal monto) {
    }

    /**
     * Producto de la demo, siempre rotulado como simulado.
     *
     * @param productoId     id
     * @param descripcion    nombre
     * @param unidad         pieza, kg...
     * @param precio         precio de ejemplo
     * @param admiteFraccion si se vende a granel
     * @param simulado       siempre verdadero
     */
    public record ProductoVista(UUID productoId, String descripcion, String unidad, BigDecimal precio,
                                boolean admiteFraccion, boolean simulado) {
    }

    /**
     * Cupon de la demo, siempre rotulado como simulado.
     *
     * @param codigo    lo que el turista muestra
     * @param descuento monto fijo
     * @param simulado  siempre verdadero
     */
    public record CuponVista(String codigo, BigDecimal descuento, boolean simulado) {
    }

    private final VentasApi ventas;
    private final CajaApi caja;
    private final CatalogoSimulado catalogo;
    private final CuponesSimulados cupones;

    /**
     * Arma el controlador.
     *
     * @param ventas   API de Ventas
     * @param caja     API de Caja
     * @param catalogo catalogo simulado
     * @param cupones  cupones simulados
     */
    public ConsultasController(VentasApi ventas, CajaApi caja, CatalogoSimulado catalogo, CuponesSimulados cupones) {
        this.ventas = Objects.requireNonNull(ventas, "ventas");
        this.caja = Objects.requireNonNull(caja, "caja");
        this.catalogo = Objects.requireNonNull(catalogo, "catalogo");
        this.cupones = Objects.requireNonNull(cupones, "cupones");
    }

    /** CF-04: cajas del negocio. */
    @QueryMapping
    public List<Caja> cajas(DataFetchingEnvironment entorno) {
        return caja.cajas(ContextoGraphQl.requerido(entorno));
    }

    /** CF-01: turno abierto o en corte de una caja, con su estado efectivo. */
    @QueryMapping
    public Turno turnoActivo(@Argument UUID cajaId, DataFetchingEnvironment entorno) {
        return caja.turnoActivo(ContextoGraphQl.requerido(entorno), cajaId).orElse(null);
    }

    /** CF-02: esperado por metodo y movimientos de un turno. */
    @QueryMapping
    public SaldoTurno saldoTurno(@Argument UUID turnoId, DataFetchingEnvironment entorno) {
        return caja.saldoTurno(ContextoGraphQl.requerido(entorno), turnoId);
    }

    /** CF-03: cortes del negocio. */
    @QueryMapping
    public List<Corte> cortes(DataFetchingEnvironment entorno) {
        return caja.cortes(ContextoGraphQl.requerido(entorno));
    }

    /** VF-01: una venta del negocio. */
    @QueryMapping
    public Venta venta(@Argument UUID id, DataFetchingEnvironment entorno) {
        return ventas.venta(ContextoGraphQl.requerido(entorno), id).orElse(null);
    }

    /** VF-02: ventas de un turno. */
    @QueryMapping
    public List<Venta> ventasDelTurno(@Argument UUID turnoId, DataFetchingEnvironment entorno) {
        return ventas.ventasDelTurno(ContextoGraphQl.requerido(entorno), turnoId);
    }

    /** Productos de la demo (Catalogo aun no existe). */
    @QueryMapping
    public List<ProductoVista> productosSimulados(DataFetchingEnvironment entorno) {
        ContextoGraphQl.requerido(entorno);
        return catalogo.todos().stream().map(p -> new ProductoVista(p.productoId(), p.descripcion(), p.unidad(),
                p.precio(), p.admiteFraccion(), true)).toList();
    }

    /** Cupones de la demo (Pasaporte aun no existe). */
    @QueryMapping
    public List<CuponVista> cuponesSimulados(DataFetchingEnvironment entorno) {
        ContextoGraphQl.requerido(entorno);
        return cupones.todos().stream().map(c -> new CuponVista(c.codigo(), c.descuento(), true)).toList();
    }

    /** El esperado del saldo como lista, en el orden de los metodos. */
    @SchemaMapping(typeName = "SaldoTurno", field = "esperadoPorMetodo")
    public List<MontoVista> esperadoDelSaldo(SaldoTurno saldo) {
        return montos(saldo.esperadoPorMetodo());
    }

    /** El esperado del corte como lista. */
    @SchemaMapping(typeName = "Corte", field = "esperadoPorMetodo")
    public List<MontoVista> esperadoDelCorte(Corte corte) {
        return montos(corte.esperadoPorMetodo());
    }

    /** Lo contado en el corte como lista. */
    @SchemaMapping(typeName = "Corte", field = "contadoPorMetodo")
    public List<MontoVista> contadoDelCorte(Corte corte) {
        return montos(corte.contadoPorMetodo());
    }

    private static List<MontoVista> montos(Map<MetodoPago, BigDecimal> porMetodo) {
        return porMetodo.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(e -> new MontoVista(e.getKey(), e.getValue())).toList();
    }
}
