package mx.negocio360.restaurantes.aplicacion;

import java.time.Clock;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.restaurantes.dominio.CodigoError;
import mx.negocio360.restaurantes.dominio.Comanda;
import mx.negocio360.restaurantes.dominio.Cuenta;
import mx.negocio360.restaurantes.dominio.Estacion;
import mx.negocio360.restaurantes.dominio.EstadoDeComanda;
import mx.negocio360.restaurantes.dominio.Mesa;
import mx.negocio360.restaurantes.dominio.RestaurantesException;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;

/**
 * El tablero de cocina y barra (CD-01 a CD-09): las comandas de cada estacion, su avance de uno en uno y la devolucion de
 * lista a en preparacion. Cada estacion tiene su propio estado (CD-03). El negocio y el actor salen del contexto y cada
 * operacion exige su permiso: preparar para llegar a lista, entregar para el ultimo paso (CD-02).
 */
public final class CocinaServicio {

    /** Tope de una pagina del tablero (D-131). */
    public static final int MAX_PAGINA = 100;

    private final RepositorioCuentas cuentas;
    private final RepositorioSalon salon;
    private final PublicadorEventos eventos;
    private final Clock reloj;
    private final Object cerrojo = new Object();

    /**
     * Arma el servicio.
     *
     * @param cuentas almacen de cuentas y comandas
     * @param salon   almacen del salon, para el nombre de la mesa
     * @param eventos publicador de eventos
     * @param reloj   reloj de la plataforma
     */
    public CocinaServicio(RepositorioCuentas cuentas, RepositorioSalon salon, PublicadorEventos eventos, Clock reloj) {
        this.cuentas = Objects.requireNonNull(cuentas, "cuentas");
        this.salon = Objects.requireNonNull(salon, "salon");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    /**
     * Las comandas del tablero, de la mas antigua a la mas nueva. Sin estados pide las no entregadas. Exige
     * {@code restaurantes.comanda.ver}.
     *
     * @param estacion cocina o barra, o nulo para las dos (el filtro es de pantalla, CD-09)
     * @param estados  los estados que se piden, o nulo para todos menos entregada
     * @param primeros de 1 a 100; mas se recorta
     * @throws RestaurantesException CONSULTA_INVALIDA si {@code primeros} es menor que 1
     */
    public List<ComandaVista> comandas(ContextoNegocio contexto, Autorizacion autorizacion, Estacion estacion,
                                       Set<EstadoDeComanda> estados, int primeros) {
        autorizacion.exigir(PermisosDeRestaurantes.COMANDA_VER);
        if (primeros < 1) {
            throw new RestaurantesException(CodigoError.CONSULTA_INVALIDA);
        }
        var pedidos = estados == null || estados.isEmpty()
                ? EnumSet.complementOf(EnumSet.of(EstadoDeComanda.ENTREGADA)) : EnumSet.copyOf(estados);
        var ahora = reloj.instant();
        return cuentas.comandas(contexto.negocioId()).stream()
                .filter(c -> estacion == null || c.estacion() == estacion).filter(c -> pedidos.contains(c.estado()))
                .sorted(Comparator.comparing(Comanda::enviadaEn).thenComparing(Comanda::id))
                .limit(Math.min(primeros, MAX_PAGINA))
                .map(c -> cuentas.cuenta(c.negocioId(), c.cuentaId()).map(cuenta -> Tablero.vista(c, cuenta,
                        mesa(cuenta), ahora)).orElse(null))
                .filter(Objects::nonNull).toList();
    }

    /**
     * Avanza la comanda un paso (CD-01): hasta lista con {@code restaurantes.comanda.preparar} y a entregada con
     * {@code restaurantes.comanda.entregar} (CD-02). Publica {@code comanda_actualizada}.
     *
     * @throws RestaurantesException COMANDA_NO_ENCONTRADA o TRANSICION_INVALIDA si ya esta entregada
     */
    public ComandaVista avanzarComanda(ContextoNegocio contexto, Autorizacion autorizacion, UUID comandaId) {
        synchronized (cerrojo) {
            var comanda = comandaDe(contexto, comandaId);
            if (comanda.estado() == EstadoDeComanda.ENTREGADA) {
                throw new RestaurantesException(CodigoError.TRANSICION_INVALIDA);
            }
            var destino = EstadoDeComanda.values()[comanda.estado().ordinal() + 1];
            autorizacion.exigir(destino == EstadoDeComanda.ENTREGADA ? PermisosDeRestaurantes.COMANDA_ENTREGAR
                    : PermisosDeRestaurantes.COMANDA_PREPARAR);
            return cambiar(contexto, comanda, destino);
        }
    }

    /**
     * Devuelve una comanda lista a en preparacion (CD-01). Exige {@code restaurantes.comanda.preparar}.
     *
     * @throws RestaurantesException COMANDA_NO_ENCONTRADA o TRANSICION_INVALIDA si no estaba lista
     */
    public ComandaVista devolverComanda(ContextoNegocio contexto, Autorizacion autorizacion, UUID comandaId) {
        autorizacion.exigir(PermisosDeRestaurantes.COMANDA_PREPARAR);
        synchronized (cerrojo) {
            var comanda = comandaDe(contexto, comandaId);
            if (comanda.estado() != EstadoDeComanda.LISTA) {
                throw new RestaurantesException(CodigoError.TRANSICION_INVALIDA);
            }
            return cambiar(contexto, comanda, EstadoDeComanda.EN_PREPARACION);
        }
    }

    private ComandaVista cambiar(ContextoNegocio contexto, Comanda comanda, EstadoDeComanda destino) {
        var ahora = reloj.instant();
        var nueva = comanda.a(destino, contexto.actor(), ahora);
        if (!cuentas.reemplazar(nueva, comanda.version())) {
            throw new RestaurantesException(CodigoError.CONCURRENCIA);
        }
        eventos.publicar(EventosDeRestaurantes.comandaActualizada(nueva, List.of(), ahora));
        var cuenta = cuentas.cuenta(nueva.negocioId(), nueva.cuentaId())
                .orElseThrow(() -> new RestaurantesException(CodigoError.CUENTA_NO_ENCONTRADA));
        return Tablero.vista(nueva, cuenta, mesa(cuenta), ahora);
    }

    private Comanda comandaDe(ContextoNegocio contexto, UUID comandaId) {
        return cuentas.comanda(contexto.negocioId(), comandaId)
                .orElseThrow(() -> new RestaurantesException(CodigoError.COMANDA_NO_ENCONTRADA));
    }

    private String mesa(Cuenta cuenta) {
        if (cuenta.mesaId() == null) {
            return "Mostrador";
        }
        return salon.mesa(cuenta.negocioId(), cuenta.mesaId()).map(Mesa::etiqueta).orElse("Mesa");
    }
}
