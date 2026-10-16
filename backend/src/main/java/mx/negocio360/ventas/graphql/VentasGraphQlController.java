package mx.negocio360.ventas.graphql;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.compartido.catalogo.ProductoParaVenta;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.compartido.paginacion.Pagina;
import mx.negocio360.ventas.FiltroVentas;
import mx.negocio360.ventas.ResumenTurno;
import mx.negocio360.ventas.VentasApi;
import mx.negocio360.ventas.dominio.Venta;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * Los resolvedores de consulta de Ventas (VF-01 a VF-03 y G1). Solo traducen GraphQL a {@link VentasApi}: el negocio, el
 * actor y los permisos salen de la sesion que dejo el filtro de entrada ({@link EntradaGraphQl}), nunca de un argumento
 * (SG-02); sin sesion o sin negocio activo fallan cerrado. Un id de otro negocio responde vacio, igual que uno que no existe.
 */
@Controller
@ConditionalOnProperty(name = "ventas.almacen")
public class VentasGraphQlController {

    /** Una pagina de ventas con la forma de {@code VentaConexion}. */
    public record VentaConexion(List<Venta> ventas, Map<String, Object> pageInfo) {
    }

    private final VentasApi ventas;

    /**
     * Arma el controlador.
     *
     * @param ventas API de Ventas
     */
    public VentasGraphQlController(VentasApi ventas) {
        this.ventas = Objects.requireNonNull(ventas, "ventas");
    }

    /** VF-01: una venta del negocio, o nulo. */
    @QueryMapping
    public Venta venta(@Argument UUID id, @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return ventas.venta(entrada.exigirContexto(), entrada.autorizacion(), id).orElse(null);
    }

    /** VF-02: ventas del negocio, de la mas reciente a la mas antigua, con cursor. */
    @QueryMapping
    public VentaConexion ventas(@Argument FiltroVentas filtro, @Argument int primeros, @Argument String despues,
                                @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        Pagina<Venta> pagina = ventas.ventas(entrada.exigirContexto(), entrada.autorizacion(), filtro, primeros,
                despues);
        var info = new java.util.LinkedHashMap<String, Object>();
        info.put("hayMas", pagina.hayMas());
        info.put("cursorFinal", pagina.cursorFinal());
        return new VentaConexion(pagina.elementos(), info);
    }

    /** VF-03: conteos y totales de un turno. */
    @QueryMapping
    public ResumenTurno resumenTurno(@Argument UUID turnoId,
                                     @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return ventas.resumenTurno(entrada.exigirContexto(), entrada.autorizacion(), turnoId);
    }

    /** G1: productos que se pueden vender. */
    @QueryMapping
    public List<ProductoParaVenta> productosParaVenta(@Argument String texto, @Argument int primeros,
                                                      @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return ventas.productosParaVenta(entrada.exigirContexto(), entrada.autorizacion(), texto, primeros);
    }

    /** El total por metodo de un resumen, como lista de {@code MontoPorMetodo} en orden de metodo. */
    @SchemaMapping(typeName = "ResumenTurno", field = "totalesPorMetodo")
    public List<Map<String, Object>> totalesPorMetodo(ResumenTurno resumen) {
        return resumen.totalesPorMetodo().entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(e -> Map.<String, Object>of("metodo", e.getKey(), "monto", e.getValue())).toList();
    }
}
