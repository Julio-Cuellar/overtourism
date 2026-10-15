package mx.negocio360.contabilidad.aplicacion;

import java.time.Clock;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioCuentas;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioPeriodos;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioPolizas;
import mx.negocio360.contabilidad.dominio.Asiento;
import mx.negocio360.contabilidad.dominio.CodigoError;
import mx.negocio360.contabilidad.dominio.ContabilidadException;
import mx.negocio360.contabilidad.dominio.OrigenPoliza;
import mx.negocio360.contabilidad.dominio.Periodo;
import mx.negocio360.contabilidad.dominio.Poliza;

/**
 * Unico punto por donde se registran polizas, sean manuales, reversas o automaticas. Revisa las cuentas (A-04) y
 * escribe la poliza en su periodo.
 */
public final class LibroDiario {

    /**
     * Datos de quien y por que se registra la poliza.
     *
     * @param origen           automatica, manual o reversa
     * @param eventoId         evento que la origino, o nulo
     * @param referenciaTipo   VENTA, CANJE, CORTE o PENDIENTE, o nulo
     * @param referenciaId     id de la referencia, o nulo
     * @param reviertePolizaId poliza que revierte, o nulo
     * @param creadoPor        actor o {@code sistema}
     */
    public record Registro(OrigenPoliza origen, UUID eventoId, String referenciaTipo, UUID referenciaId,
                           UUID reviertePolizaId, String creadoPor) {

        /**
         * Registro de una poliza capturada por una persona.
         *
         * @param actor quien la captura
         * @return el registro
         */
        public static Registro manual(String actor) {
            return new Registro(OrigenPoliza.MANUAL, null, null, null, null, actor);
        }
    }

    private final RepositorioCuentas cuentas;
    private final RepositorioPeriodos periodos;
    private final RepositorioPolizas polizas;
    private final Clock reloj;

    /**
     * Crea el libro.
     *
     * @param cuentas  tabla de cuentas
     * @param periodos tabla de periodos
     * @param polizas  tablas de polizas y movimientos
     * @param reloj    reloj de la plataforma
     */
    public LibroDiario(RepositorioCuentas cuentas, RepositorioPeriodos periodos, RepositorioPolizas polizas,
                       Clock reloj) {
        this.cuentas = Objects.requireNonNull(cuentas, "cuentas");
        this.periodos = Objects.requireNonNull(periodos, "periodos");
        this.polizas = Objects.requireNonNull(polizas, "polizas");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    /**
     * Revisa que todas las cuentas del asiento existan en el negocio, sean afectables y esten activas (A-04).
     *
     * @param negocioId negocio
     * @param asiento   asiento a revisar
     * @throws ContabilidadException {@code CUENTA_NO_ENCONTRADA}, {@code CUENTA_NO_AFECTABLE} o
     *                               {@code CUENTA_INACTIVA}
     */
    public void validarCuentas(UUID negocioId, Asiento asiento) {
        for (var renglon : asiento.renglones()) {
            var cuenta = cuentas.porId(negocioId, renglon.cuentaId())
                    .orElseThrow(() -> new ContabilidadException(CodigoError.CUENTA_NO_ENCONTRADA));
            if (!cuenta.afectable()) {
                throw new ContabilidadException(CodigoError.CUENTA_NO_AFECTABLE, cuenta.id());
            }
            if (!cuenta.recibeMovimientos()) {
                throw new ContabilidadException(CodigoError.CUENTA_INACTIVA, cuenta.id());
            }
        }
    }

    /**
     * Registra el asiento como poliza.
     *
     * @param negocioId negocio
     * @param asiento   asiento cuadrado
     * @param registro  origen y referencias
     * @return la poliza registrada
     * @throws ContabilidadException los codigos de {@link #validarCuentas}
     */
    public Poliza registrar(UUID negocioId, Asiento asiento, Registro registro) {
        validarCuentas(negocioId, asiento);
        var ahora = reloj.instant();
        var periodo = periodoDe(negocioId, YearMonth.from(asiento.fecha()));
        var poliza = new Poliza(UuidV7.generar(ahora), negocioId, periodo.id(), asiento.tipo(), 0, asiento.fecha(),
                asiento.concepto(), registro.origen(), registro.eventoId(), registro.referenciaTipo(),
                registro.referenciaId(), false, registro.reviertePolizaId(), asiento.total(), asiento.renglones(),
                registro.creadoPor(), ahora);
        polizas.insertar(poliza);
        return poliza;
    }

    private Periodo periodoDe(UUID negocioId, YearMonth mes) {
        return periodos.delMes(negocioId, mes).orElseGet(() -> {
            var nuevo = Periodo.abierto(UuidV7.generar(reloj.instant()), negocioId, mes, reloj.instant());
            periodos.guardar(nuevo);
            return nuevo;
        });
    }
}
