package mx.negocio360.contabilidad.dominio;

import static mx.negocio360.contabilidad.fakes.Afirmaciones.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * A-07: centavos HALF_UP; el IVA se calcula sobre la base y el redondeo queda en el renglon de ventas. La tasa de 16 %
 * de estas pruebas es un dato de ejemplo, no una tasa verificada (F-01).
 */
class DesgloseIvaTest {

    private static final BigDecimal DIECISEIS = new BigDecimal("0.16");

    @ParameterizedTest(name = "total {0} -> ventas {1} e IVA {2}")
    @CsvSource({"100.00, 86.21, 13.79", "232.00, 200.00, 32.00", "0.01, 0.01, 0.00", "29.00, 25.00, 4.00"})
    @DisplayName("A-07: con IVA incluido, el desglose de los ejemplos del diseno")
    void desglose_ejemplosDelDiseno(String total, String ventas, String iva) {
        var desglose = DesgloseIva.de(new BigDecimal(total), DIECISEIS);

        assertThat(desglose.ventas()).isEqualTo(new BigDecimal(ventas));
        assertThat(desglose.iva()).isEqualTo(new BigDecimal(iva));
    }

    @Test
    @DisplayName("A-07: para todo total de 0.01 a 300.00, ventas + IVA es exactamente el total")
    void desglose_siempreSumaElTotal() {
        for (var centavos = 1; centavos <= 30_000; centavos++) {
            var total = BigDecimal.valueOf(centavos, 2);
            var desglose = DesgloseIva.de(total, DIECISEIS);
            var base = total.divide(BigDecimal.ONE.add(DIECISEIS), 10, RoundingMode.HALF_UP)
                    .setScale(2, RoundingMode.HALF_UP);

            assertThat(desglose.ventas().add(desglose.iva())).isEqualTo(total);
            assertThat(desglose.iva()).isEqualTo(base.multiply(DIECISEIS).setScale(2, RoundingMode.HALF_UP));
        }
    }

    @Test
    @DisplayName("A-07 y F-01: sin tasa (exento) todo va a ventas y el IVA es cero")
    void desglose_exento_todoAVentas() {
        var desglose = DesgloseIva.de(new BigDecimal("150.00"), null);

        assertThat(desglose.ventas()).isEqualTo(new BigDecimal("150.00"));
        assertThat(desglose.iva()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    @DisplayName("A-07: con tasa cero el IVA es cero")
    void desglose_tasaCero_ivaCero() {
        var desglose = DesgloseIva.de(new BigDecimal("80.00"), BigDecimal.ZERO);

        assertThat(desglose.iva()).isEqualTo(new BigDecimal("0.00"));
        assertThat(desglose.ventas()).isEqualTo(new BigDecimal("80.00"));
    }

    @ParameterizedTest(name = "tasa {0}")
    @CsvSource({"-0.01", "1.00", "0.16001"})
    @DisplayName("A-07: una tasa negativa, de 100 % o mas, o con mas de 4 decimales falla con TASA_INVALIDA")
    void desglose_tasaInvalida_falla(String tasa) {
        fallaCon(() -> DesgloseIva.de(new BigDecimal("10.00"), new BigDecimal(tasa)), CodigoError.TASA_INVALIDA);
    }
}
