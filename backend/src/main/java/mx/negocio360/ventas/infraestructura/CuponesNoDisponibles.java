package mx.negocio360.ventas.infraestructura;

import java.math.BigDecimal;
import java.util.UUID;
import mx.negocio360.ventas.aplicacion.puertos.CuponCotizado;
import mx.negocio360.ventas.aplicacion.puertos.CuponesDePasaporte;
import mx.negocio360.ventas.dominio.CodigoError;
import mx.negocio360.ventas.dominio.VentasException;

/**
 * Los cupones mientras Pasaporte no exista (D-134): todo codigo es {@code CUPON_INVALIDO}, asi que se cobra sin cupon.
 * Cuando Pasaporte exista, su adaptador reemplaza a este bean.
 */
public final class CuponesNoDisponibles implements CuponesDePasaporte {

    @Override
    public CuponCotizado cotizar(UUID negocioId, String codigo, BigDecimal subtotal) {
        throw new VentasException(CodigoError.CUPON_INVALIDO);
    }

    @Override
    public CuponCotizado canjear(UUID negocioId, String codigo, UUID ventaId, BigDecimal subtotal,
                                 BigDecimal descuentoEsperado) {
        throw new VentasException(CodigoError.CUPON_INVALIDO);
    }
}
