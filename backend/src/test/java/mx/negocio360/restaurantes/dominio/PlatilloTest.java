package mx.negocio360.restaurantes.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** MN-01, MN-05, MN-06, MN-08 y MN-09: lo que es un platillo y cuando se puede pedir. */
class PlatilloTest {

    private static final UUID NEGOCIO = UUID.fromString("0192f3a0-7c10-7000-8000-000000000001");
    private static final Instant AHORA = Instant.parse("2026-10-16T18:00:00Z");

    private static Platillo cemita() {
        return Platillo.nuevo(UUID.fromString("0192f3a0-7c10-7000-8000-0000000000a1"), NEGOCIO, "Cemita poblana", "Con milanesa",
                new BigDecimal("85.00"), null, AHORA);
    }

    private static void falla(Runnable accion, CodigoError esperado) {
        assertThatThrownBy(accion::run).isInstanceOf(RestaurantesException.class).extracting("codigo").isEqualTo(esperado);
    }

    @Test
    @DisplayName("un platillo nuevo es activo, no publicado, sin agotar, sin receta (NINGUNO) y en la version 1")
    void nuevo_valoresPorOmision() {
        var p = cemita();

        assertThat(p.activo()).isTrue();
        assertThat(p.publicado()).isFalse();
        assertThat(p.agotado()).isFalse();
        assertThat(p.modo()).isEqualTo(ModoInventario.NINGUNO);
        assertThat(p.version()).isEqualTo(1);
        assertThat(p.etiquetas()).isEmpty();
        assertThat(p.precio()).isEqualByComparingTo("85.00").extracting(BigDecimal::scale).isEqualTo(2);
    }

    @Test
    @DisplayName("MN-01: el nombre mide de 2 a 80 caracteres sin contar los espacios de los lados")
    void nombre_largo() {
        falla(() -> Platillo.nuevo(UUID.randomUUID(), NEGOCIO, "A", null, new BigDecimal("10"), null, AHORA), CodigoError.NOMBRE_INVALIDO);
        falla(() -> Platillo.nuevo(UUID.randomUUID(), NEGOCIO, "  ", null, new BigDecimal("10"), null, AHORA), CodigoError.NOMBRE_INVALIDO);
        falla(() -> Platillo.nuevo(UUID.randomUUID(), NEGOCIO, "x".repeat(81), null, new BigDecimal("10"), null, AHORA),
                CodigoError.NOMBRE_INVALIDO);
        assertThat(Platillo.nuevo(UUID.randomUUID(), NEGOCIO, "  Mole  ", null, new BigDecimal("10"), null, AHORA).nombre())
                .isEqualTo("Mole");
        assertThat(Platillo.nuevo(UUID.randomUUID(), NEGOCIO, "x".repeat(80), null, new BigDecimal("10"), null, AHORA)).isNotNull();
    }

    @Test
    @DisplayName("MN-01: el precio es mayor que 0, de a lo mas 99,999.99, y se redondea a dos decimales")
    void precio_limites() {
        falla(() -> Platillo.nuevo(UUID.randomUUID(), NEGOCIO, "Agua", null, BigDecimal.ZERO, null, AHORA), CodigoError.PRECIO_INVALIDO);
        falla(() -> Platillo.nuevo(UUID.randomUUID(), NEGOCIO, "Agua", null, new BigDecimal("-1"), null, AHORA), CodigoError.PRECIO_INVALIDO);
        falla(() -> Platillo.nuevo(UUID.randomUUID(), NEGOCIO, "Agua", null, new BigDecimal("100000.00"), null, AHORA),
                CodigoError.PRECIO_INVALIDO);
        falla(() -> Platillo.nuevo(UUID.randomUUID(), NEGOCIO, "Agua", null, null, null, AHORA), CodigoError.PRECIO_INVALIDO);
        assertThat(Platillo.nuevo(UUID.randomUUID(), NEGOCIO, "Agua", null, new BigDecimal("99999.99"), null, AHORA).precio())
                .isEqualByComparingTo("99999.99");
        assertThat(Platillo.nuevo(UUID.randomUUID(), NEGOCIO, "Agua", null, new BigDecimal("35.005"), null, AHORA).precio())
                .isEqualByComparingTo("35.01");
    }

    @Test
    @DisplayName("MN-01: un precio que se redondea a 0.00 es invalido")
    void precio_redondeadoACero() {
        falla(() -> Platillo.nuevo(UUID.randomUUID(), NEGOCIO, "Agua", null, new BigDecimal("0.004"), null, AHORA), CodigoError.PRECIO_INVALIDO);
    }

    @Test
    @DisplayName("la descripcion es opcional y tiene hasta 300 caracteres")
    void descripcion() {
        falla(() -> Platillo.nuevo(UUID.randomUUID(), NEGOCIO, "Agua", "x".repeat(301), new BigDecimal("10"), null, AHORA),
                CodigoError.DESCRIPCION_INVALIDA);
        assertThat(Platillo.nuevo(UUID.randomUUID(), NEGOCIO, "Agua", "   ", new BigDecimal("10"), null, AHORA).descripcion()).isNull();
    }

    @Test
    @DisplayName("MN-08: hasta 8 etiquetas, sin repetir")
    void etiquetas_tope() {
        var p = cemita().conEtiquetas(Set.of(EtiquetaPlatillo.PICANTE, EtiquetaPlatillo.TIPICO_POBLANO), AHORA);

        assertThat(p.etiquetas()).containsExactlyInAnyOrder(EtiquetaPlatillo.PICANTE, EtiquetaPlatillo.TIPICO_POBLANO);
        assertThat(p.version()).isEqualTo(2);
        falla(() -> cemita().conEtiquetas(Set.of(EtiquetaPlatillo.values()), AHORA), CodigoError.LIMITE_EXCEDIDO);
    }

    @Test
    @DisplayName("MN-05: agotado impide pedirlo, no venderlo; marcar y desmarcar sube la version")
    void agotado() {
        var agotado = cemita().conAgotado(true, AHORA);

        assertThat(agotado.agotado()).isTrue();
        assertThat(agotado.version()).isEqualTo(2);
        falla(() -> agotado.exigirOrdenable(LocalDate.of(2026, 10, 16)), CodigoError.PLATILLO_AGOTADO);
        assertThat(agotado.conAgotado(false, AHORA).ordenable(LocalDate.of(2026, 10, 16))).isTrue();
    }

    @Test
    @DisplayName("MN-06: fuera de temporada no se puede ordenar; sin temporada siempre esta en temporada")
    void temporada() {
        var nogada = cemita().conTemporada(new Temporada(MonthDay.of(8, 1), MonthDay.of(9, 30)), AHORA);

        assertThat(nogada.ordenable(LocalDate.of(2026, 9, 15))).isTrue();
        assertThat(nogada.ordenable(LocalDate.of(2026, 10, 1))).isFalse();
        falla(() -> nogada.exigirOrdenable(LocalDate.of(2026, 10, 1)), CodigoError.FUERA_DE_TEMPORADA);
        assertThat(cemita().ordenable(LocalDate.of(2026, 10, 1))).isTrue();
        assertThat(nogada.conTemporada(null, AHORA).ordenable(LocalDate.of(2026, 10, 1))).isTrue();
    }

    @Test
    @DisplayName("un platillo dado de baja no se puede ordenar y la baja es logica: el platillo sigue existiendo")
    void baja() {
        var baja = cemita().conBaja(AHORA);

        assertThat(baja.activo()).isFalse();
        falla(() -> baja.exigirOrdenable(LocalDate.of(2026, 10, 16)), CodigoError.PLATILLO_NO_ORDENABLE);
    }

    @Test
    @DisplayName("MN-09: publicar solo cambia el menu publico y se puede quitar")
    void publicar() {
        var publicado = cemita().conPublicado(true, AHORA);

        assertThat(publicado.publicado()).isTrue();
        assertThat(publicado.ordenable(LocalDate.of(2026, 10, 16))).isTrue();
        assertThat(cemita().ordenable(LocalDate.of(2026, 10, 16))).as("sin publicar tambien se vende en el salon").isTrue();
    }

    @Test
    @DisplayName("el modo de inventario se cambia con su propia operacion y sube la version")
    void modo() {
        assertThat(cemita().conModo(ModoInventario.RECETA, AHORA).modo()).isEqualTo(ModoInventario.RECETA);
        assertThat(cemita().conModo(ModoInventario.RECETA, AHORA).version()).isEqualTo(2);
    }

    @Test
    @DisplayName("editar nombre, descripcion y precio vuelve a validar y sube la version una sola vez")
    void editar() {
        var editado = cemita().conDatos("Cemita especial", "Con aguacate", new BigDecimal("95"), AHORA);

        assertThat(editado.nombre()).isEqualTo("Cemita especial");
        assertThat(editado.precio()).isEqualByComparingTo("95.00");
        assertThat(editado.version()).isEqualTo(2);
        falla(() -> cemita().conDatos("X", null, new BigDecimal("95"), AHORA), CodigoError.NOMBRE_INVALIDO);
    }
}
