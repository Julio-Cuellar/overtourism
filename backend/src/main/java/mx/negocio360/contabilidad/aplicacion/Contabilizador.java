package mx.negocio360.contabilidad.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.compartido.tiempo.ZonaMexico;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioEventosProcesados;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioReglas;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioVentasContables;
import mx.negocio360.contabilidad.contratos.VentaRegistradaV1;
import mx.negocio360.contabilidad.dominio.Asiento;
import mx.negocio360.contabilidad.dominio.DesgloseIva;
import mx.negocio360.contabilidad.dominio.Disparador;
import mx.negocio360.contabilidad.dominio.EventoProcesado;
import mx.negocio360.contabilidad.dominio.OrigenPoliza;
import mx.negocio360.contabilidad.dominio.ReglaContable;
import mx.negocio360.contabilidad.dominio.Renglon;
import mx.negocio360.contabilidad.dominio.ResultadoEvento;
import mx.negocio360.contabilidad.dominio.TipoPoliza;
import mx.negocio360.contabilidad.dominio.VentaContable;

/**
 * Escrituras del sistema (seccion 7.3): convierte los eventos de otros modulos en polizas. No pide confirmacion
 * humana porque son consecuencia de hechos ya confirmados (D-22). Nunca llama a otro modulo para completar un dato.
 */
public final class Contabilizador {

    /** Actor de las polizas automaticas. */
    public static final String SISTEMA = "sistema";

    /** Motivo de un evento que ya se habia procesado (E-01). */
    public static final String YA_PROCESADO = "YA_PROCESADO";
    /** Motivo de un evento distinto sobre una venta, cupon o corte ya contabilizado (E-02). */
    public static final String DUPLICADO_NATURAL = "DUPLICADO_NATURAL";

    private final RepositorioEventosProcesados procesados;
    private final RepositorioReglas reglas;
    private final RepositorioVentasContables ventas;
    private final LibroDiario libro;
    private final Clock reloj;

    /**
     * Crea el contabilizador.
     *
     * @param procesados tabla de eventos procesados
     * @param reglas     tabla de reglas
     * @param ventas     tabla de huellas de venta
     * @param libro  libro diario
     * @param reloj  reloj de la plataforma
     */
    public Contabilizador(RepositorioEventosProcesados procesados, RepositorioReglas reglas,
                          RepositorioVentasContables ventas, LibroDiario libro, Clock reloj) {
        this.procesados = Objects.requireNonNull(procesados, "procesados");
        this.reglas = Objects.requireNonNull(reglas, "reglas");
        this.ventas = Objects.requireNonNull(ventas, "ventas");
        this.libro = Objects.requireNonNull(libro, "libro");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    /**
     * {@code contabilizarVenta}: poliza de ingreso con cargo a la cuenta del medio de pago y abono a ventas e IVA
     * (F-02), con el desglose de A-07.
     *
     * @param evento sobre de {@code venta.registrada} v1
     * @return el resultado del evento; OMITIDO con {@value #YA_PROCESADO} si ya se habia procesado (E-01) o con
     *         {@value #DUPLICADO_NATURAL} si la venta ya estaba contabilizada (E-02)
     */
    public ResultadoEvento contabilizarVenta(SobreEvento evento) {
        return procesar(evento, this::aplicarVenta);
    }

    /**
     * Aplica un evento una sola vez. La fila de {@code evento_procesado} se escribe junto con el efecto: si el
     * efecto falla, el evento no queda marcado y la reentrega lo intenta de nuevo (E-01).
     */
    private ResultadoEvento procesar(SobreEvento evento, Function<SobreEvento, ResultadoEvento> efecto) {
        if (procesados.de(evento.negocioId(), evento.id()).isPresent()) {
            return ResultadoEvento.omitido(YA_PROCESADO);
        }
        var resultado = efecto.apply(evento);
        var fila = new EventoProcesado(evento.negocioId(), evento.id(), evento.tipo(), evento.version(),
                reloj.instant(), resultado.resultado(), resultado.motivo());
        if (!procesados.insertar(fila)) {
            throw new IllegalStateException("Otra entrega del mismo evento gano la carrera; se deshace esta");
        }
        return resultado;
    }

    private ResultadoEvento aplicarVenta(SobreEvento evento) {
        var negocioId = evento.negocioId();
        var venta = VentaRegistradaV1.leer(evento.datos());
        if (ventas.porVenta(negocioId, venta.ventaId()).isPresent()) {
            return ResultadoEvento.omitido(DUPLICADO_NATURAL);
        }
        var regla = reglaVigente(negocioId, Disparador.VENTA, venta.medioPago()).orElseThrow();
        var desglose = DesgloseIva.de(venta.total(), regla.tasaIva());
        var renglones = new ArrayList<Renglon>();
        renglones.add(new Renglon(regla.cuentaCargoId(), desglose.ventas().add(desglose.iva()), null, null));
        renglones.add(new Renglon(regla.cuentaAbonoId(), null, desglose.ventas(), null));
        if (desglose.iva().signum() > 0) {
            renglones.add(new Renglon(regla.cuentaImpuestoId(), null, desglose.iva(), null));
        }
        var asiento = new Asiento(TipoPoliza.INGRESO, fechaContable(evento.ocurridoEn()),
                "Venta " + corto(venta.ventaId()), renglones);
        var poliza = libro.registrar(negocioId, asiento, new LibroDiario.Registro(OrigenPoliza.AUTOMATICA,
                evento.id(), "VENTA", venta.ventaId(), null, SISTEMA));
        ventas.guardar(new VentaContable(UuidV7.generar(reloj.instant()), negocioId, venta.ventaId(),
                venta.cajaSesionId(), venta.medioPago(), venta.total(), desglose.ventas(), desglose.iva(),
                evento.ocurridoEn(), poliza.id(), false));
        return ResultadoEvento.aplicado(poliza.id());
    }

    private Optional<ReglaContable> reglaVigente(UUID negocioId, Disparador disparador, String condicion) {
        var ahora = reloj.instant();
        return reglas.delNegocio(negocioId).stream()
                .filter(r -> r.disparador() == disparador && Objects.equals(r.condicion(), condicion)
                        && r.vigenteEn(ahora))
                .findFirst();
    }

    private static LocalDate fechaContable(Instant instante) {
        return LocalDate.ofInstant(instante, ZonaMexico.ZONA);
    }

    private static String corto(UUID id) {
        return id.toString().substring(0, 8);
    }
}
