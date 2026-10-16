package mx.negocio360.ventas.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.math.BigDecimal;
import java.util.UUID;
import mx.negocio360.ventas.dominio.CodigoError;
import mx.negocio360.ventas.dominio.VentasException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** D-134: sin Pasaporte, todo codigo de cupon es invalido y se cobra sin cupon. */
class CuponesNoDisponiblesTest {

    private final CuponesNoDisponibles cupones = new CuponesNoDisponibles();

    @Test
    @DisplayName("cotizar y canjear responden CUPON_INVALIDO")
    void todoCodigo_esInvalido() {
        var alCotizar = catchThrowable(() -> cupones.cotizar(UUID.randomUUID(), "PASAPORTE-20", new BigDecimal("100.00")));
        var alCanjear = catchThrowable(() -> cupones.canjear(UUID.randomUUID(), "PASAPORTE-20", UUID.randomUUID(),
                new BigDecimal("100.00"), new BigDecimal("20.00")));

        assertThat(alCotizar).isInstanceOf(VentasException.class);
        assertThat(((VentasException) alCotizar).codigo()).isEqualTo(CodigoError.CUPON_INVALIDO);
        assertThat(((VentasException) alCanjear).codigo()).isEqualTo(CodigoError.CUPON_INVALIDO);
    }
}
