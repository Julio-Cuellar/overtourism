package mx.negocio360.compartido.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Lo que debe cumplir cualquier {@link FuenteDeProductos}: la de Restaurantes, la de Miscelaneos, la de Catalogo o la
 * simulada de desarrollo (kit de contrato, docs/planes/integracion-ventas-caja.md §11.3, D-128, HZ-28). Cada vertical
 * extiende esta clase, aporta su fuente ya sembrada y los datos que el contrato necesita, y pasa la misma bateria.
 *
 * <p>Lo que depende de que la fuente distinga cosas (productos dados de baja, aislamiento por negocio) es opcional: si la
 * fuente no puede tenerlo, la subclase lo deja vacio y esa prueba se omite, no se da por cumplida en silencio.
 */
public abstract class ContratoDeFuenteDeProductosTest {

    /** La fuente bajo prueba, ya sembrada con los productos de {@link #negocioConProductos()}. */
    protected abstract FuenteDeProductos fuente();

    /** Un negocio al que la fuente ofrece al menos dos productos vendibles. */
    protected abstract UUID negocioConProductos();

    /** Los ids de los productos vendibles (activos) del negocio; al menos dos. */
    protected abstract Set<UUID> idsVendibles();

    /** Un producto dado de baja de ese negocio, si la fuente distingue la baja; vacio si no. */
    protected Optional<UUID> idDadoDeBaja() {
        return Optional.empty();
    }

    /**
     * Otro negocio con productos propios, si la fuente aisla por negocio (MN-03). Vacio para una fuente que da lo mismo a
     * todos, como la simulada de desarrollo.
     */
    protected Optional<UUID> otroNegocioConProductos() {
        return Optional.empty();
    }

    /** Los ids de los productos del otro negocio; solo se consulta si {@link #otroNegocioConProductos()} no esta vacio. */
    protected Set<UUID> idsDelOtroNegocio() {
        return Set.of();
    }

    @Test
    @DisplayName("preciosParaVenta devuelve cada producto vendible pedido, con id, descripcion, unidad y precio positivo")
    void preciosDeLosVendibles() {
        var respuesta = fuente().preciosParaVenta(negocioConProductos(), idsVendibles());

        assertThat(respuesta.keySet()).containsExactlyInAnyOrderElementsOf(idsVendibles());
        respuesta.forEach((id, producto) -> {
            assertThat(producto.productoId()).isEqualTo(id);
            assertThat(producto.descripcion()).isNotBlank();
            assertThat(producto.unidad()).isNotBlank();
            assertThat(producto.precio()).isPositive();
            assertThat(producto.activo()).isTrue();
        });
    }

    @Test
    @DisplayName("un id que la fuente no conoce no aparece y no estorba a los demas")
    void idDesconocidoNoAparece() {
        var desconocido = UUID.randomUUID();
        var pedido = new HashSet<>(idsVendibles());
        pedido.add(desconocido);

        var respuesta = fuente().preciosParaVenta(negocioConProductos(), pedido);

        assertThat(respuesta).doesNotContainKey(desconocido);
        assertThat(respuesta.keySet()).containsAll(idsVendibles());
    }

    @Test
    @DisplayName("sin ids no hay productos")
    void sinIdsNoHayNada() {
        assertThat(fuente().preciosParaVenta(negocioConProductos(), Set.of())).isEmpty();
    }

    @Test
    @DisplayName("buscar sin texto devuelve solo productos activos y respeta el limite")
    void buscarSinTextoYLimite() {
        var todos = fuente().buscar(negocioConProductos(), null, 50);
        var uno = fuente().buscar(negocioConProductos(), "", 1);

        assertThat(todos).isNotEmpty().allSatisfy(producto -> assertThat(producto.activo()).isTrue());
        assertThat(uno).hasSize(1);
    }

    @Test
    @DisplayName("buscar por texto encuentra el producto por su descripcion sin distinguir mayusculas")
    void buscarPorTexto() {
        var primero = fuente().buscar(negocioConProductos(), null, 1).get(0);

        var encontrados = fuente().buscar(negocioConProductos(), primero.descripcion().toLowerCase(Locale.ROOT), 50);

        assertThat(encontrados).extracting(ProductoParaVenta::productoId).contains(primero.productoId());
    }

    @Test
    @DisplayName("buscar un texto que no coincide con nada devuelve vacio")
    void buscarSinCoincidencia() {
        assertThat(fuente().buscar(negocioConProductos(), "zzz-sin-coincidencia-9f3a", 50)).isEmpty();
    }

    @Test
    @DisplayName("un producto dado de baja no se ofrece al buscar, y si se pregunta por su precio sale inactivo")
    void productoDadoDeBaja() {
        assumeThat(idDadoDeBaja()).as("la fuente distingue la baja").isPresent();
        var baja = idDadoDeBaja().orElseThrow();

        var ofrecidos = fuente().buscar(negocioConProductos(), null, 50).stream().map(ProductoParaVenta::productoId)
                .collect(Collectors.toSet());
        var consultado = fuente().preciosParaVenta(negocioConProductos(), Set.of(baja));

        assertThat(ofrecidos).doesNotContain(baja);
        consultado.values().forEach(producto -> assertThat(producto.activo()).isFalse());
    }

    @Test
    @DisplayName("MN-03: un negocio no ve los productos de otro, ni al consultar precios ni al buscar")
    void aislamientoPorNegocio() {
        assumeThat(otroNegocioConProductos()).as("la fuente aisla por negocio").isPresent();
        var otro = otroNegocioConProductos().orElseThrow();

        var preciosAjenos = fuente().preciosParaVenta(negocioConProductos(), idsDelOtroNegocio());
        var buscadosPorElOtro = fuente().buscar(otro, null, 50).stream().map(ProductoParaVenta::productoId)
                .collect(Collectors.toSet());

        assertThat(preciosAjenos).isEmpty();
        assertThat(buscadosPorElOtro).doesNotContainAnyElementsOf(idsVendibles());
    }

    @Test
    @DisplayName("R-V2: el modo de inventario de un producto que la fuente no conoce es NINGUNO")
    void modoDeInventarioPorOmision() {
        assertThat(fuente().modoInventario(negocioConProductos(), UUID.randomUUID())).isEqualTo(ModoInventario.NINGUNO);
    }
}
