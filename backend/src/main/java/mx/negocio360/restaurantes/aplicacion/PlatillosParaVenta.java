package mx.negocio360.restaurantes.aplicacion;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.catalogo.FuenteDeProductos;
import mx.negocio360.compartido.catalogo.ModoInventario;
import mx.negocio360.compartido.catalogo.ProductoParaVenta;
import mx.negocio360.restaurantes.dominio.Platillo;

/**
 * La fuente de productos de Restaurantes para Ventas (MN-02, MN-03, MN-14): implementa la {@link FuenteDeProductos} de la
 * plataforma (D-128, D-214), asi que Ventas la ve junto con las demas fuentes sin conocer a Restaurantes (R4). El negocio
 * llega como argumento porque quien llama es Ventas, que lo toma de su contexto; esta clase nunca devuelve algo de otro
 * negocio. Tambien declara el modo de inventario de cada platillo (R-V2, D-163).
 *
 * <p>Agotado, temporada y publicado gobiernan el pedido en el salon, no el cobro: una cuenta con un platillo que se agoto
 * despues de pedirlo se cobra igual, asi que para Ventas «activo» es solo «no esta dado de baja» (MN-14). Lo que se ofrece
 * al buscar para un cobro de mostrador si es lo que se puede pedir hoy.
 */
public final class PlatillosParaVenta implements FuenteDeProductos {

    /** Tope de ids por consulta. */
    public static final int MAX_IDS = 100;
    /** Tope de resultados de una busqueda. */
    public static final int MAX_BUSQUEDA = 50;
    /** La unica unidad de un platillo (D-162). */
    public static final String UNIDAD = "pieza";

    private final RepositorioMenu menu;
    private final Clock reloj;

    /**
     * Arma la fuente.
     *
     * @param menu  almacen del menu
     * @param reloj reloj de la plataforma, para la temporada
     */
    public PlatillosParaVenta(RepositorioMenu menu, Clock reloj) {
        this.menu = Objects.requireNonNull(menu, "menu");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    /**
     * Precios vigentes de los platillos pedidos, solo del negocio indicado.
     *
     * @param negocioId   negocio del contexto, nunca de un argumento del usuario
     * @param productoIds ids a consultar (maximo 100)
     * @return id → producto; los que no son de ese negocio o no existen no aparecen; los dados de baja salen con
     *         {@code activo = false}
     * @throws IllegalArgumentException si piden mas de 100
     */
    @Override
    public Map<UUID, ProductoParaVenta> preciosParaVenta(UUID negocioId, Set<UUID> productoIds) {
        if (productoIds.size() > MAX_IDS) {
            throw new IllegalArgumentException("Se piden mas de " + MAX_IDS + " productos");
        }
        var respuesta = new HashMap<UUID, ProductoParaVenta>();
        menu.platillos(negocioId, productoIds).forEach((id, platillo) -> respuesta.put(id, producto(platillo)));
        return respuesta;
    }

    /**
     * Busqueda por texto para el cajero: solo platillos que se pueden pedir hoy (activos, no agotados y en temporada), por
     * nombre.
     *
     * @param negocioId negocio del contexto
     * @param texto     parte del nombre, sin distinguir mayusculas; vacio o nulo devuelve los primeros
     * @param limite    de 1 a 50 (mas se recorta a 50)
     * @throws IllegalArgumentException si el limite es menor que 1
     */
    @Override
    public List<ProductoParaVenta> buscar(UUID negocioId, String texto, int limite) {
        if (limite < 1) {
            throw new IllegalArgumentException("El limite debe ser al menos 1");
        }
        var hoy = LocalDate.now(reloj.withZone(MenuServicio.ZONA));
        var buscado = texto == null ? "" : texto.strip().toLowerCase(Locale.ROOT);
        return menu.platillos(negocioId).stream().filter(p -> p.ordenable(hoy))
                .filter(p -> buscado.isEmpty() || p.nombre().toLowerCase(Locale.ROOT).contains(buscado))
                .sorted(Comparator.comparing((Platillo p) -> p.nombre().toLowerCase(Locale.ROOT)).thenComparing(Platillo::id))
                .limit(Math.min(limite, MAX_BUSQUEDA)).map(PlatillosParaVenta::producto).toList();
    }

    /**
     * Como descuenta inventario el platillo (K-05, D-163): el que le puso el negocio, o {@code NINGUNO} si el platillo no
     * existe o es de otro negocio.
     */
    @Override
    public ModoInventario modoInventario(UUID negocioId, UUID productoId) {
        return menu.platillo(negocioId, productoId).map(p -> ModoInventario.valueOf(p.modo().name()))
                .orElse(ModoInventario.NINGUNO);
    }

    private static ProductoParaVenta producto(Platillo p) {
        return new ProductoParaVenta(p.id(), p.nombre(), UNIDAD, p.precio(), false, p.activo());
    }
}
