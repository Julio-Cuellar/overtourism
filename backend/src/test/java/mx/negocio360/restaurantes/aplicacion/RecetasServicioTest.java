package mx.negocio360.restaurantes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.compartido.seguridad.PermisoDenegadoException;
import mx.negocio360.restaurantes.dominio.CodigoError;
import mx.negocio360.restaurantes.dominio.ModoInventario;
import mx.negocio360.restaurantes.dominio.RestaurantesException;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** RC-04 a RC-07, RC-09 y RC-10: ver la receta sin costos, el costo teorico y el margen con cifras exactas, y desactivarla. */
class RecetasServicioTest {

    private final EntornoDeRecetas e = new EntornoDeRecetas();

    private PlatilloVista platillo(String nombre, String precio) {
        return e.menu.crearPlatillo(e.ctxA, e.todo, new DatosPlatillo(nombre, null, new BigDecimal(precio), null, Set.of(), null));
    }

    private void guardar(UUID platillo, int rendimiento, InsumoComando... insumos) {
        var token = e.preparacion.preparar(e.ctxA, new GuardarRecetaComando(platillo, rendimiento, List.of(insumos))).token();
        e.preparacion.confirmar(e.ctxA, token);
    }

    private static InsumoComando insumo(UUID articulo, String cantidad, String unidad, String merma) {
        return new InsumoComando(articulo, new BigDecimal(cantidad), unidad, new BigDecimal(merma));
    }

    private static void falla(Runnable accion, CodigoError esperado) {
        assertThatThrownBy(accion::run).isInstanceOf(RestaurantesException.class).extracting("codigo").isEqualTo(esperado);
    }

    @Test
    @DisplayName("RC-05 y RC-06: la cemita cuesta 43.25, deja 41.75 de margen y 49.12 %, con el costo de cada insumo")
    void costo_cemita() {
        var cemita = platillo("Cemita poblana", "85");
        guardar(cemita.id(), 1, insumo(e.inventario.articulo("Pan", "pieza", "6.00"), "1", "pieza", "0"),
                insumo(e.inventario.articulo("Milanesa", "g", "0.18"), "120", "g", "5"),
                insumo(e.inventario.articulo("Quesillo", "g", "0.16"), "50", "g", "0"),
                insumo(e.inventario.articulo("Aguacate", "g", "0.07"), "60", "g", "10"));

        var costo = e.recetas.costoDeReceta(e.ctxA, e.todo, cemita.id());

        assertThat(costo.incompleto()).isFalse();
        assertThat(costo.costoPorPorcion()).isEqualByComparingTo("41.30");
        assertThat(costo.precio()).isEqualByComparingTo("85.00");
        assertThat(costo.margen()).isEqualByComparingTo("43.70");
        assertThat(costo.margenPct()).isEqualByComparingTo("51.41");
        assertThat(costo.version()).isEqualTo(1);
        assertThat(costo.insumos()).extracting(CostoDeInsumo::nombre).containsExactly("Pan", "Milanesa", "Quesillo", "Aguacate");
        assertThat(costo.insumos().get(1).costo()).isEqualByComparingTo("22.68");
        assertThat(costo.insumos().get(1).cantidadBase()).isEqualByComparingTo("120");
    }

    @Test
    @DisplayName("RC-05: el rendimiento reparte el costo: una olla de mole de 40 porciones")
    void costo_conRendimiento() {
        var mole = platillo("Mole poblano", "120");
        guardar(mole.id(), 40, insumo(e.inventario.articulo("Pollo", "g", "0.09"), "4", "kg", "0"),
                insumo(e.inventario.articulo("Chile mulato", "g", "0.20"), "500", "g", "0"),
                insumo(e.inventario.articulo("Chocolate", "g", "0.30"), "200", "g", "0"));

        var costo = e.recetas.costoDeReceta(e.ctxA, e.todo, mole.id());

        assertThat(costo.rendimiento()).isEqualTo(40);
        assertThat(costo.costoPorPorcion()).isEqualByComparingTo("13.00");
        assertThat(costo.margen()).isEqualByComparingTo("107.00");
    }

    @Test
    @DisplayName("RC-05: el redondeo es HALF_UP a dos decimales: 100.00 entre 3 porciones da 33.33")
    void costo_redondeo() {
        var sopa = platillo("Sopa", "50");
        guardar(sopa.id(), 3, insumo(e.inventario.articulo("Caldo", "ml", "0.1"), "1000", "ml", "0"));

        assertThat(e.recetas.costoDeReceta(e.ctxA, e.todo, sopa.id()).costoPorPorcion()).isEqualByComparingTo("33.33");
    }

    @Test
    @DisplayName("RC-05: sin costo en algun insumo, el costo es incompleto, no hay margen y se listan los que faltan")
    void costo_incompleto() {
        var taco = platillo("Taco", "30");
        var sinCosto = e.inventario.articulo("Salsa", "g", null);
        var conCostoCero = e.inventario.articulo("Cilantro", "g", "0");
        guardar(taco.id(), 1, insumo(e.inventario.articulo("Tortilla", "pieza", "1.5"), "2", "pieza", "0"),
                insumo(sinCosto, "10", "g", "0"), insumo(conCostoCero, "2", "g", "0"));

        var costo = e.recetas.costoDeReceta(e.ctxA, e.todo, taco.id());

        assertThat(costo.incompleto()).isTrue();
        assertThat(costo.costoPorPorcion()).isNull();
        assertThat(costo.margen()).isNull();
        assertThat(costo.margenPct()).isNull();
        assertThat(costo.insumosSinCosto()).containsExactlyInAnyOrder(sinCosto, conCostoCero);
    }

    @Test
    @DisplayName("RC-07 y RC-10: el costo es el de la version vigente y exige restaurantes.costo.ver")
    void costo_versionYPermiso() {
        var taco = platillo("Taco", "30");
        var tortilla = e.inventario.articulo("Tortilla", "pieza", "1.5");
        guardar(taco.id(), 1, insumo(tortilla, "2", "pieza", "0"));
        e.reloj.avanzar(java.time.Duration.ofMinutes(5));
        guardar(taco.id(), 1, insumo(tortilla, "3", "pieza", "0"));

        assertThat(e.recetas.costoDeReceta(e.ctxA, e.todo, taco.id()).costoPorPorcion()).isEqualByComparingTo("4.50");
        assertThat(e.recetas.costoDeReceta(e.ctxA, e.todo, taco.id()).version()).isEqualTo(2);
        assertThatThrownBy(() -> e.recetas.costoDeReceta(e.ctxA, Autorizacion.de(Set.of(PermisosDeRestaurantes.RECETA_VER)), taco.id()))
                .isInstanceOf(PermisoDenegadoException.class);
    }

    @Test
    @DisplayName("receta: se ve con nombres, cantidades, unidades y merma, y sin ninguna cifra de costo")
    void receta_sinCostos() {
        var taco = platillo("Taco", "30");
        guardar(taco.id(), 2, insumo(e.inventario.articulo("Tortilla", "pieza", "1.5"), "2", "pieza", "0"),
                insumo(e.inventario.articulo("Carne", "g", "0.2"), "80", "g", "3.5"));

        var vista = e.recetas.receta(e.ctxA, Autorizacion.de(Set.of(PermisosDeRestaurantes.RECETA_VER)), taco.id());

        assertThat(vista.platilloId()).isEqualTo(taco.id());
        assertThat(vista.version()).isEqualTo(1);
        assertThat(vista.rendimiento()).isEqualTo(2);
        assertThat(vista.activa()).isTrue();
        assertThat(vista.insumos()).extracting(InsumoVista::nombre).containsExactly("Tortilla", "Carne");
        assertThat(vista.insumos().get(1).mermaPct()).isEqualByComparingTo("3.50");
        assertThat(java.util.Arrays.stream(InsumoVista.class.getRecordComponents()).map(c -> c.getName()))
                .containsExactlyInAnyOrder("articuloId", "nombre", "cantidad", "unidad", "mermaPct");
    }

    @Test
    @DisplayName("receta: sin receta responde RECETA_NO_ENCONTRADA, sin el permiso falla, y la de otro negocio no se ve")
    void receta_noEncontradaYPermiso() {
        var taco = platillo("Taco", "30");

        falla(() -> e.recetas.receta(e.ctxA, e.todo, taco.id()), CodigoError.RECETA_NO_ENCONTRADA);
        falla(() -> e.recetas.costoDeReceta(e.ctxA, e.todo, taco.id()), CodigoError.RECETA_NO_ENCONTRADA);
        assertThatThrownBy(() -> e.recetas.receta(e.ctxA, Autorizacion.de(Set.of()), taco.id())).isInstanceOf(PermisoDenegadoException.class);
        guardar(taco.id(), 1, insumo(e.inventario.articulo("Tortilla", "pieza", "1.5"), "2", "pieza", "0"));
        falla(() -> e.recetas.receta(e.ctxB, e.todo, taco.id()), CodigoError.RECETA_NO_ENCONTRADA);
    }

    @Test
    @DisplayName("un articulo que ya no esta en Inventario se muestra sin nombre en lugar de romper la consulta")
    void receta_articuloDesaparecido() {
        var taco = platillo("Taco", "30");
        var tortilla = e.inventario.articulo("Tortilla", "pieza", "1.5");
        guardar(taco.id(), 1, insumo(tortilla, "2", "pieza", "0"));
        e.inventario.articulos.remove(tortilla);

        var vista = e.recetas.receta(e.ctxA, e.todo, taco.id());

        assertThat(vista.insumos().get(0).nombre()).isNull();
        assertThat(e.recetas.costoDeReceta(e.ctxA, e.todo, taco.id()).incompleto()).isTrue();
    }

    @Test
    @DisplayName("RC-09: desactivar la receta de un platillo en modo RECETA falla; con otro modo la desactiva")
    void desactivar() {
        var taco = platillo("Taco", "30");
        guardar(taco.id(), 1, insumo(e.inventario.articulo("Tortilla", "pieza", "1.5"), "2", "pieza", "0"));
        e.menu.cambiarModo(e.ctxA, e.todo, taco.id(), ModoInventario.RECETA);

        falla(() -> e.recetas.desactivarReceta(e.ctxA, e.todo, taco.id()), CodigoError.RECETA_REQUERIDA);
        e.menu.cambiarModo(e.ctxA, e.todo, taco.id(), ModoInventario.NINGUNO);

        assertThat(e.recetas.desactivarReceta(e.ctxA, e.todo, taco.id()).activa()).isFalse();
        assertThat(e.menu.platillo(e.ctxA, e.todo, taco.id()).tieneReceta()).isFalse();
        assertThat(e.menu.resumenDeMenu(e.ctxA, e.todo).conReceta()).isZero();
    }

    @Test
    @DisplayName("desactivar exige restaurantes.receta.editar")
    void desactivar_permiso() {
        var taco = platillo("Taco", "30");
        guardar(taco.id(), 1, insumo(e.inventario.articulo("Tortilla", "pieza", "1.5"), "2", "pieza", "0"));

        assertThatThrownBy(() -> e.recetas.desactivarReceta(e.ctxA, Autorizacion.de(Set.of(PermisosDeRestaurantes.RECETA_VER)), taco.id()))
                .isInstanceOf(PermisoDenegadoException.class);
    }

    @Test
    @DisplayName("el menu cuenta la cobertura de recetas con las recetas reales")
    void cobertura() {
        var a = platillo("A platillo", "10");
        platillo("B platillo", "10");
        guardar(a.id(), 1, insumo(e.inventario.articulo("Tortilla", "pieza", "1.5"), "2", "pieza", "0"));

        var resumen = e.menu.resumenDeMenu(e.ctxA, e.todo);

        assertThat(resumen.conReceta()).isEqualTo(1);
        assertThat(resumen.coberturaDeRecetasPct()).isEqualByComparingTo("50.00");
        assertThat(e.menu.platillos(e.ctxA, e.todo, new FiltroPlatillos(null, null, false, false, true), 20, null).nodos())
                .extracting(PlatilloVista::nombre).containsExactly("B platillo");
    }
}
