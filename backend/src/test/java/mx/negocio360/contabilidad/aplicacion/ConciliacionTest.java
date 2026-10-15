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

/**
 * J-03: se comparan las ventas en efectivo contabilizadas de la sesion con las que reporta Caja; si difieren, el corte
 * queda en DESCUADRE_VENTAS y se publica un aviso. No hay ajuste automatico.
 */
class ConciliacionTest {

    private final Escenario e = new Escenario();
    private final UUID sesion = UUID.randomUUID();

    @BeforeEach
    void activar() {
        e.activar(e.ctxA);
    }

    private void ventaEnEfectivo(String total) {
        e.api.contabilizarVenta(Eventos.venta(e.negocioA, UUID.randomUUID(), total, "EFECTIVO", Fixtures.AHORA,
                sesion));
    }

    @Test
    @DisplayName("J-03: ventas de la sesion iguales a las de Caja y conteo exacto: el corte CUADRA y no hay aviso")
    void ventasIguales_cuadra() {
        ventaEnEfectivo("100.00");
        ventaEnEfectivo("50.00");
        e.api.contabilizarVenta(Eventos.venta(e.negocioA, UUID.randomUUID(), "70.00", "TARJETA", Fixtures.AHORA,
                sesion));

        e.api.contabilizarCorte(Eventos.corte(e.negocioA, UUID.randomUUID(), sesion, "650.00", "650.00", "150.00",
                Fixtures.AHORA));

        var corte = e.api.cortes(e.ctxA).get(0);
        assertThat(corte.conciliacion()).isEqualTo(Conciliacion.CUADRA);
        assertThat(corte.ventasEfectivoContables()).isEqualByComparingTo("150.00");
        assertThat(e.eventos.deTipo("contabilidad.descuadre_detectado")).isEmpty();
    }

    @Test
    @DisplayName("J-03: Caja reporta 1,350.00 y se contabilizaron 1,118.00: DESCUADRE_VENTAS y un aviso")
    void ventasDistintas_descuadre() {
        ventaEnEfectivo("1118.00");
        var corteId = UUID.randomUUID();

        var resultado = e.api.contabilizarCorte(Eventos.corte(e.negocioA, corteId, sesion, "1850.00", "1834.50",
                "1350.00", Fixtures.AHORA));

        var corte = e.api.cortes(e.ctxA).get(0);
        assertThat(corte.conciliacion()).isEqualTo(Conciliacion.DESCUADRE_VENTAS);
        assertThat(corte.descuadrePublicadoEn()).isEqualTo(Fixtures.AHORA);
        var aviso = e.eventos.deTipo("contabilidad.descuadre_detectado");
        assertThat(aviso).singleElement().satisfies(s -> {
            assertThat(s.datos()).containsEntry("corte_contable_id", corte.id().toString())
                    .containsEntry("corte_id", corteId.toString()).containsEntry("tipo", "DESCUADRE_VENTAS")
                    .containsEntry("monto", "232.00");
            assertThat(s.origen()).isEqualTo("contabilidad");
        });
        assertThat(resultado.polizaId()).as("la diferencia de conteo si genera su poliza").isNotNull();
        assertThat(e.polizas.delNegocio(e.negocioA)).as("venta y faltante; ningun ajuste de ventas").hasSize(2);
    }

    @Test
    @DisplayName("J-03: sin ventas_efectivo de Caja no se compara; con diferencia de conteo queda DIFERENCIA_CAJA")
    void sinVentasDeCaja_noSeCompara() {
        ventaEnEfectivo("100.00");

        e.api.contabilizarCorte(Eventos.corte(e.negocioA, UUID.randomUUID(), sesion, "200.00", "190.00", null,
                Fixtures.AHORA));

        assertThat(e.api.cortes(e.ctxA).get(0).conciliacion()).isEqualTo(Conciliacion.DIFERENCIA_CAJA);
    }

    @Test
    @DisplayName("J-03: las ventas de otra sesion no cuentan para este corte")
    void ventasDeOtraSesion_noCuentan() {
        e.api.contabilizarVenta(Eventos.venta(e.negocioA, UUID.randomUUID(), "999.00", "EFECTIVO", Fixtures.AHORA,
                UUID.randomUUID()));
        ventaEnEfectivo("40.00");

        e.api.contabilizarCorte(Eventos.corte(e.negocioA, UUID.randomUUID(), sesion, "40.00", "40.00", "40.00",
                Fixtures.AHORA));

        assertThat(e.api.cortes(e.ctxA).get(0).conciliacion()).isEqualTo(Conciliacion.CUADRA);
    }
}
