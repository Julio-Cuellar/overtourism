package mx.negocio360.ventas.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.catalogo.ProductoParaVenta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Los productos simulados de desarrollo (F4 del plan de integracion): seis, rotulados y fieles al contrato del SPI. */
class FuenteDeProductosSimuladaTest {

    private final FuenteDeProductosSimulada fuente = new FuenteDeProductosSimulada();
    private final UUID negocio = UUID.randomUUID();

    @Test
    @DisplayName("son seis, activos y cada nombre dice que es simulado")
    void seis_rotulados() {
        var todos = fuente.buscar(negocio, null, 50);

        assertThat(todos).hasSize(6);
        assertThat(todos).allSatisfy(p -> {
            assertThat(p.descripcion()).startsWith("[SIMULADO]");
            assertThat(p.activo()).isTrue();
        });
    }

    @Test
    @DisplayName("el granel admite fraccion y las piezas no; buscar filtra por texto y respeta el limite")
    void fraccion_busqueda_limite() {
        assertThat(fuente.buscar(negocio, "camote", 10)).singleElement()
                .satisfies(p -> assertThat(p.admiteFraccion()).isTrue());
        assertThat(fuente.buscar(negocio, "TACO", 10)).singleElement()
                .satisfies(p -> assertThat(p.admiteFraccion()).isFalse());
        assertThat(fuente.buscar(negocio, null, 2)).hasSize(2);
        assertThat(fuente.buscar(negocio, "no existe", 10)).isEmpty();
    }

    @Test
    @DisplayName("preciosParaVenta devuelve solo los ids que conoce")
    void precios_soloLosConocidos() {
        var conocido = fuente.buscar(negocio, "mole", 1).get(0).productoId();

        var precios = fuente.preciosParaVenta(negocio, Set.of(conocido, UUID.randomUUID()));

        assertThat(precios).containsOnlyKeys(conocido);
        assertThat(precios.get(conocido)).extracting(ProductoParaVenta::unidad).isEqualTo("pieza");
    }
}
