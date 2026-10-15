package mx.negocio360.contabilidad.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Huella de una venta contabilizada (tabla {@code venta_contable}). Sirve para la idempotencia por venta (E-02) y para
 * conciliar el efectivo con la caja (J-03).
 *
 * @param id           UUIDv7
 * @param negocioId    negocio
 * @param ventaId      venta; referencia logica a Ventas, sin llave foranea
 * @param cajaSesionId sesion de caja, o nula
 * @param medioPago    medio de pago
 * @param total        total cobrado
 * @param base         parte de ventas
 * @param iva          parte de IVA
 * @param ocurridoEn   momento de la venta
 * @param polizaId     poliza registrada
 * @param revertida    si su poliza se revirtio
 */
public record VentaContable(UUID id, UUID negocioId, UUID ventaId, UUID cajaSesionId, String medioPago,
                            BigDecimal total, BigDecimal base, BigDecimal iva, Instant ocurridoEn, UUID polizaId,
                            boolean revertida) {
}
