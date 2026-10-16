package mx.negocio360.compartido.dinero;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 4.3 / D-20: una sola utilidad de redondeo HALF_UP para cantidades, costos, dinero y factores. */
class RedondeoDeInventarioTest {

    private static BigDecimal d(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    @DisplayName("cantidad y costo unitario a 4 decimales, HALF_UP")
    void cantidadYCosto_cuatroDecimales() {
        assertThat(Redondeo.cantidad(d("1.00005"))).isEqualTo(d("1.0001"));
        assertThat(Redondeo.cantidad(d("1.00004"))).isEqualTo(d("1.0000"));
        assertThat(Redondeo.costoUnitario(d("0.01805"))).isEqualTo(d("0.0181"));
        assertThat(Redondeo.cantidad(d("2"))).isEqualTo(d("2.0000"));
    }

    @Test
    @DisplayName("dinero a 2 decimales; la media aleja del cero tambien en negativos")
    void dinero_dosDecimales() {
        assertThat(Redondeo.dinero(d("0.375"))).isEqualTo(d("0.38"));
        assertThat(Redondeo.dinero(d("-0.375"))).isEqualTo(d("-0.38"));
        assertThat(Redondeo.dinero(d("0.374"))).isEqualTo(d("0.37"));
    }

    @Test
    @DisplayName("factor a 8 decimales")
    void factor_ochoDecimales() {
        assertThat(Redondeo.factor(d("0.001"))).isEqualTo(d("0.00100000"));
        assertThat(Redondeo.factor(d("0.123456785"))).isEqualTo(d("0.12345679"));
    }

    @Test
    @DisplayName("dividir usa la escala pedida con HALF_UP")
    void dividir_escalaPedida() {
        assertThat(Redondeo.dividir(d("57"), d("3000"), Redondeo.ESCALA_COSTO)).isEqualTo(d("0.0190"));
        assertThat(Redondeo.dividir(d("1"), d("3"), Redondeo.ESCALA_FACTOR)).isEqualTo(d("0.33333333"));
    }
}
