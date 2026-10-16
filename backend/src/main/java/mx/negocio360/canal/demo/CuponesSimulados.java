package mx.negocio360.canal.demo;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import mx.negocio360.ventas.aplicacion.puertos.CuponCotizado;
import mx.negocio360.ventas.aplicacion.puertos.CuponesDePasaporte;
import mx.negocio360.ventas.dominio.CodigoError;
import mx.negocio360.ventas.dominio.VentasException;

/**
 * Pasaporte <b>simulado</b> para la demo (Pasaporte aun no existe): dos cupones de monto fijo. Cumple el contrato del
 * puerto (D-87): si al canjear el descuento no es el esperado, rechaza con {@code COMANDO_CAMBIO} sin canjear.
 *
 * <p>Diferencia con el Pasaporte real, a proposito para que la demo se pueda repetir: aqui un codigo se puede usar
 * varias veces. El real lo canjeara una sola vez (VT-07, D-34).
 */
public final class CuponesSimulados implements CuponesDePasaporte {

    /**
     * Un cupon de la demo.
     *
     * @param cuponId   id opaco
     * @param codigo    lo que el turista muestra
     * @param descuento monto fijo, escala 2
     */
    public record CuponDeDemo(UUID cuponId, String codigo, BigDecimal descuento) {
    }

    private static final List<CuponDeDemo> CUPONES = List.of(
            new CuponDeDemo(UUID.fromString("0192f1b0-00c1-7000-8000-0000000c0001"), "PASAPORTE-20",
                    new BigDecimal("20.00")),
            new CuponDeDemo(UUID.fromString("0192f1b0-00c2-7000-8000-0000000c0002"), "PASAPORTE-50",
                    new BigDecimal("50.00")));

    @Override
    public CuponCotizado cotizar(UUID negocioId, String codigo, BigDecimal subtotal) {
        var cupon = CUPONES.stream().filter(c -> c.codigo().equalsIgnoreCase(codigo)).findFirst()
                .orElseThrow(() -> new VentasException(CodigoError.CUPON_INVALIDO));
        return new CuponCotizado(cupon.cuponId(), cupon.descuento());
    }

    @Override
    public CuponCotizado canjear(UUID negocioId, String codigo, UUID ventaId, BigDecimal subtotal,
                                 BigDecimal descuentoEsperado) {
        var cotizado = cotizar(negocioId, codigo, subtotal);
        if (cotizado.descuento().compareTo(descuentoEsperado) != 0) {
            throw new VentasException(CodigoError.COMANDO_CAMBIO);
        }
        return cotizado;
    }

    /**
     * Los cupones de la demo, para que la app del turista los muestre.
     *
     * @return cupones en orden fijo
     */
    public List<CuponDeDemo> todos() {
        return CUPONES;
    }
}
