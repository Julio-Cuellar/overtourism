package mx.negocio360.identidad.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Giros que reconoce la plataforma (Restaurantes, Museos y Misceláneas). */
class GiroTest {

    @ParameterizedTest
    @CsvSource({"RESTAURANTE,RESTAURANTE", "restaurante,RESTAURANTE", "  Museo ,MUSEO", "miscelanea,MISCELANEA",
        "MISCELÁNEA,MISCELANEA"})
    @DisplayName("el giro se reconoce sin importar mayusculas, espacios ni el acento de miscelanea")
    void de_giroValido_loReconoce(String entrada, Giro esperado) {
        assertThat(Giro.de(entrada)).contains(esperado);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "FARMACIA", "restaurante;drop", "museos"})
    @DisplayName("un giro desconocido no se reconoce")
    void de_giroInvalido_vacio(String entrada) {
        assertThat(Giro.de(entrada)).isEmpty();
    }
}
