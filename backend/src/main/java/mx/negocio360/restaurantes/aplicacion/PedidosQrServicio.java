package mx.negocio360.restaurantes.aplicacion;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.restaurantes.dominio.CodigoError;
import mx.negocio360.restaurantes.dominio.Comanda;
import mx.negocio360.restaurantes.dominio.Cuenta;
import mx.negocio360.restaurantes.dominio.Estacion;
import mx.negocio360.restaurantes.dominio.EstadoDeCuenta;
import mx.negocio360.restaurantes.dominio.EstadoDeLinea;
import mx.negocio360.restaurantes.dominio.EstadoDeComanda;
import mx.negocio360.restaurantes.dominio.LineaDeCuenta;
import mx.negocio360.restaurantes.dominio.Mesa;
import mx.negocio360.restaurantes.dominio.ModoServicio;
import mx.negocio360.restaurantes.dominio.OrigenDeCuenta;
import mx.negocio360.restaurantes.dominio.Restaurante;
import mx.negocio360.restaurantes.dominio.RestaurantesException;
import mx.negocio360.restaurantes.dominio.Servicio;

/**
 * El pedido del comensal por QR (QR-03 a QR-20): sin sesion, de mesa en mesa por el token opaco de su QR. Escanear da la
 * mesa y si acepta pedidos; pedir abre la cuenta si la mesa no la tiene (QR-05), manda las lineas directo a cocina con una
 * comanda por estacion (QR-06) y avisa a los meseros (QR-07); el dispositivo consulta lo suyo (QR-11) y puede pedir la
 * cuenta (QR-13).
 *
 * <p>Es la unica superficie anonima que escribe, asi que se defiende sola: limites del pedido y de la cuenta (QR-15), tasa
 * por token y por dispositivo (QR-16), llave de idempotencia (CT-14) y un token que solo sirve en su mesa (QR-19). Todo
 * token invalido, rotado, de una mesa dada de baja o de un negocio no visible responde igual (QR-03). Del dispositivo se
 * guarda solo su hash con clave (QR-12).
 */
public final class PedidosQrServicio {

    /** Quien figura como autor de las comandas que llegan por QR. */
    static final String ACTOR_QR = "QR";

    private final RepositorioSalon salon;
    private final RepositorioCuentas cuentas;
    private final RepositorioMenu menu;
    private final RepositorioRestaurantes restaurantes;
    private final CuentasServicio cuentasServicio;
    private final SalonServicio disponibilidad;
    private final PublicadorEventos eventos;
    private final HashDeDispositivo hash;
    private final LimitesDeQr limites;
    private final Clock reloj;
    private final LimiteDeTasa tasa;
    private final Object cerrojo = new Object();
    private final Map<String, Recordado> recordados = new HashMap<>();

    /** Un pedido ya hecho: lo que se pidio (huella) y lo que se respondio, hasta {@code expira}. */
    private record Recordado(String huella, ResultadoPedidoQrVista resultado, Instant expira) {
    }

    /**
     * Arma el servicio.
     *
     * @param salon           almacen del salon, para resolver el token
     * @param cuentas         almacen de cuentas y comandas
     * @param menu            almacen del menu, para precios y estaciones
     * @param restaurantes    extension de restaurante, con su visibilidad (PB-01)
     * @param cuentasServicio las cuentas, cuyas ayudas comparte
     * @param disponibilidad  recalcula y publica la disponibilidad cuando una cuenta abre la mesa
     * @param eventos         publicador de eventos
     * @param hash            hash con clave del dispositivo
     * @param limites         limites del QR
     * @param reloj           reloj de la plataforma
     */
    public PedidosQrServicio(RepositorioSalon salon, RepositorioCuentas cuentas, RepositorioMenu menu,
                             RepositorioRestaurantes restaurantes, CuentasServicio cuentasServicio,
                             SalonServicio disponibilidad, PublicadorEventos eventos, HashDeDispositivo hash,
                             LimitesDeQr limites, Clock reloj) {
        this.salon = Objects.requireNonNull(salon, "salon");
        this.cuentas = Objects.requireNonNull(cuentas, "cuentas");
        this.menu = Objects.requireNonNull(menu, "menu");
        this.restaurantes = Objects.requireNonNull(restaurantes, "restaurantes");
        this.cuentasServicio = Objects.requireNonNull(cuentasServicio, "cuentasServicio");
        this.disponibilidad = Objects.requireNonNull(disponibilidad, "disponibilidad");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.hash = Objects.requireNonNull(hash, "hash");
        this.limites = Objects.requireNonNull(limites, "limites");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        this.tasa = new LimiteDeTasa(limites.pedidosPorVentana(), limites.ventanaDeTasa());
    }

    // ---- escanear (QR-03) ----

    /**
     * La mesa de un QR y si acepta pedidos; vacio si el token no sirve (QR-03).
     *
     * @param token el token opaco del QR, o nulo
     */
    public Optional<MesaPublicaVista> mesaPorQr(String token) {
        return resolver(token).map(mesa -> {
            var servicio = salon.servicio(mesa.negocioId());
            var cuenta = cuentas.cuentaNoPagadaDeMesa(mesa.negocioId(), mesa.id());
            var ahora = reloj.instant();
            return new MesaPublicaVista(mesa.negocioId(), mesa.etiqueta(), servicio.abierto(),
                    motivoDeRechazo(mesa, servicio, cuenta.orElse(null), ahora) == null, estadoPublico(cuenta.orElse(null)));
        });
    }

    // ---- seguimiento (QR-11) ----

    /**
     * Las lineas que pidio ese dispositivo y el estado de la cuenta; vacio si el token no sirve. Nunca las de otros
     * dispositivos ni totales (QR-11).
     */
    public Optional<EstadoDePedidoQrVista> estadoDePedidoQr(String token, String dispositivoId) {
        return resolver(token).map(mesa -> {
            var cuenta = cuentas.cuentaNoPagadaDeMesa(mesa.negocioId(), mesa.id()).orElse(null);
            return estadoDe(cuenta, dispositivoId == null || dispositivoId.isBlank() ? null : hash.de(dispositivoId));
        });
    }

    // ---- pedir (QR-05, QR-06, QR-07, QR-15, QR-16) ----

    /**
     * Pide platillos desde el QR: abre la cuenta si la mesa no tiene, manda las lineas a cocina y avisa a los meseros.
     * Repetir la misma llave con el mismo pedido devuelve lo mismo sin duplicar (CT-14).
     *
     * @param lineas    lo que se pide
     * @param llave     la llave de idempotencia ({@code Idempotency-Key})
     * @throws RestaurantesException LLAVE_REQUERIDA, LLAVE_REUTILIZADA, DISPOSITIVO_INVALIDO, QR_INVALIDO, SERVICIO_CERRADO,
     *                               QR_DESHABILITADO, QR_PAUSADO, CUENTA_SOLICITADA, TASA_EXCEDIDA, CUENTA_VACIA,
     *                               LIMITE_QR_EXCEDIDO, CANTIDAD_INVALIDA, NOTA_INVALIDA, PLATILLO_NO_ENCONTRADO,
     *                               PLATILLO_AGOTADO, FUERA_DE_TEMPORADA o PLATILLO_NO_ORDENABLE
     */
    public ResultadoPedidoQrVista pedirPorQr(String token, String dispositivoId, List<LineaDePedidoQr> lineas, String llave) {
        if (llave == null || llave.isBlank()) {
            throw new RestaurantesException(CodigoError.LLAVE_REQUERIDA);
        }
        if (dispositivoId == null || dispositivoId.isBlank()) {
            throw new RestaurantesException(CodigoError.DISPOSITIVO_INVALIDO);
        }
        synchronized (cerrojo) {
            var mesa = resolver(token).orElseThrow(() -> new RestaurantesException(CodigoError.QR_INVALIDO));
            var dispositivo = hash.de(dispositivoId);
            var ahora = reloj.instant();
            var huella = mesa.id() + "|" + lineas;
            var clave = dispositivo + "|" + llave.strip();
            var anterior = recordado(clave, ahora);
            if (anterior != null) {
                if (!anterior.huella().equals(huella)) {
                    throw new RestaurantesException(CodigoError.LLAVE_REUTILIZADA);
                }
                return anterior.resultado();
            }
            var resultado = pedir(mesa, dispositivo, lineas, ahora);
            recordados.put(clave, new Recordado(huella, resultado, ahora.plus(limites.retencionDeLlaves())));
            return resultado;
        }
    }

    private ResultadoPedidoQrVista pedir(Mesa mesa, String dispositivo, List<LineaDePedidoQr> pedidas, Instant ahora) {
        var negocio = mesa.negocioId();
        var servicio = salon.servicio(negocio);
        var existente = cuentas.cuentaNoPagadaDeMesa(negocio, mesa.id()).orElse(null);
        var rechazo = motivoDeRechazo(mesa, servicio, existente, ahora);
        if (rechazo != null) {
            throw new RestaurantesException(rechazo);
        }
        exigirTasa(mesa, dispositivo, "p", ahora);

        var preparadas = preparar(negocio, pedidas);
        var pedido = preparadas.stream().map(p -> p.platillo().precio().multiply(BigDecimal.valueOf(p.pedida().cantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        exigirDentroDeLaCuenta(existente, preparadas.size(), pedido);

        var abrio = false;
        for (var intento = 0; intento < 3; intento++) {
            var cuenta = cuentas.cuentaNoPagadaDeMesa(negocio, mesa.id()).orElse(null);
            if (cuenta == null) {
                try {
                    cuenta = cuentasServicio.abrirInterna(negocio, mesa.id(), OrigenDeCuenta.QR, null, null, ahora);
                    abrio = true;
                } catch (RestaurantesException e) {
                    if (e.codigo() == CodigoError.MESA_OCUPADA) {
                        continue;
                    }
                    throw e;
                }
            }
            if (cuenta.estado() == EstadoDeCuenta.POR_COBRAR) {
                throw new RestaurantesException(CodigoError.CUENTA_SOLICITADA);
            }
            exigirDentroDeLaCuenta(cuenta, preparadas.size(), pedido);
            var guardada = guardarPedido(cuenta, mesa, dispositivo, preparadas, abrio, ahora);
            if (guardada != null) {
                return guardada;
            }
        }
        throw new RestaurantesException(CodigoError.CONCURRENCIA);
    }

    /** Una linea pedida con el platillo ya validado. */
    private record Preparada(LineaDePedidoQr pedida, mx.negocio360.restaurantes.dominio.Platillo platillo) {
    }

    private List<Preparada> preparar(UUID negocio, List<LineaDePedidoQr> pedidas) {
        if (pedidas == null || pedidas.isEmpty()) {
            throw new RestaurantesException(CodigoError.CUENTA_VACIA);
        }
        if (pedidas.size() > limites.lineasPorPedido()) {
            throw new RestaurantesException(CodigoError.LIMITE_QR_EXCEDIDO);
        }
        var resultado = new ArrayList<Preparada>();
        for (var pedida : pedidas) {
            if (pedida == null || pedida.platilloId() == null) {
                throw new RestaurantesException(CodigoError.PLATILLO_NO_ENCONTRADO);
            }
            if (pedida.cantidad() < 1) {
                throw new RestaurantesException(CodigoError.CANTIDAD_INVALIDA);
            }
            if (pedida.cantidad() > limites.cantidadPorLinea()) {
                throw new RestaurantesException(CodigoError.LIMITE_QR_EXCEDIDO);
            }
            // La nota se valida aqui, antes de escribir nada: el constructor de la linea es quien conoce el largo.
            var platillo = cuentasServicio.platilloOrdenable(negocio, pedida.platilloId());
            LineaDeCuenta.pendiente(UuidV7.generar(reloj.instant()), platillo.id(), platillo.nombre(), platillo.precio(),
                    pedida.cantidad(), pedida.nota(), pedida.marcaAlergia(), Estacion.COCINA, reloj.instant());
            resultado.add(new Preparada(pedida, platillo));
        }
        return resultado;
    }

    /** Una cuenta abierta por QR no pasa de las lineas ni del monto maximo (QR-15); la del personal no tiene tope. */
    private void exigirDentroDeLaCuenta(Cuenta cuenta, int lineasNuevas, BigDecimal montoNuevo) {
        if (cuenta != null && cuenta.origen() != OrigenDeCuenta.QR) {
            return;
        }
        var vivas = cuenta == null ? 0 : cuenta.lineas().stream().filter(l -> l.estado() != EstadoDeLinea.ANULADA).count();
        var monto = cuenta == null ? BigDecimal.ZERO : cuenta.total();
        if (vivas + lineasNuevas > limites.lineasPorCuenta() || monto.add(montoNuevo).compareTo(limites.montoPorCuenta()) > 0) {
            throw new RestaurantesException(CodigoError.LIMITE_QR_EXCEDIDO);
        }
    }

    /** Guarda las lineas y sus comandas; nulo si alguien se adelanto a la cuenta (el llamador reintenta). */
    private ResultadoPedidoQrVista guardarPedido(Cuenta cuenta, Mesa mesa, String dispositivo, List<Preparada> preparadas,
                                                 boolean abrio, Instant ahora) {
        var envioId = UuidV7.generar(ahora);
        var comandas = new ArrayList<Comanda>();
        var lineasNuevas = new ArrayList<LineaDeCuenta>();
        for (var estacion : Estacion.values()) {
            var deLaEstacion = preparadas.stream().filter(p -> cuentasServicio.estacionDe(p.platillo()) == estacion).toList();
            if (deLaEstacion.isEmpty()) {
                continue;
            }
            var comandaId = UuidV7.generar(ahora);
            var lineas = deLaEstacion.stream().map(p -> LineaDeCuenta.porQr(UuidV7.generar(ahora), p.platillo().id(),
                    p.platillo().nombre(), p.platillo().precio(), p.pedida().cantidad(), p.pedida().nota(),
                    p.pedida().marcaAlergia(), estacion, comandaId, envioId, dispositivo, ahora)).toList();
            lineasNuevas.addAll(lineas);
            comandas.add(Comanda.nueva(comandaId, cuenta.negocioId(), envioId, cuenta.id(), estacion,
                    lineas.stream().map(LineaDeCuenta::id).toList(), ACTOR_QR, ahora));
        }
        Cuenta guardada;
        try {
            guardada = cuentasServicio.guardar(cuenta, c -> {
                var todas = new ArrayList<>(c.lineas);
                todas.addAll(lineasNuevas);
                c.lineas = todas;
            });
        } catch (RestaurantesException e) {
            if (e.codigo() == CodigoError.CONCURRENCIA) {
                return null;
            }
            throw e;
        }
        comandas.forEach(cuentas::insertar);
        var etiqueta = mesa.etiqueta();
        for (var comanda : comandas) {
            var suyas = guardada.lineas().stream().filter(l -> comanda.lineaIds().contains(l.id())).toList();
            eventos.publicar(EventosDeRestaurantes.comandaEnviada(comanda, guardada, etiqueta, suyas, ahora));
        }
        eventos.publicar(EventosDeRestaurantes.pedidoQrRecibido(guardada, etiqueta, lineasNuevas.size(), abrio, ahora));
        if (abrio) {
            disponibilidad.recalcularDisponibilidad(cuenta.negocioId());
        }
        registrarTasa(mesa, dispositivo, "p", ahora);
        return new ResultadoPedidoQrVista(abrio, estadoDe(guardada, dispositivo));
    }

    // ---- pedir la cuenta (QR-13) ----

    /**
     * Pide la cuenta desde el QR: pasa a por cobrar y avisa a meseros y caja. Si ya se habia pedido, no se repite (QR-13).
     *
     * @throws RestaurantesException QR_INVALIDO, DISPOSITIVO_INVALIDO, TASA_EXCEDIDA, CUENTA_VACIA (no hay cuenta o no
     *                               tiene lineas vivas) o LINEAS_SIN_ENVIAR
     */
    public EstadoDePedidoQrVista pedirCuentaPorQr(String token, String dispositivoId) {
        if (dispositivoId == null || dispositivoId.isBlank()) {
            throw new RestaurantesException(CodigoError.DISPOSITIVO_INVALIDO);
        }
        synchronized (cerrojo) {
            var mesa = resolver(token).orElseThrow(() -> new RestaurantesException(CodigoError.QR_INVALIDO));
            var dispositivo = hash.de(dispositivoId);
            var ahora = reloj.instant();
            var cuenta = cuentas.cuentaNoPagadaDeMesa(mesa.negocioId(), mesa.id())
                    .orElseThrow(() -> new RestaurantesException(CodigoError.CUENTA_VACIA));
            if (cuenta.estado() == EstadoDeCuenta.POR_COBRAR) {
                return estadoDe(cuenta, dispositivo);
            }
            exigirTasa(mesa, dispositivo, "c", ahora);
            var pedida = cuentasServicio.pedirInterno(mesa.negocioId(), cuenta.id(), ACTOR_QR);
            registrarTasa(mesa, dispositivo, "c", ahora);
            return estadoDe(cuentas.cuenta(mesa.negocioId(), pedida.id()).orElse(null), dispositivo);
        }
    }

    // ---- ayudas ----

    /** La mesa activa de un token, solo si su negocio es visible para el turista (QR-04). */
    private Optional<Mesa> resolver(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return salon.mesaPorToken(token).filter(Mesa::activa)
                .filter(m -> restaurantes.porNegocio(m.negocioId()).filter(Restaurante::visible).isPresent());
    }

    /** Por que la mesa no acepta pedidos ahora, o nulo si los acepta (QR-05). */
    private static CodigoError motivoDeRechazo(Mesa mesa, Servicio servicio, Cuenta cuenta, Instant ahora) {
        if (!servicio.abierto()) {
            return CodigoError.SERVICIO_CERRADO;
        }
        if (!servicio.qrHabilitado() || servicio.modo() == ModoServicio.MOSTRADOR) {
            return CodigoError.QR_DESHABILITADO;
        }
        if (mesa.bloqueada() || mesa.qrPausado(ahora)) {
            return CodigoError.QR_PAUSADO;
        }
        if (cuenta != null && cuenta.estado() == EstadoDeCuenta.POR_COBRAR) {
            return CodigoError.CUENTA_SOLICITADA;
        }
        return null;
    }

    private static EstadoPublicoDeCuenta estadoPublico(Cuenta cuenta) {
        if (cuenta == null) {
            return EstadoPublicoDeCuenta.SIN_CUENTA;
        }
        return cuenta.estado() == EstadoDeCuenta.POR_COBRAR ? EstadoPublicoDeCuenta.SOLICITADA : EstadoPublicoDeCuenta.ABIERTA;
    }

    /** Lo que ve un dispositivo: sus lineas de la cuenta de la mesa, de la mas vieja a la mas nueva (QR-11). */
    private EstadoDePedidoQrVista estadoDe(Cuenta cuenta, String dispositivo) {
        if (cuenta == null) {
            return new EstadoDePedidoQrVista(EstadoPublicoDeCuenta.SIN_CUENTA, List.of());
        }
        if (dispositivo == null) {
            return new EstadoDePedidoQrVista(estadoPublico(cuenta), List.of());
        }
        var estados = new LinkedHashMap<UUID, EstadoDeComanda>();
        cuentas.comandas(cuenta.negocioId()).stream().filter(c -> c.cuentaId().equals(cuenta.id()))
                .forEach(c -> estados.put(c.id(), c.estado()));
        var suyas = cuenta.lineas().stream().filter(l -> dispositivo.equals(l.dispositivoHash()))
                .sorted(Comparator.comparing(LineaDeCuenta::creadaEn)).map(l -> vistaDeLinea(l, estados)).toList();
        return new EstadoDePedidoQrVista(estadoPublico(cuenta), suyas);
    }

    private static LineaDePedidoQrVista vistaDeLinea(LineaDeCuenta linea, Map<UUID, EstadoDeComanda> comandas) {
        if (linea.estado() == EstadoDeLinea.ANULADA) {
            return new LineaDePedidoQrVista(linea.id(), linea.nombre(), linea.cantidad(), EstadoDeLineaQr.CANCELADO,
                    MensajeDeMotivo.de(linea.motivo(), linea.textoDelMotivo()));
        }
        var comanda = linea.comandaId() == null ? null : comandas.get(linea.comandaId());
        var estado = comanda == null ? EstadoDeLineaQr.EN_COCINA : switch (comanda) {
            case NUEVA -> EstadoDeLineaQr.EN_COCINA;
            case EN_PREPARACION -> EstadoDeLineaQr.PREPARANDO;
            case LISTA -> EstadoDeLineaQr.LISTO;
            case ENTREGADA -> EstadoDeLineaQr.ENTREGADO;
        };
        return new LineaDePedidoQrVista(linea.id(), linea.nombre(), linea.cantidad(), estado, null);
    }

    // ---- tasa (QR-16) y llaves (CT-14) ----

    private void exigirTasa(Mesa mesa, String dispositivo, String clase, Instant ahora) {
        if (tasa.excede(clase + ":t:" + mesa.qrToken(), ahora) || tasa.excede(clase + ":d:" + dispositivo, ahora)) {
            throw new RestaurantesException(CodigoError.TASA_EXCEDIDA);
        }
    }

    private void registrarTasa(Mesa mesa, String dispositivo, String clase, Instant ahora) {
        tasa.registrar(clase + ":t:" + mesa.qrToken(), ahora);
        tasa.registrar(clase + ":d:" + dispositivo, ahora);
    }

    private Recordado recordado(String clave, Instant ahora) {
        recordados.values().removeIf(r -> !r.expira().isAfter(ahora));
        return recordados.get(clave);
    }

    /** Nunca muestra tokens ni dispositivos. */
    @Override
    public String toString() {
        return "PedidosQrServicio";
    }
}
