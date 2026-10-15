package mx.negocio360.caja.aplicacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.caja.aplicacion.puertos.LibroDeCaja;
import mx.negocio360.caja.aplicacion.puertos.RepositorioCaja;
import mx.negocio360.caja.contratos.PagoV1;
import mx.negocio360.caja.contratos.VentaAnuladaV1;
import mx.negocio360.caja.contratos.VentaRegistradaV1;
import mx.negocio360.caja.dominio.EstadoTurno;
import mx.negocio360.caja.dominio.EstadoVentaAplicada;
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
 * <p>Pendiente CJ-11 (P2): una venta para un turno ya cerrado se reporta como {@link ResultadoAplicacion#SIN_TURNO_ABIERTO}
 * y no se registra como ajuste.
 */
public final class ConsumidorVentas {

    /** Nombre de este consumidor en {@code evento_procesado}. */
    public static final String CONSUMIDOR = "caja.ventas";

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
     * Procesa un evento del bus.
     *
     * @param evento sobre recibido
     * @return que se hizo
     * @throws IllegalArgumentException si los datos no cumplen el contrato (error permanente)
     */
    public ResultadoAplicacion procesar(SobreEvento evento) {
        Objects.requireNonNull(evento, "evento");
        return switch (evento.tipo()) {
            case VentaRegistradaV1.TIPO -> {
                var venta = mapeador.convertValue(evento.datos(), VentaRegistradaV1.class);
                validarMetodos(venta.pagos());
                yield libro.registrarEvento(CONSUMIDOR, evento.id()) ? aplicar(evento, venta)
                        : ResultadoAplicacion.YA_PROCESADO;
            }
            case VentaAnuladaV1.TIPO -> {
                var venta = mapeador.convertValue(evento.datos(), VentaAnuladaV1.class);
                validarMetodos(venta.pagos());
                yield libro.registrarEvento(CONSUMIDOR, evento.id()) ? revertir(evento, venta)
                        : ResultadoAplicacion.YA_PROCESADO;
            }
            default -> ResultadoAplicacion.IGNORADO;
        };
    }

    private ResultadoAplicacion aplicar(SobreEvento evento, VentaRegistradaV1 venta) {
        var negocio = evento.negocioId();
        var previa = libro.ventaAplicada(negocio, venta.ventaId());
        if (previa.isPresent()) {
            if (previa.get().estado() != EstadoVentaAplicada.ANULADO_PREVIO) {
                return ResultadoAplicacion.YA_APLICADA;
            }
            libro.guardarVentaAplicada(new VentaAplicada(negocio, venta.ventaId(), venta.turnoId(),
                    EstadoVentaAplicada.REVERTIDA));
            return ResultadoAplicacion.OMITIDA_ANULADO_PREVIO;
        }
        var turno = turnoNoCerrado(negocio, venta.turnoId());
        if (turno == null) {
            return ResultadoAplicacion.SIN_TURNO_ABIERTO;
        }
        registrar(evento, turno, venta.ventaId(), venta.pagos(), TipoMovimiento.VENTA);
        libro.guardarVentaAplicada(new VentaAplicada(negocio, venta.ventaId(), turno.id(),
                EstadoVentaAplicada.APLICADA));
        return ResultadoAplicacion.APLICADA;
    }

    private ResultadoAplicacion revertir(SobreEvento evento, VentaAnuladaV1 venta) {
        var negocio = evento.negocioId();
        var previa = libro.ventaAplicada(negocio, venta.ventaId());
        if (previa.isEmpty()) {
            libro.guardarVentaAplicada(new VentaAplicada(negocio, venta.ventaId(), venta.turnoId(),
                    EstadoVentaAplicada.ANULADO_PREVIO));
            return ResultadoAplicacion.MARCADA_ANULADO_PREVIO;
        }
        if (previa.get().estado() != EstadoVentaAplicada.APLICADA) {
            return ResultadoAplicacion.YA_REVERTIDA;
        }
        var turno = turnoNoCerrado(negocio, previa.get().turnoId());
        if (turno == null) {
            return ResultadoAplicacion.SIN_TURNO_ABIERTO;
        }
        registrar(evento, turno, venta.ventaId(), venta.pagos(), TipoMovimiento.ANULACION);
        libro.guardarVentaAplicada(new VentaAplicada(negocio, venta.ventaId(), turno.id(),
                EstadoVentaAplicada.REVERTIDA));
        return ResultadoAplicacion.REVERTIDA;
    }

    /** Un movimiento por pago; la anulacion va en negativo. La hora es la del hecho en Ventas. */
    private void registrar(SobreEvento evento, Turno turno, UUID ventaId, List<PagoV1> pagos, TipoMovimiento tipo) {
        for (var pago : pagos) {
            var monto = Redondeo.dinero(pago.monto());
            libro.agregar(new MovimientoCaja(UuidV7.generar(evento.ocurridoEn()), evento.negocioId(), turno.id(), tipo,
                    pago.metodoDeCaja(), tipo == TipoMovimiento.ANULACION ? monto.negate() : monto, ventaId,
                    evento.id(), null, evento.ocurridoEn(), null));
        }
    }

    private Turno turnoNoCerrado(UUID negocioId, UUID turnoId) {
        return cajas.turno(negocioId, turnoId).filter(t -> t.estado() != EstadoTurno.CERRADO).orElse(null);
    }

    private static void validarMetodos(List<PagoV1> pagos) {
        pagos.forEach(PagoV1::metodoDeCaja);
    }
}
