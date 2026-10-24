package mx.negocio360.restaurantes.aplicacion;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.restaurantes.dominio.Categoria;
import mx.negocio360.restaurantes.dominio.Comanda;
import mx.negocio360.restaurantes.dominio.CodigoError;
import mx.negocio360.restaurantes.dominio.Cuenta;
import mx.negocio360.restaurantes.dominio.Estacion;
import mx.negocio360.restaurantes.dominio.EstadoDeComanda;
import mx.negocio360.restaurantes.dominio.EstadoDeCuenta;
import mx.negocio360.restaurantes.dominio.EstadoDeLinea;
import mx.negocio360.restaurantes.dominio.LineaDeCuenta;
import mx.negocio360.restaurantes.dominio.Mesa;
import mx.negocio360.restaurantes.dominio.ModoServicio;
import mx.negocio360.restaurantes.dominio.MotivoDeAnulacion;
import mx.negocio360.restaurantes.dominio.OrigenDeCuenta;
import mx.negocio360.restaurantes.dominio.Platillo;
import mx.negocio360.restaurantes.dominio.RestaurantesException;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;

/**
 * Las cuentas del restaurante (CT-01 a CT-15, QR-08, QR-10): abrir, agregar lineas, enviar a cocina, pedir la cuenta,
 * anular y cancelar con motivo, liberar la mesa y dejar lista la cuenta para que la caja la cobre con Ventas. El negocio
 * y el actor salen del contexto y cada operacion exige su permiso (AZ-04).
 *
 * <p>La cuenta abierta vive aqui y no en Ventas, que registra una venta de un solo golpe (H-R2). Lo que cambia la
 * ocupacion de una mesa recalcula la disponibilidad que se publica (MS-06). No guarda datos personales del cliente (CT-12).
 */
public final class CuentasServicio {

    /** Cuanto tiempo se guardan las notas y el hash del dispositivo de una linea (CT-12, D-172). */
    static final Duration RETENCION = Duration.ofDays(30);

    private final RepositorioCuentas cuentas;
    private final RepositorioMenu menu;
    private final RepositorioSalon salon;
    private final SalonServicio disponibilidad;
    private final PublicadorEventos eventos;
    private final LimitesDeQr limites;
    private final Clock reloj;
    private final Object cerrojo = new Object();

    /**
     * Arma el servicio.
     *
     * @param cuentas      almacen de cuentas y comandas
     * @param menu         almacen del menu, para precios y estaciones
     * @param salon        almacen del salon, para mesas y servicio
     * @param disponibilidad recalcula y publica la disponibilidad cuando cambia la ocupacion
     * @param eventos      publicador de eventos
     * @param reloj        reloj de la plataforma
     */
    public CuentasServicio(RepositorioCuentas cuentas, RepositorioMenu menu, RepositorioSalon salon,
                           SalonServicio disponibilidad, PublicadorEventos eventos, Clock reloj) {
        this(cuentas, menu, salon, disponibilidad, eventos, LimitesDeQr.PREDETERMINADOS, reloj);
    }

    /** Igual, con los limites del QR que gobiernan la defensa contra bromas (QR-17). */
    public CuentasServicio(RepositorioCuentas cuentas, RepositorioMenu menu, RepositorioSalon salon,
                           SalonServicio disponibilidad, PublicadorEventos eventos, LimitesDeQr limites, Clock reloj) {
        this.limites = Objects.requireNonNull(limites, "limites");
        this.cuentas = Objects.requireNonNull(cuentas, "cuentas");
        this.menu = Objects.requireNonNull(menu, "menu");
        this.salon = Objects.requireNonNull(salon, "salon");
        this.disponibilidad = Objects.requireNonNull(disponibilidad, "disponibilidad");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    // ---- abrir (CT-01) ----

    /**
     * Abre una cuenta de mesa o, en mostrador, sin mesa. Exige {@code restaurantes.cuenta.abrir}.
     *
     * @param mesaId     la mesa, o nulo en mostrador (MS-08)
     * @param comensales de 1 a 50
     * @throws RestaurantesException SERVICIO_CERRADO, MESA_REQUERIDA, MESA_NO_PERMITIDA, MESA_NO_ENCONTRADA, MESA_BLOQUEADA,
     *                               MESA_OCUPADA o COMENSALES_INVALIDOS
     */
    public CuentaVista abrirCuenta(ContextoNegocio contexto, Autorizacion autorizacion, UUID mesaId, int comensales) {
        autorizacion.exigir(PermisosDeRestaurantes.CUENTA_ABRIR);
        CuentaVista vista;
        synchronized (cerrojo) {
            var negocio = contexto.negocioId();
            var servicio = salon.servicio(negocio);
            if (!servicio.abierto()) {
                throw new RestaurantesException(CodigoError.SERVICIO_CERRADO);
            }
            exigirMesaSegunElModo(contexto, servicio.modo(), mesaId);
            var ahora = reloj.instant();
            var cuenta = abrirInterna(negocio, mesaId, OrigenDeCuenta.PERSONAL, comensales, contexto.actor(), ahora);
            vista = vista(cuenta);
        }
        disponibilidad.recalcularDisponibilidad(contexto.negocioId());
        return vista;
    }

    private void exigirMesaSegunElModo(ContextoNegocio contexto, ModoServicio modo, UUID mesaId) {
        if (mesaId == null) {
            if (modo == ModoServicio.MESAS) {
                throw new RestaurantesException(CodigoError.MESA_REQUERIDA);
            }
            return;
        }
        if (modo == ModoServicio.MOSTRADOR) {
            throw new RestaurantesException(CodigoError.MESA_NO_PERMITIDA);
        }
        var mesa = salon.mesa(contexto.negocioId(), mesaId).filter(Mesa::activa)
                .orElseThrow(() -> new RestaurantesException(CodigoError.MESA_NO_ENCONTRADA));
        if (mesa.bloqueada()) {
            throw new RestaurantesException(CodigoError.MESA_BLOQUEADA);
        }
    }

    /** Abre y guarda una cuenta: lo comparten el personal y el primer pedido por QR (QR-05). */
    Cuenta abrirInterna(UUID negocio, UUID mesaId, OrigenDeCuenta origen, Integer comensales, String abiertaPor,
                        Instant ahora) {
        if (mesaId != null && cuentas.cuentaNoPagadaDeMesa(negocio, mesaId).isPresent()) {
            throw new RestaurantesException(CodigoError.MESA_OCUPADA);
        }
        var cuenta = Cuenta.nueva(UuidV7.generar(ahora), negocio, cuentas.siguienteNumero(negocio), mesaId, origen,
                comensales, abiertaPor, ahora);
        if (!cuentas.insertar(cuenta)) {
            throw new RestaurantesException(CodigoError.MESA_OCUPADA);
        }
        return cuenta;
    }

    // ---- lineas (CT-02, CT-03) ----

    /**
     * Agrega una linea pendiente a una cuenta abierta (CT-02). Exige {@code restaurantes.cuenta.abrir}.
     *
     * @throws RestaurantesException CUENTA_NO_ENCONTRADA, CUENTA_NO_ABIERTA, PLATILLO_NO_ENCONTRADO, PLATILLO_AGOTADO,
     *                               FUERA_DE_TEMPORADA, PLATILLO_NO_ORDENABLE, CANTIDAD_INVALIDA o NOTA_INVALIDA
     */
    public CuentaVista agregarLinea(ContextoNegocio contexto, Autorizacion autorizacion, UUID cuentaId, UUID platilloId,
                                    int cantidad, String nota, boolean marcaAlergia) {
        autorizacion.exigir(PermisosDeRestaurantes.CUENTA_ABRIR);
        synchronized (cerrojo) {
            var cuenta = cuentaDe(contexto.negocioId(), cuentaId);
            exigirAbierta(cuenta);
            var platillo = platilloOrdenable(contexto.negocioId(), platilloId);
            var ahora = reloj.instant();
            var linea = LineaDeCuenta.pendiente(UuidV7.generar(ahora), platillo.id(), platillo.nombre(), platillo.precio(),
                    cantidad, nota, marcaAlergia, estacionDe(platillo), ahora);
            return vista(guardar(cuenta, c -> c.lineas = concat(c.lineas, linea)));
        }
    }

    /**
     * Quita una linea que todavia no se envia (CT-03). Exige {@code restaurantes.cuenta.abrir}.
     *
     * @throws RestaurantesException CUENTA_NO_ABIERTA, LINEA_NO_ENCONTRADA o TRANSICION_INVALIDA si ya se envio
     */
    public CuentaVista quitarLineaPendiente(ContextoNegocio contexto, Autorizacion autorizacion, UUID cuentaId, UUID lineaId) {
        autorizacion.exigir(PermisosDeRestaurantes.CUENTA_ABRIR);
        synchronized (cerrojo) {
            var cuenta = cuentaDe(contexto.negocioId(), cuentaId);
            exigirAbierta(cuenta);
            var linea = lineaDe(cuenta, lineaId);
            if (linea.estado() != EstadoDeLinea.PENDIENTE) {
                throw new RestaurantesException(CodigoError.TRANSICION_INVALIDA);
            }
            return vista(guardar(cuenta, c -> c.lineas = c.lineas.stream().filter(l -> !l.id().equals(lineaId)).toList()));
        }
    }

    // ---- enviar a cocina (CT-04) ----

    /**
     * Envia las lineas pendientes: una comanda por estacion, con el mismo envio. Exige {@code restaurantes.cuenta.abrir}.
     *
     * @throws RestaurantesException CUENTA_NO_ABIERTA o SIN_LINEAS_PENDIENTES
     */
    public List<ComandaVista> enviarACocina(ContextoNegocio contexto, Autorizacion autorizacion, UUID cuentaId) {
        autorizacion.exigir(PermisosDeRestaurantes.CUENTA_ABRIR);
        synchronized (cerrojo) {
            var cuenta = cuentaDe(contexto.negocioId(), cuentaId);
            exigirAbierta(cuenta);
            var pendientes = cuenta.lineas().stream().filter(l -> l.estado() == EstadoDeLinea.PENDIENTE).toList();
            if (pendientes.isEmpty()) {
                throw new RestaurantesException(CodigoError.SIN_LINEAS_PENDIENTES);
            }
            var ahora = reloj.instant();
            var envioId = UuidV7.generar(ahora);
            var comandas = new ArrayList<Comanda>();
            var nuevasLineas = new LinkedHashMap<UUID, LineaDeCuenta>();
            for (var estacion : Estacion.values()) {
                var deLaEstacion = pendientes.stream().filter(l -> l.estacion() == estacion).toList();
                if (deLaEstacion.isEmpty()) {
                    continue;
                }
                var comanda = Comanda.nueva(UuidV7.generar(ahora), cuenta.negocioId(), envioId, cuenta.id(), estacion,
                        deLaEstacion.stream().map(LineaDeCuenta::id).toList(), contexto.actor(), ahora);
                cuentas.insertar(comanda);
                comandas.add(comanda);
                deLaEstacion.forEach(l -> nuevasLineas.put(l.id(), l.cambiar(c -> {
                    c.estado = EstadoDeLinea.ENVIADA;
                    c.comandaId = comanda.id();
                    c.envioId = envioId;
                })));
            }
            var actualizada = guardar(cuenta, c -> c.lineas = c.lineas.stream()
                    .map(l -> nuevasLineas.getOrDefault(l.id(), l)).toList());
            var mesa = etiquetaDeMesa(actualizada);
            for (var comanda : comandas) {
                var lineas = actualizada.lineas().stream().filter(l -> comanda.lineaIds().contains(l.id())).toList();
                eventos.publicar(EventosDeRestaurantes.comandaEnviada(comanda, actualizada, mesa, lineas, ahora));
            }
            return comandas.stream().map(c -> vistaDeComanda(c, actualizada)).toList();
        }
    }

    // ---- pedir la cuenta (CT-05, CT-06) ----

    /**
     * Pasa la cuenta a por cobrar con el total a los precios vigentes (CT-05). Exige {@code restaurantes.cuenta.abrir}.
     *
     * @throws RestaurantesException CUENTA_NO_ABIERTA, CUENTA_VACIA o LINEAS_SIN_ENVIAR
     */
    public CuentaVista pedirCuenta(ContextoNegocio contexto, Autorizacion autorizacion, UUID cuentaId) {
        autorizacion.exigir(PermisosDeRestaurantes.CUENTA_ABRIR);
        return pedirInterno(contexto.negocioId(), cuentaId, contexto.actor());
    }

    /** Pide la cuenta en nombre de quien sea: el personal ({@code actor}) o el comensal por QR ({@code "QR"}). */
    CuentaVista pedirInterno(UUID negocio, UUID cuentaId, String solicitadaPor) {
        CuentaVista vista;
        synchronized (cerrojo) {
            var cuenta = cuentaDe(negocio, cuentaId);
            exigirAbierta(cuenta);
            var vigentes = cuenta.lineas().stream().filter(l -> l.estado() != EstadoDeLinea.ANULADA).toList();
            if (vigentes.isEmpty()) {
                throw new RestaurantesException(CodigoError.CUENTA_VACIA);
            }
            if (vigentes.stream().anyMatch(l -> l.estado() == EstadoDeLinea.PENDIENTE)) {
                throw new RestaurantesException(CodigoError.LINEAS_SIN_ENVIAR);
            }
            var ahora = reloj.instant();
            var repreciadas = cuenta.lineas().stream().map(l -> l.estado() == EstadoDeLinea.ANULADA ? l
                    : l.cambiar(c -> menu.platillo(negocio, l.platilloId()).ifPresent(p -> c.precio = p.precio()))).toList();
            var total = repreciadas.stream().map(LineaDeCuenta::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            var pedida = guardar(cuenta, c -> {
                c.lineas = repreciadas;
                c.estado = EstadoDeCuenta.POR_COBRAR;
                c.solicitadaEn = ahora;
                c.solicitadaPor = solicitadaPor;
                c.totalCalculado = total;
            });
            eventos.publicar(EventosDeRestaurantes.cuentaSolicitada(pedida, etiquetaDeMesa(pedida), solicitadaPor, ahora));
            vista = vista(pedida);
        }
        return vista;
    }

    /**
     * Regresa una cuenta por cobrar a abierta, antes del cobro (CT-06). Exige {@code restaurantes.cuenta.abrir}.
     *
     * @throws RestaurantesException CUENTA_NO_SOLICITADA
     */
    public CuentaVista reabrirCuenta(ContextoNegocio contexto, Autorizacion autorizacion, UUID cuentaId) {
        autorizacion.exigir(PermisosDeRestaurantes.CUENTA_ABRIR);
        synchronized (cerrojo) {
            var cuenta = cuentaDe(contexto.negocioId(), cuentaId);
            if (cuenta.estado() != EstadoDeCuenta.POR_COBRAR) {
                throw new RestaurantesException(CodigoError.CUENTA_NO_SOLICITADA);
            }
            return vista(guardar(cuenta, c -> {
                c.estado = EstadoDeCuenta.ABIERTA;
                c.solicitadaEn = null;
                c.solicitadaPor = null;
                c.totalCalculado = null;
            }));
        }
    }

    /** Marca que un mesero ya esta con la mesa; es solo informativo (QR-08). Exige {@code restaurantes.cuenta.abrir}. */
    public CuentaVista confirmarPresencia(ContextoNegocio contexto, Autorizacion autorizacion, UUID cuentaId) {
        autorizacion.exigir(PermisosDeRestaurantes.CUENTA_ABRIR);
        synchronized (cerrojo) {
            var cuenta = cuentaDe(contexto.negocioId(), cuentaId);
            if (!cuenta.estado().noPagada()) {
                throw new RestaurantesException(CodigoError.CUENTA_NO_ABIERTA);
            }
            return vista(guardar(cuenta, c -> c.presenciaConfirmada = true));
        }
    }

    // ---- anular lineas y cancelar cuentas (CT-03, CT-11, QR-10) ----

    /**
     * Anula una linea enviada con motivo mientras su comanda no este lista (CT-03). Exige
     * {@code restaurantes.linea.anular}.
     *
     * @throws RestaurantesException MOTIVO_REQUERIDO, MOTIVO_INVALIDO, LINEA_NO_ENCONTRADA, CUENTA_NO_ABIERTA,
     *                               TRANSICION_INVALIDA o LINEA_PREPARADA si su comanda ya esta lista o entregada
     */
    public CuentaVista anularLinea(ContextoNegocio contexto, Autorizacion autorizacion, UUID lineaId,
                                   MotivoDeAnulacion motivo, String texto) {
        autorizacion.exigir(PermisosDeRestaurantes.LINEA_ANULAR);
        return anularLineaInterno(contexto, lineaId, motivo, texto, false);
    }

    /**
     * Anula una linea ya lista o entregada, que ya hizo merma (CT-03). Exige {@code restaurantes.linea.anular_preparada}; en
     * GraphQL viaja por preparar y confirmar.
     */
    public CuentaVista anularLineaPreparada(ContextoNegocio contexto, Autorizacion autorizacion, UUID lineaId,
                                            MotivoDeAnulacion motivo, String texto) {
        autorizacion.exigir(PermisosDeRestaurantes.LINEA_ANULAR_PREPARADA);
        return anularLineaInterno(contexto, lineaId, motivo, texto, true);
    }

    private CuentaVista anularLineaInterno(ContextoNegocio contexto, UUID lineaId, MotivoDeAnulacion motivo, String texto,
                                           boolean incluirPreparadas) {
        exigirMotivo(motivo, texto);
        synchronized (cerrojo) {
            var cuenta = cuentas.cuentaDeLinea(contexto.negocioId(), lineaId)
                    .orElseThrow(() -> new RestaurantesException(CodigoError.LINEA_NO_ENCONTRADA));
            if (!cuenta.estado().noPagada()) {
                throw new RestaurantesException(CodigoError.CUENTA_NO_ABIERTA);
            }
            var linea = lineaDe(cuenta, lineaId);
            if (linea.estado() != EstadoDeLinea.ENVIADA) {
                throw new RestaurantesException(CodigoError.TRANSICION_INVALIDA);
            }
            if (!incluirPreparadas && comandaPreparada(cuenta.negocioId(), linea)) {
                throw new RestaurantesException(CodigoError.LINEA_PREPARADA);
            }
            var ahora = reloj.instant();
            var actualizada = guardar(cuenta, c -> {
                c.lineas = c.lineas.stream().map(l -> l.id().equals(lineaId) ? anulada(l, motivo, texto, ahora) : l).toList();
                if (c.estado == EstadoDeCuenta.POR_COBRAR) {
                    c.totalCalculado = c.lineas.stream().map(LineaDeCuenta::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
                }
            });
            cerrarComandasVacias(actualizada, List.of(lineaId), contexto.actor(), ahora);
            return vista(actualizada);
        }
    }

    /**
     * Cancela la cuenta con motivo: anula sus lineas y libera la mesa (CT-11). Directa si ninguna linea llego a lista;
     * si alguna lo hizo, exige el permiso de gerente por {@link #cancelarCuentaPreparada}. Exige
     * {@code restaurantes.cuenta.cancelar}.
     *
     * @throws RestaurantesException MOTIVO_REQUERIDO, MOTIVO_INVALIDO, CUENTA_NO_ABIERTA o CUENTA_CON_LINEAS_PREPARADAS
     */
    public CuentaVista cancelarCuenta(ContextoNegocio contexto, Autorizacion autorizacion, UUID cuentaId,
                                      MotivoDeAnulacion motivo, String texto) {
        autorizacion.exigir(PermisosDeRestaurantes.CUENTA_CANCELAR);
        return cancelarInterno(contexto, cuentaId, motivo, texto, false);
    }

    /** Cancela una cuenta con lineas ya listas o entregadas. Exige {@code restaurantes.linea.anular_preparada}. */
    public CuentaVista cancelarCuentaPreparada(ContextoNegocio contexto, Autorizacion autorizacion, UUID cuentaId,
                                               MotivoDeAnulacion motivo, String texto) {
        autorizacion.exigir(PermisosDeRestaurantes.LINEA_ANULAR_PREPARADA);
        return cancelarInterno(contexto, cuentaId, motivo, texto, true);
    }

    private CuentaVista cancelarInterno(ContextoNegocio contexto, UUID cuentaId, MotivoDeAnulacion motivo, String texto,
                                        boolean incluirPreparadas) {
        exigirMotivo(motivo, texto);
        CuentaVista vista;
        synchronized (cerrojo) {
            var cuenta = cuentaDe(contexto.negocioId(), cuentaId);
            if (!cuenta.estado().noPagada()) {
                throw new RestaurantesException(CodigoError.CUENTA_NO_ABIERTA);
            }
            if (!incluirPreparadas && cuenta.lineas().stream().anyMatch(l -> l.estado() == EstadoDeLinea.ENVIADA
                    && comandaPreparada(cuenta.negocioId(), l))) {
                throw new RestaurantesException(CodigoError.CUENTA_CON_LINEAS_PREPARADAS);
            }
            var ahora = reloj.instant();
            var anuladas = cuenta.lineas().stream().filter(l -> l.estado() != EstadoDeLinea.ANULADA)
                    .map(LineaDeCuenta::id).toList();
            var cancelada = guardar(cuenta, c -> {
                c.lineas = c.lineas.stream().map(l -> l.estado() == EstadoDeLinea.ANULADA ? l : anulada(l, motivo, texto, ahora))
                        .toList();
                c.estado = EstadoDeCuenta.CANCELADA;
                c.cerradaEn = ahora;
                c.motivoDeCierre = motivo;
            });
            cerrarComandasVacias(cancelada, anuladas, contexto.actor(), ahora);
            pausarSiHayBromas(cancelada, ahora);
            vista = vista(cancelada);
        }
        disponibilidad.recalcularDisponibilidad(contexto.negocioId());
        return vista;
    }

    /**
     * QR-17: tres cuentas abiertas por QR y canceladas por {@code SIN_COMENSALES} en la misma mesa dentro de una hora
     * pausan el QR de esa mesa una hora y lo avisan. Una cuenta del personal no cuenta.
     */
    private void pausarSiHayBromas(Cuenta cancelada, Instant ahora) {
        if (cancelada.origen() != OrigenDeCuenta.QR || cancelada.motivoDeCierre() != MotivoDeAnulacion.SIN_COMENSALES
                || cancelada.mesaId() == null) {
            return;
        }
        var desde = ahora.minus(limites.ventanaDeBromas());
        var bromas = cuentas.cuentas(cancelada.negocioId()).stream()
                .filter(c -> c.origen() == OrigenDeCuenta.QR && cancelada.mesaId().equals(c.mesaId())
                        && c.estado() == EstadoDeCuenta.CANCELADA && c.motivoDeCierre() == MotivoDeAnulacion.SIN_COMENSALES
                        && c.cerradaEn() != null && !c.cerradaEn().isBefore(desde)).count();
        if (bromas < limites.cancelacionesParaPausar()) {
            return;
        }
        for (var intento = 0; intento < 3; intento++) {
            var mesa = salon.mesa(cancelada.negocioId(), cancelada.mesaId()).filter(Mesa::activa).orElse(null);
            if (mesa == null || mesa.qrPausado(ahora)) {
                return;
            }
            var hasta = ahora.plus(SalonServicio.PAUSA_DEL_QR);
            if (salon.reemplazar(mesa.conQrPausadoHasta(hasta, ahora), mesa.version())) {
                eventos.publicar(EventosDeRestaurantes.qrPausado(cancelada.negocioId(), mesa.etiqueta(), hasta, ahora));
                return;
            }
        }
    }

    /**
     * Cierra una cuenta cobrada fuera del flujo, sin venta, y libera la mesa (CT-15). Exige
     * {@code restaurantes.servicio.operar} y el permiso de gerente ({@code restaurantes.linea.anular_preparada}), con
     * motivo.
     *
     * @throws RestaurantesException MOTIVO_REQUERIDO o CUENTA_NO_SOLICITADA
     */
    public CuentaVista liberarMesa(ContextoNegocio contexto, Autorizacion autorizacion, UUID cuentaId, String motivo) {
        autorizacion.exigir(PermisosDeRestaurantes.SERVICIO_OPERAR);
        autorizacion.exigir(PermisosDeRestaurantes.LINEA_ANULAR_PREPARADA);
        if (motivo == null || motivo.isBlank()) {
            throw new RestaurantesException(CodigoError.MOTIVO_REQUERIDO);
        }
        CuentaVista vista;
        synchronized (cerrojo) {
            var cuenta = cuentaDe(contexto.negocioId(), cuentaId);
            if (cuenta.estado() != EstadoDeCuenta.POR_COBRAR) {
                throw new RestaurantesException(CodigoError.CUENTA_NO_SOLICITADA);
            }
            var ahora = reloj.instant();
            var pagada = guardar(cuenta, c -> {
                c.estado = EstadoDeCuenta.PAGADA;
                c.cerradaEn = ahora;
            });
            vista = vista(pagada);
        }
        disponibilidad.recalcularDisponibilidad(contexto.negocioId());
        return vista;
    }

    /**
     * Lo que se mostraria al preparar anular una linea ya preparada, sin escribir (CT-03): la linea, su estado y el aviso de
     * que ya hay merma. Lo usa el manejador del comando.
     */
    String resumenDeAnulacion(ContextoNegocio contexto, UUID lineaId, MotivoDeAnulacion motivo, String texto) {
        exigirMotivo(motivo, texto);
        var cuenta = cuentas.cuentaDeLinea(contexto.negocioId(), lineaId)
                .orElseThrow(() -> new RestaurantesException(CodigoError.LINEA_NO_ENCONTRADA));
        if (!cuenta.estado().noPagada()) {
            throw new RestaurantesException(CodigoError.CUENTA_NO_ABIERTA);
        }
        var linea = lineaDe(cuenta, lineaId);
        if (linea.estado() != EstadoDeLinea.ENVIADA) {
            throw new RestaurantesException(CodigoError.TRANSICION_INVALIDA);
        }
        var estado = linea.comandaId() == null ? null : cuentas.comanda(contexto.negocioId(), linea.comandaId())
                .map(Comanda::estado).orElse(null);
        return "Anular " + linea.cantidad() + " × " + linea.nombre() + " de la cuenta " + cuenta.numero()
                + (estado != null && estado.preparada() ? ", que ya estaba " + (estado == EstadoDeComanda.LISTA ? "lista"
                : "entregada") + ". Ya se preparó: se registra como merma." : ".")
                + " Motivo: " + MensajeDeMotivo.de(motivo, texto) + ".";
    }

    /** Lo que se mostraria al preparar cancelar una cuenta con platillos ya preparados, sin escribir (CT-11). */
    String resumenDeCancelacion(ContextoNegocio contexto, UUID cuentaId, MotivoDeAnulacion motivo, String texto) {
        exigirMotivo(motivo, texto);
        var cuenta = cuentaDe(contexto.negocioId(), cuentaId);
        if (!cuenta.estado().noPagada()) {
            throw new RestaurantesException(CodigoError.CUENTA_NO_ABIERTA);
        }
        var vivas = cuenta.lineas().stream().filter(l -> l.estado() != EstadoDeLinea.ANULADA).toList();
        var preparadas = vivas.stream().filter(l -> l.estado() == EstadoDeLinea.ENVIADA
                && comandaPreparada(cuenta.negocioId(), l)).count();
        return "Cancelar la cuenta " + cuenta.numero() + " con " + vivas.size() + " línea(s), " + preparadas
                + " ya preparada(s): se registra como merma y se libera la mesa. Motivo: "
                + MensajeDeMotivo.de(motivo, texto) + ".";
    }

    // ---- consultas ----

    /** Una cuenta con sus lineas. Exige {@code restaurantes.cuenta.ver}. */
    public CuentaVista cuenta(ContextoNegocio contexto, Autorizacion autorizacion, UUID cuentaId) {
        autorizacion.exigir(PermisosDeRestaurantes.CUENTA_VER);
        return vista(cuentaDe(contexto.negocioId(), cuentaId));
    }

    /**
     * Las cuentas abiertas o por cobrar, de una mesa o de todo el negocio, por numero. Exige
     * {@code restaurantes.cuenta.ver}.
     *
     * @param mesaId la mesa, o nulo para todo el negocio
     */
    public List<CuentaVista> cuentasAbiertas(ContextoNegocio contexto, Autorizacion autorizacion, UUID mesaId) {
        autorizacion.exigir(PermisosDeRestaurantes.CUENTA_VER);
        return cuentas.cuentas(contexto.negocioId()).stream().filter(c -> c.estado().noPagada())
                .filter(c -> mesaId == null || mesaId.equals(c.mesaId()))
                .sorted(Comparator.comparingInt(Cuenta::numero)).map(this::vista).toList();
    }

    /**
     * Lo que la caja necesita para cobrar: lineas agregadas por platillo, total y referencia (CT-07). Exige
     * {@code restaurantes.cuenta.ver}.
     *
     * @throws RestaurantesException CUENTA_NO_SOLICITADA si no esta por cobrar
     */
    public CuentaParaCobro cuentaParaCobro(ContextoNegocio contexto, Autorizacion autorizacion, UUID cuentaId) {
        autorizacion.exigir(PermisosDeRestaurantes.CUENTA_VER);
        var cuenta = cuentaDe(contexto.negocioId(), cuentaId);
        if (cuenta.estado() != EstadoDeCuenta.POR_COBRAR) {
            throw new RestaurantesException(CodigoError.CUENTA_NO_SOLICITADA);
        }
        var porPlatillo = new LinkedHashMap<UUID, CuentaParaCobro.LineaDeCobro>();
        for (var linea : cuenta.lineas()) {
            if (linea.estado() == EstadoDeLinea.ANULADA) {
                continue;
            }
            porPlatillo.merge(linea.platilloId(), new CuentaParaCobro.LineaDeCobro(linea.platilloId(), linea.nombre(),
                    linea.cantidad()), (a, b) -> new CuentaParaCobro.LineaDeCobro(a.productoId(), a.descripcion(),
                    a.cantidad() + b.cantidad()));
        }
        return new CuentaParaCobro(cuenta.id(), cuenta.referencia(), cuenta.totalCalculado(),
                List.copyOf(porPlatillo.values()));
    }

    // ---- datos que se borran (CT-12) ----

    /**
     * Borra la nota y el hash del dispositivo de las lineas con mas de 30 dias (CT-12, D-172). Lo llama una tarea diaria.
     *
     * @return cuantas lineas se limpiaron
     */
    public int purgarDatosViejos(Instant ahora) {
        var limite = ahora.minus(RETENCION);
        var limpiadas = 0;
        synchronized (cerrojo) {
            for (var cuenta : cuentas.cuentasConDatosAnterioresA(limite)) {
                var cuantas = (int) cuenta.lineas().stream().filter(l -> l.creadaEn().isBefore(limite)
                        && (l.nota() != null || l.dispositivoHash() != null)).count();
                var limpia = cuenta.cambiar(c -> c.lineas = c.lineas.stream().map(l -> l.creadaEn().isBefore(limite)
                        ? l.cambiar(x -> {
                            x.nota = null;
                            x.dispositivoHash = null;
                        }) : l).toList());
                if (cuentas.reemplazar(limpia, cuenta.version())) {
                    limpiadas += cuantas;
                }
            }
        }
        return limpiadas;
    }

    // ---- ayudas (compartidas con el pedido por QR) ----

    Cuenta cuentaDe(UUID negocioId, UUID cuentaId) {
        return cuentas.cuenta(negocioId, cuentaId)
                .orElseThrow(() -> new RestaurantesException(CodigoError.CUENTA_NO_ENCONTRADA));
    }

    private static void exigirAbierta(Cuenta cuenta) {
        if (cuenta.estado() != EstadoDeCuenta.ABIERTA) {
            throw new RestaurantesException(CodigoError.CUENTA_NO_ABIERTA);
        }
    }

    private static LineaDeCuenta lineaDe(Cuenta cuenta, UUID lineaId) {
        return cuenta.lineas().stream().filter(l -> l.id().equals(lineaId)).findFirst()
                .orElseThrow(() -> new RestaurantesException(CodigoError.LINEA_NO_ENCONTRADA));
    }

    Platillo platilloOrdenable(UUID negocioId, UUID platilloId) {
        var platillo = menu.platillo(negocioId, platilloId)
                .orElseThrow(() -> new RestaurantesException(CodigoError.PLATILLO_NO_ENCONTRADO));
        platillo.exigirOrdenable(LocalDate.now(reloj.withZone(MenuServicio.ZONA)));
        return platillo;
    }

    Estacion estacionDe(Platillo platillo) {
        if (platillo.categoriaId() == null) {
            return Estacion.COCINA;
        }
        return menu.categoria(platillo.negocioId(), platillo.categoriaId()).map(Categoria::estacion).orElse(Estacion.COCINA);
    }

    /** Guarda la cuenta con el cambio; si otra persona se adelanto, CONCURRENCIA. */
    Cuenta guardar(Cuenta cuenta, java.util.function.Consumer<Cuenta.Cambios> cambio) {
        var nueva = cuenta.cambiar(cambio);
        if (!cuentas.reemplazar(nueva, cuenta.version())) {
            throw new RestaurantesException(CodigoError.CONCURRENCIA);
        }
        return nueva;
    }

    private static List<LineaDeCuenta> concat(List<LineaDeCuenta> lineas, LineaDeCuenta nueva) {
        var todas = new ArrayList<>(lineas);
        todas.add(nueva);
        return todas;
    }

    private static LineaDeCuenta anulada(LineaDeCuenta linea, MotivoDeAnulacion motivo, String texto, Instant ahora) {
        return linea.cambiar(c -> {
            c.estado = EstadoDeLinea.ANULADA;
            c.motivo = motivo;
            c.textoDelMotivo = motivo == MotivoDeAnulacion.OTRO ? texto.strip() : null;
            c.anuladaEn = ahora;
        });
    }

    private static void exigirMotivo(MotivoDeAnulacion motivo, String texto) {
        if (motivo == null) {
            throw new RestaurantesException(CodigoError.MOTIVO_REQUERIDO);
        }
        if (motivo == MotivoDeAnulacion.OTRO) {
            if (texto == null || texto.isBlank()) {
                throw new RestaurantesException(CodigoError.MOTIVO_REQUERIDO);
            }
            if (texto.strip().length() > LineaDeCuenta.MAX_TEXTO) {
                throw new RestaurantesException(CodigoError.MOTIVO_INVALIDO);
            }
        }
    }

    private boolean comandaPreparada(UUID negocioId, LineaDeCuenta linea) {
        return linea.comandaId() != null && cuentas.comanda(negocioId, linea.comandaId())
                .map(c -> c.estado().preparada()).orElse(false);
    }

    /** Despues de anular lineas, publica la comanda actualizada y cierra la que se quedo sin lineas vivas (CD-05). */
    private void cerrarComandasVacias(Cuenta cuenta, List<UUID> lineasAnuladas, String actor, Instant ahora) {
        var comandasAfectadas = cuenta.lineas().stream().filter(l -> lineasAnuladas.contains(l.id()))
                .map(LineaDeCuenta::comandaId).filter(Objects::nonNull).distinct().toList();
        for (var comandaId : comandasAfectadas) {
            var comanda = cuentas.comanda(cuenta.negocioId(), comandaId).orElse(null);
            if (comanda == null) {
                continue;
            }
            var anuladasDeEsta = lineasAnuladas.stream().filter(comanda.lineaIds()::contains).toList();
            var sinVivas = cuenta.lineas().stream().filter(l -> comanda.lineaIds().contains(l.id()))
                    .allMatch(l -> l.estado() == EstadoDeLinea.ANULADA);
            var resultado = comanda;
            if (sinVivas && comanda.estado() != EstadoDeComanda.ENTREGADA) {
                var cerrada = comanda.cerradaPorAnulacion(actor, ahora);
                if (cuentas.reemplazar(cerrada, comanda.version())) {
                    resultado = cerrada;
                }
            }
            eventos.publicar(EventosDeRestaurantes.comandaActualizada(resultado, anuladasDeEsta, ahora));
        }
    }

    String etiquetaDeMesa(Cuenta cuenta) {
        if (cuenta.mesaId() == null) {
            return "Mostrador";
        }
        return salon.mesa(cuenta.negocioId(), cuenta.mesaId()).map(Mesa::etiqueta).orElse("Mesa");
    }

    CuentaVista vista(Cuenta cuenta) {
        var estados = new LinkedHashMap<UUID, EstadoDeComanda>();
        cuentas.comandas(cuenta.negocioId()).stream().filter(c -> c.cuentaId().equals(cuenta.id()))
                .forEach(c -> estados.put(c.id(), c.estado()));
        var lineas = cuenta.lineas().stream().map(l -> LineaVista.de(l, l.comandaId() == null ? null : estados.get(l.comandaId())))
                .toList();
        var total = cuenta.estado() == EstadoDeCuenta.POR_COBRAR && cuenta.totalCalculado() != null
                ? cuenta.totalCalculado() : cuenta.total();
        return new CuentaVista(cuenta.id(), cuenta.numero(), cuenta.mesaId(), etiquetaDeMesa(cuenta), cuenta.estado(),
                cuenta.origen(), cuenta.comensales(), cuenta.abiertaEn(), cuenta.solicitadaEn(), cuenta.presenciaConfirmada(),
                total, cuenta.discrepancia(), cuenta.ventaAnulada(), cuenta.ventaId(), lineas, cuenta.version());
    }

    ComandaVista vistaDeComanda(Comanda comanda, Cuenta cuenta) {
        return Tablero.vista(comanda, cuenta, etiquetaDeMesa(cuenta), reloj.instant());
    }

    /** Un resumen sin datos de la peticion. */
    @Override
    public String toString() {
        return "CuentasServicio";
    }

    /** Las lineas de la cuenta por su id, para quien arma vistas propias. */
    Map<UUID, LineaDeCuenta> lineasPorId(Cuenta cuenta) {
        var mapa = new LinkedHashMap<UUID, LineaDeCuenta>();
        cuenta.lineas().forEach(l -> mapa.put(l.id(), l));
        return mapa;
    }
}
