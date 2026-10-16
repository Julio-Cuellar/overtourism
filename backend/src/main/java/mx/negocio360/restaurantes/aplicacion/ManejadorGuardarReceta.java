package mx.negocio360.restaurantes.aplicacion;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.compartido.comandos.ManejadorComando;
import mx.negocio360.compartido.comandos.ResultadoComando;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.restaurantes.dominio.CodigoError;
import mx.negocio360.restaurantes.dominio.InsumoDeReceta;
import mx.negocio360.restaurantes.dominio.Platillo;
import mx.negocio360.restaurantes.dominio.Receta;
import mx.negocio360.restaurantes.dominio.RecetaVersion;
import mx.negocio360.restaurantes.dominio.RestaurantesException;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;

/**
 * RC-01 a RC-08 GuardarReceta (D-165): valida la receta contra Inventario, calcula el costo teorico y el margen y los
 * muestra al preparar; al confirmar revalida con las cifras de ese momento y, si el resumen ya no es el que se mostro,
 * {@code COMANDO_CAMBIO}. Crea la version siguiente y cierra la anterior en el mismo instante. El permiso
 * {@code restaurantes.receta.editar} se exige en los dos pasos; las cifras de costo solo se muestran a quien tiene
 * {@code restaurantes.costo.ver} (RC-10).
 */
public final class ManejadorGuardarReceta implements ManejadorComando<GuardarRecetaComando> {

    /** Nombre estable del comando en {@code preparacion.comando_tipo}. */
    public static final String TIPO = "guardar_receta";

    private final RepositorioMenu menu;
    private final RepositorioRecetas recetas;
    private final InventarioPuerto inventario;
    private final AutorizacionDeRestaurantes autorizacion;
    private final Clock reloj;

    /** Todo lo que se calcula al preparar y se vuelve a calcular al confirmar. */
    private record Calculo(Platillo platillo, List<InsumoDeReceta> insumos, RecetaVersion version, String resumen) {
    }

    /**
     * Arma el manejador.
     *
     * @param menu         almacen del menu
     * @param recetas      almacen de recetas
     * @param inventario   articulos, unidades y costos
     * @param autorizacion permisos de la sesion
     * @param reloj        reloj de la plataforma
     */
    public ManejadorGuardarReceta(RepositorioMenu menu, RepositorioRecetas recetas, InventarioPuerto inventario,
                                  AutorizacionDeRestaurantes autorizacion, Clock reloj) {
        this.menu = Objects.requireNonNull(menu, "menu");
        this.recetas = Objects.requireNonNull(recetas, "recetas");
        this.inventario = Objects.requireNonNull(inventario, "inventario");
        this.autorizacion = Objects.requireNonNull(autorizacion, "autorizacion");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public String tipo() {
        return TIPO;
    }

    @Override
    public Class<GuardarRecetaComando> claseComando() {
        return GuardarRecetaComando.class;
    }

    /**
     * Valida y muestra la receta con su costo, sin escribir.
     *
     * @throws RestaurantesException RECETA_INVALIDA, INSUMO_REPETIDO, PLATILLO_NO_ENCONTRADO, ARTICULO_NO_ENCONTRADO,
     *                               ARTICULO_INACTIVO, SERIE_EN_RECETA, UNIDAD_NO_CONVERTIBLE o INVENTARIO_NO_DISPONIBLE
     */
    @Override
    public String preparar(ContextoNegocio contexto, GuardarRecetaComando comando) {
        return calcular(contexto, comando).resumen();
    }

    /**
     * Guarda la version siguiente.
     *
     * @throws RestaurantesException COMANDO_CAMBIO si el resumen cambio, CONCURRENCIA, o los de preparar
     */
    @Override
    public ResultadoComando confirmar(ContextoNegocio contexto, GuardarRecetaComando comando, String resumenPreparado) {
        var calculo = calcular(contexto, comando);
        if (!calculo.resumen().equals(resumenPreparado)) {
            throw new RestaurantesException(CodigoError.COMANDO_CAMBIO);
        }
        var ahora = reloj.instant();
        var platilloId = calculo.platillo().id();
        var existente = recetas.porPlatillo(contexto.negocioId(), platilloId);
        Receta guardada;
        if (existente.isPresent()) {
            guardada = existente.get().conNuevaVersion(comando.rendimiento(), calculo.insumos(), contexto.actor(), ahora);
            if (!recetas.reemplazar(guardada, existente.get().version())) {
                throw new RestaurantesException(CodigoError.CONCURRENCIA);
            }
        } else {
            guardada = Receta.nueva(UuidV7.generar(ahora), contexto.negocioId(), platilloId, comando.rendimiento(),
                    calculo.insumos(), contexto.actor(), ahora);
            recetas.insertar(guardada);
        }
        return ResultadoComando.ok(PlantillasRestaurantes.RECETA_GUARDADA, platilloId,
                Map.of("platillo_id", platilloId.toString(), "version", Integer.toString(calculo.version().numero())));
    }

    private Calculo calcular(ContextoNegocio contexto, GuardarRecetaComando comando) {
        autorizacion.exigir(contexto, PermisosDeRestaurantes.RECETA_EDITAR);
        if (comando.platilloId() == null) {
            throw new RestaurantesException(CodigoError.PLATILLO_NO_ENCONTRADO);
        }
        var platillo = menu.platillo(contexto.negocioId(), comando.platilloId()).filter(Platillo::activo)
                .orElseThrow(() -> new RestaurantesException(CodigoError.PLATILLO_NO_ENCONTRADO));
        var insumos = insumosDe(comando);
        Receta.exigirValida(comando.rendimiento(), insumos);
        var ahora = reloj.instant();
        var numero = recetas.porPlatillo(contexto.negocioId(), platillo.id()).flatMap(Receta::ultimaVersion)
                .map(v -> v.numero() + 1).orElse(1);
        var version = new RecetaVersion(numero, comando.rendimiento(), ahora, null, insumos, contexto.actor());
        var articulos = articulosDeInventario(contexto, insumos);
        var bases = basesDe(contexto, insumos);
        var nombres = insumos.stream().map(i -> articulos.get(i.articuloId()).nombre()).toList();
        var costo = autorizacion.puede(contexto, PermisosDeRestaurantes.COSTO_VER)
                ? CalculadoraDeCosto.calcular(platillo.id(), version, platillo.precio(), articulos, bases) : null;
        return new Calculo(platillo, insumos, version, PlantillasRestaurantes.resumenReceta(platillo.nombre(), version, nombres, costo));
    }

    private static List<InsumoDeReceta> insumosDe(GuardarRecetaComando comando) {
        if (comando.insumos() == null) {
            throw new RestaurantesException(CodigoError.RECETA_INVALIDA);
        }
        return comando.insumos().stream().map(i -> {
            if (i == null) {
                throw new RestaurantesException(CodigoError.RECETA_INVALIDA);
            }
            return new InsumoDeReceta(i.articuloId(), i.cantidad(), i.unidad(), i.mermaPct());
        }).toList();
    }

    /** Los articulos deben existir, estar activos y no ser de series (RC-03). */
    private Map<UUID, ArticuloDeInventario> articulosDeInventario(ContextoNegocio contexto, List<InsumoDeReceta> insumos) {
        var ids = insumos.stream().map(InsumoDeReceta::articuloId).collect(java.util.stream.Collectors.toSet());
        Map<UUID, ArticuloDeInventario> articulos;
        try {
            articulos = inventario.articulos(contexto, ids);
        } catch (FalloDeInventario fallo) {
            throw traducir(fallo);
        }
        for (var insumo : insumos) {
            var articulo = articulos.get(insumo.articuloId());
            if (articulo == null) {
                throw new RestaurantesException(CodigoError.ARTICULO_NO_ENCONTRADO);
            }
            if (!articulo.activo()) {
                throw new RestaurantesException(CodigoError.ARTICULO_INACTIVO);
            }
            if (articulo.rastreo() == Rastreo.SERIE) {
                throw new RestaurantesException(CodigoError.SERIE_EN_RECETA);
            }
        }
        return articulos;
    }

    /** Cada cantidad debe poder llevarse a la unidad base de su articulo (RC-02). */
    private Map<UUID, CantidadEnBase> basesDe(ContextoNegocio contexto, List<InsumoDeReceta> insumos) {
        try {
            var lineas = insumos.stream().map(i -> new LineaDeInsumo(i.articuloId(), i.cantidad(), i.unidad())).toList();
            var respuesta = new HashMap<UUID, CantidadEnBase>();
            inventario.aBase(contexto, lineas).forEach(b -> respuesta.put(b.articuloId(), b));
            return respuesta;
        } catch (FalloDeInventario fallo) {
            throw traducir(fallo);
        }
    }

    private static RestaurantesException traducir(FalloDeInventario fallo) {
        if (fallo.reintentable()) {
            return new RestaurantesException(CodigoError.INVENTARIO_NO_DISPONIBLE);
        }
        return switch (fallo.codigo()) {
            case "ARTICULO_NO_ENCONTRADO" -> new RestaurantesException(CodigoError.ARTICULO_NO_ENCONTRADO);
            case "ARTICULO_INACTIVO" -> new RestaurantesException(CodigoError.ARTICULO_INACTIVO);
            case "UNIDAD_NO_CONVERTIBLE" -> new RestaurantesException(CodigoError.UNIDAD_NO_CONVERTIBLE);
            default -> new RestaurantesException(CodigoError.RECETA_INVALIDA);
        };
    }
}
