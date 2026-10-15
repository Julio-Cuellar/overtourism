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
import mx.negocio360.compartido.dinero.Redondeo;
import mx.negocio360.ventas.ResumenTurno;
import mx.negocio360.ventas.VentasApi;
import mx.negocio360.ventas.aplicacion.puertos.AutorizacionVentas;
import mx.negocio360.ventas.aplicacion.puertos.CatalogoDeVentas;
import mx.negocio360.ventas.aplicacion.puertos.CuponesDePasaporte;
import mx.negocio360.ventas.aplicacion.puertos.PublicadorEventos;
import mx.negocio360.ventas.aplicacion.puertos.RepositorioVentas;
import mx.negocio360.ventas.dominio.CodigoError;
import mx.negocio360.ventas.dominio.EstadoVenta;
import mx.negocio360.ventas.dominio.Venta;
import mx.negocio360.ventas.dominio.VentasException;

/** Implementacion de {@link VentasApi}: escrituras por preparar -&gt; confirmar y consultas por negocio. */
public final class VentasServicio implements VentasApi {

    private final ServicioPreparacion preparacion;
    private final RepositorioVentas ventas;

    /**
     * Arma el servicio con un servicio de preparacion ya configurado.
     *
     * @param preparacion preparar -&gt; confirmar con los manejadores de Ventas
     * @param ventas      repositorio de ventas
     */
    public VentasServicio(ServicioPreparacion preparacion, RepositorioVentas ventas) {
        this.preparacion = Objects.requireNonNull(preparacion, "preparacion");
        this.ventas = Objects.requireNonNull(ventas, "ventas");
    }

    /**
     * Arma el modulo completo a partir de sus puertos.
     *
     * @param catalogo      precios de Catalogo
     * @param cupones       cupones de Pasaporte
     * @param ventas        repositorio de ventas
     * @param preparaciones tabla {@code preparacion} de Ventas
     * @param eventos       outbox
     * @param autorizacion  permisos de Identidad
     * @param reloj         reloj de la plataforma
     * @return el servicio listo para usarse
     */
    public static VentasServicio armar(CatalogoDeVentas catalogo, CuponesDePasaporte cupones, RepositorioVentas ventas,
                                       RepositorioPreparaciones preparaciones, PublicadorEventos eventos,
                                       AutorizacionVentas autorizacion, Clock reloj) {
        var manejadores = List.of(
                new ManejadorRegistrarVenta(catalogo, cupones, ventas, eventos, reloj),
                new ManejadorAnularVenta(ventas, autorizacion, eventos, reloj));
        var preparacion = new ServicioPreparacion(preparaciones, manejadores, reloj,
                fallo -> new VentasException(CodigoError.valueOf(fallo.name())));
        return new VentasServicio(preparacion, ventas);
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
    public Optional<Venta> venta(ContextoNegocio contexto, UUID ventaId) {
        return ventas.porId(contexto.negocioId(), ventaId);
    }

    @Override
    public List<Venta> ventasDelTurno(ContextoNegocio contexto, UUID turnoId) {
        return ventas.porTurno(contexto.negocioId(), turnoId);
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
