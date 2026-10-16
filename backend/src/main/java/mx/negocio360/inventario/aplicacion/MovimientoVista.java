package mx.negocio360.inventario.aplicacion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import mx.negocio360.inventario.dominio.CausaTipo;
import mx.negocio360.inventario.dominio.MotivoMerma;
import mx.negocio360.inventario.dominio.TipoMovimiento;

/** Una linea del kardex: lo capturado, la conversion aplicada y lo que quedo despues (U-05). */
public record MovimientoVista(UUID id, TipoMovimiento tipo, CausaTipo causaTipo, MotivoMerma motivo,
                              BigDecimal cantidadCapturada, String unidadCapturada, BigDecimal factorAplicado,
                              BigDecimal cantidadBase, BigDecimal costoUnitario, BigDecimal costoTotal,
                              BigDecimal cppResultante, BigDecimal saldoResultante, Instant ocurridoEn,
                              String registradoPor) {
}
