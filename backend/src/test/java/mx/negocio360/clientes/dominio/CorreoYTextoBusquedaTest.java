package mx.negocio360.clientes.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** I-07 (correo) e I-04 (texto de busqueda sin mayusculas ni acentos). */
class CorreoYTextoBusquedaTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "ANA@Correo.com|ana@correo.com",
            "' ana@correo.com '|ana@correo.com",
            "ana.perez+tienda@mail.mx|ana.perez+tienda@mail.mx"})
    @DisplayName("el correo valido se guarda en minusculas y sin espacios en los extremos")
    void normalizarCorreo_correoValido_devuelveMinusculas(String entrada, String esperado) {
        assertThat(Correo.normalizar(entrada)).contains(esperado);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "ana@", "ana", "@correo.com", "ana@correo", "a b@c.com", "ana@@c.com",
            "ana@correo..", "ana@.com"})
    @DisplayName("el correo mal formado se rechaza")
    void normalizarCorreo_correoInvalido_devuelveVacio(String entrada) {
        assertThat(Correo.normalizar(entrada)).isEmpty();
    }

    @Test
    @DisplayName("el correo de mas de 120 caracteres se rechaza")
    void normalizarCorreo_masDe120Caracteres_devuelveVacio() {
        var largo = "a".repeat(110) + "@correo.com";

        assertThat(Correo.normalizar(largo)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "'  José   PÉREZ '|jose perez",
            "Niño|nino",
            "MARIA|maria",
            "ÁÉÍÓÚÜ|aeiouu"})
    @DisplayName("quita acentos, pasa a minusculas y junta espacios")
    void normalizarTexto_conAcentosYMayusculas_devuelveTextoPlano(String entrada, String esperado) {
        assertThat(TextoBusqueda.normalizar(entrada)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("el texto nulo se trata como vacio")
    void normalizarTexto_nulo_devuelveVacio() {
        assertThat(TextoBusqueda.normalizar(null)).isEmpty();
    }
}
