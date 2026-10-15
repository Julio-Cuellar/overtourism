package mx.negocio360.contabilidad.aplicacion;

import static mx.negocio360.contabilidad.fakes.Afirmaciones.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import mx.negocio360.contabilidad.dominio.CodigoError;
import mx.negocio360.contabilidad.dominio.Cuenta;
import mx.negocio360.contabilidad.dominio.EstadoCuenta;
import mx.negocio360.contabilidad.dominio.EstadoPendiente;
import mx.negocio360.contabilidad.dominio.OrigenPoliza;
import mx.negocio360.contabilidad.dominio.Pendiente;
import mx.negocio360.contabilidad.dominio.ResultadoEvento.Resultado;
import mx.negocio360.contabilidad.fakes.Escenario;
import mx.negocio360.contabilidad.fakes.Eventos;
import mx.negocio360.contabilidad.fakes.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** E-04: ningun evento se pierde ni bloquea la cola; lo que no se puede contabilizar solo queda pendiente. */
class PendientesTest {

    private final Escenario e = new Escenario();

    @BeforeEach
    void activar() {
        e.activar(e.ctxA);
    }

    private Pendiente unicoPendiente() {
        var abiertos = e.api.pendientes(e.ctxA, EstadoPendiente.ABIERTO);
        assertThat(abiertos).hasSize(1);
        return abiertos.get(0);
    }

    @Test
    @DisplayName("E-04: una venta con medio VALES sin regla queda pendiente y la siguiente venta se contabiliza")
    void medioSinRegla_quedaPendiente_yLaColaSigue() {
        var vales = e.api.contabilizarVenta(Eventos.venta(e.negocioA, "80.00", "VALES", Fixtures.AHORA));
        var efectivo = e.api.contabilizarVenta(Eventos.venta(e.negocioA, "20.00", "EFECTIVO", Fixtures.AHORA));

        assertThat(vales.resultado()).isEqualTo(Resultado.PENDIENTE);
        assertThat(vales.motivo()).isEqualTo("REGLA_FALTANTE");
        assertThat(efectivo.resultado()).isEqualTo(Resultado.APLICADO);
        assertThat(unicoPendiente().motivo()).isEqualTo("REGLA_FALTANTE");
        assertThat(e.polizas.delNegocio(e.negocioA)).hasSize(1);
    }

    @Test
    @DisplayName("E-04: una venta sin medio_pago (contrato actual de Ventas) queda pendiente con REGLA_FALTANTE")
    void sinMedioPago_quedaPendiente() {
        var resultado = e.api.contabilizarVenta(Eventos.venta(e.negocioA, "80.00", null, Fixtures.AHORA));

        assertThat(resultado.motivo()).isEqualTo("REGLA_FALTANTE");
    }

    @Test
    @DisplayName("E-04: un total que no es decimal plano, negativo o con mas de 2 decimales queda pendiente")
    void datosInvalidos_quedanPendientes() {
        for (var total : List.of("abc", "-5.00", "10.001")) {
            var datos = new LinkedHashMap<String, Object>();
            datos.put("venta_id", UUID.randomUUID().toString());
            datos.put("total", total);
            datos.put("medio_pago", "EFECTIVO");
            var resultado = e.api.contabilizarVenta(Eventos.sobre("venta.registrada", 1, e.negocioA, "ventas",
                    Fixtures.AHORA, UUID.randomUUID(), datos));

            assertThat(resultado.motivo()).as("total %s", total).isEqualTo("DATOS_INVALIDOS");
        }
        assertThat(e.api.pendientes(e.ctxA, EstadoPendiente.ABIERTO)).hasSize(3);
    }

    @Test
    @DisplayName("E-04: si la cuenta de la regla esta inactiva, la venta queda pendiente con CUENTA_INACTIVA")
    void cuentaInactiva_quedaPendiente() {
        var caja = e.cuenta(e.ctxA, "101.01");
        e.cuentas.guardar(new Cuenta(caja.id(), caja.negocioId(), caja.codigo(), caja.nombre(), caja.tipo(),
                caja.naturaleza(), caja.padreId(), true, true, null, EstadoCuenta.INACTIVA, caja.version() + 1));

        var resultado = e.api.contabilizarVenta(Eventos.venta(e.negocioA, "10.00", "EFECTIVO", Fixtures.AHORA));

        assertThat(resultado.motivo()).isEqualTo("CUENTA_INACTIVA");
    }

    @Test
    @DisplayName("E-04: el pendiente guarda solo los campos del contrato que usa Contabilidad")
    void pendiente_guardaSoloCamposDelContrato() {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("venta_id", UUID.randomUUID().toString());
        datos.put("total", "80.00");
        datos.put("medio_pago", "VALES");
        datos.put("cliente_id", UUID.randomUUID().toString());
        datos.put("lineas", List.of());
        e.api.contabilizarVenta(Eventos.sobre("venta.registrada", 1, e.negocioA, "ventas", Fixtures.AHORA,
                UUID.randomUUID(), datos));

        assertThat(unicoPendiente().datos()).containsOnlyKeys("venta_id", "total", "medio_pago");
    }

    @Test
    @DisplayName("E-04: la reentrega de un evento pendiente no crea otro pendiente")
    void reentrega_noDuplicaPendiente() {
        var evento = Eventos.venta(e.negocioA, "80.00", "VALES", Fixtures.AHORA);
        e.api.contabilizarVenta(evento);

        assertThat(e.api.contabilizarVenta(evento).motivo()).isEqualTo("YA_PROCESADO");
        assertThat(e.api.pendientes(e.ctxA, EstadoPendiente.ABIERTO)).hasSize(1);
    }

    @Test
    @DisplayName("E-04: resolver un pendiente con cuentas elegidas registra una poliza manual que apunta a el")
    void resolver_contabilizar() {
        e.api.contabilizarVenta(Eventos.venta(e.negocioA, "80.00", "VALES", Fixtures.AHORA));
        var pendiente = unicoPendiente();

        var polizaId = e.confirmar(e.ctxA, ResolverPendienteComando.contabilizar(pendiente.id(),
                List.of(new RenglonManual(e.cuenta(e.ctxA, "103.01").id(), Fixtures.m("80.00"), null, null),
                        new RenglonManual(e.cuenta(e.ctxA, "401.01").id(), null, Fixtures.m("80.00"), null))))
                .recursoId();

        var poliza = e.api.poliza(e.ctxA, polizaId).orElseThrow();
        assertThat(poliza.origen()).isEqualTo(OrigenPoliza.MANUAL);
        assertThat(poliza.referenciaTipo()).isEqualTo("PENDIENTE");
        assertThat(poliza.referenciaId()).isEqualTo(pendiente.id());
        assertThat(poliza.fecha()).isEqualTo(Fixtures.HOY);
        var resuelto = e.api.pendientes(e.ctxA, EstadoPendiente.CONTABILIZADO).get(0);
        assertThat(resuelto.resueltoConPolizaId()).isEqualTo(polizaId);
        assertThat(e.api.pendientes(e.ctxA, EstadoPendiente.ABIERTO)).isEmpty();
    }

    @Test
    @DisplayName("E-04: descartar exige motivo; un pendiente resuelto no se resuelve otra vez")
    void resolver_descartar() {
        e.api.contabilizarVenta(Eventos.venta(e.negocioA, "80.00", "VALES", Fixtures.AHORA));
        var pendiente = unicoPendiente();

        fallaCon(() -> e.api.preparar(e.ctxA, ResolverPendienteComando.descartar(pendiente.id(), " ")),
                CodigoError.MOTIVO_REQUERIDO);
        e.confirmar(e.ctxA, ResolverPendienteComando.descartar(pendiente.id(), "Venta de prueba en caja"));

        var descartado = e.api.pendientes(e.ctxA, EstadoPendiente.DESCARTADO).get(0);
        assertThat(descartado.motivoResolucion()).isEqualTo("Venta de prueba en caja");
        fallaCon(() -> e.api.preparar(e.ctxA, ResolverPendienteComando.descartar(pendiente.id(), "otra vez")),
                CodigoError.PENDIENTE_YA_RESUELTO);
    }

    @Test
    @DisplayName("E-04: un pendiente de otro negocio o inexistente falla con PENDIENTE_NO_ENCONTRADO")
    void resolver_pendienteAjeno_falla() {
        e.activar(e.ctxB);
        e.api.contabilizarVenta(Eventos.venta(e.negocioA, "80.00", "VALES", Fixtures.AHORA));
        var pendiente = unicoPendiente();

        fallaCon(() -> e.api.preparar(e.ctxB, ResolverPendienteComando.descartar(pendiente.id(), "x")),
                CodigoError.PENDIENTE_NO_ENCONTRADO);
        assertThat(e.api.pendientes(e.ctxB, EstadoPendiente.ABIERTO)).isEmpty();
    }
}
