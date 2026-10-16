package mx.negocio360.inventario.aplicacion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.inventario.InventarioApi;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioArticulos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioMovimientos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioSaldos;
import mx.negocio360.inventario.dominio.CodigoError;
import mx.negocio360.inventario.dominio.InventarioException;
import mx.negocio360.inventario.dominio.Movimiento;

/** Implementacion de {@link InventarioApi}: reparte las escrituras a preparar -> confirmar y resuelve consultas. */
public final class InventarioServicio implements InventarioApi {

    static final int MAX_POR_PAGINA = 100;
    static final int MAX_TEXTO_BUSQUEDA = 80;
    static final int MAX_LINEAS_A_BASE = 100;

    private final ServicioPreparacion preparacion;
    private final RepositorioArticulos articulos;
    private final RepositorioSaldos saldos;
    private final RepositorioMovimientos movimientos;
    private final ConsumosServicio consumo;

    /** Arma el servicio; {@code consumo} debe compartir la secuencia de ids con los manejadores de {@code preparacion}. */
    public InventarioServicio(ServicioPreparacion preparacion, RepositorioArticulos articulos,
                              RepositorioSaldos saldos, RepositorioMovimientos movimientos,
                              ConsumosServicio consumo) {
        this.preparacion = Objects.requireNonNull(preparacion, "preparacion");
        this.articulos = Objects.requireNonNull(articulos, "articulos");
        this.saldos = Objects.requireNonNull(saldos, "saldos");
        this.movimientos = Objects.requireNonNull(movimientos, "movimientos");
        this.consumo = Objects.requireNonNull(consumo, "consumo");
    }

    @Override
    public PreparacionCreada preparar(ContextoNegocio contexto, Comando comando) {
        return preparacion.preparar(contexto, comando);
    }

    @Override
    public ResultadoComando confirmar(ContextoNegocio contexto, String token) {
        return preparacion.confirmar(contexto, token);
    }

    @Override
    public Optional<ArticuloVista> articulo(ContextoNegocio contexto, UUID articuloId) {
        if (articuloId == null) {
            return Optional.empty();
        }
        return articulos.porId(contexto.negocioId(), articuloId).map(articulo -> vista(new ArticuloConSaldo(articulo,
                saldos.porArticulo(contexto.negocioId(), articulo.id()).orElse(null))));
    }

    @Override
    public Pagina<ArticuloVista> articulos(ContextoNegocio contexto, String texto, boolean soloAlertas, int primeros,
                                           String despues) {
        if (texto != null && texto.strip().length() > MAX_TEXTO_BUSQUEDA) {
            throw new IllegalArgumentException("El texto de busqueda excede el maximo permitido");
        }
        var limite = paginaEfectiva(primeros);
        var cursor = leerCursor(despues);
        var filas = articulos.buscar(contexto.negocioId(), texto, soloAlertas, cursor, limite + 1);
        var nodos = filas.stream().limit(limite).map(InventarioServicio::vista).toList();
        var ultimo = nodos.isEmpty() ? null : nodos.get(nodos.size() - 1).id().toString();
        return new Pagina<>(nodos, filas.size() > limite, ultimo);
    }

    @Override
    public Pagina<MovimientoVista> kardex(ContextoNegocio contexto, UUID articuloId, int primeros, String despues) {
        var limite = paginaEfectiva(primeros);
        var cursor = leerCursor(despues);
        Operaciones.existente(articulos, contexto, articuloId);
        var filas = movimientos.kardex(contexto.negocioId(), articuloId, cursor, limite + 1);
        var nodos = filas.stream().limit(limite).map(InventarioServicio::vista).toList();
        var ultimo = nodos.isEmpty() ? null : nodos.get(nodos.size() - 1).id().toString();
        return new Pagina<>(nodos, filas.size() > limite, ultimo);
    }

    @Override
    public Map<UUID, ArticuloBase> articulosPorId(ContextoNegocio contexto, Set<UUID> ids) {
        if (ids.size() > MAX_LINEAS_A_BASE) {
            throw new IllegalArgumentException("Se pueden pedir como maximo " + MAX_LINEAS_A_BASE + " articulos");
        }
        var respuesta = new LinkedHashMap<UUID, ArticuloBase>();
        for (var id : ids) {
            articulos.porId(contexto.negocioId(), id).ifPresent(articulo -> {
                var cpp = saldos.porArticulo(contexto.negocioId(), articulo.id()).map(s -> s.cpp())
                        .orElse(java.math.BigDecimal.ZERO);
                respuesta.put(id, new ArticuloBase(articulo.id(), articulo.nombre(), articulo.unidadBase().codigo(),
                        cpp, articulo.activo(), articulo.rastreo()));
            });
        }
        return Map.copyOf(respuesta);
    }

    @Override
    public List<CantidadEnBase> aBase(ContextoNegocio contexto, List<CantidadPedida> lineas) {
        if (lineas.size() > MAX_LINEAS_A_BASE) {
            throw new IllegalArgumentException("Se pueden convertir como maximo " + MAX_LINEAS_A_BASE + " cantidades");
        }
        var resultado = new ArrayList<CantidadEnBase>(lineas.size());
        for (var linea : lineas) {
            var articulo = Operaciones.activo(articulos, contexto, linea.articuloId());
            Operaciones.exigirCantidadCapturada(linea.cantidad(), false);
            var conversion = articulo.aBase(linea.cantidad(), linea.unidad());
            if (conversion.cantidadBase().signum() <= 0) {
                throw new InventarioException(CodigoError.CANTIDAD_INVALIDA);
            }
            resultado.add(new CantidadEnBase(articulo.id(), conversion.cantidadBase(),
                    articulo.unidadBase().codigo(), conversion.factorAplicado()));
        }
        return List.copyOf(resultado);
    }

    @Override
    public ConsumoResultado registrarConsumo(ContextoNegocio contexto, String consumidor, UUID causaId,
                                             Instant ocurridoEn, List<LineaDeConsumo> lineas) {
        return consumo.registrar(contexto, consumidor, causaId, ocurridoEn, lineas);
    }

    @Override
    public ConsumoResultado revertirConsumo(ContextoNegocio contexto, String consumidor, UUID causaId) {
        return consumo.revertir(contexto, consumidor, causaId);
    }

    private static ArticuloVista vista(ArticuloConSaldo fila) {
        var articulo = fila.articulo();
        var saldo = fila.saldoOInicial();
        return new ArticuloVista(articulo.id(), articulo.nombre(), articulo.categoria(),
                articulo.unidadBase().codigo(), articulo.minimoBase(), articulo.unidadesPropias(),
                saldo.cantidadBase(), saldo.cpp(), saldo.valor(), articulo.activo(), fila.bajoMinimo(),
                articulo.version());
    }

    private static MovimientoVista vista(Movimiento m) {
        return new MovimientoVista(m.id(), m.tipo(), m.causaTipo(), m.motivo(), m.cantidadCapturada(),
                m.unidadCapturada(), m.factorAplicado(), m.cantidadBase(), m.costoUnitario(), m.costoTotal(),
                m.cppResultante(), m.saldoResultante(), m.ocurridoEn(), m.registradoPor());
    }

    private static int paginaEfectiva(int pedido) {
        if (pedido < 1) {
            throw new IllegalArgumentException("El tamano de pagina debe ser mayor que cero: " + pedido);
        }
        return Math.min(pedido, MAX_POR_PAGINA);
    }

    private static UUID leerCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        var texto = cursor.strip();
        try {
            var id = UUID.fromString(texto);
            // UUID.fromString acepta formas abreviadas ("1-1-1-1-1"): solo se admite la canonica.
            if (id.toString().equals(texto.toLowerCase(Locale.ROOT))) {
                return id;
            }
        } catch (IllegalArgumentException e) {
            // Se responde igual que con una forma abreviada.
        }
        throw new IllegalArgumentException("El cursor no es valido");
    }
}
