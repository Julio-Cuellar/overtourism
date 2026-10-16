package mx.negocio360.inventario.aplicacion;

import static mx.negocio360.inventario.fakes.Afirmaciones.fallaCon;
import static mx.negocio360.inventario.fakes.Escenario.d;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import mx.negocio360.inventario.dominio.CodigoError;
import mx.negocio360.inventario.fakes.Escenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Consultas de InventarioApi: articulo, articulos, aBase y aislamiento entre negocios (6.1, 10). */
class ConsultasTest {

    private final Escenario e = new Escenario();

    @Test
    @DisplayName("articulos busca por parte del nombre, sin importar mayusculas, solo entre activos")
    void articulos_porTexto_filtra() {
        var harina = e.harina(e.ctxA);
        e.crear(e.ctxA, "Harina integral", "g", null, null, null);
        e.crear(e.ctxA, "Azúcar", "g", null, null, null);

        var pagina = e.api.articulos(e.ctxA, " HARINA ", false, 10, null);

        assertThat(pagina.nodos()).extracting(ArticuloVista::nombre)
                .containsExactlyInAnyOrder("Harina", "Harina integral");
        assertThat(pagina.nodos()).extracting(ArticuloVista::id).contains(harina);
        assertThat(pagina.hayMas()).isFalse();
    }

    @Test
    @DisplayName("soloAlertas devuelve los que estan en o bajo su minimo, o en negativo")
    void articulos_soloAlertas_filtra() {
        var harina = e.harina(e.ctxA);
        var azucar = e.crear(e.ctxA, "Azúcar", "g", "100", "g", null);
        var sal = e.crear(e.ctxA, "Sal", "g", null, null, null);
        e.entrada(e.ctxA, azucar, "1", "kg", "25");
        e.entrada(e.ctxA, sal, "1", "kg", "10");
        e.salida(e.ctxA, sal, "2", "kg");

        var alertas = e.api.articulos(e.ctxA, null, true, 10, null);

        assertThat(alertas.nodos()).extracting(ArticuloVista::id).containsExactlyInAnyOrder(harina, sal);
        assertThat(e.vista(e.ctxA, azucar).bajoMinimo()).isFalse();
    }

    @Test
    @DisplayName("articulos pagina por cursor, con maximo de 100 por pagina")
    void articulos_paginacion() {
        for (var i = 0; i < 3; i++) {
            e.crear(e.ctxA, "Insumo " + i, "pieza", null, null, null);
        }

        var primera = e.api.articulos(e.ctxA, null, false, 2, null);
        var segunda = e.api.articulos(e.ctxA, null, false, 2, primera.cursor());

        assertThat(primera.nodos()).hasSize(2);
        assertThat(primera.hayMas()).isTrue();
        assertThat(segunda.nodos()).hasSize(1);
        assertThat(segunda.hayMas()).isFalse();
        assertThat(segunda.cursor()).isEqualTo(segunda.nodos().get(0).id().toString());
        assertThat(e.api.articulos(e.ctxA, null, false, 500, null).nodos()).hasSize(3);
    }

    @Test
    @DisplayName("pagina de cero, cursor mal formado o texto de mas de 80 son errores de quien llama")
    void articulos_parametrosInvalidos_fallan() {
        assertThatThrownBy(() -> e.api.articulos(e.ctxA, null, false, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> e.api.articulos(e.ctxA, null, false, 10, "1-1-1-1-1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> e.api.articulos(e.ctxA, null, false, 10, "no-es-uuid"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> e.api.articulos(e.ctxA, "a".repeat(81), false, 10, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> e.api.kardex(e.ctxA, e.harina(e.ctxA), 0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("un negocio no ve los articulos de otro")
    void aislamiento_otroNegocio_noVe() {
        var ajena = e.harina(e.ctxB);

        assertThat(e.api.articulo(e.ctxA, ajena)).isEmpty();
        assertThat(e.api.articulos(e.ctxA, null, false, 10, null).nodos()).isEmpty();
        assertThat(e.api.articulos(e.ctxB, null, false, 10, null).nodos()).hasSize(1);
    }

    @Test
    @DisplayName("aBase convierte varias cantidades a la unidad base de cada articulo (para Restaurantes)")
    void aBase_varias_convierte() {
        var harina = e.harina(e.ctxA);
        var refresco = e.crear(e.ctxA, "Refresco", "pieza", null, null, null);

        var resultado = e.api.aBase(e.ctxA, List.of(new CantidadPedida(harina, d("1.5"), "costal"),
                new CantidadPedida(refresco, d("2"), "pieza")));

        assertThat(resultado).containsExactly(
                new CantidadEnBase(harina, d("37500.0000"), "g", d("25000.00000000")),
                new CantidadEnBase(refresco, d("2.0000"), "pieza", d("1.00000000")));
    }

    @Test
    @DisplayName("aBase falla con el codigo del primer problema y respeta el tope de 100 lineas")
    void aBase_errores() {
        var harina = e.harina(e.ctxA);
        var ajena = e.harina(e.ctxB);

        fallaCon(() -> e.api.aBase(e.ctxA, List.of(new CantidadPedida(ajena, d("1"), "g"))),
                CodigoError.ARTICULO_NO_ENCONTRADO);
        fallaCon(() -> e.api.aBase(e.ctxA, List.of(new CantidadPedida(harina, d("1"), "ml"))),
                CodigoError.UNIDAD_NO_CONVERTIBLE);
        fallaCon(() -> e.api.aBase(e.ctxA, List.of(new CantidadPedida(harina, d("0"), "g"))),
                CodigoError.CANTIDAD_INVALIDA);
        var demasiadas = Collections.nCopies(101, new CantidadPedida(harina, d("1"), "g"));
        assertThatThrownBy(() -> e.api.aBase(e.ctxA, demasiadas)).isInstanceOf(IllegalArgumentException.class);
        assertThat(e.api.aBase(e.ctxA, List.of())).isEmpty();
    }

    @Test
    @DisplayName("articulo inexistente devuelve vacio")
    void articulo_inexistente_vacio() {
        assertThat(e.api.articulo(e.ctxA, UUID.randomUUID())).isEmpty();
    }
}
