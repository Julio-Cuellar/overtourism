package mx.negocio360.negocios.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** NG-01: el nombre del negocio. */
class NombreNegocioTest {

    @Test
    @DisplayName("acepta de 2 a 80 caracteres, con acentos y signos")
    void acepta_loValido() {
        assertThat(NombreNegocio.de("Ño").valor()).isEqualTo("Ño");
        assertThat(NombreNegocio.de("x".repeat(80)).valor()).hasSize(80);
        assertThat(NombreNegocio.de("Tacos «El Güero» & Hijos").valor()).isEqualTo("Tacos «El Güero» & Hijos");
    }

    @Test
    @DisplayName("quita los espacios de los extremos y colapsa los de en medio, tambien los de ancho especial")
    void normaliza_espacios() {
        assertThat(NombreNegocio.de("  Fonda     Chela  ").valor()).isEqualTo("Fonda Chela");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "A", "  A  ", "A\u0000", "Fon\tda", "Fon\nda", "Fonda​x", "Fonda‮x",
            "Fonda"})
    @DisplayName("rechaza nulo, vacio, de un caracter y con caracteres de control o invisibles")
    void rechaza_loInvalido(String entrada) {
        assertThatThrownBy(() -> NombreNegocio.de(entrada)).isInstanceOf(NegociosException.class)
                .extracting(e -> ((NegociosException) e).codigo()).isEqualTo(CodigoError.NOMBRE_INVALIDO);
    }

    @Test
    @DisplayName("rechaza 81 caracteres y entradas enormes sin procesarlas")
    void rechaza_lasLargas() {
        assertThatThrownBy(() -> NombreNegocio.de("x".repeat(81))).isInstanceOf(NegociosException.class);
        assertThatThrownBy(() -> NombreNegocio.de("x".repeat(100_000))).isInstanceOf(NegociosException.class);
    }

    @Test
    @DisplayName("cuenta caracteres, no unidades de UTF-16: un emoji vale uno")
    void cuenta_puntosDeCodigo() {
        assertThat(NombreNegocio.de("🌮".repeat(2)).valor()).isEqualTo("🌮🌮");
        assertThat(NombreNegocio.de("🌮".repeat(80)).valor()).hasSize(160);
    }
}
