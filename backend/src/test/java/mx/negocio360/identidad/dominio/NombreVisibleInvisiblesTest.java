package mx.negocio360.identidad.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** AC-04 y SI-16: un nombre que se ve vacío, partido en líneas o sin una sola letra no es un nombre. */
class NombreVisibleInvisiblesTest {

    @ParameterizedTest
    @ValueSource(strings = {"ㅤㅤㅤ", "ﾠﾠ", "⠀⠀", "Ana Ruiz", "Ana Ruiz",
        "ᅟᅠᅟ", "!!", "- -", "··"})
    @DisplayName("se rechazan los nombres invisibles, con separadores de linea o sin ninguna letra ni digito")
    void normalizar_invisibleOSinLetras_seRechaza(String entrada) {
        assertThat(NombreVisible.normalizar(entrada)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ana López", "O'Brien", "Ana-María", "José Luis", "Zoë", "李雷", "R2 D2", "Ñandú"})
    @DisplayName("los nombres reales con acentos, apostrofes, guiones, digitos o ideogramas se aceptan")
    void normalizar_nombresReales_seAceptan(String entrada) {
        assertThat(NombreVisible.normalizar(entrada)).contains(entrada);
    }

    @Test
    @DisplayName("una entrada de un millon de caracteres se rechaza al momento, sin recorrerla toda con expresiones regulares")
    void normalizar_entradaEnorme_seRechazaRapido() {
        var enorme = "a ".repeat(500_000);
        var inicio = System.nanoTime();

        var resultado = NombreVisible.normalizar(enorme);

        assertThat(resultado).isEmpty();
        assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofMillis(200));
    }
}
