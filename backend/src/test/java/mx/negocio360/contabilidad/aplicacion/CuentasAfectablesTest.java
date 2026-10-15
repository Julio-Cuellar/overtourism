package mx.negocio360.contabilidad.aplicacion;

import static mx.negocio360.contabilidad.fakes.Afirmaciones.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import mx.negocio360.contabilidad.dominio.CodigoError;
import mx.negocio360.contabilidad.dominio.EstadoCuenta;
import mx.negocio360.contabilidad.dominio.OrigenPoliza;
import mx.negocio360.contabilidad.dominio.TipoPoliza;
import mx.negocio360.contabilidad.fakes.Escenario;
import mx.negocio360.contabilidad.fakes.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** A-04: solo se mueven cuentas afectables y activas. */
class CuentasAfectablesTest {

    private final Escenario e = new Escenario();

    @BeforeEach
    void activar() {
        e.activar(e.ctxA);
    }

    private RegistrarPolizaManualComando poliza(UUID cargo, UUID abono) {
        return new RegistrarPolizaManualComando(TipoPoliza.DIARIO, Fixtures.HOY, "Ajuste de prueba",
                List.of(new RenglonManual(cargo, Fixtures.m("50.00"), null, null),
                        new RenglonManual(abono, null, Fixtures.m("50.00"), null)));
    }

    @Test
    @DisplayName("A-04: una poliza manual entre cuentas afectables y activas se registra con sus renglones")
    void registrar_cuentasValidas_seRegistra() {
        var caja = e.cuenta(e.ctxA, "101.01").id();
        var bancos = e.cuenta(e.ctxA, "102.01").id();

        var id = e.confirmar(e.ctxA, poliza(bancos, caja)).recursoId();

        var registrada = e.api.poliza(e.ctxA, id).orElseThrow();
        assertThat(registrada.origen()).isEqualTo(OrigenPoliza.MANUAL);
        assertThat(registrada.total()).isEqualByComparingTo("50.00");
        assertThat(registrada.renglones()).extracting(r -> r.cuentaId()).containsExactly(bancos, caja);
        assertThat(registrada.creadoPor()).isEqualTo("locatario-a");
    }

    @Test
    @DisplayName("A-04: cargar a 101 (cuenta padre) falla con CUENTA_NO_AFECTABLE")
    void registrar_cuentaPadre_falla() {
        fallaCon(() -> e.api.preparar(e.ctxA, poliza(e.cuenta(e.ctxA, "101").id(), e.cuenta(e.ctxA, "401.01").id())),
                CodigoError.CUENTA_NO_AFECTABLE);
    }

    @Test
    @DisplayName("A-04: mover una cuenta inactiva falla con CUENTA_INACTIVA")
    void registrar_cuentaInactiva_falla() {
        var bancos = e.cuenta(e.ctxA, "102.01");
        e.cuentas.guardar(new mx.negocio360.contabilidad.dominio.Cuenta(bancos.id(), bancos.negocioId(),
                bancos.codigo(), bancos.nombre(), bancos.tipo(), bancos.naturaleza(), bancos.padreId(), true, false,
                null, EstadoCuenta.INACTIVA, 1));

        fallaCon(() -> e.api.preparar(e.ctxA, poliza(bancos.id(), e.cuenta(e.ctxA, "101.01").id())),
                CodigoError.CUENTA_INACTIVA);
    }

    @Test
    @DisplayName("A-04: una cuenta de otro negocio o inexistente falla con CUENTA_NO_ENCONTRADA")
    void registrar_cuentaAjena_falla() {
        e.activar(e.ctxB);
        var cajaDeB = e.cuenta(e.ctxB, "101.01").id();

        fallaCon(() -> e.api.preparar(e.ctxA, poliza(cajaDeB, e.cuenta(e.ctxA, "401.01").id())),
                CodigoError.CUENTA_NO_ENCONTRADA);
        fallaCon(() -> e.api.preparar(e.ctxA, poliza(UUID.randomUUID(), e.cuenta(e.ctxA, "401.01").id())),
                CodigoError.CUENTA_NO_ENCONTRADA);
    }

    @Test
    @DisplayName("A-04: si la cuenta se desactiva entre preparar y confirmar, confirmar falla con CUENTA_INACTIVA")
    void registrar_cuentaDesactivadaAntesDeConfirmar_falla() {
        var bancos = e.cuenta(e.ctxA, "102.01");
        var token = e.api.preparar(e.ctxA, poliza(bancos.id(), e.cuenta(e.ctxA, "101.01").id())).token();
        e.cuentas.guardar(new mx.negocio360.contabilidad.dominio.Cuenta(bancos.id(), bancos.negocioId(),
                bancos.codigo(), bancos.nombre(), bancos.tipo(), bancos.naturaleza(), bancos.padreId(), true, false,
                null, EstadoCuenta.INACTIVA, 1));

        fallaCon(() -> e.api.confirmar(e.ctxA, token), CodigoError.CUENTA_INACTIVA);
    }

    @Test
    @DisplayName("A-04: una poliza de otro negocio no se ve")
    void consultar_polizaAjena_noSeVe() {
        var id = e.confirmar(e.ctxA, poliza(e.cuenta(e.ctxA, "102.01").id(), e.cuenta(e.ctxA, "101.01").id()))
                .recursoId();

        assertThat(e.api.poliza(e.ctxB, id)).isEmpty();
    }
}
