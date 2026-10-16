package mx.negocio360.ventas.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.catalogo.FuenteDeProductos;
import mx.negocio360.compartido.catalogo.ProductoParaVenta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** D-128: Ventas junta las fuentes de productos sin conocer a ningun vertical. */
class CatalogoCompuestoTest {

    private static final UUID NEGOCIO_A = UUID.randomUUID();
    private static final UUID NEGOCIO_B = UUID.randomUUID();
    private static final UUID TACO = UUID.fromString("0191f1b0-0010-7000-8000-000000000010");
    private static final UUID GRANEL = UUID.fromString("0191f1b0-0020-7000-8000-000000000020");

    /** Una fuente que solo conoce los productos que se le dan, de un solo negocio. */
    private static FuenteDeProductos fuente(UUID negocio, ProductoParaVenta... productos) {
        var porId = new HashMap<UUID, ProductoParaVenta>();
        for (var p : productos) {
            porId.put(p.productoId(), p);
        }
        return new FuenteDeProductos() {
            @Override
            public Map<UUID, ProductoParaVenta> preciosParaVenta(UUID negocioId, Set<UUID> ids) {
                var encontrados = new HashMap<UUID, ProductoParaVenta>();
                if (negocioId.equals(negocio)) {
                    ids.forEach(id -> {
                        if (porId.containsKey(id)) {
                            encontrados.put(id, porId.get(id));
                        }
                    });
                }
                return encontrados;
            }

            @Override
            public List<ProductoParaVenta> buscar(UUID negocioId, String texto, int limite) {
                if (!negocioId.equals(negocio)) {
                    return List.of();
                }
                return porId.values().stream().filter(p -> texto == null
                        || p.descripcion().toLowerCase().contains(texto.toLowerCase())).limit(limite).toList();
            }
        };
    }

    private static ProductoParaVenta taco(String precio) {
        return new ProductoParaVenta(TACO, "Taco de canasta", "pieza", new BigDecimal(precio), false, true);
    }

    private static ProductoParaVenta granel() {
        return new ProductoParaVenta(GRANEL, "Amaranto a granel", "kg", new BigDecimal("80.00"), true, true);
    }

    @Test
    @DisplayName("junta los productos de varias fuentes y un id que ninguna conoce no aparece")
    void preciosParaVenta_variasFuentes() {
        var catalogo = new CatalogoCompuesto(List.of(fuente(NEGOCIO_A, taco("20.00")), fuente(NEGOCIO_A, granel())));

        var precios = catalogo.preciosParaVenta(NEGOCIO_A, Set.of(TACO, GRANEL, UUID.randomUUID()));

        assertThat(precios).containsOnlyKeys(TACO, GRANEL);
        assertThat(precios.get(GRANEL).admiteFraccion()).isTrue();
    }

    @Test
    @DisplayName("las fuentes solo entregan productos del negocio que pregunta")
    void preciosParaVenta_aislaPorNegocio() {
        var catalogo = new CatalogoCompuesto(List.of(fuente(NEGOCIO_A, taco("20.00"))));

        assertThat(catalogo.preciosParaVenta(NEGOCIO_B, Set.of(TACO))).isEmpty();
        assertThat(catalogo.buscar(NEGOCIO_B, "taco", 10)).isEmpty();
    }

    @Test
    @DisplayName("si dos fuentes reclaman el mismo id gana la primera")
    void preciosParaVenta_idRepetido_ganaLaPrimera() {
        var catalogo = new CatalogoCompuesto(List.of(fuente(NEGOCIO_A, taco("20.00")), fuente(NEGOCIO_A, taco("99.00"))));

        assertThat(catalogo.preciosParaVenta(NEGOCIO_A, Set.of(TACO)).get(TACO).precio())
                .isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("una fuente que devuelve un producto con otro id del pedido se ignora")
    void preciosParaVenta_productoConOtroId_seIgnora() {
        var rara = new FuenteDeProductos() {
            @Override
            public Map<UUID, ProductoParaVenta> preciosParaVenta(UUID negocioId, Set<UUID> ids) {
                return Map.of(GRANEL, taco("1.00"));
            }

            @Override
            public List<ProductoParaVenta> buscar(UUID negocioId, String texto, int limite) {
                return List.of();
            }
        };

        assertThat(new CatalogoCompuesto(List.of(rara)).preciosParaVenta(NEGOCIO_A, Set.of(GRANEL))).isEmpty();
    }

    @Test
    @DisplayName("buscar junta las fuentes, no repite ids, deja fuera los inactivos y respeta el limite")
    void buscar_juntaSinRepetirYRespetaElLimite() {
        var inactivo = new ProductoParaVenta(UUID.randomUUID(), "Taco viejo", "pieza", new BigDecimal("1.00"), false,
                false);
        var catalogo = new CatalogoCompuesto(List.of(fuente(NEGOCIO_A, taco("20.00"), inactivo),
                fuente(NEGOCIO_A, taco("99.00"), granel())));

        assertThat(catalogo.buscar(NEGOCIO_A, null, 50)).extracting(ProductoParaVenta::productoId)
                .containsExactlyInAnyOrder(TACO, GRANEL);
        assertThat(catalogo.buscar(NEGOCIO_A, null, 1)).hasSize(1);
        assertThat(catalogo.buscar(NEGOCIO_A, null, 500).size()).isLessThanOrEqualTo(50);
    }

    @Test
    @DisplayName("sin ninguna fuente, nada se encuentra")
    void sinFuentes_nadaSeEncuentra() {
        var catalogo = new CatalogoCompuesto(List.of());

        assertThat(catalogo.preciosParaVenta(NEGOCIO_A, Set.of(TACO))).isEmpty();
        assertThat(catalogo.buscar(NEGOCIO_A, "taco", 10)).isEmpty();
    }
}
