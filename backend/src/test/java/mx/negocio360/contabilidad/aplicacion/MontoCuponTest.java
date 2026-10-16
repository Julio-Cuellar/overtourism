package mx.negocio360.contabilidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import mx.negocio360.contabilidad.dominio.EstadoPendiente;
import mx.negocio360.contabilidad.dominio.ResultadoEvento.Resultado;
import mx.negocio360.contabilidad.fakes.Escenario;
import mx.negocio360.contabilidad.fakes.Eventos;
import mx.negocio360.contabilidad.fakes.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

/** U-04: el descuento es positivo y en centavos; cero no tiene efecto; negativo o ausente crea un pendiente. */
class MontoCuponTest {

    private final Escenario e = new Escenario();

    @BeforeEach
    void activar() {
        e.activar(e.ctxA);
    }

    @Test
    @DisplayName("U-04: un descuento de 0.00 queda SIN_EFECTO con MONTO_CERO y sin poliza")
    void montoCero_sinEfecto() {
        var cuponId = UUID.randomUUID();

        var resultado = e.api.contabilizarCanje(Eventos.canje(e.negocioA, cuponId, UUID.randomUUID(), "0.00",
                "NEGOCIO", Fixtures.AHORA));

        assertThat(resultado.resultado()).isEqualTo(Resultado.OMITIDO);
        assertThat(resultado.motivo()).isEqualTo("MONTO_CERO");
        assertThat(e.canjes.porCupon(e.negocioA, cuponId).orElseThrow().motivo()).isEqualTo("MONTO_CERO");
        assertThat(e.polizas.delNegocio(e.negocioA)).isEmpty();
        assertThat(e.api.pendientes(e.ctxA, EstadoPendiente.ABIERTO)).isEmpty();
    }

    @ParameterizedTest(name = "monto {0}, con venta {1}")
    @CsvSource(value = {"-5.00, true", "-5.00, false", "NULL, true", "NULL, false", "10.001, true"},
            nullValues = "NULL")
    @DisplayName("U-04: un descuento negativo, ausente o con mas de 2 decimales queda pendiente con DATOS_INVALIDOS")
    void montoInvalido_quedaPendiente(String monto, boolean conVenta) {
        var cuponId = UUID.randomUUID();

        var resultado = e.api.contabilizarCanje(Eventos.canje(e.negocioA, cuponId,
                conVenta ? UUID.randomUUID() : null, monto, "NEGOCIO", Fixtures.AHORA));

        assertThat(resultado.resultado()).isEqualTo(Resultado.PENDIENTE);
        assertThat(resultado.motivo()).isEqualTo("DATOS_INVALIDOS");
        assertThat(e.canjes.porCupon(e.negocioA, cuponId)).isEmpty();
        assertThat(e.polizas.delNegocio(e.negocioA)).isEmpty();
    }
}
