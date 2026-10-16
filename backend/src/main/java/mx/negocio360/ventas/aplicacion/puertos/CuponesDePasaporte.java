package mx.negocio360.ventas.aplicacion.puertos;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Puerto hacia Pasaporte (J2 y J3, D-34, D-137). Pasaporte aun no existe y no consume eventos de Ventas: Ventas lo
 * llama al preparar (solo lectura) y al confirmar (canje). Los fallos llegan como
 * {@link mx.negocio360.ventas.dominio.VentasException} con {@code CUPON_INVALIDO}, {@code CUPON_YA_CANJEADO},
 * {@code CUPON_NO_APLICA} o {@code ERROR_TEMPORAL}.
 */
public interface CuponesDePasaporte {

    /**
     * Calcula el descuento del cupon sin canjearlo (VT-07 al preparar).
     *
     * @param negocioId negocio que vende
     * @param codigo    codigo que muestra el turista
     * @param subtotal  subtotal de la venta, escala 2
     * @return cupon y descuento
     */
    CuponCotizado cotizar(UUID negocioId, String codigo, BigDecimal subtotal);

    /**
     * Canjea el cupon para esa venta (VT-07 al confirmar). En produccion corre en la misma transaccion que la venta.
     * Si el descuento que Pasaporte calcula ahora no es {@code descuentoEsperado}, rechaza con {@code COMANDO_CAMBIO}
     * <b>sin canjear</b>: el cupon no se gasta en una venta que no se va a registrar.
     *
     * @param negocioId         negocio que vende
     * @param codigo            codigo que muestra el turista
     * @param ventaId           venta que se esta registrando
     * @param subtotal          subtotal de la venta, escala 2
     * @param descuentoEsperado descuento que vio el cajero en el resumen, escala 2
     * @return cupon y descuento canjeados
     */
    CuponCotizado canjear(UUID negocioId, String codigo, UUID ventaId, BigDecimal subtotal,
                          BigDecimal descuentoEsperado);
}
