package mx.negocio360.inventario.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 7.3 y R5: contrato v1 de los eventos que publica Inventario. Cifras como cadena decimal, sin nulos. */
class EventosInventarioContratoTest {

    private static final UUID MOVIMIENTO = UUID.randomUUID();
    private static final UUID ARTICULO = UUID.randomUUID();

    private static BigDecimal d(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    @DisplayName("movimiento_registrado v1: claves del contrato y causa_id solo cuando existe")
    void movimientoRegistrado_claves() {
        var causa = UUID.randomUUID();
        var evento = new MovimientoRegistrado(MOVIMIENTO, ARTICULO, "SALIDA", d("-240.0000"), "g", d("0.1800"),
                d("-43.20"), d("0.1800"), d("4760.0000"), "VENTA", causa);

        assertThat(MovimientoRegistrado.TIPO).isEqualTo("inventario.movimiento_registrado");
        assertThat(MovimientoRegistrado.VERSION).isEqualTo(1);
        assertThat(evento.datos()).containsOnlyKeys("movimiento_id", "articulo_id", "tipo", "cantidad_base",
                "unidad_base", "costo_unitario", "costo_total", "cpp_resultante", "saldo_resultante", "causa_tipo",
                "causa_id");
        assertThat(evento.datos()).containsEntry("cantidad_base", "-240.0000")
                .containsEntry("costo_total", "-43.20")
                .containsEntry("causa_id", causa.toString());
        assertThat(evento.datos().values()).allMatch(String.class::isInstance);
    }

    @Test
    @DisplayName("bajo_minimo, repuesto y saldo_negativo v1")
    void alertas_claves() {
        var bajo = new AlertaDeSaldo(AlertaDeSaldo.BAJO_MINIMO, ARTICULO, "Harina", d("900.0000"),
                d("1000.0000"), "g");
        var negativo = new AlertaDeSaldo(AlertaDeSaldo.SALDO_NEGATIVO, ARTICULO, "Harina", d("-1.0000"),
                d("1000.0000"), "g");

        assertThat(bajo.datos()).containsOnlyKeys("articulo_id", "nombre", "saldo_base", "minimo_base",
                "unidad_base");
        assertThat(negativo.datos()).containsOnlyKeys("articulo_id", "nombre", "saldo_base", "unidad_base");
        assertThat(AlertaDeSaldo.REPUESTO).isEqualTo("inventario.repuesto");
        assertThat(AlertaDeSaldo.VERSION).isEqualTo(1);
        assertThatThrownBy(() -> new AlertaDeSaldo("inventario.otra", ARTICULO, "Harina", d("1"), d("1"), "g"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
