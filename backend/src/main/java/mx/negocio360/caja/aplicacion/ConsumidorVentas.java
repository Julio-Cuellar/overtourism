package mx.negocio360.caja.aplicacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.caja.aplicacion.puertos.LibroDeCaja;
import mx.negocio360.caja.aplicacion.puertos.RepositorioCaja;
import mx.negocio360.caja.contratos.PagoV1;
import mx.negocio360.caja.contratos.VentaAnuladaV1;
import mx.negocio360.caja.contratos.VentaRegistradaV1;
import mx.negocio360.caja.dominio.EstadoTurno;
import mx.negocio360.caja.dominio.EstadoVentaAplicada;
import mx.negocio360.caja.dominio.MetodoPago;
import mx.negocio360.caja.dominio.MovimientoCaja;
import mx.negocio360.caja.dominio.TipoMovimiento;
import mx.negocio360.caja.dominio.Turno;
import mx.negocio360.caja.dominio.VentaAplicada;
import mx.negocio360.compartido.dinero.Redondeo;
import mx.negocio360.compartido.eventos.MapeadorEventos;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;

/**
 * Listener de Caja para {@code venta.registrada} v1 y {@code venta.anulada} v1 (CF-20, CF-21; CJ-03, CJ-04). Son
 * escrituras del sistema: no piden confirmacion humana porque la venta ya se confirmo (mismo criterio que D-22).
 *
 * <p>Idempotente dos veces: por id de evento ({@code evento_procesado}, R3) y por venta ({@code venta_aplicada}),
 * porque la entrega es al menos una vez y sin orden global. El negocio sale del sobre, nunca de los datos.
 *
 * <p>Correcciones de la revision del 2026-10-15:
 * <ul>
 *   <li>A1: el evento se marca como procesado solo si se aplico. Si el turno no existe o ya cerro, responde
 *       {@link ResultadoAplicacion#SIN_TURNO_ABIERTO} sin marcarlo, para que no se pierda (CJ-11 sigue pendiente).</li>
 *   <li>M3: no confia en los datos del evento. Exige origen {@code ventas}, pagos con metodo conocido y montos
 *       positivos, acotados y con 2 decimales; si no, {@link ResultadoAplicacion#DATOS_INVALIDOS} sin efecto. Una
 *       anulacion revierte los movimientos que Caja registro para esa venta, no los montos que diga el evento.</li>
 *   <li>M4: un evento que no cumple el contrato ya no lanza excepcion en cada reintento; se reporta y no se marca.</li>
 * </ul>
 *
 * <p>Pendiente con los adaptadores reales (F5): marcar el evento, escribir los movimientos y guardar
 * {@code venta_aplicada} deben ir en una sola transaccion.
 */
public final class ConsumidorVentas {

    /** Nombre de este consumidor en {@code evento_procesado}. */
    public static final String CONSUMIDOR = "caja.ventas";
    /** Unico modulo que puede publicar los eventos de venta. */
    public static final String ORIGEN_VENTAS = "ventas";

    private final RepositorioCaja cajas;
    private final LibroDeCaja libro;
    private final ObjectMapper mapeador = MapeadorEventos.crear();

    /**
     * Arma el consumidor.
     *
     * @param cajas tablas caja y turno
     * @param libro movimientos, ventas aplicadas y eventos procesados
     */
    public ConsumidorVentas(RepositorioCaja cajas, LibroDeCaja libro) {
        this.cajas = Objects.requireNonNull(cajas, "cajas");
        this.libro = Objects.requireNonNull(libro, "libro");
    }

    /**
     * Procesa un evento del bus. Nunca lanza por datos malos: los reporta como
     * {@link ResultadoAplicacion#DATOS_INVALIDOS}.
     *
     * @param evento sobre recibido
     * @return que se hizo; si {@link ResultadoAplicacion#completado()} es falso, el evento no se marco
     */
    public ResultadoAplicacion procesar(SobreEvento evento) {
        Objects.requireNonNull(evento, "evento");
        return switch (evento.tipo()) {
            case VentaRegistradaV1.TIPO -> leer(evento, VentaRegistradaV1.class)
                    .filter(venta -> pagosValidos(venta.pagos()))
                    .map(venta -> aplicar(evento, venta))
                    .orElse(ResultadoAplicacion.DATOS_INVALIDOS);
            case VentaAnuladaV1.TIPO -> leer(evento, VentaAnuladaV1.class)
                    .map(venta -> revertir(evento, venta.ventaId(), venta.turnoId()))
                    .orElse(ResultadoAplicacion.DATOS_INVALIDOS);
            default -> ResultadoAplicacion.IGNORADO;
        };
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

    private ResultadoAplicacion aplicar(SobreEvento evento, VentaRegistradaV1 venta) {
        var negocio = evento.negocioId();
        var previa = libro.ventaAplicada(negocio, venta.ventaId());
        if (previa.isPresent()) {
            return marcar(evento) ? yaVista(negocio, venta, previa.get()) : ResultadoAplicacion.YA_PROCESADO;
        }
        var turno = turnoNoCerrado(negocio, venta.turnoId());
        if (turno == null) {
            return ResultadoAplicacion.SIN_TURNO_ABIERTO;
        }
        if (!marcar(evento)) {
            return ResultadoAplicacion.YA_PROCESADO;
        }
        for (var pago : venta.pagos()) {
            libro.agregar(movimiento(evento, turno, venta.ventaId(), TipoMovimiento.VENTA, pago.metodoDeCaja(),
                    Redondeo.dinero(pago.monto())));
        }
        libro.guardarVentaAplicada(new VentaAplicada(negocio, venta.ventaId(), turno.id(),
                EstadoVentaAplicada.APLICADA));
        return ResultadoAplicacion.APLICADA;
    }

    /** La venta ya se habia visto: o ya se aplico, o su anulacion llego antes y no debe sumar (D-19). */
    private ResultadoAplicacion yaVista(UUID negocio, VentaRegistradaV1 venta, VentaAplicada previa) {
        if (previa.estado() != EstadoVentaAplicada.ANULADO_PREVIO) {
            return ResultadoAplicacion.YA_APLICADA;
        }
        libro.guardarVentaAplicada(new VentaAplicada(negocio, venta.ventaId(), venta.turnoId(),
                EstadoVentaAplicada.REVERTIDA));
        return ResultadoAplicacion.OMITIDA_ANULADO_PREVIO;
    }

    private ResultadoAplicacion revertir(SobreEvento evento, UUID ventaId, UUID turnoDelEvento) {
        var negocio = evento.negocioId();
        var previa = libro.ventaAplicada(negocio, ventaId);
        if (previa.isEmpty()) {
            if (!marcar(evento)) {
                return ResultadoAplicacion.YA_PROCESADO;
            }
            libro.guardarVentaAplicada(new VentaAplicada(negocio, ventaId, turnoDelEvento,
                    EstadoVentaAplicada.ANULADO_PREVIO));
            return ResultadoAplicacion.MARCADA_ANULADO_PREVIO;
        }
        if (previa.get().estado() != EstadoVentaAplicada.APLICADA) {
            return marcar(evento) ? ResultadoAplicacion.YA_REVERTIDA : ResultadoAplicacion.YA_PROCESADO;
        }
        var turno = turnoNoCerrado(negocio, previa.get().turnoId());
        if (turno == null) {
            return ResultadoAplicacion.SIN_TURNO_ABIERTO;
        }
        if (!marcar(evento)) {
            return ResultadoAplicacion.YA_PROCESADO;
        }
        for (var registrado : movimientosDeVenta(negocio, turno.id(), ventaId)) {
            libro.agregar(movimiento(evento, turno, ventaId, TipoMovimiento.ANULACION, registrado.metodo(),
                    registrado.monto().negate()));
        }
        libro.guardarVentaAplicada(new VentaAplicada(negocio, ventaId, turno.id(), EstadoVentaAplicada.REVERTIDA));
        return ResultadoAplicacion.REVERTIDA;
    }

    /** Los movimientos VENTA que Caja escribio para esa venta: lo unico que una anulacion puede revertir. */
    private List<MovimientoCaja> movimientosDeVenta(UUID negocio, UUID turnoId, UUID ventaId) {
        return libro.movimientos(negocio, turnoId).stream()
                .filter(m -> m.tipo() == TipoMovimiento.VENTA && ventaId.equals(m.ventaId())).toList();
    }

    /** Un movimiento por pago; la hora es la del hecho en Ventas. */
    private static MovimientoCaja movimiento(SobreEvento evento, Turno turno, UUID ventaId, TipoMovimiento tipo,
                                             MetodoPago metodo, BigDecimal monto) {
        return new MovimientoCaja(UuidV7.generar(evento.ocurridoEn()), evento.negocioId(), turno.id(), tipo, metodo,
                monto, ventaId, evento.id(), null, evento.ocurridoEn(), null);
    }

    /** Anota el evento como procesado (R3); falso si ya lo estaba. Solo se llama cuando el evento se va a aplicar. */
    private boolean marcar(SobreEvento evento) {
        return libro.registrarEvento(CONSUMIDOR, evento.id());
    }

    private Turno turnoNoCerrado(UUID negocioId, UUID turnoId) {
        return cajas.turno(negocioId, turnoId).filter(t -> t.estado() != EstadoTurno.CERRADO).orElse(null);
    }

    /** Al menos un pago, cada uno con metodo conocido y monto positivo, acotado y con 2 decimales. */
    private static boolean pagosValidos(List<PagoV1> pagos) {
        return !pagos.isEmpty() && pagos.stream().allMatch(ConsumidorVentas::pagoValido);
    }

    private static boolean pagoValido(PagoV1 pago) {
        var metodoConocido = Arrays.stream(MetodoPago.values()).anyMatch(m -> m.name().equals(pago.metodo()));
        return metodoConocido && Redondeo.dineroValido(pago.monto()) && pago.monto().signum() > 0;
    }
}
