package mx.negocio360.restaurantes.aplicacion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Lo que el salon muestra de una cuenta que todavia no se paga (MS-02): suficiente para dibujar la mesa, sin sus lineas.
 *
 * @param cuentaId             la cuenta
 * @param numero               consecutivo por negocio
 * @param estado               {@code ABIERTA} o {@code POR_COBRAR}
 * @param total                suma de las lineas no anuladas, con los precios vigentes
 * @param abiertaEn            cuando se abrio
 * @param porQr                verdadero si la abrio el primer pedido por QR
 * @param presenciaConfirmada  verdadero si un mesero ya esta con la mesa (QR-08)
 */
public record ResumenDeCuenta(UUID cuentaId, int numero, String estado, BigDecimal total, Instant abiertaEn, boolean porQr,
                              boolean presenciaConfirmada) {
}
