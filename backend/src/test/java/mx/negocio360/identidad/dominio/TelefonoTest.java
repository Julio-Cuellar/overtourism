package mx.negocio360.identidad.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** IDN-1.2 · AC-01: el telefono se normaliza a E.164 y es el identificador de la cuenta. */
class TelefonoTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "2221234567", "222 123 4567", "+52 222 123 4567", "52 222 123 4567", "(222) 123-4567",
        "+52 (222) 123.4567", "222 123 4567", "  +522221234567  "})
    @DisplayName("todas las formas habituales de escribir el mismo numero dan el mismo E.164")
    void normalizar_formasHabituales_dan522221234567(String entrada) {
        assertThat(Telefono.normalizar(entrada)).contains("+522221234567");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        "   ", "123", "0221234567", "1221234567", "+1 222 123 4567", "+52222123456", "+5222212345678",
        "222123456a", "222-123-45-678", "+52 222 123 4567 ext 9"})
    @DisplayName("un numero ausente, de otro pais, con lada 0 o 1, corto, largo o con letras se rechaza")
    void normalizar_entradasInvalidas_devuelveVacio(String entrada) {
        assertThat(Telefono.normalizar(entrada)).isEmpty();
    }

    @Test
    @DisplayName("una entrada de mas de 40 caracteres se rechaza sin analizarla")
    void normalizar_entradaLarga_devuelveVacio() {
        assertThat(Telefono.normalizar("2".repeat(41))).isEmpty();
    }

    @Test
    @DisplayName("enmascarar muestra solo los ultimos 4 digitos")
    void enmascarar_telefonoNormalizado_muestraCuatroDigitos() {
        assertThat(Telefono.enmascarar("+522221234567")).isEqualTo("•••• 4567");
    }
}
