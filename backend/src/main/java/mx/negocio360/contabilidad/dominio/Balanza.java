package mx.negocio360.contabilidad.dominio;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Balanza de comprobacion de un mes: una fila por cuenta afectable y los totales de cargos y abonos.
 *
 * @param periodo     mes
 * @param filas       filas por cuenta, ordenadas por codigo
 * @param totalCargos suma de cargos del mes
 * @param totalAbonos suma de abonos del mes
 * @param aviso       aviso de que el reporte no es fiscal (F-03)
 */
public record Balanza(YearMonth periodo, List<Fila> filas, BigDecimal totalCargos, BigDecimal totalAbonos,
                      String aviso) {

    /**
     * Fila de una cuenta.
     *
     * @param cuentaId     cuenta
     * @param codigo       codigo de la cuenta
     * @param nombre       nombre de la cuenta
     * @param naturaleza   naturaleza
     * @param saldoInicial saldo al empezar el mes
     * @param cargos       cargos del mes
     * @param abonos       abonos del mes
     * @param saldoFinal   saldo al terminar el mes
     */
    public record Fila(UUID cuentaId, String codigo, String nombre, Naturaleza naturaleza, BigDecimal saldoInicial,
                       BigDecimal cargos, BigDecimal abonos, BigDecimal saldoFinal) {
    }

    public Balanza {
        filas = List.copyOf(filas);
    }

    /**
     * Dice si la balanza cuadra: cargos y abonos del mes suman lo mismo.
     *
     * @return verdadero si cuadra
     */
    public boolean cuadra() {
        return totalCargos.compareTo(totalAbonos) == 0;
    }

    /**
     * Fila de una cuenta por codigo.
     *
     * @param codigo codigo de la cuenta
     * @return la fila
     * @throws NoSuchElementException si la cuenta no aparece en la balanza
     */
    public Fila fila(String codigo) {
        return filas.stream().filter(f -> f.codigo().equals(codigo)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("La balanza no tiene la cuenta " + codigo));
    }
}
