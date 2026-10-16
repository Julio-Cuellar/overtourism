package mx.negocio360.inventario.aplicacion;

import static mx.negocio360.inventario.fakes.Afirmaciones.fallaCon;
import static mx.negocio360.inventario.fakes.Escenario.d;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import mx.negocio360.inventario.dominio.CodigoError;
import mx.negocio360.inventario.fakes.Escenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** CrearArticulo, EditarArticulo, DefinirUnidadPropia y BajaArticulo (U-01, U-03, A-04). */
class ArticulosTest {

    private final Escenario e = new Escenario();

    private static CrearArticuloComando crear(String nombre, String categoria, String base, String minimo,
                                              String unidadMinimo, List<UnidadPropiaPedida> propias) {
        return new CrearArticuloComando(nombre, categoria, base, minimo == null ? null : d(minimo), unidadMinimo,
                propias);
    }

    static Stream<Arguments> altasInvalidas() {
        return Stream.of(
                Arguments.of("nombre vacio", crear("  ", null, "g", null, null, null), CodigoError.NOMBRE_INVALIDO),
                Arguments.of("nombre nulo", crear(null, null, "g", null, null, null), CodigoError.NOMBRE_INVALIDO),
                Arguments.of("nombre de 81", crear("a".repeat(81), null, "g", null, null, null),
                        CodigoError.NOMBRE_INVALIDO),
                Arguments.of("nombre con control", crear("Har\u0000ina", null, "g", null, null, null),
                        CodigoError.NOMBRE_INVALIDO),
                Arguments.of("categoria de 41", crear("Harina", "c".repeat(41), "g", null, null, null),
                        CodigoError.CATEGORIA_INVALIDA),
                Arguments.of("base desconocida", crear("Harina", null, "costal", null, null, null),
                        CodigoError.UNIDAD_NO_CONVERTIBLE),
                Arguments.of("minimo negativo", crear("Harina", null, "g", "-1", "g", null),
                        CodigoError.CANTIDAD_INVALIDA),
                Arguments.of("minimo sin unidad convertible", crear("Harina", null, "g", "1", "l", null),
                        CodigoError.UNIDAD_NO_CONVERTIBLE),
                Arguments.of("unidad propia con nombre estandar", crear("Harina", null, "g", null, null,
                        List.of(new UnidadPropiaPedida("kg", d("1000")))), CodigoError.UNIDAD_PROPIA_INVALIDA),
                Arguments.of("unidad propia con factor cero", crear("Harina", null, "g", null, null,
                        List.of(new UnidadPropiaPedida("costal", d("0")))), CodigoError.UNIDAD_PROPIA_INVALIDA),
                Arguments.of("unidad propia repetida", crear("Harina", null, "g", null, null, List.of(
                        new UnidadPropiaPedida("costal", d("25000")), new UnidadPropiaPedida("Costal", d("1")))),
                        CodigoError.UNIDAD_PROPIA_INVALIDA),
                Arguments.of("unidad propia con 9 decimales", crear("Harina", null, "g", null, null,
                        List.of(new UnidadPropiaPedida("pizca", d("0.123456789")))),
                        CodigoError.UNIDAD_PROPIA_INVALIDA));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("altasInvalidas")
    @DisplayName("un alta invalida falla al preparar con su codigo y no deja nada guardado")
    void preparar_altaInvalida_fallaSinEscribir(String caso, CrearArticuloComando comando, CodigoError codigo) {
        fallaCon(() -> e.api.preparar(e.ctxA, comando), codigo);

        assertThat(e.almacen.cuantosArticulos()).isZero();
    }

    @Test
    @DisplayName("crear guarda el articulo con saldo cero, el minimo en unidad base y sus unidades propias")
    void crear_valido_guardaConSaldoCero() {
        var id = e.harina(e.ctxA);

        var vista = e.vista(e.ctxA, id);
        assertThat(vista.nombre()).isEqualTo("Harina");
        assertThat(vista.unidadBase()).isEqualTo("g");
        assertThat(vista.minimoBase()).isEqualTo(d("1000.0000"));
        assertThat(vista.unidadesPropias()).isEqualTo(Map.of("costal", d("25000.00000000")));
        assertThat(vista.cantidadBase()).isEqualTo(d("0.0000"));
        assertThat(vista.cpp()).isEqualTo(d("0.0000"));
        assertThat(vista.valor()).isEqualTo(d("0.00"));
        assertThat(vista.activo()).isTrue();
        assertThat(vista.bajoMinimo()).isTrue();
        assertThat(e.eventos.todos()).isEmpty();
    }

    @Test
    @DisplayName("el minimo se puede capturar en una unidad propia del mismo alta")
    void crear_minimoEnUnidadPropia_convierte() {
        var id = e.crear(e.ctxA, "Harina", "g", "2", "costal", List.of(new UnidadPropiaPedida("costal", d("25000"))));

        assertThat(e.vista(e.ctxA, id).minimoBase()).isEqualTo(d("50000.0000"));
    }

    @Test
    @DisplayName("sin minimo, el minimo es cero")
    void crear_sinMinimo_minimoCero() {
        var id = e.crear(e.ctxA, "Refresco", "pieza", null, null, null);

        assertThat(e.vista(e.ctxA, id).minimoBase()).isEqualTo(d("0.0000"));
    }

    @Test
    @DisplayName("el nombre es unico entre los activos del negocio, sin importar mayusculas; otro negocio puede repetirlo")
    void crear_nombreRepetido_fallaSoloEnElMismoNegocio() {
        var existente = e.harina(e.ctxA);

        var error = fallaCon(() -> e.api.preparar(e.ctxA, crear(" HARINA ", null, "kg", null, null, null)),
                CodigoError.NOMBRE_DUPLICADO);

        assertThat(error.recursoId()).contains(existente);
        e.harina(e.ctxB);
    }

    @Test
    @DisplayName("el resumen de preparar dice que se va a crear, sin escribir todavia")
    void preparar_alta_devuelveResumen() {
        var preparada = e.api.preparar(e.ctxA, crear("Harina", null, "g", "1", "kg", null));

        assertThat(preparada.resumen()).isEqualTo("Crear el artículo Harina con unidad base g y mínimo 1000 g.");
        assertThat(e.almacen.cuantosArticulos()).isZero();
    }

    @Test
    @DisplayName("editar cambia nombre, categoria y minimo; lo que viene nulo no cambia")
    void editar_camposPresentes_actualiza() {
        var id = e.harina(e.ctxA);

        e.confirmar(e.ctxA, new EditarArticuloComando(id, "Harina de trigo", "Abarrotes", d("2"), "kg"));
        e.confirmar(e.ctxA, new EditarArticuloComando(id, null, null, null, null));

        var vista = e.vista(e.ctxA, id);
        assertThat(vista.nombre()).isEqualTo("Harina de trigo");
        assertThat(vista.categoria()).isEqualTo("Abarrotes");
        assertThat(vista.minimoBase()).isEqualTo(d("2000.0000"));
        assertThat(vista.version()).isEqualTo(3L);
    }

    @Test
    @DisplayName("editar con categoria en blanco la borra")
    void editar_categoriaEnBlanco_laBorra() {
        var id = e.harina(e.ctxA);
        e.confirmar(e.ctxA, new EditarArticuloComando(id, null, "Abarrotes", null, null));

        e.confirmar(e.ctxA, new EditarArticuloComando(id, null, " ", null, null));

        assertThat(e.vista(e.ctxA, id).categoria()).isNull();
    }

    @Test
    @DisplayName("editar al nombre de otro articulo activo falla; conservar el propio nombre no")
    void editar_nombreDeOtro_falla() {
        var harina = e.harina(e.ctxA);
        e.crear(e.ctxA, "Azúcar", "g", null, null, null);

        fallaCon(() -> e.api.preparar(e.ctxA, new EditarArticuloComando(harina, "azúcar", null, null, null)),
                CodigoError.NOMBRE_DUPLICADO);
        e.confirmar(e.ctxA, new EditarArticuloComando(harina, "HARINA", null, null, null));
        assertThat(e.vista(e.ctxA, harina).nombre()).isEqualTo("HARINA");
    }

    @Test
    @DisplayName("un articulo de otro negocio no existe para quien edita")
    void editar_articuloAjeno_noEncontrado() {
        var ajeno = e.harina(e.ctxB);

        fallaCon(() -> e.api.preparar(e.ctxA, new EditarArticuloComando(ajeno, "Mio", null, null, null)),
                CodigoError.ARTICULO_NO_ENCONTRADO);
        fallaCon(() -> e.api.preparar(e.ctxA, new EditarArticuloComando(UUID.randomUUID(), "Mio", null, null,
                null)), CodigoError.ARTICULO_NO_ENCONTRADO);
    }

    @Test
    @DisplayName("U-03: definir una unidad propia nueva la deja disponible para convertir")
    void definirUnidad_nueva_quedaDisponible() {
        var id = e.harina(e.ctxA);

        e.confirmar(e.ctxA, new DefinirUnidadPropiaComando(id, "Bulto", d("50000")));

        assertThat(e.vista(e.ctxA, id).unidadesPropias()).containsEntry("bulto", d("50000.00000000"));
        var enBase = e.api.aBase(e.ctxA, List.of(new CantidadPedida(id, d("1"), "bulto")));
        assertThat(enBase.get(0).cantidadBase()).isEqualTo(d("50000.0000"));
    }

    @Test
    @DisplayName("U-03: una unidad propia no puede repetirse ni llamarse como una estandar")
    void definirUnidad_repetidaOEstandar_falla() {
        var id = e.harina(e.ctxA);

        fallaCon(() -> e.api.preparar(e.ctxA, new DefinirUnidadPropiaComando(id, "COSTAL", d("1"))),
                CodigoError.UNIDAD_PROPIA_INVALIDA);
        fallaCon(() -> e.api.preparar(e.ctxA, new DefinirUnidadPropiaComando(id, "ml", d("1"))),
                CodigoError.UNIDAD_PROPIA_INVALIDA);
        fallaCon(() -> e.api.preparar(e.ctxA, new DefinirUnidadPropiaComando(id, "", d("1"))),
                CodigoError.UNIDAD_PROPIA_INVALIDA);
        fallaCon(() -> e.api.preparar(e.ctxA, new DefinirUnidadPropiaComando(id, "lata", null)),
                CodigoError.UNIDAD_PROPIA_INVALIDA);
    }

    @Test
    @DisplayName("A-04: la baja es logica y solo con saldo cero")
    void baja_saldoCero_desactiva() {
        var id = e.harina(e.ctxA);

        e.confirmar(e.ctxA, new BajaArticuloComando(id));

        var vista = e.vista(e.ctxA, id);
        assertThat(vista.activo()).isFalse();
        assertThat(e.api.articulos(e.ctxA, null, false, 10, null).nodos()).isEmpty();
    }

    @Test
    @DisplayName("A-04: con saldo distinto de cero la baja falla, tambien con saldo negativo")
    void baja_conSaldo_falla() {
        var id = e.harina(e.ctxA);
        e.entrada(e.ctxA, id, "1", "kg", "20");

        fallaCon(() -> e.api.preparar(e.ctxA, new BajaArticuloComando(id)), CodigoError.BAJA_CON_SALDO);

        e.salida(e.ctxA, id, "2", "kg");
        fallaCon(() -> e.api.preparar(e.ctxA, new BajaArticuloComando(id)), CodigoError.BAJA_CON_SALDO);
    }

    @Test
    @DisplayName("un articulo dado de baja ya no se edita, no recibe unidades y libera su nombre")
    void baja_articuloInactivo_noSeModificaYLiberaNombre() {
        var id = e.harina(e.ctxA);
        e.confirmar(e.ctxA, new BajaArticuloComando(id));

        fallaCon(() -> e.api.preparar(e.ctxA, new BajaArticuloComando(id)), CodigoError.ARTICULO_INACTIVO);
        fallaCon(() -> e.api.preparar(e.ctxA, new EditarArticuloComando(id, "Otra", null, null, null)),
                CodigoError.ARTICULO_INACTIVO);
        fallaCon(() -> e.api.preparar(e.ctxA, new DefinirUnidadPropiaComando(id, "bulto", d("2"))),
                CodigoError.ARTICULO_INACTIVO);
        e.harina(e.ctxA);
    }
}
