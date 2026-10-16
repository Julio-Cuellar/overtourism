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
 * Inventario todavia no esta conectado: todo responde un fallo reintentable. Es honesto en lugar de inventar articulos; el
 * adaptador real llega cuando Inventario ofrezca {@code rastreo} y {@code activo} en {@code aBase}, los consumos por venta y
 * {@code @NamedInterface} (R-I1, R-I2, R-I4).
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
