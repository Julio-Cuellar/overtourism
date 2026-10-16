package mx.negocio360.restaurantes.aplicacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.dinero.Redondeo;
import mx.negocio360.compartido.eventos.MapeadorEventos;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.restaurantes.contratos.VentaAnuladaV1;
import mx.negocio360.restaurantes.contratos.VentaRegistradaV1;
import mx.negocio360.restaurantes.dominio.ModoInventario;
import mx.negocio360.restaurantes.dominio.Platillo;
import mx.negocio360.restaurantes.dominio.Receta;
import mx.negocio360.restaurantes.dominio.RecetaVersion;

/**
 * El consumo de insumos por venta (CM-01 a CM-08, D-166). Con {@code venta.registrada} toma, de las lineas que son platillos
 * de este negocio en modo RECETA, la version de receta vigente en el {@code ocurrido_en} de la venta (RC-07), suma por
 * articulo {@code cantidad × (1 + merma/100) × vendidas ÷ rendimiento}, redondea una sola vez a 4 decimales (CM-05) y hace
 * <b>una</b> llamada a Inventario con consumidor {@code restaurantes} y la venta como causa (CM-02). Con
 * {@code venta.anulada} manda la reversa (CM-04). Inventario es atomico e idempotente (K-01 a K-04); aqui la idempotencia
 * es ademas por evento (R3).
 *
 * <p>Una venta nunca se bloquea por un error de inventario (A-01): un fallo permanente se guarda como consumo fallido, se
 * avisa con {@code restaurantes.consumo_fallido} y no se reintenta; uno reintentable no se marca y el evento vuelve a llegar
 * (D-122). Es un lector tolerante (R5): lo que no cumple el contrato se descarta sin lanzar.
 */
public final class ConsumidorDeVentas {

    /** Nombre con el que Restaurantes consume en Inventario (llave de idempotencia, D-18). */
    public static final String CONSUMIDOR = "restaurantes";
    /** Nombre con el que lleva su libro de eventos procesados. */
    static final String LIBRO = "restaurantes.ventas";
    /** Codigo del fallo cuando no hay receta vigente en el momento de la venta. */
    public static final String SIN_RECETA = "SIN_RECETA";
    /** Tipo del evento con el que se avisa de un fallo. */
    public static final String TIPO_FALLO = "restaurantes.consumo_fallido";
    private static final String ORIGEN_VENTAS = "ventas";
    private static final int ESCALA_INTERNA = 8;
    private static final int MAX_LINEAS_A_BASE = 100;
    private static final BigDecimal CIEN = new BigDecimal("100");

    private final RepositorioMenu menu;
    private final RepositorioRecetas recetas;
    private final InventarioPuerto inventario;
    private final RepositorioConsumos consumos;
    private final PublicadorEventos eventos;
    private final Clock reloj;
    private final ObjectMapper mapeador = MapeadorEventos.crear();

    /** Un platillo vendido y cuantas porciones. */
    private record Vendido(Platillo platillo, BigDecimal cantidad) {
    }

    /**
     * Arma el consumidor.
     *
     * @param menu       almacen del menu
     * @param recetas    almacen de recetas
     * @param inventario consumos de Inventario
     * @param consumos   consumos fallidos y eventos procesados
     * @param eventos    outbox
     * @param reloj      reloj de la plataforma
     */
    public ConsumidorDeVentas(RepositorioMenu menu, RepositorioRecetas recetas, InventarioPuerto inventario,
                              RepositorioConsumos consumos, PublicadorEventos eventos, Clock reloj) {
        this.menu = Objects.requireNonNull(menu, "menu");
        this.recetas = Objects.requireNonNull(recetas, "recetas");
        this.inventario = Objects.requireNonNull(inventario, "inventario");
        this.consumos = Objects.requireNonNull(consumos, "consumos");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    /**
     * Procesa un evento del bus. Nunca lanza por datos malos.
     *
     * @param evento sobre recibido
     * @return que se hizo; si {@link ResultadoConsumo#reintentable()} es verdadero, el evento no se marco
     */
    public ResultadoConsumo procesar(SobreEvento evento) {
        Objects.requireNonNull(evento, "evento");
        return switch (evento.tipo()) {
            case VentaRegistradaV1.TIPO -> leer(evento, VentaRegistradaV1.class)
                    .map(venta -> registrada(evento, venta)).orElse(ResultadoConsumo.DATOS_INVALIDOS);
            case VentaAnuladaV1.TIPO -> leer(evento, VentaAnuladaV1.class)
                    .map(venta -> anulada(evento, venta)).orElse(ResultadoConsumo.DATOS_INVALIDOS);
            default -> ResultadoConsumo.IGNORADO;
        };
    }

    // ---- venta registrada ----

    private ResultadoConsumo registrada(SobreEvento evento, VentaRegistradaV1 venta) {
        if (consumos.yaProcesado(LIBRO, evento.id())) {
            return ResultadoConsumo.YA_PROCESADO;
        }
        var vendidos = vendidosConReceta(evento.negocioId(), venta);
        if (vendidos.isEmpty()) {
            return ResultadoConsumo.IGNORADO;
        }
        var contexto = new ContextoNegocio(evento.negocioId(), CONSUMIDOR);
        var versiones = new LinkedHashMap<UUID, RecetaVersion>();
        var sinReceta = new ArrayList<LineaFallida>();
        for (var v : vendidos.values()) {
            var version = recetas.porPlatillo(evento.negocioId(), v.platillo().id())
                    .flatMap(r -> r.versionEn(evento.ocurridoEn()));
            if (version.isPresent()) {
                versiones.put(v.platillo().id(), version.get());
            } else {
                sinReceta.add(new LineaFallida(v.platillo().id(), v.cantidad()));
            }
        }
        if (versiones.isEmpty()) {
            fallo(evento, venta.ventaId(), SIN_RECETA, sinReceta);
            consumos.marcarProcesado(LIBRO, evento.id());
            return ResultadoConsumo.FALLIDA;
        }
        List<LineaDeConsumo> lineas;
        try {
            lineas = lineasDeConsumo(contexto, vendidos, versiones);
        } catch (FalloDeInventario fallo) {
            return alFallar(evento, venta.ventaId(), fallo, afectados(vendidos, versiones.keySet(), sinReceta));
        }
        ResultadoDeConsumo respuesta;
        try {
            respuesta = inventario.registrarConsumo(contexto, CONSUMIDOR, venta.ventaId(), evento.ocurridoEn(), lineas);
        } catch (FalloDeInventario fallo) {
            return alFallar(evento, venta.ventaId(), fallo, afectados(vendidos, versiones.keySet(), sinReceta));
        }
        if (!sinReceta.isEmpty()) {
            fallo(evento, venta.ventaId(), SIN_RECETA, sinReceta);
        }
        consumos.marcarProcesado(LIBRO, evento.id());
        return switch (respuesta) {
            case REGISTRADO -> sinReceta.isEmpty() ? ResultadoConsumo.APLICADO : ResultadoConsumo.APLICADA_CON_FALLOS;
            case YA_REGISTRADO -> ResultadoConsumo.YA_APLICADA;
            case OMITIDO_ANULADO_PREVIO -> ResultadoConsumo.OMITIDA_ANULADO_PREVIO;
            default -> ResultadoConsumo.FALLIDA;
        };
    }

    /** Los platillos de este negocio en modo RECETA que trae la venta, con la suma de porciones de cada uno. */
    private Map<UUID, Vendido> vendidosConReceta(UUID negocioId, VentaRegistradaV1 venta) {
        var ids = new java.util.HashSet<UUID>();
        venta.lineas().forEach(l -> ids.add(l.productoId()));
        var platillos = menu.platillos(negocioId, ids);
        var vendidos = new LinkedHashMap<UUID, Vendido>();
        for (var linea : venta.lineas()) {
            var platillo = platillos.get(linea.productoId());
            if (platillo != null && platillo.modo() == ModoInventario.RECETA) {
                vendidos.merge(platillo.id(), new Vendido(platillo, linea.cantidad()),
                        (a, b) -> new Vendido(a.platillo(), a.cantidad().add(b.cantidad())));
            }
        }
        return vendidos;
    }

    /** Arma las lineas de consumo: por articulo, una sola suma y un solo redondeo (CM-02, CM-05). */
    private List<LineaDeConsumo> lineasDeConsumo(ContextoNegocio contexto, Map<UUID, Vendido> vendidos,
                                                 Map<UUID, RecetaVersion> versiones) {
        var pedidas = new ArrayList<LineaDeInsumo>();
        var duenas = new ArrayList<UUID>();
        for (var entrada : versiones.entrySet()) {
            for (var insumo : entrada.getValue().insumos()) {
                pedidas.add(new LineaDeInsumo(insumo.articuloId(), insumo.cantidad(), insumo.unidad()));
                duenas.add(entrada.getKey());
            }
        }
        var bases = new ArrayList<CantidadEnBase>();
        for (int i = 0; i < pedidas.size(); i += MAX_LINEAS_A_BASE) {
            bases.addAll(inventario.aBase(contexto, pedidas.subList(i, Math.min(i + MAX_LINEAS_A_BASE, pedidas.size()))));
        }
        var sumas = new LinkedHashMap<UUID, BigDecimal>();
        var unidades = new LinkedHashMap<UUID, String>();
        for (int i = 0; i < pedidas.size(); i++) {
            var platilloId = duenas.get(i);
            var version = versiones.get(platilloId);
            var insumo = version.insumos().get(indiceDentro(duenas, i));
            var base = bases.get(i);
            var factor = BigDecimal.ONE.add(insumo.mermaPct().divide(CIEN, 4, Redondeo.MODO));
            var porcion = Redondeo.dividir(base.cantidadBase().multiply(factor), BigDecimal.valueOf(version.rendimiento()), ESCALA_INTERNA);
            sumas.merge(base.articuloId(), porcion.multiply(vendidos.get(platilloId).cantidad()), BigDecimal::add);
            unidades.putIfAbsent(base.articuloId(), base.unidadBase());
        }
        return sumas.entrySet().stream().map(s -> new LineaDeConsumo(s.getKey(), Redondeo.cantidad(s.getValue()),
                unidades.get(s.getKey()))).toList();
    }

    /** La posicion del insumo dentro de la version de su platillo, a partir de la posicion global. */
    private static int indiceDentro(List<UUID> duenas, int global) {
        var platillo = duenas.get(global);
        int primero = duenas.indexOf(platillo);
        return global - primero;
    }

    private List<LineaFallida> afectados(Map<UUID, Vendido> vendidos, java.util.Set<UUID> conVersion, List<LineaFallida> sinReceta) {
        var todos = new ArrayList<LineaFallida>();
        vendidos.values().stream().filter(v -> conVersion.contains(v.platillo().id()))
                .forEach(v -> todos.add(new LineaFallida(v.platillo().id(), v.cantidad())));
        todos.addAll(sinReceta);
        return todos;
    }

    private ResultadoConsumo alFallar(SobreEvento evento, UUID ventaId, FalloDeInventario fallo, List<LineaFallida> lineas) {
        if (fallo.reintentable()) {
            return ResultadoConsumo.REINTENTABLE;
        }
        fallo(evento, ventaId, fallo.codigo(), lineas);
        consumos.marcarProcesado(LIBRO, evento.id());
        return ResultadoConsumo.FALLIDA;
    }

    /** Guarda el consumo fallido y avisa con un evento que solo lleva la venta y el codigo (CM-08). */
    private void fallo(SobreEvento evento, UUID ventaId, String codigo, List<LineaFallida> lineas) {
        var ahora = reloj.instant();
        consumos.guardarFallido(new ConsumoFallido(UuidV7.generar(ahora), evento.negocioId(), ventaId, codigo, lineas,
                evento.ocurridoEn(), ahora));
        eventos.publicar(new SobreEvento(UuidV7.generar(ahora), TIPO_FALLO, 1, evento.negocioId(), "restaurantes", ahora,
                ventaId, evento.id(), Map.of("venta_id", ventaId.toString(), "codigo", codigo)));
    }

    // ---- venta anulada ----

    private ResultadoConsumo anulada(SobreEvento evento, VentaAnuladaV1 venta) {
        if (consumos.yaProcesado(LIBRO, evento.id())) {
            return ResultadoConsumo.YA_PROCESADO;
        }
        var ids = new java.util.HashSet<UUID>();
        venta.lineas().forEach(l -> ids.add(l.productoId()));
        var platillos = menu.platillos(evento.negocioId(), ids);
        var propios = platillos.values().stream().filter(p -> p.modo() == ModoInventario.RECETA
                || recetas.porPlatillo(evento.negocioId(), p.id()).filter(Receta::activa).isPresent()).toList();
        if (propios.isEmpty()) {
            return ResultadoConsumo.IGNORADO;
        }
        try {
            var respuesta = inventario.revertirConsumo(new ContextoNegocio(evento.negocioId(), CONSUMIDOR), CONSUMIDOR, venta.ventaId());
            consumos.marcarProcesado(LIBRO, evento.id());
            return switch (respuesta) {
                case REVERTIDO -> ResultadoConsumo.REVERTIDA;
                case YA_REVERTIDO -> ResultadoConsumo.YA_REVERTIDA;
                case MARCADO_ANULADO_PREVIO -> ResultadoConsumo.MARCADA_ANULADO_PREVIO;
                default -> ResultadoConsumo.FALLIDA;
            };
        } catch (FalloDeInventario fallo) {
            return alFallar(evento, venta.ventaId(), fallo,
                    propios.stream().map(p -> new LineaFallida(p.id(), BigDecimal.ZERO)).toList());
        }
    }

    /** Lee los datos con el contrato v1; vacio si el origen no es Ventas o los datos no cumplen el contrato. */
    private <T> Optional<T> leer(SobreEvento evento, Class<T> contrato) {
        if (!ORIGEN_VENTAS.equals(evento.origen())) {
            return Optional.empty();
        }
        try {
            return Optional.of(mapeador.convertValue(evento.datos(), contrato));
        } catch (IllegalArgumentException contratoRoto) {
            return Optional.empty();
        }
    }
}
