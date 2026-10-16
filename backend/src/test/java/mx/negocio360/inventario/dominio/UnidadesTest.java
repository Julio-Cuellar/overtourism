package mx.negocio360.inventario.dominio;

import static mx.negocio360.inventario.fakes.Afirmaciones.fallaCon;
import static mx.negocio360.inventario.fakes.Escenario.d;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** U-02 a U-05: unidades estandar, unidades propias y conversion a la unidad base. */
class UnidadesTest {

    private static final Instant AHORA = Instant.parse("2026-10-15T18:00:00Z");

    private static Articulo articulo(UnidadEstandar base, Map<String, BigDecimal> propias) {
        return Articulo.nuevo(UUID.randomUUID(), UUID.randomUUID(), "Insumo", null, base, propias, BigDecimal.ZERO,
                "locatario", AHORA);
    }

    @ParameterizedTest(name = "{0} {1} en base {2} = {3} (factor {4})")
    @CsvSource({
        "2,     kg,    G,     2000.0000, 1000.00000000",
        "1.5,   KG,    G,     1500.0000, 1000.00000000",
        "250,   g,     KG,    0.2500,    0.00100000",
        "3,     l,     ML,    3000.0000, 1000.00000000",
        "750,   ml,    L,     0.7500,    0.00100000",
        "12,    pieza, PIEZA, 12.0000,   1.00000000",
        "0.333, g,     G,     0.3330,    1.00000000"
    })
    @DisplayName("U-02: las unidades estandar convierten dentro de su dimension")
    void aBase_unidadEstandarMismaDimension_convierte(String cantidad, String unidad, UnidadEstandar base,
                                                      String esperada, String factor) {
        var conversion = articulo(base, Map.of()).aBase(d(cantidad), unidad);

        assertThat(conversion.cantidadBase()).isEqualTo(d(esperada));
        assertThat(conversion.factorAplicado()).isEqualTo(d(factor));
        assertThat(conversion.unidadCapturada()).isEqualTo(unidad.strip().toLowerCase());
        assertThat(conversion.cantidadCapturada()).isEqualTo(d(cantidad));
    }

    @ParameterizedTest(name = "{0} con base {1} no convierte")
    @CsvSource({"ml, G", "kg, PIEZA", "pieza, L", "litro, ML", "'', G", "'  ', G"})
    @DisplayName("U-04: una unidad de otra dimension o desconocida se rechaza; nunca se adivina")
    void aBase_unidadNoConvertible_falla(String unidad, UnidadEstandar base) {
        fallaCon(() -> articulo(base, Map.of()).aBase(d("1"), unidad), CodigoError.UNIDAD_NO_CONVERTIBLE);
    }

    @Test
    @DisplayName("U-04: sin unidad tambien se rechaza")
    void aBase_unidadNula_falla() {
        fallaCon(() -> articulo(UnidadEstandar.G, Map.of()).aBase(d("1"), null), CodigoError.UNIDAD_NO_CONVERTIBLE);
    }

    @Test
    @DisplayName("U-03: la unidad propia del articulo convierte con su factor y puede cruzar dimensiones")
    void aBase_unidadPropia_usaSuFactor() {
        var tortillas = articulo(UnidadEstandar.G, Map.of("manojo", d("250"), "costal", d("25000")));

        var conversion = tortillas.aBase(d("2"), " Costal ");

        assertThat(conversion.cantidadBase()).isEqualTo(d("50000.0000"));
        assertThat(conversion.factorAplicado()).isEqualTo(d("25000.00000000"));
        assertThat(conversion.unidadCapturada()).isEqualTo("costal");
    }

    @Test
    @DisplayName("U-03: una caja de 24 piezas sobre base pieza")
    void aBase_cajaDePiezas_convierte() {
        var refresco = articulo(UnidadEstandar.PIEZA, Map.of("caja", d("24")));

        assertThat(refresco.aBase(d("3"), "caja").cantidadBase()).isEqualTo(d("72.0000"));
    }

    @Test
    @DisplayName("redondeo HALF_UP a 4 decimales de la cantidad base")
    void aBase_mediaDiezmilesima_redondeaHaciaArriba() {
        var refresco = articulo(UnidadEstandar.KG, Map.of());

        assertThat(refresco.aBase(d("0.25"), "g").cantidadBase()).isEqualTo(d("0.0003"));
        assertThat(refresco.aBase(d("0.24"), "g").cantidadBase()).isEqualTo(d("0.0002"));
    }

    @Test
    @DisplayName("las unidades estandar se reconocen por codigo sin importar mayusculas ni espacios")
    void deCodigo_variantes_reconoce() {
        assertThat(UnidadEstandar.deCodigo(" KG ")).contains(UnidadEstandar.KG);
        assertThat(UnidadEstandar.deCodigo("Pieza")).contains(UnidadEstandar.PIEZA);
        assertThat(UnidadEstandar.deCodigo("kilo")).isEmpty();
        assertThat(UnidadEstandar.deCodigo(null)).isEmpty();
        assertThat(UnidadEstandar.KG.codigo()).isEqualTo("kg");
        assertThat(UnidadEstandar.KG.dimension()).isEqualTo(Dimension.MASA);
    }
}
