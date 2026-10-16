package mx.negocio360.restaurantes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.MonthDay;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.restaurantes.dominio.Temporada;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El contrato de la fuente de productos (plan de integracion §11.3, MN-02, MN-03 y MN-14): lo que Ventas puede preguntarle
 * a Restaurantes. Es la misma bateria que el kit de la Pista A (ContratoDeFuenteDeProductosTest) cuando exista en la
 * plataforma.
 */
class PlatillosParaVentaTest {

    private final EntornoDeMenu e = new EntornoDeMenu();
    private final PlatillosParaVenta fuente = new PlatillosParaVenta(e.menu, e.reloj);

    private PlatilloVista crear(String nombre, String precio) {
        return e.servicio.crearPlatillo(e.ctxA, e.todo, new DatosPlatillo(nombre, null, new BigDecimal(precio), null, Set.of(), null));
    }

    @Test
    @DisplayName("MN-02: un platillo se vende por pieza, sin fraccion, con su id, nombre y precio vigente")
    void precios_forma() {
        var p = crear("Cemita poblana", "85");

        var producto = fuente.preciosParaVenta(EntornoDeMenu.NEGOCIO_A, Set.of(p.id())).get(p.id());

        assertThat(producto.productoId()).isEqualTo(p.id());
        assertThat(producto.descripcion()).isEqualTo("Cemita poblana");
        assertThat(producto.unidad()).isEqualTo("pieza");
        assertThat(producto.admiteFraccion()).isFalse();
        assertThat(producto.precio()).isEqualByComparingTo("85.00");
        assertThat(producto.activo()).isTrue();
    }

    @Test
    @DisplayName("MN-03: un id de otro negocio y uno que no existe no aparecen")
    void precios_aislamiento() {
        var deA = crear("Cemita poblana", "85");
        var deB = e.servicio.crearPlatillo(e.ctxB, e.todo, new DatosPlatillo("Taco", null, BigDecimal.TEN, null, Set.of(), null));

        var respuesta = fuente.preciosParaVenta(EntornoDeMenu.NEGOCIO_A, Set.of(deA.id(), deB.id(), UUID.randomUUID()));

        assertThat(respuesta).containsOnlyKeys(deA.id());
    }

    @Test
    @DisplayName("el precio es el vigente: si cambia, Ventas ve el nuevo")
    void precios_vigente() {
        var p = crear("Cemita poblana", "85");
        e.servicio.editarPlatillo(e.ctxA, e.todo, p.id(), p.version(), new DatosPlatillo("Cemita poblana", null, new BigDecimal("90"), null, Set.of(), null));

        assertThat(fuente.preciosParaVenta(EntornoDeMenu.NEGOCIO_A, Set.of(p.id())).get(p.id()).precio()).isEqualByComparingTo("90.00");
    }

    @Test
    @DisplayName("un platillo dado de baja se devuelve con activo falso para que Ventas lo rechace con su codigo")
    void precios_baja() {
        var p = crear("Cemita poblana", "85");
        e.menu.reemplazar(e.menu.platillo(EntornoDeMenu.NEGOCIO_A, p.id()).orElseThrow().conBaja(EntornoDeMenu.AHORA), p.version());

        assertThat(fuente.preciosParaVenta(EntornoDeMenu.NEGOCIO_A, Set.of(p.id())).get(p.id()).activo()).isFalse();
    }

    @Test
    @DisplayName("MN-14: agotado y fuera de temporada siguen siendo vendibles para Ventas: solo la baja la hace no vendible")
    void precios_agotadoYTemporada() {
        var agotado = crear("Mole poblano", "120");
        e.servicio.marcarAgotado(e.ctxA, e.todo, agotado.id(), true);
        var nogada = e.servicio.crearPlatillo(e.ctxA, e.todo, new DatosPlatillo("Chiles en nogada", null, new BigDecimal("260"), null, Set.of(),
                new Temporada(MonthDay.of(8, 1), MonthDay.of(9, 30))));

        var respuesta = fuente.preciosParaVenta(EntornoDeMenu.NEGOCIO_A, Set.of(agotado.id(), nogada.id()));

        assertThat(respuesta.get(agotado.id()).activo()).isTrue();
        assertThat(respuesta.get(nogada.id()).activo()).isTrue();
    }

    @Test
    @DisplayName("se pueden pedir hasta 100 ids; mas es un error de quien llama")
    void precios_tope() {
        var ids = new HashSet<UUID>();
        for (int i = 0; i < 100; i++) {
            ids.add(UUID.randomUUID());
        }
        assertThat(fuente.preciosParaVenta(EntornoDeMenu.NEGOCIO_A, ids)).isEmpty();
        ids.add(UUID.randomUUID());

        assertThatThrownBy(() -> fuente.preciosParaVenta(EntornoDeMenu.NEGOCIO_A, ids)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("buscar: por parte del nombre, solo lo que se puede pedir hoy (activo, no agotado, en temporada) y de ese negocio")
    void buscar_ordenablesDelNegocio() {
        crear("Cemita poblana", "85");
        var agotado = crear("Cemita de pierna", "90");
        e.servicio.marcarAgotado(e.ctxA, e.todo, agotado.id(), true);
        e.servicio.crearPlatillo(e.ctxA, e.todo, new DatosPlatillo("Cemita de nogada", null, new BigDecimal("99"), null, Set.of(),
                new Temporada(MonthDay.of(8, 1), MonthDay.of(9, 30))));
        e.servicio.crearPlatillo(e.ctxB, e.todo, new DatosPlatillo("Cemita ajena", null, BigDecimal.TEN, null, Set.of(), null));

        var encontrados = fuente.buscar(EntornoDeMenu.NEGOCIO_A, "CEMITA", 50);

        assertThat(encontrados).extracting(ProductoDePlatillo::descripcion).containsExactly("Cemita poblana");
    }

    @Test
    @DisplayName("buscar respeta el limite (hasta 50), ordena por nombre y un texto vacio devuelve los primeros")
    void buscar_limite() {
        for (int i = 0; i < 60; i++) {
            crear("Platillo " + String.format("%02d", i), "10");
        }

        assertThat(fuente.buscar(EntornoDeMenu.NEGOCIO_A, "", 5)).extracting(ProductoDePlatillo::descripcion)
                .containsExactly("Platillo 00", "Platillo 01", "Platillo 02", "Platillo 03", "Platillo 04");
        assertThat(fuente.buscar(EntornoDeMenu.NEGOCIO_A, null, 500)).hasSize(50);
        assertThatThrownBy(() -> fuente.buscar(EntornoDeMenu.NEGOCIO_A, "x", 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
