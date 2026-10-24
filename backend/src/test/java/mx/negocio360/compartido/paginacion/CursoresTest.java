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

    @Test
    @DisplayName("por consulta: pide una fila de mas para saber si hay mas, y recorre todo sin repetir ni saltar")
    void porConsulta_recorreTodo() {
        var pedidos = new ArrayList<int[]>();
        Cursores.Consulta<Integer> consulta = (limite, desplazamiento) -> {
            pedidos.add(new int[] {limite, desplazamiento});
            return CINCUENTA.stream().skip(desplazamiento).limit(limite).toList();
        };
        var vistos = new ArrayList<Integer>();
        String cursor = null;
        Pagina<Integer> pagina;
        do {
            pagina = Cursores.pagina(consulta, 20, cursor);
            vistos.addAll(pagina.elementos());
            cursor = pagina.cursorFinal();
        } while (pagina.hayMas());

        assertThat(vistos).containsExactlyElementsOf(CINCUENTA);
        assertThat(pagina.cursorFinal()).isNull();
        assertThat(pedidos).extracting(p -> p[0]).containsOnly(21);
        assertThat(pedidos).extracting(p -> p[1]).containsExactly(0, 20, 40);
    }

    @Test
    @DisplayName("por consulta: el cursor es el mismo que el de la lista en memoria")
    void porConsulta_mismoCursorQueLaLista() {
        Cursores.Consulta<Integer> consulta = (limite, desplazamiento) ->
                CINCUENTA.stream().skip(desplazamiento).limit(limite).toList();

        assertThat(Cursores.pagina(consulta, 20, null)).isEqualTo(Cursores.pagina(CINCUENTA, 20, null));
        var segunda = Cursores.pagina(CINCUENTA, 20, null).cursorFinal();
        assertThat(Cursores.pagina(consulta, 20, segunda)).isEqualTo(Cursores.pagina(CINCUENTA, 20, segunda));
    }

    @Test
    @DisplayName("por consulta: un cursor ajeno o un tamano fuera de rango se rechaza antes de consultar")
    void porConsulta_invalido() {
        Cursores.Consulta<Integer> consulta = (limite, desplazamiento) -> {
            throw new AssertionError("no debe consultar");
        };

        assertThatThrownBy(() -> Cursores.pagina(consulta, 20, "ajeno")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Cursores.pagina(consulta, 101, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
