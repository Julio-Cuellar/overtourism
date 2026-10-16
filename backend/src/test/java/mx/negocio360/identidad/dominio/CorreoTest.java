package mx.negocio360.identidad.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** D-76: el correo identifica a la persona. Se guarda en minusculas, con una lista cerrada de caracteres (SI-16). */
class CorreoTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "ana@example.com|ana@example.com",
        "  Ana.Lopez@Example.COM  |ana.lopez@example.com",
        "a_b%c+d-e@sub.dominio.mx|a_b%c+d-e@sub.dominio.mx",
        "x@a-b.co|x@a-b.co"})
    @DisplayName("un correo valido se recorta y se pasa a minusculas")
    void normalizar_correoValido_devuelveMinusculas(String entrada, String esperado) {
        assertThat(Correo.normalizar(entrada)).contains(esperado);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "sin-arroba", "a@b", "a@@b.com", "a b@x.com", "@x.com", "a@.com", "a@x..com", "a@-x.com",
        "a@x-.com", "ñandú@x.com", "ana@exämple.com", "<script>@x.com", "ana@x.com,otro@x.com", "ana@x.com otro@x.com"})
    @DisplayName("un correo mal formado o con caracteres fuera de la lista se rechaza")
    void normalizar_correoInvalido_devuelveVacio(String entrada) {
        assertThat(Correo.normalizar(entrada)).isEmpty();
    }

    @Test
    @DisplayName("el correo mide como maximo 120 caracteres")
    void normalizar_largo_tieneTopeDe120() {
        var enElLimite = "a".repeat(120 - "@x.com".length()) + "@x.com";
        var pasado = "a".repeat(121 - "@x.com".length()) + "@x.com";

        assertThat(Correo.normalizar(enElLimite)).isPresent();
        assertThat(Correo.normalizar(pasado)).isEmpty();
    }

    @Test
    @DisplayName("enmascarar deja la primera letra y el dominio y tapa el resto")
    void enmascarar_dejaLaPrimeraLetraYElDominio() {
        assertThat(Correo.enmascarar("ana.lopez@example.com")).isEqualTo("a***@example.com");
        assertThat(Correo.enmascarar("x@y.com")).isEqualTo("x***@y.com");
    }

    @Test
    @DisplayName("enmascarar algo que no tiene forma de correo no devuelve nada de lo recibido")
    void enmascarar_sinForma_noDevuelveNada() {
        assertThat(Correo.enmascarar("sin-arroba")).isEqualTo("***");
        assertThat(Correo.enmascarar("@dominio.com")).isEqualTo("***");
    }
}
