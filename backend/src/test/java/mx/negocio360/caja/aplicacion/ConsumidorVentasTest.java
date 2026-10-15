package mx.negocio360.caja.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.caja.dominio.MetodoPago;
import mx.negocio360.caja.dominio.TipoMovimiento;
import mx.negocio360.caja.fakes.Escenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CF-20 y CF-21: Caja escucha venta.registrada y venta.anulada (CJ-03, CJ-04, R3). */
class ConsumidorVentasTest {

    private final Escenario e = new Escenario();
    private final UUID turno = e.abrirTurno(e.ctxA, "500.00");
    private final UUID venta = UUID.randomUUID();

    private BigDecimal esperado(MetodoPago metodo) {
        return e.api.saldoTurno(e.ctxA, turno).esperadoPorMetodo().get(metodo);
    }

    @Test
    @DisplayName("CJ-03: una venta con pago mixto crea un movimiento VENTA por pago")
    void registrada_pagoMixto_unMovimientoPorPago() {
        var resultado = e.consumidor.procesar(e.ventaRegistrada(e.negocioA, turno, venta,
                Map.of("EFECTIVO", "35.00", "TARJETA", "100.00")));

        assertThat(resultado).isEqualTo(ResultadoAplicacion.APLICADA);
        assertThat(esperado(MetodoPago.EFECTIVO)).isEqualTo(new BigDecimal("535.00"));
        assertThat(esperado(MetodoPago.TARJETA)).isEqualTo(new BigDecimal("100.00"));
        assertThat(e.api.saldoTurno(e.ctxA, turno).ventasAplicadas()).isEqualTo(1);
        assertThat(e.libro.todos()).filteredOn(m -> m.tipo() == TipoMovimiento.VENTA)
                .allSatisfy(m -> assertThat(m.ventaId()).isEqualTo(venta)).hasSize(2);
    }

    @Test
    @DisplayName("CJ-04: el mismo evento dos veces deja un solo efecto")
    void registrada_mismoEventoDosVeces_unEfecto() {
        var evento = e.ventaRegistrada(e.negocioA, turno, venta, Map.of("EFECTIVO", "150.00"));

        e.consumidor.procesar(evento);
        var segunda = e.consumidor.procesar(evento);

        assertThat(segunda).isEqualTo(ResultadoAplicacion.YA_PROCESADO);
        assertThat(esperado(MetodoPago.EFECTIVO)).isEqualTo(new BigDecimal("650.00"));
    }

    @Test
    @DisplayName("CJ-04: la misma venta reenviada con otro id de evento tampoco se suma dos veces")
    void registrada_mismaVentaOtroEvento_yaAplicada() {
        e.consumidor.procesar(e.ventaRegistrada(e.negocioA, turno, venta, Map.of("EFECTIVO", "150.00")));

        var segunda = e.consumidor.procesar(e.ventaRegistrada(e.negocioA, turno, venta, Map.of("EFECTIVO", "150.00")));

        assertThat(segunda).isEqualTo(ResultadoAplicacion.YA_APLICADA);
        assertThat(esperado(MetodoPago.EFECTIVO)).isEqualTo(new BigDecimal("650.00"));
    }

    @Test
    @DisplayName("CJ-03: la anulacion compensa con movimientos ANULACION negativos")
    void anulada_compensa() {
        e.consumidor.procesar(e.ventaRegistrada(e.negocioA, turno, venta, Map.of("EFECTIVO", "150.00")));

        var resultado = e.consumidor.procesar(e.ventaAnulada(e.negocioA, turno, venta, Map.of("EFECTIVO", "150.00")));

        assertThat(resultado).isEqualTo(ResultadoAplicacion.REVERTIDA);
        assertThat(esperado(MetodoPago.EFECTIVO)).isEqualTo(new BigDecimal("500.00"));
        assertThat(e.libro.todos()).filteredOn(m -> m.tipo() == TipoMovimiento.ANULACION)
                .singleElement().satisfies(m -> assertThat(m.monto()).isEqualTo(new BigDecimal("-150.00")));
        assertThat(e.consumidor.procesar(e.ventaAnulada(e.negocioA, turno, venta, Map.of("EFECTIVO", "150.00"))))
                .isEqualTo(ResultadoAplicacion.YA_REVERTIDA);
    }

    @Test
    @DisplayName("CJ-04: si la anulacion llega antes, se marca ANULADO_PREVIO y la venta tardia no suma")
    void anulada_antesDeLaVenta_noSuma() {
        var primero = e.consumidor.procesar(e.ventaAnulada(e.negocioA, turno, venta, Map.of("EFECTIVO", "150.00")));
        var despues = e.consumidor.procesar(e.ventaRegistrada(e.negocioA, turno, venta,
                Map.of("EFECTIVO", "150.00")));

        assertThat(primero).isEqualTo(ResultadoAplicacion.MARCADA_ANULADO_PREVIO);
        assertThat(despues).isEqualTo(ResultadoAplicacion.OMITIDA_ANULADO_PREVIO);
        assertThat(esperado(MetodoPago.EFECTIVO)).isEqualTo(new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("una venta de un turno de otro negocio no toca este turno")
    void registrada_turnoDeOtroNegocio_sinTurno() {
        var resultado = e.consumidor.procesar(e.ventaRegistrada(e.negocioB, turno, venta,
                Map.of("EFECTIVO", "150.00")));

        assertThat(resultado).isEqualTo(ResultadoAplicacion.SIN_TURNO_ABIERTO);
        assertThat(esperado(MetodoPago.EFECTIVO)).isEqualTo(new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("un evento de otro tipo se ignora")
    void otroTipo_seIgnora() {
        var otro = new mx.negocio360.compartido.eventos.SobreEvento(
                mx.negocio360.compartido.identidad.UuidV7.generar(e.reloj.instant()), "cliente.registrado", 1,
                e.negocioA, "clientes", e.reloj.instant(), venta, null, Map.of("cliente_id", venta.toString()));

        assertThat(e.consumidor.procesar(otro)).isEqualTo(ResultadoAplicacion.IGNORADO);
    }

    @Test
    @DisplayName("un pago con dinero como numero o un metodo desconocido es un evento mal formado")
    void eventoMalFormado_falla() {
        assertThatThrownBy(() -> e.consumidor.procesar(e.ventaRegistrada(e.negocioA, turno, venta,
                Map.of("CHEQUE", "10.00")))).isInstanceOf(IllegalArgumentException.class);
    }
}
