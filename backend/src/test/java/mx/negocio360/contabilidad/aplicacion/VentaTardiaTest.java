package mx.negocio360.contabilidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import mx.negocio360.contabilidad.dominio.Conciliacion;
import mx.negocio360.contabilidad.fakes.Escenario;
import mx.negocio360.contabilidad.fakes.Eventos;
import mx.negocio360.contabilidad.fakes.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** J-04: una venta tardia de una sesion cerrada recalcula la conciliacion; el aviso sale una sola vez por corte. */
class VentaTardiaTest {

    private final Escenario e = new Escenario();
    private final UUID sesion = UUID.randomUUID();

    @BeforeEach
    void cortarConVentaFaltante() {
        e.activar(e.ctxA);
        e.api.contabilizarVenta(Eventos.venta(e.negocioA, UUID.randomUUID(), "100.00", "EFECTIVO", Fixtures.AHORA,
                sesion));
        e.api.contabilizarCorte(Eventos.corte(e.negocioA, UUID.randomUUID(), sesion, "150.00", "150.00", "150.00",
                Fixtures.AHORA));
    }

    private void ventaTardia(String total) {
        e.api.contabilizarVenta(Eventos.venta(e.negocioA, UUID.randomUUID(), total, "EFECTIVO", Fixtures.AHORA,
                sesion));
    }

    @Test
    @DisplayName("J-04: al llegar la venta faltante, el corte pasa de DESCUADRE_VENTAS a CUADRA sin un segundo aviso")
    void ventaFaltante_cuadraSinSegundoAviso() {
        assertThat(e.api.cortes(e.ctxA).get(0).conciliacion()).isEqualTo(Conciliacion.DESCUADRE_VENTAS);

        ventaTardia("50.00");

        var corte = e.api.cortes(e.ctxA).get(0);
        assertThat(corte.conciliacion()).isEqualTo(Conciliacion.CUADRA);
        assertThat(corte.ventasEfectivoContables()).isEqualByComparingTo("150.00");
        assertThat(corte.descuadrePublicadoEn()).as("se conserva cuando se aviso").isEqualTo(Fixtures.AHORA);
        assertThat(e.eventos.deTipo("contabilidad.descuadre_detectado")).hasSize(1);
    }

    @Test
    @DisplayName("J-04: si la venta tardia no basta, sigue en DESCUADRE_VENTAS y tampoco hay segundo aviso")
    void ventaTardiaInsuficiente_unSoloAviso() {
        ventaTardia("20.00");

        var corte = e.api.cortes(e.ctxA).get(0);
        assertThat(corte.conciliacion()).isEqualTo(Conciliacion.DESCUADRE_VENTAS);
        assertThat(corte.ventasEfectivoContables()).isEqualByComparingTo("120.00");
        assertThat(e.eventos.deTipo("contabilidad.descuadre_detectado")).hasSize(1);
    }

    @Test
    @DisplayName("J-04: una venta tardia con tarjeta no cambia la conciliacion del efectivo")
    void ventaTardiaConTarjeta_noCambia() {
        e.api.contabilizarVenta(Eventos.venta(e.negocioA, UUID.randomUUID(), "50.00", "TARJETA", Fixtures.AHORA,
                sesion));

        assertThat(e.api.cortes(e.ctxA).get(0).conciliacion()).isEqualTo(Conciliacion.DESCUADRE_VENTAS);
        assertThat(e.api.cortes(e.ctxA).get(0).ventasEfectivoContables()).isEqualByComparingTo("100.00");
    }
}
