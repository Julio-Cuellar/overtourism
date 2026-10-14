package mx.negocio360.clientes.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** I-02: el telefono se normaliza a E.164 (Mexico por defecto, +52 y 10 digitos) y se rechaza si no es valido. */
class TelefonoTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "222 123 4567|+522221234567",
            "2221234567|+522221234567",
            "+52 222 123 4567|+522221234567",
            "+522221234567|+522221234567",
            "(222) 123-4567|+522221234567",
            "52 222 123 4567|+522221234567",
            "' 222.123.4567 '|+522221234567",
            "999 999 9999|+529999999999"})
    @DisplayName("normaliza los formatos comunes a E.164")
    void normalizar_formatosComunes_devuelveE164(String entrada, String esperado) {
        assertThat(Telefono.normalizar(entrada)).contains(esperado);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "222 123 4567|+522221234567",
            "222 123 4567|+522221234567",
            " +52 222 123 4567 |+522221234567"})
    @DisplayName("acepta espacios no separables, como los que trae un telefono copiado del movil")
    void normalizar_espaciosUnicode_devuelveE164(String entrada, String esperado) {
        assertThat(Telefono.normalizar(entrada)).contains(esperado);
    }

    @Test
    @DisplayName("un texto de mas de 40 caracteres se rechaza sin analizarlo")
    void normalizar_textoEnorme_devuelveVacio() {
        assertThat(Telefono.normalizar("2".repeat(41))).isEmpty();
        assertThat(Telefono.normalizar(" ".repeat(30) + "2221234567" + " ".repeat(30))).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "123", "abc", "222123456", "22212345678", "+12221234567", "+5222212345678",
            "1221234567", "0221234567", "222 123 456a", "+52 222 123", "++522221234567", "222-123-4567 ext 9"})
    @DisplayName("rechaza lo que no es un telefono mexicano de 10 digitos")
    void normalizar_entradaInvalida_devuelveVacio(String entrada) {
        assertThat(Telefono.normalizar(entrada)).isEmpty();
    }

    @Test
    @DisplayName("enmascara dejando solo los ultimos 4 digitos")
    void enmascarar_telefonoE164_dejaUltimosCuatro() {
        assertThat(Telefono.enmascarar("+522221234567")).isEqualTo("•••• 4567");
    }

    @Test
    @DisplayName("la mascara nunca contiene el telefono completo ni los digitos del medio")
    void enmascarar_telefonoE164_noFiltraDigitosDelMedio() {
        var mascara = Telefono.enmascarar("+522221234567");

        assertThat(mascara).doesNotContain("222123").doesNotContain("+52");
    }
}
