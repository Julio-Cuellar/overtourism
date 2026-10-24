package mx.negocio360.ventas.aplicacion;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import mx.negocio360.compartido.comandos.Comando;
import mx.negocio360.compartido.comandos.PreparacionCreada;
import mx.negocio360.compartido.comandos.RepositorioPreparaciones;
import mx.negocio360.compartido.comandos.ResultadoComando;
import mx.negocio360.compartido.comandos.ServicioPreparacion;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.catalogo.ProductoParaVenta;
import mx.negocio360.compartido.dinero.Redondeo;
import mx.negocio360.compartido.paginacion.Cursores;
import mx.negocio360.compartido.paginacion.Pagina;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.ventas.FiltroVentas;
import mx.negocio360.ventas.ResumenTurno;
import mx.negocio360.ventas.VentasApi;
import mx.negocio360.ventas.VerificadorDeTurno;
import mx.negocio360.ventas.aplicacion.puertos.CatalogoDeVentas;
import mx.negocio360.ventas.aplicacion.puertos.CuponesDePasaporte;
import mx.negocio360.ventas.aplicacion.puertos.PublicadorEventos;
import mx.negocio360.ventas.aplicacion.puertos.RepositorioVentas;
import mx.negocio360.ventas.dominio.CodigoError;
import mx.negocio360.ventas.dominio.EstadoVenta;
import mx.negocio360.ventas.dominio.Venta;
import mx.negocio360.ventas.dominio.VentasException;
import mx.negocio360.ventas.seguridad.PermisosDeVentas;

/** Implementacion de {@link VentasApi}: escrituras por preparar -&gt; confirmar y consultas por negocio. */
public final class VentasServicio implements VentasApi {

    private static final int MAX_CARACTERES_DE_BUSQUEDA = 80;
    private static final int MAX_PRODUCTOS_DE_BUSQUEDA = 50;

    private final ServicioPreparacion preparacion;
    private final RepositorioVentas ventas;
    private final CatalogoDeVentas catalogo;

    /**
     * Arma el servicio con un servicio de preparacion ya configurado.
     *
     * @param preparacion preparar -&gt; confirmar con los manejadores de Ventas
     * @param ventas      repositorio de ventas
     * @param catalogo    productos que se pueden vender (G1)
     */
    public VentasServicio(ServicioPreparacion preparacion, RepositorioVentas ventas, CatalogoDeVentas catalogo) {
        this.preparacion = Objects.requireNonNull(preparacion, "preparacion");
        this.ventas = Objects.requireNonNull(ventas, "ventas");
        this.catalogo = Objects.requireNonNull(catalogo, "catalogo");
    }

    /**
     * Arma el modulo completo a partir de sus puertos. Los permisos ya no son un puerto: cada llamada recibe la
     * {@link Autorizacion} de la sesion (D-126).
     *
     * @param catalogo      productos y precios (fuentes de productos, D-128)
     * @param cupones       cupones de Pasaporte
     * @param turnos        Caja dice si el turno esta abierto (VT-01)
     * @param ventas        repositorio de ventas
     * @param preparaciones tabla {@code preparacion} de Ventas
     * @param eventos       outbox
     * @param reloj         reloj de la plataforma
     * @return el servicio listo para usarse
     */
    public static VentasServicio armar(CatalogoDeVentas catalogo, CuponesDePasaporte cupones,
                                       VerificadorDeTurno turnos, RepositorioVentas ventas,
                                       RepositorioPreparaciones preparaciones, PublicadorEventos eventos,
                                       Clock reloj) {
        var manejadores = List.of(
                new ManejadorRegistrarVenta(catalogo, cupones, turnos, ventas, eventos, reloj),
                new ManejadorAnularVenta(ventas, eventos, reloj));
        var preparacion = new ServicioPreparacion(preparaciones, manejadores, reloj,
                fallo -> new VentasException(CodigoError.valueOf(fallo.name())));
        return new VentasServicio(preparacion, ventas, catalogo);
    }

    @Override
    public PreparacionCreada preparar(ContextoNegocio contexto, Autorizacion autorizacion, Comando comando) {
        return preparacion.preparar(contexto, autorizacion, comando);
    }

    @Override
    public ResultadoComando confirmar(ContextoNegocio contexto, Autorizacion autorizacion, String token) {
        return preparacion.confirmar(contexto, autorizacion, token);
    }

    @Override
    public Optional<Venta> venta(ContextoNegocio contexto, Autorizacion autorizacion, UUID ventaId) {
        autorizacion.exigir(PermisosDeVentas.VENTA_VER);
        return ventas.porId(contexto.negocioId(), ventaId);
    }

    @Override
    public Pagina<Venta> ventas(ContextoNegocio contexto, Autorizacion autorizacion, FiltroVentas filtro,
                                int primeros, String despues) {
        autorizacion.exigir(PermisosDeVentas.VENTA_VER);
        if (!Cursores.valido(primeros, despues)) {
            throw new VentasException(CodigoError.PAGINACION_INVALIDA);
        }
        var buscado = filtro == null ? FiltroVentas.TODAS : filtro;
        if (!buscado.valido()) {
            throw new VentasException(CodigoError.FILTRO_INVALIDO);
        }
        return ventas.buscar(contexto.negocioId(), buscado, primeros, despues);
    }

    @Override
    public ResumenTurno resumenTurno(ContextoNegocio contexto, Autorizacion autorizacion, UUID turnoId) {
        autorizacion.exigir(PermisosDeVentas.VENTA_VER);
        return resumenTurno(contexto, turnoId);
    }

    @Override
    public List<ProductoParaVenta> productosParaVenta(ContextoNegocio contexto, Autorizacion autorizacion,
                                                      String texto, int primeros) {
        autorizacion.exigir(PermisosDeVentas.VENTA_REGISTRAR);
        if (primeros < 1 || primeros > MAX_PRODUCTOS_DE_BUSQUEDA) {
            throw new VentasException(CodigoError.PAGINACION_INVALIDA);
        }
        if (texto != null && texto.length() > MAX_CARACTERES_DE_BUSQUEDA) {
            throw new VentasException(CodigoError.TEXTO_DEMASIADO_LARGO);
        }
        return catalogo.buscar(contexto.negocioId(), texto, primeros);
    }

    @Override
    public ResumenTurno resumenTurno(ContextoNegocio contexto, UUID turnoId) {
        var delTurno = ventas.porTurno(contexto.negocioId(), turnoId);
        var registradas = delTurno.stream().filter(v -> v.estado() == EstadoVenta.REGISTRADA).toList();
        var totales = new TreeMap<String, BigDecimal>();
        registradas.stream().flatMap(v -> v.pagos().stream())
                .forEach(p -> totales.merge(p.metodo().name(), p.monto(), BigDecimal::add));
        totales.replaceAll((metodo, total) -> Redondeo.dinero(total));
        var vigentes = preparacion.vigentes(contexto, ManejadorRegistrarVenta.TIPO, turnoId.toString());
        return new ResumenTurno(turnoId, registradas.size(), delTurno.size() - registradas.size(),
                Map.copyOf(totales), vigentes);
    }
}
