package mx.negocio360.contabilidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;
import mx.negocio360.contabilidad.dominio.OrigenPoliza;
import mx.negocio360.contabilidad.dominio.Poliza;
import mx.negocio360.contabilidad.dominio.ResultadoEvento.Resultado;
import mx.negocio360.contabilidad.dominio.TipoPoliza;
import mx.negocio360.contabilidad.fakes.Escenario;
import mx.negocio360.contabilidad.fakes.Eventos;
import mx.negocio360.contabilidad.fakes.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * U-01: canje con venta, pagado por el negocio: cargo 402.01 y abono 401.01 por la base sin IVA. Las ventas quedan en
 * bruto y el neto no cambia. La tasa de 16 % es dato de ejemplo.
 */
class CuponDescuentoTest {

    private final Escenario e = new Escenario();

    @BeforeEach
    void activar() {
        e.activar(e.ctxA);
    }

    private BigDecimal movido(Poliza poliza, String codigo, boolean cargo) {
        var cuenta = e.cuenta(e.ctxA, codigo).id();
        return poliza.renglones().stream().filter(r -> r.cuentaId().equals(cuenta))
                .map(r -> cargo ? r.cargo() : r.abono()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    @DisplayName("U-01: cupon de 29.00 pagado por el negocio: cargo 402.01 y abono 401.01 por 25.00; el IVA no cambia")
    void cupon_reclasificaLaBaseSinIva() {
        var cuponId = UUID.randomUUID();
        var ventaId = UUID.randomUUID();

        var resultado = e.api.contabilizarCanje(Eventos.canje(e.negocioA, cuponId, ventaId, "29.00", "NEGOCIO",
                Fixtures.AHORA));

        assertThat(resultado.resultado()).isEqualTo(Resultado.APLICADO);
        var poliza = e.api.poliza(e.ctxA, resultado.polizaId()).orElseThrow();
        assertThat(poliza.tipo()).isEqualTo(TipoPoliza.DIARIO);
        assertThat(poliza.origen()).isEqualTo(OrigenPoliza.AUTOMATICA);
        assertThat(poliza.referenciaTipo()).isEqualTo("CANJE");
        assertThat(poliza.referenciaId()).isEqualTo(cuponId);
        assertThat(movido(poliza, "402.01", true)).isEqualByComparingTo("25.00");
        assertThat(movido(poliza, "401.01", false)).isEqualByComparingTo("25.00");
        assertThat(movido(poliza, "208.01", false)).isEqualByComparingTo("0");
        var canje = e.canjes.porCupon(e.negocioA, cuponId).orElseThrow();
        assertThat(canje.ventaId()).isEqualTo(ventaId);
        assertThat(canje.montoDescuento()).isEqualByComparingTo("29.00");
        assertThat(canje.baseDescuento()).isEqualByComparingTo("25.00");
        assertThat(canje.polizaId()).isEqualTo(poliza.id());
    }

    @Test
    @DisplayName("U-01: con la venta de 232.00 neta, ventas brutas 225.00, descuentos 25.00 y neto 200.00 como antes")
    void cuponYVenta_netoSinCambio() {
        var ventaId = UUID.randomUUID();
        e.api.contabilizarVenta(Eventos.venta(e.negocioA, ventaId, "232.00", "EFECTIVO", Fixtures.AHORA, null));

        e.api.contabilizarCanje(Eventos.canje(e.negocioA, UUID.randomUUID(), ventaId, "29.00", "NEGOCIO",
                Fixtures.AHORA));

        var balanza = e.api.balanza(e.ctxA, YearMonth.from(Fixtures.HOY));
        assertThat(balanza.fila("401.01").saldoFinal()).isEqualByComparingTo("225.00");
        assertThat(balanza.fila("402.01").saldoFinal()).isEqualByComparingTo("25.00");
        assertThat(balanza.fila("208.01").saldoFinal()).isEqualByComparingTo("32.00");
        assertThat(balanza.fila("401.01").saldoFinal().subtract(balanza.fila("402.01").saldoFinal()))
                .isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("U-01 y K-3: sin financiador se asume que paga el negocio")
    void sinFinanciador_pagaElNegocio() {
        var resultado = e.api.contabilizarCanje(Eventos.canje(e.negocioA, UUID.randomUUID(), UUID.randomUUID(),
                "29.00", null, Fixtures.AHORA));

        assertThat(resultado.resultado()).isEqualTo(Resultado.APLICADO);
    }

    @Test
    @DisplayName("U-01 y U-02 (P2): un cupon de la plataforma, sin regla configurada, queda pendiente")
    void cuponDeLaPlataforma_quedaPendiente() {
        var resultado = e.api.contabilizarCanje(Eventos.canje(e.negocioA, UUID.randomUUID(), UUID.randomUUID(),
                "29.00", "PLATAFORMA", Fixtures.AHORA));

        assertThat(resultado.motivo()).isEqualTo("REGLA_FALTANTE");
    }

    @Test
    @DisplayName("U-01 y E-02: otro evento del mismo cupon queda OMITIDO con DUPLICADO_NATURAL")
    void mismoCupon_unaVez() {
        var cuponId = UUID.randomUUID();
        e.api.contabilizarCanje(Eventos.canje(e.negocioA, cuponId, UUID.randomUUID(), "29.00", "NEGOCIO",
                Fixtures.AHORA));

        var segundo = e.api.contabilizarCanje(Eventos.canje(e.negocioA, cuponId, UUID.randomUUID(), "29.00",
                "NEGOCIO", Fixtures.AHORA));

        assertThat(segundo.motivo()).isEqualTo("DUPLICADO_NATURAL");
        assertThat(e.polizas.delNegocio(e.negocioA)).hasSize(1);
    }
}
