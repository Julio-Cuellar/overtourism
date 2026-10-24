package mx.negocio360.compartido.catalogo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Quien ofrece productos para vender (D-128). Misceláneos y Restaurantes lo implementan con los suyos; cuando exista
 * Catalogo sera otra implementacion. Ventas inyecta todas las fuentes y las compone sin conocer a ningun vertical (R4).
 *
 * <p>El {@code negocioId} siempre viene del contexto de la sesion, nunca de un argumento del usuario: una fuente solo
 * devuelve productos de ese negocio. Lo propio del giro llega como datos (unidad y si admite fraccion, D-139).
 */
public interface FuenteDeProductos {

    /**
     * Precios vigentes de los productos pedidos, solo del negocio indicado.
     *
     * @param negocioId   negocio del contexto
     * @param productoIds productos a consultar (maximo 100)
     * @return mapa id → producto; los que esta fuente no conoce en ese negocio no aparecen
     */
    Map<UUID, ProductoParaVenta> preciosParaVenta(UUID negocioId, Set<UUID> productoIds);

    /**
     * Busqueda por texto para el cajero. Solo productos activos de ese negocio.
     *
     * @param negocioId negocio del contexto
     * @param texto     texto a buscar; nulo o vacio es «los primeros»
     * @param limite    cuantos devolver como maximo (hasta 50)
     * @return productos activos que coinciden, con el orden que decida la fuente
     */
    List<ProductoParaVenta> buscar(UUID negocioId, String texto, int limite);

    /**
     * Como descuenta inventario un producto de esta fuente (R-V2, K-05). Opcional: por omision es {@link
     * ModoInventario#NINGUNO}, que es lo correcto para una fuente sin control de existencias. Un vertical con recetas o
     * con vinculo a articulos lo sobreescribe; un producto que la fuente no conoce en ese negocio es {@code NINGUNO}.
     *
     * @param negocioId  negocio del contexto
     * @param productoId producto a consultar
     * @return el modo de inventario del producto
     */
    default ModoInventario modoInventario(UUID negocioId, UUID productoId) {
        return ModoInventario.NINGUNO;
    }
}
