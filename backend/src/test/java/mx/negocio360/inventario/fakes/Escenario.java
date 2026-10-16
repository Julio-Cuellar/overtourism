package mx.negocio360.inventario.fakes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.inventario.InventarioApi;
import mx.negocio360.inventario.aplicacion.ArticuloVista;
import mx.negocio360.inventario.aplicacion.Comando;
import mx.negocio360.inventario.aplicacion.ConsumosServicio;
import mx.negocio360.inventario.aplicacion.CrearArticuloComando;
import mx.negocio360.inventario.aplicacion.InventarioServicio;
import mx.negocio360.inventario.aplicacion.Manejadores;
import mx.negocio360.inventario.aplicacion.RegistrarEntradaComando;
import mx.negocio360.inventario.aplicacion.RegistrarSalidaComando;
import mx.negocio360.inventario.aplicacion.ResultadoComando;
import mx.negocio360.inventario.aplicacion.SecuenciaDeIds;
import mx.negocio360.inventario.aplicacion.ServicioPreparacion;
import mx.negocio360.inventario.aplicacion.UnidadPropiaPedida;

/** Arma el modulo de Inventario con dobles en memoria y un reloj controlado por la prueba. */
public final class Escenario {

    public static final Instant AHORA = Instant.parse("2026-10-15T18:00:00Z");

    public final RelojMutable reloj = new RelojMutable(AHORA);
    public final UUID negocioA = UUID.randomUUID();
    public final UUID negocioB = UUID.randomUUID();
    public final ContextoNegocio ctxA = new ContextoNegocio(negocioA, "locatario-a");
    public final ContextoNegocio ctxB = new ContextoNegocio(negocioB, "locatario-b");

    public final AlmacenInventarioEnMemoria almacen = new AlmacenInventarioEnMemoria();
    public final RepositorioPreparacionesEnMemoria preparaciones = new RepositorioPreparacionesEnMemoria();
    public final PublicadorEventosEnMemoria eventos = new PublicadorEventosEnMemoria();
    public final RepositorioConsumosEnMemoria consumos = new RepositorioConsumosEnMemoria();

    public final InventarioApi api;

    public Escenario() {
        var ids = new SecuenciaDeIds();
        var manejadores = Manejadores.todos(almacen, almacen, almacen, eventos, reloj, ids);
        var consumo = new ConsumosServicio(almacen, almacen, almacen, consumos, eventos, reloj, ids);
        api = new InventarioServicio(new ServicioPreparacion(preparaciones, manejadores, reloj), almacen, almacen,
                almacen, consumo);
    }

    public static BigDecimal d(String valor) {
        return new BigDecimal(valor);
    }

    /** Prepara y confirma un comando, como lo haria el panel en dos pasos. */
    public ResultadoComando confirmar(ContextoNegocio contexto, Comando comando) {
        return api.confirmar(contexto, api.preparar(contexto, comando).token());
    }

    public UUID crear(ContextoNegocio contexto, String nombre, String unidadBase, String minimo, String unidadMinimo,
                      List<UnidadPropiaPedida> propias) {
        return confirmar(contexto, new CrearArticuloComando(nombre, null, unidadBase,
                minimo == null ? null : d(minimo), unidadMinimo, propias)).recursoId();
    }

    /** Harina en gramos, con minimo de 1 kg y la unidad propia {@code costal} de 25 kg. */
    public UUID harina(ContextoNegocio contexto) {
        return crear(contexto, "Harina", "g", "1", "kg", List.of(new UnidadPropiaPedida("costal", d("25000"))));
    }

    public void entrada(ContextoNegocio contexto, UUID articuloId, String cantidad, String unidad, String costo) {
        confirmar(contexto, new RegistrarEntradaComando(articuloId, d(cantidad), unidad, d(costo), null));
    }

    public void salida(ContextoNegocio contexto, UUID articuloId, String cantidad, String unidad) {
        confirmar(contexto, new RegistrarSalidaComando(articuloId, d(cantidad), unidad));
    }

    public ArticuloVista vista(ContextoNegocio contexto, UUID articuloId) {
        return api.articulo(contexto, articuloId).orElseThrow();
    }
}
