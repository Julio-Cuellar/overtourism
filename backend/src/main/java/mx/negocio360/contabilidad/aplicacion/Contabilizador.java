package mx.negocio360.contabilidad.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.compartido.tiempo.ZonaMexico;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioReglas;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioVentasContables;
import mx.negocio360.contabilidad.contratos.VentaRegistradaV1;
import mx.negocio360.contabilidad.dominio.Asiento;
import mx.negocio360.contabilidad.dominio.DesgloseIva;
import mx.negocio360.contabilidad.dominio.Disparador;
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

    private final RepositorioReglas reglas;
    private final RepositorioVentasContables ventas;
    private final LibroDiario libro;
    private final Clock reloj;

    /**
     * Crea el contabilizador.
     *
     * @param reglas tabla de reglas
     * @param ventas tabla de huellas de venta
     * @param libro  libro diario
     * @param reloj  reloj de la plataforma
     */
    public Contabilizador(RepositorioReglas reglas, RepositorioVentasContables ventas, LibroDiario libro,
                          Clock reloj) {
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
     * @return el resultado del evento
     */
    public ResultadoEvento contabilizarVenta(SobreEvento evento) {
        var negocioId = evento.negocioId();
        var venta = VentaRegistradaV1.leer(evento.datos());
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
