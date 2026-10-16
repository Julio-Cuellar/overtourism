package mx.negocio360.restaurantes.infraestructura;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.inventario.InventarioApi;
import mx.negocio360.inventario.aplicacion.ArticuloBase;
import mx.negocio360.inventario.aplicacion.CantidadPedida;
import mx.negocio360.inventario.dominio.CodigoError;
import mx.negocio360.inventario.dominio.InventarioException;
import mx.negocio360.restaurantes.aplicacion.ArticuloDeInventario;
import mx.negocio360.restaurantes.aplicacion.CantidadEnBase;
import mx.negocio360.restaurantes.aplicacion.FalloDeInventario;
import mx.negocio360.restaurantes.aplicacion.InventarioPuerto;
import mx.negocio360.restaurantes.aplicacion.LineaDeConsumo;
import mx.negocio360.restaurantes.aplicacion.LineaDeInsumo;
import mx.negocio360.restaurantes.aplicacion.Rastreo;
import mx.negocio360.restaurantes.aplicacion.ResultadoDeConsumo;

/**
 * Conecta Restaurantes con Inventario: traduce el puerto propio {@link InventarioPuerto} a {@link InventarioApi} y los
 * errores de Inventario a {@link FalloDeInventario}. Es el unico lugar de Restaurantes que conoce los tipos de Inventario
 * (R-I1 a R-I4, D-09). Los errores {@code CONCURRENCIA} y {@code ERROR_TEMPORAL} son reintentables; los demas, permanentes
 * (6.5 de inventario.md). Inventario limita a 100 ids o lineas por llamada, asi que las consultas largas se piden por tandas.
 */
public final class InventarioApiComoPuerto implements InventarioPuerto {

    /** Maximo de ids o lineas por llamada a {@code articulosPorId} y {@code aBase}. */
    static final int TANDA = 100;

    private final InventarioApi inventario;

    /**
     * Arma el adaptador.
     *
     * @param inventario API publica de Inventario
     */
    public InventarioApiComoPuerto(InventarioApi inventario) {
        this.inventario = Objects.requireNonNull(inventario, "inventario");
    }

    @Override
    public Map<UUID, ArticuloDeInventario> articulos(ContextoNegocio contexto, Set<UUID> ids) {
        var respuesta = new HashMap<UUID, ArticuloDeInventario>();
        for (var tanda : tandas(List.copyOf(ids))) {
            var encontrados = llamar(() -> inventario.articulosPorId(contexto, Set.copyOf(tanda)));
            encontrados.forEach((id, a) -> respuesta.put(id, traducir(a)));
        }
        return Map.copyOf(respuesta);
    }

    @Override
    public List<CantidadEnBase> aBase(ContextoNegocio contexto, List<LineaDeInsumo> lineas) {
        var respuesta = new ArrayList<CantidadEnBase>(lineas.size());
        for (var tanda : tandas(lineas)) {
            var pedidas = tanda.stream().map(l -> new CantidadPedida(l.articuloId(), l.cantidad(), l.unidad())).toList();
            llamar(() -> inventario.aBase(contexto, pedidas)).forEach(c ->
                    respuesta.add(new CantidadEnBase(c.articuloId(), c.cantidadBase(), c.unidadBase())));
        }
        return List.copyOf(respuesta);
    }

    @Override
    public ResultadoDeConsumo registrarConsumo(ContextoNegocio contexto, String consumidor, UUID causaId,
                                               Instant ocurridoEn, List<LineaDeConsumo> lineas) {
        var traducidas = lineas.stream()
                .map(l -> new mx.negocio360.inventario.aplicacion.LineaDeConsumo(l.articuloId(), l.cantidad(), l.unidad()))
                .toList();
        var resultado = llamar(() -> inventario.registrarConsumo(contexto, consumidor, causaId, ocurridoEn, traducidas));
        return ResultadoDeConsumo.valueOf(resultado.estado().name());
    }

    @Override
    public ResultadoDeConsumo revertirConsumo(ContextoNegocio contexto, String consumidor, UUID causaId) {
        var resultado = llamar(() -> inventario.revertirConsumo(contexto, consumidor, causaId));
        return ResultadoDeConsumo.valueOf(resultado.estado().name());
    }

    private static ArticuloDeInventario traducir(ArticuloBase a) {
        return new ArticuloDeInventario(a.id(), a.nombre(), a.unidadBase(), a.cpp(), a.activo(),
                Rastreo.valueOf(a.rastreo().name()));
    }

    private static <T> List<List<T>> tandas(List<T> elementos) {
        var tandas = new ArrayList<List<T>>();
        for (var desde = 0; desde < elementos.size(); desde += TANDA) {
            tandas.add(elementos.subList(desde, Math.min(desde + TANDA, elementos.size())));
        }
        return tandas;
    }

    /** Ejecuta la llamada y traduce el error de Inventario; un limite superado es una cantidad invalida y permanente. */
    private static <T> T llamar(Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (InventarioException e) {
            var reintentable = e.codigo() == CodigoError.CONCURRENCIA || e.codigo() == CodigoError.ERROR_TEMPORAL;
            throw new FalloDeInventario(e.codigo().name(), reintentable);
        } catch (IllegalArgumentException e) {
            throw new FalloDeInventario(CodigoError.CANTIDAD_INVALIDA.name(), false);
        }
    }
}
