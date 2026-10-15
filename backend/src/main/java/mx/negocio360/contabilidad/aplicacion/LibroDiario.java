package mx.negocio360.contabilidad.aplicacion;

import java.time.Clock;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioCuentas;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioPeriodos;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioPolizas;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioSaldos;
import mx.negocio360.contabilidad.dominio.Asiento;
import mx.negocio360.contabilidad.dominio.Balanza;
import mx.negocio360.contabilidad.dominio.CodigoError;
import mx.negocio360.contabilidad.dominio.ContabilidadException;
import mx.negocio360.contabilidad.dominio.OrigenPoliza;
import mx.negocio360.contabilidad.dominio.Periodo;
import mx.negocio360.contabilidad.dominio.Poliza;

/**
 * Unico punto por donde se registran polizas, sean manuales, reversas o automaticas. Revisa las cuentas (A-04) y
 * escribe la poliza en su periodo con su folio (A-05) y actualiza los saldos (A-08). En Postgres todo ocurre en una
 * sola transaccion; con los dobles en memoria, la actualizacion de saldos se serializa.
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
    private final Mayor mayor;

    /**
     * Crea el libro.
     *
     * @param cuentas  tabla de cuentas
     * @param periodos tabla de periodos
     * @param polizas  tablas de polizas y movimientos
     * @param saldos   tabla de saldos por cuenta y periodo
     * @param reloj    reloj de la plataforma
     */
    public LibroDiario(RepositorioCuentas cuentas, RepositorioPeriodos periodos, RepositorioPolizas polizas,
                       RepositorioSaldos saldos, Clock reloj) {
        this.cuentas = Objects.requireNonNull(cuentas, "cuentas");
        this.periodos = Objects.requireNonNull(periodos, "periodos");
        this.polizas = Objects.requireNonNull(polizas, "polizas");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        this.mayor = new Mayor(cuentas, periodos, saldos);
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
            validarCuenta(negocioId, renglon.cuentaId());
        }
    }

    /**
     * Revisa que una cuenta exista en el negocio, sea afectable y este activa (A-04).
     *
     * @param negocioId negocio
     * @param cuentaId  cuenta; nula cuenta como inexistente
     * @throws ContabilidadException {@code CUENTA_NO_ENCONTRADA}, {@code CUENTA_NO_AFECTABLE} o
     *                               {@code CUENTA_INACTIVA}
     */
    public void validarCuenta(UUID negocioId, UUID cuentaId) {
        var cuenta = cuentaId == null ? null : cuentas.porId(negocioId, cuentaId).orElse(null);
        if (cuenta == null) {
            throw new ContabilidadException(CodigoError.CUENTA_NO_ENCONTRADA);
        }
        if (!cuenta.afectable()) {
            throw new ContabilidadException(CodigoError.CUENTA_NO_AFECTABLE, cuenta.id());
        }
        if (!cuenta.recibeMovimientos()) {
            throw new ContabilidadException(CodigoError.CUENTA_INACTIVA, cuenta.id());
        }
    }

    /**
     * Revisa todo lo que se puede revisar antes de registrar: las cuentas (A-04) y que el mes del asiento no este
     * cerrado (T-01).
     *
     * @param negocioId negocio
     * @param asiento   asiento a revisar
     * @throws ContabilidadException los codigos de {@link #validarCuentas} o {@code PERIODO_CERRADO}
     */
    public void validar(UUID negocioId, Asiento asiento) {
        validarCuentas(negocioId, asiento);
        var periodo = periodos.delMes(negocioId, YearMonth.from(asiento.fecha()));
        if (periodo.isPresent() && !periodo.get().abierto()) {
            throw new ContabilidadException(CodigoError.PERIODO_CERRADO, periodo.get().id());
        }
    }

    /**
     * Registra el asiento como poliza.
     *
     * @param negocioId negocio
     * @param asiento   asiento cuadrado
     * @param registro  origen y referencias
     * @return la poliza registrada
     * @throws ContabilidadException los codigos de {@link #validar}
     */
    public Poliza registrar(UUID negocioId, Asiento asiento, Registro registro) {
        validar(negocioId, asiento);
        var ahora = reloj.instant();
        var periodo = periodoDe(negocioId, YearMonth.from(asiento.fecha()));
        var folio = polizas.siguienteFolio(negocioId, periodo.id(), asiento.tipo());
        var poliza = new Poliza(UuidV7.generar(ahora), negocioId, periodo.id(), asiento.tipo(), folio,
                asiento.fecha(), asiento.concepto(), registro.origen(), registro.eventoId(), registro.referenciaTipo(),
                registro.referenciaId(), false, registro.reviertePolizaId(), asiento.total(), asiento.renglones(),
                registro.creadoPor(), ahora);
        polizas.insertar(poliza);
        synchronized (mayor) {
            mayor.aplicar(poliza, periodo);
        }
        return poliza;
    }

    /**
     * Balanza de comprobacion de un mes.
     *
     * @param negocioId negocio
     * @param mes       anio y mes
     * @return la balanza; si el mes no tiene periodo, cada cuenta muestra el saldo que arrastra
     */
    public Balanza balanza(UUID negocioId, YearMonth mes) {
        synchronized (mayor) {
            return mayor.balanza(negocioId, mes);
        }
    }

    private synchronized Periodo periodoDe(UUID negocioId, YearMonth mes) {
        return periodos.delMes(negocioId, mes).orElseGet(() -> {
            var nuevo = Periodo.abierto(UuidV7.generar(reloj.instant()), negocioId, mes, reloj.instant());
            periodos.guardar(nuevo);
            return nuevo;
        });
    }
}
