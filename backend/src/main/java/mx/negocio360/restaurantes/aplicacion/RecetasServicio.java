package mx.negocio360.restaurantes.aplicacion;

import java.time.Clock;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.restaurantes.dominio.CodigoError;
import mx.negocio360.restaurantes.dominio.ModoInventario;
import mx.negocio360.restaurantes.dominio.Platillo;
import mx.negocio360.restaurantes.dominio.Receta;
import mx.negocio360.restaurantes.dominio.RecetaVersion;
import mx.negocio360.restaurantes.dominio.RestaurantesException;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;

/**
 * Consultas y desactivacion de recetas. Ver la receta exige {@code restaurantes.receta.ver} y nunca trae costos (RC-10);
 * el costo teorico y el margen exigen {@code restaurantes.costo.ver} (RC-05, RC-06). Guardar es el comando
 * {@link ManejadorGuardarReceta}.
 */
public final class RecetasServicio {

    private final RepositorioRecetas recetas;
    private final RepositorioMenu menu;
    private final InventarioPuerto inventario;
    private final Clock reloj;

    /**
     * Arma el servicio.
     *
     * @param recetas    almacen de recetas
     * @param menu       almacen del menu, para el precio y el modo
     * @param inventario nombres y costos de los articulos
     * @param reloj      reloj de la plataforma
     */
    public RecetasServicio(RepositorioRecetas recetas, RepositorioMenu menu, InventarioPuerto inventario, Clock reloj) {
        this.recetas = Objects.requireNonNull(recetas, "recetas");
        this.menu = Objects.requireNonNull(menu, "menu");
        this.inventario = Objects.requireNonNull(inventario, "inventario");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    /**
     * La receta de un platillo, sin costos.
     *
     * @throws RestaurantesException RECETA_NO_ENCONTRADA si no tiene
     */
    public RecetaVista receta(ContextoNegocio contexto, Autorizacion autorizacion, UUID platilloId) {
        autorizacion.exigir(PermisosDeRestaurantes.RECETA_VER);
        return vista(contexto, recetaDe(contexto, platilloId));
    }

    /**
     * El costo teorico y el margen de la version vigente.
     *
     * @throws RestaurantesException RECETA_NO_ENCONTRADA si no tiene receta activa
     */
    public CostoDeReceta costoDeReceta(ContextoNegocio contexto, Autorizacion autorizacion, UUID platilloId) {
        autorizacion.exigir(PermisosDeRestaurantes.COSTO_VER);
        var receta = recetaDe(contexto, platilloId);
        var version = receta.versionVigente().orElseThrow(() -> new RestaurantesException(CodigoError.RECETA_NO_ENCONTRADA));
        var platillo = menu.platillo(contexto.negocioId(), platilloId)
                .orElseThrow(() -> new RestaurantesException(CodigoError.PLATILLO_NO_ENCONTRADO));
        var ids = version.insumos().stream().map(i -> i.articuloId()).collect(Collectors.toSet());
        var articulos = articulos(contexto, ids);
        var bases = new HashMap<UUID, CantidadEnBase>();
        for (var insumo : version.insumos()) {
            var articulo = articulos.get(insumo.articuloId());
            if (articulo == null || !articulo.activo()) {
                continue;
            }
            try {
                inventario.aBase(contexto, List.of(new LineaDeInsumo(insumo.articuloId(), insumo.cantidad(), insumo.unidad())))
                        .forEach(b -> bases.put(b.articuloId(), b));
            } catch (FalloDeInventario sinConversion) {
                if (sinConversion.reintentable()) {
                    throw new RestaurantesException(CodigoError.INVENTARIO_NO_DISPONIBLE);
                }
            }
        }
        return CalculadoraDeCosto.calcular(platilloId, version, platillo.precio(), articulos, bases);
    }

    /**
     * Desactiva la receta (RC-09): cierra su version abierta. Falla si el platillo descuenta por receta.
     *
     * @throws RestaurantesException RECETA_NO_ENCONTRADA o RECETA_REQUERIDA si el platillo esta en modo RECETA
     */
    public RecetaVista desactivarReceta(ContextoNegocio contexto, Autorizacion autorizacion, UUID platilloId) {
        autorizacion.exigir(PermisosDeRestaurantes.RECETA_EDITAR);
        var receta = recetaDe(contexto, platilloId);
        var platillo = menu.platillo(contexto.negocioId(), platilloId).orElse(null);
        if (platillo != null && platillo.modo() == ModoInventario.RECETA) {
            throw new RestaurantesException(CodigoError.RECETA_REQUERIDA);
        }
        var desactivada = receta.desactivada(reloj.instant());
        if (!recetas.reemplazar(desactivada, receta.version())) {
            throw new RestaurantesException(CodigoError.CONCURRENCIA);
        }
        return vista(contexto, desactivada);
    }

    private Receta recetaDe(ContextoNegocio contexto, UUID platilloId) {
        return recetas.porPlatillo(contexto.negocioId(), platilloId)
                .orElseThrow(() -> new RestaurantesException(CodigoError.RECETA_NO_ENCONTRADA));
    }

    private RecetaVista vista(ContextoNegocio contexto, Receta receta) {
        RecetaVersion version = receta.versionVigente().or(receta::ultimaVersion)
                .orElseThrow(() -> new RestaurantesException(CodigoError.RECETA_NO_ENCONTRADA));
        var articulos = articulos(contexto, version.insumos().stream().map(i -> i.articuloId()).collect(Collectors.toSet()));
        var insumos = version.insumos().stream().map(i -> new InsumoVista(i.articuloId(),
                articulos.containsKey(i.articuloId()) ? articulos.get(i.articuloId()).nombre() : null, i.cantidad(),
                i.unidad(), i.mermaPct())).toList();
        return new RecetaVista(receta.platilloId(), version.numero(), version.rendimiento(), receta.activa(),
                version.vigenteDesde(), insumos);
    }

    private Map<UUID, ArticuloDeInventario> articulos(ContextoNegocio contexto, java.util.Set<UUID> ids) {
        try {
            return inventario.articulos(contexto, new HashSet<>(ids));
        } catch (FalloDeInventario fallo) {
            throw new RestaurantesException(CodigoError.INVENTARIO_NO_DISPONIBLE);
        }
    }
}
