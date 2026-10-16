package mx.negocio360.contabilidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import mx.negocio360.contabilidad.dominio.ResultadoEvento.Resultado;
import mx.negocio360.contabilidad.fakes.Escenario;
import mx.negocio360.contabilidad.fakes.Eventos;
import mx.negocio360.contabilidad.fakes.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** U-03: un canje sin venta no genera poliza; queda SIN_EFECTO con motivo SIN_VENTA para la estadistica. */
class CuponSinVentaTest {

    private final Escenario e = new Escenario();

    @BeforeEach
    void activar() {
        e.activar(e.ctxA);
    }

    @Test
    @DisplayName("U-03: canje sin venta_id, por ejemplo una entrada de cortesia: SIN_EFECTO y ninguna poliza")
    void sinVenta_sinPoliza() {
        var cuponId = UUID.randomUUID();

        var resultado = e.api.contabilizarCanje(Eventos.canje(e.negocioA, cuponId, null, "80.00", "NEGOCIO",
                Fixtures.AHORA));

        assertThat(resultado.resultado()).isEqualTo(Resultado.OMITIDO);
        assertThat(resultado.motivo()).isEqualTo("SIN_VENTA");
        assertThat(e.polizas.delNegocio(e.negocioA)).isEmpty();
        var canje = e.canjes.porCupon(e.negocioA, cuponId).orElseThrow();
        assertThat(canje.contabilizado()).isFalse();
        assertThat(canje.motivo()).isEqualTo("SIN_VENTA");
        assertThat(canje.polizaId()).isNull();
        assertThat(canje.montoDescuento()).isEqualByComparingTo("80.00");
        assertThat(e.eventos.todos()).isEmpty();
    }

    @Test
    @DisplayName("U-03: la huella sin efecto tambien cuenta como llave natural del cupon")
    void sinVenta_otraEntregaDelCupon_seOmite() {
        var cuponId = UUID.randomUUID();
        e.api.contabilizarCanje(Eventos.canje(e.negocioA, cuponId, null, "80.00", "NEGOCIO", Fixtures.AHORA));

        var segundo = e.api.contabilizarCanje(Eventos.canje(e.negocioA, cuponId, UUID.randomUUID(), "80.00",
                "NEGOCIO", Fixtures.AHORA));

        assertThat(segundo.motivo()).isEqualTo("DUPLICADO_NATURAL");
        assertThat(e.polizas.delNegocio(e.negocioA)).isEmpty();
    }
}
