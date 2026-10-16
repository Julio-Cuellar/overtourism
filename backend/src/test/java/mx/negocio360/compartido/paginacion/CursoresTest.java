package mx.negocio360.compartido.paginacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** D-131 y SG-07: cursor opaco, maximo 100 por pagina. */
class CursoresTest {

    private static final List<Integer> CINCUENTA = IntStream.rangeClosed(1, 50).boxed().toList();

    @Test
    @DisplayName("la primera pagina trae los primeros N, dice que hay mas y da un cursor")
    void primeraPagina_traeLosPrimeros() {
        var pagina = Cursores.pagina(CINCUENTA, 20, null);

        assertThat(pagina.elementos()).containsExactlyElementsOf(CINCUENTA.subList(0, 20));
        assertThat(pagina.hayMas()).isTrue();
        assertThat(pagina.cursorFinal()).isNotBlank();
    }

    @Test
    @DisplayName("seguir el cursor recorre todo sin repetir ni saltar, y la ultima pagina ya no trae cursor")
    void recorrerTodo_sinRepetirNiSaltar() {
        var vistos = new ArrayList<Integer>();
        String cursor = null;
        Pagina<Integer> pagina;
        do {
            pagina = Cursores.pagina(CINCUENTA, 20, cursor);
            vistos.addAll(pagina.elementos());
            cursor = pagina.cursorFinal();
        } while (pagina.hayMas());

        assertThat(vistos).containsExactlyElementsOf(CINCUENTA);
        assertThat(pagina.cursorFinal()).isNull();
    }

    @Test
    @DisplayName("una lista vacia devuelve una pagina vacia sin cursor")
    void listaVacia_paginaVacia() {
        var pagina = Cursores.pagina(List.<Integer>of(), 20, null);

        assertThat(pagina.elementos()).isEmpty();
        assertThat(pagina.hayMas()).isFalse();
        assertThat(pagina.cursorFinal()).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 101, Integer.MAX_VALUE})
    @DisplayName("un tamano fuera de 1 a 100 no es valido")
    void tamanoFueraDeRango_noEsValido(int primeros) {
        assertThat(Cursores.valido(primeros, null)).isFalse();
        assertThatThrownBy(() -> Cursores.pagina(CINCUENTA, primeros, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "no-es-un-cursor", "cDE6LTE", "cDE6YWJj", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"})
    @DisplayName("un cursor que no emitimos nosotros no es valido")
    void cursorAjeno_noEsValido(String cursor) {
        assertThat(Cursores.valido(20, cursor)).isFalse();
    }

    @Test
    @DisplayName("100 es valido y 1 tambien")
    void limites_sonValidos() {
        assertThat(Cursores.valido(100, null)).isTrue();
        assertThat(Cursores.valido(1, null)).isTrue();
    }
}
