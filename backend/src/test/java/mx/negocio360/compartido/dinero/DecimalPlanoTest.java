package mx.negocio360.compartido.dinero;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Una sola gramatica para el dinero y las cantidades: la usan el escalar de GraphQL y el JSON de los eventos. */
class DecimalPlanoTest {

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "207.00", "-1.5", "12345678901234", "12345678901234.12345678"})
    @DisplayName("lee un decimal plano y conserva su escala")
    void leer_formaPlanaValida_devuelveElDecimalConSuEscala(String texto) {
        assertThat(DecimalPlano.leer(texto)).hasValue(new BigDecimal(texto));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "1E+3", "1e3", "+5", ".5", "5.", "1E+999999999", "abc", "1 000", "207,00",
            "123456789012345", "1.123456789", " 207.00", "207.00 ", "--1", "1.2.3"})
    @DisplayName("rechaza exponentes, signos, puntos sueltos, espacios y numeros demasiado largos")
    void leer_formaNoPlana_devuelveVacio(String texto) {
        assertThat(DecimalPlano.leer(texto)).isEmpty();
    }

    @Test
    @DisplayName("un texto nulo no es un decimal")
    void leer_nulo_devuelveVacio() {
        assertThat(DecimalPlano.leer(null)).isEmpty();
    }

    @Test
    @DisplayName("escribe siempre en forma plana, sin notacion cientifica")
    void escribir_exponente_usaFormaPlana() {
        assertThat(DecimalPlano.escribir(new BigDecimal("1E+2"))).isEqualTo("100");
        assertThat(DecimalPlano.escribir(new BigDecimal("207.50"))).isEqualTo("207.50");
    }
}
