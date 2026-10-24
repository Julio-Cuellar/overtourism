package mx.negocio360.restaurantes.aplicacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.compartido.eventos.MapeadorEventos;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.restaurantes.contratos.VentaAnuladaV1;
import mx.negocio360.restaurantes.contratos.VentaRegistradaV1;
import mx.negocio360.restaurantes.dominio.CodigoError;
import mx.negocio360.restaurantes.dominio.Cuenta;
import mx.negocio360.restaurantes.dominio.EstadoDeCuenta;
import mx.negocio360.restaurantes.dominio.EstadoDeLinea;
import mx.negocio360.restaurantes.dominio.LineaDeCuenta;
import mx.negocio360.restaurantes.dominio.RestaurantesException;

/**
 * El vinculo de las cuentas con Ventas (CT-08 a CT-10). Restaurantes no escribe la venta: la cobra la caja con
 * {@code registrarVenta} y la {@code referencia} de la cuenta (CT-07). Aqui se lee de vuelta:
 * <ul>
 *   <li>{@code venta.registrada} con {@code restaurantes:cuenta:<id>} de una cuenta de ese negocio la deja {@code PAGADA}
 *       con su {@code venta_id} y libera la mesa. Si lo vendido difiere de las lineas de la cuenta queda
 *       {@code discrepancia} y se avisa (CT-08). La venta gana: una cuenta todavia {@code ABIERTA} tambien se paga, siempre
 *       con discrepancia (CT-09). Anular y volver a vender con la misma referencia reemplaza a la venta anulada.</li>
 *   <li>{@code venta.anulada} de una venta vinculada deja la cuenta {@code PAGADA} con {@code venta_anulada}; la mesa no se
 *       reocupa (CT-10).</li>
 * </ul>
 * Es un lector tolerante (R5) con entrega al menos una vez: repetir un evento no cambia nada (idempotente por {@code venta_id},
 * R3) y lo que no cumple el contrato o no es de este negocio se ignora sin lanzar. Solo una carrera con otra escritura de la
 * misma cuenta lanza, para que el evento vuelva a llegar.
 */
public final class VinculoDeCuentasConVentas {

    private static final String ORIGEN_VENTAS = "ventas";
    private static final int INTENTOS = 3;

    /** Que se hizo con un evento de Ventas. */
    public enum Resultado {
        /** La cuenta quedo pagada con esta venta. */
        VINCULADA,
        /** La cuenta ya estaba pagada con esta venta: no se cambio nada. */
        YA_VINCULADA,
        /** La cuenta ya esta pagada con otra venta que no se anulo: se conserva la primera. */
        YA_PAGADA,
        /** La cuenta se cancelo: una venta no la revive. */
        CUENTA_CANCELADA,
        /** La referencia nombra una cuenta que no existe en este negocio. */
        SIN_CUENTA,
        /** Se anoto que la venta de la cuenta se anulo. */
        ANULACION_REGISTRADA,
        /** El evento no es para este lector o no nombra una cuenta de Restaurantes. */
        IGNORADO,
        /** El evento no cumple el contrato que se lee. */
        DATOS_INVALIDOS
    }

    private final RepositorioCuentas cuentas;
    private final CuentasServicio servicio;
    private final SalonServicio disponibilidad;
    private final PublicadorEventos eventos;
    private final ObjectMapper mapeador = MapeadorEventos.crear();

    /**
     * Arma el vinculo.
     *
     * @param cuentas        almacen de cuentas
     * @param servicio       las cuentas, cuyas ayudas comparte
     * @param disponibilidad recalcula y publica la disponibilidad cuando una cuenta libera la mesa
     * @param eventos        publicador de eventos
     */
    public VinculoDeCuentasConVentas(RepositorioCuentas cuentas, CuentasServicio servicio, SalonServicio disponibilidad,
                                     PublicadorEventos eventos) {
        this.cuentas = Objects.requireNonNull(cuentas, "cuentas");
        this.servicio = Objects.requireNonNull(servicio, "servicio");
        this.disponibilidad = Objects.requireNonNull(disponibilidad, "disponibilidad");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
    }

    /**
     * Aplica un evento del bus; los que no son de Ventas, de otra version o de otro tipo se ignoran.
     *
     * @throws RestaurantesException CONCURRENCIA si otra escritura se adelanto tres veces; el evento debe volver a llegar
     */
    public Resultado procesar(SobreEvento evento) {
        Objects.requireNonNull(evento, "evento");
        if (!ORIGEN_VENTAS.equals(evento.origen()) || evento.version() != 1) {
            return Resultado.IGNORADO;
        }
        try {
            return switch (evento.tipo()) {
                case VentaRegistradaV1.TIPO -> registrada(evento, mapeador.convertValue(evento.datos(), VentaRegistradaV1.class));
                case VentaAnuladaV1.TIPO -> anulada(evento, mapeador.convertValue(evento.datos(), VentaAnuladaV1.class));
                default -> Resultado.IGNORADO;
            };
        } catch (IllegalArgumentException e) {
            return Resultado.DATOS_INVALIDOS;
        }
    }

    // ---- venta registrada (CT-08, CT-09) ----

    private Resultado registrada(SobreEvento evento, VentaRegistradaV1 venta) {
        var cuentaId = Cuenta.idDeReferencia(venta.referencia());
        if (cuentaId.isEmpty()) {
            return Resultado.IGNORADO;
        }
        var negocio = evento.negocioId();
        var vendido = vendidoPor(venta);
        for (var intento = 0; intento < INTENTOS; intento++) {
            var cuenta = cuentas.cuenta(negocio, cuentaId.get()).orElse(null);
            if (cuenta == null) {
                return Resultado.SIN_CUENTA;
            }
            if (venta.ventaId().equals(cuenta.ventaId())) {
                return Resultado.YA_VINCULADA;
            }
            if (cuenta.estado() == EstadoDeCuenta.CANCELADA) {
                return Resultado.CUENTA_CANCELADA;
            }
            if (cuenta.estado() == EstadoDeCuenta.PAGADA && !cuenta.ventaAnulada()) {
                return Resultado.YA_PAGADA;
            }
            var discrepancia = cuenta.estado() == EstadoDeCuenta.ABIERTA || !coincide(cuenta, vendido);
            var pagada = intentarPagar(cuenta, venta.ventaId(), discrepancia, evento.ocurridoEn());
            if (pagada == null) {
                continue;
            }
            if (discrepancia) {
                eventos.publicar(EventosDeRestaurantes.cuentaConDiscrepancia(pagada, servicio.etiquetaDeMesa(pagada),
                        venta.ventaId(), evento.ocurridoEn()));
            }
            disponibilidad.recalcularDisponibilidad(negocio);
            return Resultado.VINCULADA;
        }
        throw new RestaurantesException(CodigoError.CONCURRENCIA);
    }

    /** Guarda la cuenta pagada; nulo si otra escritura se adelanto. */
    private Cuenta intentarPagar(Cuenta cuenta, UUID ventaId, boolean discrepancia, Instant cuando) {
        try {
            return servicio.guardar(cuenta, c -> {
                var yaCerrada = cuenta.estado() == EstadoDeCuenta.PAGADA;
                c.estado = EstadoDeCuenta.PAGADA;
                c.ventaId = ventaId;
                c.ventaAnulada = false;
                c.discrepancia = discrepancia;
                c.cerradaEn = yaCerrada ? cuenta.cerradaEn() : cuando;
            });
        } catch (RestaurantesException e) {
            if (e.codigo() == CodigoError.CONCURRENCIA) {
                return null;
            }
            throw e;
        }
    }

    /** Cuanto se vendio de cada producto. */
    private static Map<UUID, BigDecimal> vendidoPor(VentaRegistradaV1 venta) {
        var vendido = new LinkedHashMap<UUID, BigDecimal>();
        venta.lineas().forEach(l -> vendido.merge(l.productoId(), l.cantidad(), BigDecimal::add));
        return vendido;
    }

    /** Verdadero si lo vendido es exactamente lo que la cuenta tiene sin anular. */
    private static boolean coincide(Cuenta cuenta, Map<UUID, BigDecimal> vendido) {
        var enCuenta = new LinkedHashMap<UUID, BigDecimal>();
        cuenta.lineas().stream().filter(l -> l.estado() != EstadoDeLinea.ANULADA)
                .forEach((LineaDeCuenta l) -> enCuenta.merge(l.platilloId(), BigDecimal.valueOf(l.cantidad()), BigDecimal::add));
        if (!enCuenta.keySet().equals(vendido.keySet())) {
            return false;
        }
        return enCuenta.entrySet().stream().allMatch(e -> e.getValue().compareTo(vendido.get(e.getKey())) == 0);
    }

    // ---- venta anulada (CT-10) ----

    private Resultado anulada(SobreEvento evento, VentaAnuladaV1 venta) {
        for (var intento = 0; intento < INTENTOS; intento++) {
            var cuenta = cuentas.cuentaDeVenta(evento.negocioId(), venta.ventaId()).orElse(null);
            if (cuenta == null) {
                return Resultado.IGNORADO;
            }
            if (cuenta.ventaAnulada()) {
                return Resultado.ANULACION_REGISTRADA;
            }
            try {
                servicio.guardar(cuenta, c -> c.ventaAnulada = true);
                return Resultado.ANULACION_REGISTRADA;
            } catch (RestaurantesException e) {
                if (e.codigo() != CodigoError.CONCURRENCIA) {
                    throw e;
                }
            }
        }
        throw new RestaurantesException(CodigoError.CONCURRENCIA);
    }

    @Override
    public String toString() {
        return "VinculoDeCuentasConVentas";
    }
}
