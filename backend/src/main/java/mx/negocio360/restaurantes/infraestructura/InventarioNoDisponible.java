package mx.negocio360.restaurantes.infraestructura;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.restaurantes.aplicacion.ArticuloDeInventario;
import mx.negocio360.restaurantes.aplicacion.CantidadEnBase;
import mx.negocio360.restaurantes.aplicacion.FalloDeInventario;
import mx.negocio360.restaurantes.aplicacion.InventarioPuerto;
import mx.negocio360.restaurantes.aplicacion.LineaDeConsumo;
import mx.negocio360.restaurantes.aplicacion.LineaDeInsumo;
import mx.negocio360.restaurantes.aplicacion.ResultadoDeConsumo;

/**
 * Respaldo cuando no hay {@code InventarioApi} en el contexto (por ejemplo, sin {@code inventario.almacen}): todo responde un
 * fallo reintentable. Es honesto en lugar de inventar articulos. Con Inventario encendido se usa
 * {@link InventarioApiComoPuerto}.
 */
public final class InventarioNoDisponible implements InventarioPuerto {

    private static FalloDeInventario fallo() {
        return new FalloDeInventario("ERROR_TEMPORAL", true);
    }

    @Override
    public Map<UUID, ArticuloDeInventario> articulos(ContextoNegocio contexto, Set<UUID> ids) {
        throw fallo();
    }

    @Override
    public List<CantidadEnBase> aBase(ContextoNegocio contexto, List<LineaDeInsumo> lineas) {
        throw fallo();
    }

    @Override
    public ResultadoDeConsumo registrarConsumo(ContextoNegocio contexto, String consumidor, UUID causaId, Instant ocurridoEn,
                                               List<LineaDeConsumo> lineas) {
        throw fallo();
    }

    @Override
    public ResultadoDeConsumo revertirConsumo(ContextoNegocio contexto, String consumidor, UUID causaId) {
        throw fallo();
    }
}
