package mx.negocio360.compartido.dinero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** D-20: una sola utilidad de redondeo, HALF_UP, dinero a 2 decimales y cantidades y precios a 4. */
class RedondeoTest {

    @ParameterizedTest
    @CsvSource({"10.005, 10.01", "10.004, 10.00", "-10.005, -10.01", "60, 60.00", "0.125, 0.13"})
    @DisplayName("dinero redondea a 2 decimales con HALF_UP, tambien la media centesima")
    void dinero_redondeaHalfUpADosDecimales(String entrada, String esperado) {
        assertThat(Redondeo.dinero(new BigDecimal(entrada))).isEqualTo(new BigDecimal(esperado));
    }

    @ParameterizedTest
    @CsvSource({"0.5, 0.5000", "1.23455, 1.2346", "2, 2.0000"})
    @DisplayName("cantidad y precio unitario quedan con 4 decimales")
    void cantidadYPrecio_cuatroDecimales(String entrada, String esperado) {
        assertThat(Redondeo.cantidad(new BigDecimal(entrada))).isEqualTo(new BigDecimal(esperado));
        assertThat(Redondeo.precio(new BigDecimal(entrada))).isEqualTo(new BigDecimal(esperado));
    }

    @Test
    @DisplayName("0.5 kg a 120.00 da un importe de 60.00 (ejemplo del diseño)")
    void importe_granel_ejemploDelDiseno() {
        var importe = Redondeo.dinero(new BigDecimal("0.5000").multiply(new BigDecimal("120.0000")));

        assertThat(importe).isEqualTo(new BigDecimal("60.00"));
    }

    @Test
    @DisplayName("un valor nulo es un error de programacion")
    void nulo_falla() {
        assertThatThrownBy(() -> Redondeo.dinero(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("cantidadComoTexto escribe el decimal plano sin ceros de mas ni notacion cientifica")
    void cantidadComoTexto_sinCerosDeMas() {
        assertThat(Redondeo.cantidadComoTexto(new BigDecimal("2.0000"))).isEqualTo("2");
        assertThat(Redondeo.cantidadComoTexto(new BigDecimal("0.5000"))).isEqualTo("0.5");
        assertThat(Redondeo.cantidadComoTexto(new BigDecimal("100.0000"))).isEqualTo("100");
    }

    @ParameterizedTest
    @CsvSource({"150.00, true", "-150.00, true", "0, true", "999999999999.99, true", "1000000000000, false",
            "1.001, false", "1E+999999999, false", "1E-999999999, false"})
    @DisplayName("M1: dineroValido acepta hasta NUMERIC(14, 2) y 2 decimales, y rechaza exponentes enormes sin calcular")
    void dineroValido_rangoYEscala(String valor, boolean valido) {
        assertThat(Redondeo.dineroValido(new BigDecimal(valor))).isEqualTo(valido);
        assertThat(Redondeo.dineroValido(null)).isFalse();
    }
}
