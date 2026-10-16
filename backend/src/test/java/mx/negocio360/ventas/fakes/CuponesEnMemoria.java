package mx.negocio360.ventas.fakes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.ventas.aplicacion.puertos.CuponCotizado;
import mx.negocio360.ventas.aplicacion.puertos.CuponesDePasaporte;
import mx.negocio360.ventas.dominio.CodigoError;
import mx.negocio360.ventas.dominio.VentasException;

/**
 * Doble de Pasaporte. Cada codigo da un porcentaje de descuento o un monto fijo. Un cupon canjeado ya no sirve
 * (CUPON_YA_CANJEADO), como hara Pasaporte con {@code canjearCupon} (D-34). Si al canjear el descuento no es el
 * esperado, rechaza con COMANDO_CAMBIO sin canjear, como exige el puerto.
 */
public final class CuponesEnMemoria implements CuponesDePasaporte {

    /** Un canje registrado, para que la prueba lo revise. */
    public record Canje(UUID negocioId, String codigo, UUID ventaId, BigDecimal subtotal,
                        BigDecimal descuentoEsperado) {
    }

    private record Regla(UUID cuponId, BigDecimal porcentaje, BigDecimal montoFijo) {
    }

    private final Map<String, Regla> reglas = new HashMap<>();
    private final List<Canje> canjes = new ArrayList<>();

    /** Cuando es verdadero, canjear falla como si Pasaporte estuviera caido. */
    public boolean fallarAlCanjear;
    /** Si no es nulo, el descuento que Pasaporte calcula al canjear (simula que la regla cambio entre pasos). */
    public BigDecimal descuentoAlCanjear;
    private int cotizaciones;

    public CuponesEnMemoria porcentaje(String codigo, UUID cuponId, String porcentaje) {
        reglas.put(codigo, new Regla(cuponId, new BigDecimal(porcentaje), null));
        return this;
    }

    public CuponesEnMemoria montoFijo(String codigo, UUID cuponId, String monto) {
        reglas.put(codigo, new Regla(cuponId, null, new BigDecimal(monto)));
        return this;
    }

    @Override
    public CuponCotizado cotizar(UUID negocioId, String codigo, BigDecimal subtotal) {
        cotizaciones++;
        var regla = reglas.get(codigo);
        if (regla == null) {
            throw new VentasException(CodigoError.CUPON_INVALIDO);
        }
        if (canjes.stream().anyMatch(c -> c.codigo().equals(codigo))) {
            throw new VentasException(CodigoError.CUPON_YA_CANJEADO);
        }
        var descuento = regla.montoFijo() != null ? regla.montoFijo()
                : subtotal.multiply(regla.porcentaje()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        return new CuponCotizado(regla.cuponId(), descuento);
    }

    @Override
    public CuponCotizado canjear(UUID negocioId, String codigo, UUID ventaId, BigDecimal subtotal,
                                 BigDecimal descuentoEsperado) {
        if (fallarAlCanjear) {
            throw new VentasException(CodigoError.ERROR_TEMPORAL);
        }
        var cotizado = cotizar(negocioId, codigo, subtotal);
        var descuento = descuentoAlCanjear != null ? descuentoAlCanjear : cotizado.descuento();
        if (descuento.compareTo(descuentoEsperado) != 0) {
            throw new VentasException(CodigoError.COMANDO_CAMBIO);
        }
        canjes.add(new Canje(negocioId, codigo, ventaId, subtotal, descuentoEsperado));
        return new CuponCotizado(cotizado.cuponId(), descuento);
    }

    /** Cuantas veces se pregunto a Pasaporte por un cupon. */
    public int cotizaciones() {
        return cotizaciones;
    }

    public List<Canje> canjes() {
        return List.copyOf(canjes);
    }
}
