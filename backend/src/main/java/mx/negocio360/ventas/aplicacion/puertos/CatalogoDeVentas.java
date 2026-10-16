package mx.negocio360.ventas.aplicacion.puertos;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Puerto hacia Catalogo (J1, {@code CatalogoApi.preciosParaVenta}). Catalogo aun no existe; el adaptador real llamara
 * a su API publica en el mismo proceso (R2, R6).
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
}
