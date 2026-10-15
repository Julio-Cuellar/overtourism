package mx.negocio360.contabilidad.aplicacion;

import static mx.negocio360.contabilidad.fakes.Afirmaciones.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import mx.negocio360.contabilidad.dominio.CodigoError;
import mx.negocio360.contabilidad.dominio.EstadoCuenta;
import mx.negocio360.contabilidad.dominio.ReglaContable;
import mx.negocio360.contabilidad.fakes.Escenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** C-04: una cuenta usada por una regla vigente no se desactiva. */
class CuentaEnUsoTest {

    private final Escenario e = new Escenario();

    @BeforeEach
    void activar() {
        e.activar(e.ctxA);
    }

    @ParameterizedTest(name = "cuenta {0}")
    @ValueSource(strings = {"208.01", "101.01", "401.01", "402.01", "601.01", "704.01"})
    @DisplayName("C-04: desactivar una cuenta que usa una regla vigente falla con CUENTA_EN_USO")
    void desactivar_cuentaEnUso_falla(String codigo) {
        var cuenta = e.cuenta(e.ctxA, codigo);

        fallaCon(() -> e.api.preparar(e.ctxA, new DesactivarCuentaComando(cuenta.id(), cuenta.version())),
                CodigoError.CUENTA_EN_USO);
    }

    @Test
    @DisplayName("C-04: una cuenta que ninguna regla usa si se desactiva")
    void desactivar_cuentaSinRegla_seDesactiva() {
        var cuenta = e.cuenta(e.ctxA, "105.01");

        e.confirmar(e.ctxA, new DesactivarCuentaComando(cuenta.id(), cuenta.version()));

        assertThat(e.cuenta(e.ctxA, "105.01").estado()).isEqualTo(EstadoCuenta.INACTIVA);
    }

    @Test
    @DisplayName("C-04: si la unica regla que la usaba ya vencio, la cuenta si se desactiva")
    void desactivar_reglaVencida_seDesactiva() {
        var iva = e.cuenta(e.ctxA, "208.01");
        for (var regla : e.reglas.delNegocio(e.negocioA)) {
            if (regla.usaCuenta(iva.id())) {
                e.reglas.guardar(new ReglaContable(regla.id(), regla.negocioId(), regla.disparador(),
                        regla.condicion(), regla.cuentaCargoId(), regla.cuentaAbonoId(), regla.cuentaImpuestoId(),
                        regla.tasaIva(), regla.fuente(), regla.vigenteDesde(), e.reloj.instant(),
                        regla.version() + 1));
            }
        }
        e.reloj.avanzar(Duration.ofMinutes(1));

        e.confirmar(e.ctxA, new DesactivarCuentaComando(iva.id(), iva.version()));

        assertThat(e.cuenta(e.ctxA, "208.01").estado()).isEqualTo(EstadoCuenta.INACTIVA);
    }
}
