package mx.negocio360.contabilidad.dominio;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Saldo de una cuenta en un periodo (tabla {@code saldo_cuenta}). Es derivado: nadie lo edita, solo cambia al
 * registrar polizas (A-08).
 *
 * @param negocioId    negocio
 * @param cuentaId     cuenta
 * @param periodoId    periodo
 * @param saldoInicial saldo final del periodo anterior
 * @param cargos       suma de cargos del periodo
 * @param abonos       suma de abonos del periodo
 * @param saldoFinal   saldo inicial mas el neto del periodo, con el signo de la naturaleza
 * @param version      version para bloqueo optimista
 */
public record Saldo(UUID negocioId, UUID cuentaId, UUID periodoId, BigDecimal saldoInicial, BigDecimal cargos,
                    BigDecimal abonos, BigDecimal saldoFinal, long version) {

    /**
     * Saldo sin movimientos que arranca con un saldo inicial.
     *
     * @param negocioId    negocio
     * @param cuentaId     cuenta
     * @param periodoId    periodo
     * @param saldoInicial saldo con que arranca
     * @return el saldo
     */
    public static Saldo inicial(UUID negocioId, UUID cuentaId, UUID periodoId, BigDecimal saldoInicial) {
        var cero = Montos.exacto(BigDecimal.ZERO);
        return new Saldo(negocioId, cuentaId, periodoId, saldoInicial, cero, cero, saldoInicial, 0);
    }

    /**
     * Neto de un renglon visto desde la naturaleza de la cuenta: positivo si aumenta su saldo.
     *
     * @param cargo      cargo del renglon
     * @param abono      abono del renglon
     * @param naturaleza naturaleza de la cuenta
     * @return el neto con signo
     */
    public static BigDecimal neto(BigDecimal cargo, BigDecimal abono, Naturaleza naturaleza) {
        var neto = cargo.subtract(abono);
        return naturaleza == Naturaleza.DEUDORA ? neto : neto.negate();
    }

    /**
     * Saldo con un renglon mas.
     *
     * @param cargo      cargo del renglon
     * @param abono      abono del renglon
     * @param naturaleza naturaleza de la cuenta
     * @return el saldo nuevo
     */
    public Saldo con(BigDecimal cargo, BigDecimal abono, Naturaleza naturaleza) {
        return new Saldo(negocioId, cuentaId, periodoId, saldoInicial, cargos.add(cargo), abonos.add(abono),
                saldoFinal.add(neto(cargo, abono, naturaleza)), version + 1);
    }

    /**
     * Saldo con el inicial (y por tanto el final) desplazado, cuando un periodo anterior recibio una poliza.
     *
     * @param delta cuanto cambia el saldo inicial
     * @return el saldo nuevo
     */
    public Saldo desplazado(BigDecimal delta) {
        return new Saldo(negocioId, cuentaId, periodoId, saldoInicial.add(delta), cargos, abonos,
                saldoFinal.add(delta), version + 1);
    }
}
