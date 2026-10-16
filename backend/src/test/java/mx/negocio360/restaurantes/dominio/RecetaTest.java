package mx.negocio360.restaurantes.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** RC-01, RC-02, RC-04 y RC-07: la receta es una lista de versiones inmutables; cada guardado crea una y cierra la anterior. */
class RecetaTest {

    private static final UUID NEGOCIO = UUID.fromString("0192f3a0-7c10-7000-8000-00000000000a");
    private static final UUID PLATILLO = UUID.fromString("0192f3a0-7c10-7000-8000-0000000000a1");
    private static final UUID PAN = UUID.fromString("0192f3a0-7c10-7000-8000-0000000000b1");
    private static final UUID MILANESA = UUID.fromString("0192f3a0-7c10-7000-8000-0000000000b2");
    private static final Instant T0 = Instant.parse("2026-10-16T12:00:00Z");
    private static final Instant T1 = Instant.parse("2026-10-16T13:00:00Z");
    private static final Instant T2 = Instant.parse("2026-10-16T14:00:00Z");

    private static InsumoDeReceta pan() {
        return new InsumoDeReceta(PAN, new BigDecimal("1"), "pieza", BigDecimal.ZERO);
    }

    private static InsumoDeReceta milanesa() {
        return new InsumoDeReceta(MILANESA, new BigDecimal("120"), "g", new BigDecimal("5"));
    }

    private static Receta nueva() {
        return Receta.nueva(UUID.randomUUID(), NEGOCIO, PLATILLO, 1, List.of(pan(), milanesa()), "ana", T0);
    }

    private static void falla(Runnable accion, CodigoError esperado) {
        assertThatThrownBy(accion::run).isInstanceOf(RestaurantesException.class).extracting("codigo").isEqualTo(esperado);
    }

    @Test
    @DisplayName("RC-01: una receta nueva tiene la version 1, vigente desde ahora y sin fin")
    void nueva_version1() {
        var r = nueva();

        assertThat(r.versiones()).hasSize(1);
        var v = r.versionVigente().orElseThrow();
        assertThat(v.numero()).isEqualTo(1);
        assertThat(v.vigenteDesde()).isEqualTo(T0);
        assertThat(v.vigenteHasta()).isNull();
        assertThat(v.insumos()).hasSize(2);
        assertThat(v.creadaPor()).isEqualTo("ana");
        assertThat(r.activa()).isTrue();
        assertThat(r.version()).isEqualTo(1);
    }

    @Test
    @DisplayName("RC-01: guardar de nuevo crea la version siguiente y cierra la anterior en ese instante; solo una queda abierta")
    void nuevaVersion_cierraLaAnterior() {
        var r = nueva().conNuevaVersion(2, List.of(pan()), "beto", T1);

        assertThat(r.versiones()).extracting(RecetaVersion::numero).containsExactly(1, 2);
        assertThat(r.versiones().get(0).vigenteHasta()).isEqualTo(T1);
        assertThat(r.versiones().get(1).vigenteDesde()).isEqualTo(T1);
        assertThat(r.versiones().get(1).vigenteHasta()).isNull();
        assertThat(r.versiones().stream().filter(v -> v.vigenteHasta() == null)).hasSize(1);
        assertThat(r.version()).isEqualTo(2);
    }

    @Test
    @DisplayName("RC-01: las versiones ya guardadas no cambian: la 1 conserva sus insumos y su rendimiento")
    void versionesInmutables() {
        var v1 = nueva();
        var v2 = v1.conNuevaVersion(40, List.of(pan()), "ana", T1);

        assertThat(v2.versiones().get(0).insumos()).hasSize(2);
        assertThat(v2.versiones().get(0).rendimiento()).isEqualTo(1);
        assertThat(v2.versiones().get(1).rendimiento()).isEqualTo(40);
        assertThat(v1.versiones()).as("el original no se toca").hasSize(1);
    }

    @Test
    @DisplayName("RC-07: la version de un instante es la que estaba vigente entonces, no la de ahora")
    void vigenteEn() {
        var r = nueva().conNuevaVersion(1, List.of(pan()), "ana", T1);

        assertThat(r.versionEn(Instant.parse("2026-10-16T11:59:59Z"))).isEmpty();
        assertThat(r.versionEn(T0).orElseThrow().numero()).isEqualTo(1);
        assertThat(r.versionEn(Instant.parse("2026-10-16T12:59:59Z")).orElseThrow().numero()).isEqualTo(1);
        assertThat(r.versionEn(T1).orElseThrow().numero()).as("al instante del cambio ya manda la nueva").isEqualTo(2);
        assertThat(r.versionEn(T2).orElseThrow().numero()).isEqualTo(2);
    }

    @Test
    @DisplayName("desactivar cierra la version abierta: deja de estar activa, pero una venta de antes de ese momento aun la encuentra")
    void desactivar() {
        var r = nueva().desactivada(T1);

        assertThat(r.activa()).isFalse();
        assertThat(r.versionVigente()).isEmpty();
        assertThat(r.versionEn(T0).orElseThrow().numero()).isEqualTo(1);
        assertThat(r.versionEn(T2)).isEmpty();
        assertThat(r.conNuevaVersion(1, List.of(pan()), "ana", T2).activa()).as("guardar de nuevo la reactiva").isTrue();
    }

    @Test
    @DisplayName("RC-02: de 1 a 30 insumos")
    void insumos_cantidad() {
        falla(() -> Receta.nueva(UUID.randomUUID(), NEGOCIO, PLATILLO, 1, List.of(), "ana", T0), CodigoError.RECETA_INVALIDA);
        var treinta = java.util.stream.IntStream.range(0, 30).mapToObj(i -> new InsumoDeReceta(UUID.randomUUID(), BigDecimal.ONE, "g", BigDecimal.ZERO)).toList();
        assertThat(Receta.nueva(UUID.randomUUID(), NEGOCIO, PLATILLO, 1, treinta, "ana", T0).versionVigente().orElseThrow().insumos()).hasSize(30);
        var treintaYUno = java.util.stream.IntStream.range(0, 31).mapToObj(i -> new InsumoDeReceta(UUID.randomUUID(), BigDecimal.ONE, "g", BigDecimal.ZERO)).toList();
        falla(() -> Receta.nueva(UUID.randomUUID(), NEGOCIO, PLATILLO, 1, treintaYUno, "ana", T0), CodigoError.RECETA_INVALIDA);
    }

    @Test
    @DisplayName("RC-02: un articulo no se repite en la misma version")
    void insumos_repetido() {
        falla(() -> Receta.nueva(UUID.randomUUID(), NEGOCIO, PLATILLO, 1, List.of(pan(), pan()), "ana", T0), CodigoError.INSUMO_REPETIDO);
    }

    @Test
    @DisplayName("RC-02: la cantidad es mayor que 0 (4 decimales) y la merma va de 0 a 100 (2 decimales)")
    void insumo_valores() {
        falla(() -> new InsumoDeReceta(PAN, BigDecimal.ZERO, "g", BigDecimal.ZERO), CodigoError.RECETA_INVALIDA);
        falla(() -> new InsumoDeReceta(PAN, new BigDecimal("-1"), "g", BigDecimal.ZERO), CodigoError.RECETA_INVALIDA);
        falla(() -> new InsumoDeReceta(PAN, new BigDecimal("0.00004"), "g", BigDecimal.ZERO), CodigoError.RECETA_INVALIDA);
        falla(() -> new InsumoDeReceta(PAN, BigDecimal.ONE, "g", new BigDecimal("-0.01")), CodigoError.RECETA_INVALIDA);
        falla(() -> new InsumoDeReceta(PAN, BigDecimal.ONE, "g", new BigDecimal("100.01")), CodigoError.RECETA_INVALIDA);
        falla(() -> new InsumoDeReceta(PAN, BigDecimal.ONE, " ", BigDecimal.ZERO), CodigoError.RECETA_INVALIDA);
        falla(() -> new InsumoDeReceta(PAN, BigDecimal.ONE, "x".repeat(21), BigDecimal.ZERO), CodigoError.RECETA_INVALIDA);
        falla(() -> new InsumoDeReceta(null, BigDecimal.ONE, "g", BigDecimal.ZERO), CodigoError.RECETA_INVALIDA);
        assertThat(new InsumoDeReceta(PAN, new BigDecimal("0.0001"), " g ", new BigDecimal("100")).unidad()).isEqualTo("g");
        assertThat(new InsumoDeReceta(PAN, new BigDecimal("1.23456"), "g", new BigDecimal("5.005")).cantidad()).isEqualByComparingTo("1.2346");
        assertThat(new InsumoDeReceta(PAN, BigDecimal.ONE, "g", null).mermaPct()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("RC-04: el rendimiento es un entero de 1 a 500")
    void rendimiento() {
        falla(() -> Receta.nueva(UUID.randomUUID(), NEGOCIO, PLATILLO, 0, List.of(pan()), "ana", T0), CodigoError.RECETA_INVALIDA);
        falla(() -> Receta.nueva(UUID.randomUUID(), NEGOCIO, PLATILLO, 501, List.of(pan()), "ana", T0), CodigoError.RECETA_INVALIDA);
        assertThat(Receta.nueva(UUID.randomUUID(), NEGOCIO, PLATILLO, 500, List.of(pan()), "ana", T0).versionVigente().orElseThrow().rendimiento()).isEqualTo(500);
    }

    @Test
    @DisplayName("la lista de versiones y la de insumos son inmutables")
    void inmutable() {
        var r = nueva();

        assertThatThrownBy(() -> r.versiones().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> r.versionVigente().orElseThrow().insumos().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
