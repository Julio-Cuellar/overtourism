package mx.negocio360.contabilidad.dominio;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Huella de un cupon canjeado (tabla {@code canje_contable}).
 *
 * @param id             UUIDv7
 * @param negocioId      negocio
 * @param cuponId        cupon; referencia logica a Pasaporte, sin llave foranea
 * @param ventaId        venta en que se uso, o nula
 * @param montoDescuento descuento con IVA
 * @param baseDescuento  descuento sin IVA, el que se reclasifica (U-01)
 * @param financiador    NEGOCIO o PLATAFORMA
 * @param contabilizado  verdadero si genero poliza; falso si quedo sin efecto
 * @param motivo         por que quedo sin efecto, o nulo
 * @param polizaId       poliza, o nula si quedo sin efecto
 */
public record CanjeContable(UUID id, UUID negocioId, UUID cuponId, UUID ventaId, BigDecimal montoDescuento,
                            BigDecimal baseDescuento, String financiador, boolean contabilizado, String motivo,
                            UUID polizaId) {
}
