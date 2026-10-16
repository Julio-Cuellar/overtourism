package mx.negocio360.restaurantes.aplicacion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;

/**
 * Un Inventario de mentiras para las pruebas: articulos con su unidad base y su CPP, conversiones g/kg y ml/l, y consumos
 * que se anotan y se pueden hacer fallar. Cumple el contrato de {@link InventarioPuerto}; el adaptador real es de la Pista de
 * Inventario (R-I1 a R-I4).
 */
final class InventarioDePrueba implements InventarioPuerto {

    record Articulo(UUID id, String nombre, String unidadBase, BigDecimal cpp, boolean activo, Rastreo rastreo) {
    }

    record ConsumoAnotado(UUID negocioId, String consumidor, UUID causaId, Instant ocurridoEn, List<LineaDeConsumo> lineas) {
    }

    private static final Map<String, BigDecimal> FACTOR = Map.of("g", BigDecimal.ONE, "kg", new BigDecimal("1000"),
            "ml", BigDecimal.ONE, "l", new BigDecimal("1000"), "pieza", BigDecimal.ONE);

    final Map<UUID, Articulo> articulos = new HashMap<>();
    final List<ConsumoAnotado> consumos = new ArrayList<>();
    final Set<UUID> revertidas = new HashSet<>();
    final Map<String, Integer> llamadas = new LinkedHashMap<>();
    RuntimeException fallaAlConsumir;

    UUID articulo(String nombre, String unidadBase, String cpp) {
        var id = UUID.randomUUID();
        articulos.put(id, new Articulo(id, nombre, unidadBase, cpp == null ? null : new BigDecimal(cpp), true, Rastreo.NINGUNO));
        return id;
    }

    void reemplazar(Articulo a) {
        articulos.put(a.id(), a);
    }

    private void llamo(String nombre) {
        llamadas.merge(nombre, 1, Integer::sum);
    }

    @Override
    public Map<UUID, ArticuloDeInventario> articulos(ContextoNegocio contexto, Set<UUID> ids) {
        llamo("articulos");
        var respuesta = new HashMap<UUID, ArticuloDeInventario>();
        for (var id : ids) {
            var a = articulos.get(id);
            if (a != null) {
                respuesta.put(id, new ArticuloDeInventario(a.id(), a.nombre(), a.unidadBase(), a.cpp(), a.activo(), a.rastreo()));
            }
        }
        return respuesta;
    }

    @Override
    public List<CantidadEnBase> aBase(ContextoNegocio contexto, List<LineaDeInsumo> lineas) {
        llamo("aBase");
        var respuesta = new ArrayList<CantidadEnBase>();
        for (var linea : lineas) {
            var a = articulos.get(linea.articuloId());
            if (a == null) {
                throw new FalloDeInventario("ARTICULO_NO_ENCONTRADO", false);
            }
            var origen = FACTOR.get(linea.unidad());
            var base = FACTOR.get(a.unidadBase());
            var mismaFamilia = origen != null && (linea.unidad().equals(a.unidadBase())
                    || (esMasa(linea.unidad()) && esMasa(a.unidadBase())) || (esVolumen(linea.unidad()) && esVolumen(a.unidadBase())));
            if (!mismaFamilia) {
                throw new FalloDeInventario("UNIDAD_NO_CONVERTIBLE", false);
            }
            respuesta.add(new CantidadEnBase(a.id(), linea.cantidad().multiply(origen).divide(base, 4, java.math.RoundingMode.HALF_UP),
                    a.unidadBase()));
        }
        return respuesta;
    }

    private static boolean esMasa(String u) {
        return u.equals("g") || u.equals("kg");
    }

    private static boolean esVolumen(String u) {
        return u.equals("ml") || u.equals("l");
    }

    @Override
    public ResultadoDeConsumo registrarConsumo(ContextoNegocio contexto, String consumidor, UUID causaId, Instant ocurridoEn,
                                               List<LineaDeConsumo> lineas) {
        llamo("registrarConsumo");
        if (fallaAlConsumir != null) {
            throw fallaAlConsumir;
        }
        var previo = consumos.stream().filter(c -> c.negocioId().equals(contexto.negocioId()) && c.consumidor().equals(consumidor)
                && c.causaId().equals(causaId)).findFirst();
        if (revertidas.contains(causaId)) {
            return ResultadoDeConsumo.OMITIDO_ANULADO_PREVIO;
        }
        if (previo.isPresent()) {
            return ResultadoDeConsumo.YA_REGISTRADO;
        }
        consumos.add(new ConsumoAnotado(contexto.negocioId(), consumidor, causaId, ocurridoEn, List.copyOf(lineas)));
        return ResultadoDeConsumo.REGISTRADO;
    }

    @Override
    public ResultadoDeConsumo revertirConsumo(ContextoNegocio contexto, String consumidor, UUID causaId) {
        llamo("revertirConsumo");
        if (fallaAlConsumir != null) {
            throw fallaAlConsumir;
        }
        if (!revertidas.add(causaId)) {
            return ResultadoDeConsumo.YA_REVERTIDO;
        }
        var habia = consumos.removeIf(c -> c.negocioId().equals(contexto.negocioId()) && c.consumidor().equals(consumidor)
                && c.causaId().equals(causaId));
        return habia ? ResultadoDeConsumo.REVERTIDO : ResultadoDeConsumo.MARCADO_ANULADO_PREVIO;
    }
}
