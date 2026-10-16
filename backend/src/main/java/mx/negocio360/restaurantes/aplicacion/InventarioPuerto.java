package mx.negocio360.restaurantes.aplicacion;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;

/**
 * Lo que Restaurantes necesita de Inventario (docs/diseno/inventario.md &sect;6 y restaurantes.md &sect;10). Es un puerto propio
 * para que el resto de Restaurantes no conozca los tipos de Inventario: el unico adaptador real es
 * {@code InventarioApiComoPuerto} (R-I1 a R-I4), y {@code InventarioNoDisponible} responde cuando Inventario no esta
 * encendido.
 */
public interface InventarioPuerto {

    /**
     * Los articulos que existen en el negocio; los que no, no aparecen.
     *
     * @throws FalloDeInventario si Inventario no responde
     */
    Map<UUID, ArticuloDeInventario> articulos(ContextoNegocio contexto, Set<UUID> ids);

    /**
     * Lleva cada cantidad a la unidad base de su articulo.
     *
     * @throws FalloDeInventario ARTICULO_NO_ENCONTRADO, UNIDAD_NO_CONVERTIBLE o CANTIDAD_INVALIDA si alguna linea no sirve
     */
    List<CantidadEnBase> aBase(ContextoNegocio contexto, List<LineaDeInsumo> lineas);

    /**
     * Descuenta el consumo de una venta, atomico e idempotente por {@code (negocio, causaId, consumidor)} (K-01 y K-02).
     *
     * @throws FalloDeInventario permanente (ARTICULO_INACTIVO, UNIDAD_NO_CONVERTIBLE...) o reintentable
     */
    ResultadoDeConsumo registrarConsumo(ContextoNegocio contexto, String consumidor, UUID causaId, Instant ocurridoEn,
                                        List<LineaDeConsumo> lineas);

    /**
     * Revierte el consumo de una venta anulada al costo original (K-03); si el consumo no llego, deja la marca (K-04).
     *
     * @throws FalloDeInventario si Inventario no responde
     */
    ResultadoDeConsumo revertirConsumo(ContextoNegocio contexto, String consumidor, UUID causaId);
}
