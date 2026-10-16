package mx.negocio360.negocios.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** NG-09: el telefono y el correo de contacto del negocio. */
class ContactoNegocioTest {

    @ParameterizedTest
    @ValueSource(strings = {"2225550101", "222 555 0101", "222-555-0101", "(222) 555 0101", "+52 222 555 0101",
            "+522225550101", "52 222 555 0101"})
    @DisplayName("el telefono mexicano de diez digitos se guarda como +52 y diez digitos")
    void telefono_seNormaliza(String entrada) {
        assertThat(ContactoNegocio.telefono(entrada)).isEqualTo("+522225550101");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "123", "222555010", "22255501011", "+1 222 555 0101", "abcdefghij", "222 555 01a1",
            "+52 222 555 0101 ext 5", "222\u0000 555 0101"})
    @DisplayName("rechaza lo que no es un telefono mexicano de diez digitos")
    void telefono_rechazaLoInvalido(String entrada) {
        assertThatThrownBy(() -> ContactoNegocio.telefono(entrada)).isInstanceOf(NegociosException.class)
                .extracting(e -> ((NegociosException) e).codigo()).isEqualTo(CodigoError.CONTACTO_INVALIDO);
    }

    @Test
    @DisplayName("el correo se guarda en minusculas y sin espacios de los extremos")
    void correo_seNormaliza() {
        assertThat(ContactoNegocio.correo("  Chela@Fonda.EXAMPLE ")).isEqualTo("chela@fonda.example");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "a@", "@b.com", "sin-arroba", "dos@@b.com", "con espacio@b.com", "a@b", "a@b.c\u0000m"})
    @DisplayName("rechaza lo que no es un correo")
    void correo_rechazaLoInvalido(String entrada) {
        assertThatThrownBy(() -> ContactoNegocio.correo(entrada)).isInstanceOf(NegociosException.class)
                .extracting(e -> ((NegociosException) e).codigo()).isEqualTo(CodigoError.CONTACTO_INVALIDO);
    }

    @Test
    @DisplayName("rechaza un correo de mas de 120 caracteres")
    void correo_rechazaElLargo() {
        assertThatThrownBy(() -> ContactoNegocio.correo("a".repeat(111) + "@b.example"))
                .isInstanceOf(NegociosException.class);
    }

    @Test
    @DisplayName("un mensaje de error no repite el dato que se escribio")
    void error_noRepiteElDato() {
        assertThatThrownBy(() -> ContactoNegocio.correo("secreto-sin-arroba"))
                .hasMessageNotContaining("secreto");
    }
}
