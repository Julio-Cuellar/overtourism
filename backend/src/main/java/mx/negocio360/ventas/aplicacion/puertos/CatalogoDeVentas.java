package mx.negocio360.ventas.aplicacion.puertos;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.catalogo.ProductoParaVenta;

/**
 * Puerto de Ventas hacia los productos que se venden (J1). Lo implementa {@code CatalogoCompuesto}, que junta las
 * {@link mx.negocio360.compartido.catalogo.FuenteDeProductos} del sistema (D-128): Ventas no sabe quien las ofrece.
 */
public interface CatalogoDeVentas {

    /**
     * Precios vigentes de los productos pedidos, solo del negocio indicado.
     *
     * @param negocioId   negocio del contexto, nunca de un argumento del usuario
     * @param productoIds productos a consultar (maximo 100)
     * @return mapa id → producto; los que no existen en ese negocio no aparecen
     */
    Map<UUID, ProductoParaVenta> preciosParaVenta(UUID negocioId, Set<UUID> productoIds);

    /**
     * Busqueda por texto para el cajero (G1).
     *
     * @param negocioId negocio del contexto
     * @param texto     texto a buscar; nulo o vacio es «los primeros»
     * @param limite    cuantos devolver como maximo, hasta 50
     * @return productos activos que coinciden
     */
    List<ProductoParaVenta> buscar(UUID negocioId, String texto, int limite);
}
