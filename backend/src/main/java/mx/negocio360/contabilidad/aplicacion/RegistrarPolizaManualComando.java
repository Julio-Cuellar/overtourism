package mx.negocio360.contabilidad.aplicacion;

import java.time.LocalDate;
import java.util.List;
import mx.negocio360.contabilidad.dominio.TipoPoliza;

/**
 * Registra una poliza capturada por una persona, por ejemplo un gasto o el deposito de las ventas con tarjeta.
 *
 * @param tipo      tipo de poliza
 * @param fecha     fecha contable
 * @param concepto  descripcion, de 1 a 200 caracteres
 * @param renglones al menos dos renglones que cuadren (A-01)
 */
public record RegistrarPolizaManualComando(TipoPoliza tipo, LocalDate fecha, String concepto,
                                           List<RenglonManual> renglones) implements Comando {
}
