package mx.negocio360.inventario.graphql;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.compartido.graphql.comandos.ComandoGraphQlException;
import mx.negocio360.inventario.InventarioApi;
import mx.negocio360.inventario.aplicacion.ArticuloVista;
import mx.negocio360.inventario.aplicacion.MovimientoVista;
import mx.negocio360.inventario.aplicacion.Pagina;
import mx.negocio360.inventario.seguridad.PermisosDeInventario;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * Los resolvedores de consulta de Inventario ({@code inventario}, {@code articulo}, {@code kardex}). Solo traducen GraphQL a
 * {@link InventarioApi}: el negocio, el actor y los permisos salen de la sesion que dejo el filtro de entrada
 * ({@link EntradaGraphQl}), nunca de un argumento (SG-02); cada consulta vuelve a exigir su permiso, ademas de la puerta de
 * la plataforma (dos barreras). Un id de otro negocio responde vacio, igual que uno que no existe.
 */
@Controller
@ConditionalOnProperty(name = "inventario.almacen")
public class InventarioGraphQlController {

    /** Una pagina de articulos con la forma de {@code ArticuloDeInventarioConexion}. */
    public record ArticuloConexion(List<ArticuloVista> nodos, Map<String, Object> pageInfo) {
    }

    /** Una pagina de movimientos con la forma de {@code MovimientoDeInventarioConexion}. */
    public record MovimientoConexion(List<MovimientoVista> nodos, Map<String, Object> pageInfo) {
    }

    /** Una unidad propia con la forma de {@code UnidadPropiaDeInventario}. */
    public record UnidadPropia(String nombre, BigDecimal factor) {
    }

    private final InventarioApi inventario;

    /**
     * Arma el controlador.
     *
     * @param inventario API de Inventario
     */
    public InventarioGraphQlController(InventarioApi inventario) {
        this.inventario = Objects.requireNonNull(inventario, "inventario");
    }

    /** Articulos con su saldo, hasta 100 por pagina. */
    @QueryMapping
    public ArticuloConexion inventario(@Argument String texto, @Argument boolean soloAlertas, @Argument int primeros,
                                       @Argument String despues,
                                       @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        var contexto = entrada.exigirContexto();
        entrada.autorizacion().exigir(PermisosDeInventario.ARTICULO_VER);
        try {
            Pagina<ArticuloVista> pagina = inventario.articulos(contexto, texto, soloAlertas, primeros, despues);
            return new ArticuloConexion(pagina.nodos(), info(pagina.hayMas(), pagina.cursor()));
        } catch (IllegalArgumentException e) {
            throw new ComandoGraphQlException(ComandoGraphQlException.Codigo.CONSULTA_INVALIDA);
        }
    }

    /** Un articulo con su saldo, o nulo. */
    @QueryMapping
    public ArticuloVista articulo(@Argument UUID id, @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        var contexto = entrada.exigirContexto();
        entrada.autorizacion().exigir(PermisosDeInventario.ARTICULO_VER);
        return inventario.articulo(contexto, id).orElse(null);
    }

    /** Movimientos de un articulo, en orden de registro. */
    @QueryMapping
    public MovimientoConexion kardex(@Argument UUID articuloId, @Argument int primeros, @Argument String despues,
                                     @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        var contexto = entrada.exigirContexto();
        entrada.autorizacion().exigir(PermisosDeInventario.ARTICULO_VER);
        try {
            var pagina = inventario.kardex(contexto, articuloId, primeros, despues);
            return new MovimientoConexion(pagina.nodos(), info(pagina.hayMas(), pagina.cursor()));
        } catch (IllegalArgumentException e) {
            throw new ComandoGraphQlException(ComandoGraphQlException.Codigo.CONSULTA_INVALIDA);
        }
    }

    /** Las unidades propias del articulo como lista, en orden de nombre. */
    @SchemaMapping(typeName = "ArticuloDeInventario", field = "unidadesPropias")
    public List<UnidadPropia> unidadesPropias(ArticuloVista articulo) {
        return articulo.unidadesPropias().entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(e -> new UnidadPropia(e.getKey(), e.getValue())).toList();
    }

    private static Map<String, Object> info(boolean hayMas, String cursor) {
        var info = new LinkedHashMap<String, Object>();
        info.put("hayMas", hayMas);
        info.put("cursorFinal", cursor);
        return info;
    }
}
