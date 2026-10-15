package mx.negocio360.contabilidad.aplicacion;

import java.time.YearMonth;

/**
 * Cierra un mes contable (T-01 a T-03). Despues del cierre el mes no recibe polizas y sus saldos quedan congelados.
 *
 * @param periodo anio y mes a cerrar
 */
public record CerrarPeriodoComando(YearMonth periodo) implements Comando {
}
