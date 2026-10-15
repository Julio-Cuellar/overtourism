package mx.negocio360.contabilidad.dominio;

import static mx.negocio360.contabilidad.fakes.Afirmaciones.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * A-02: cada renglon tiene un cargo o un abono mayor que cero, nunca los dos ni ninguno. La parte de la base (CHECK en
 * {@code movimiento}) se prueba en {@code ChecksContabilidadPostgresTest}, que necesita Docker.
 */
class RenglonTest {

    private static final UUID CUENTA = UUID.randomUUID();

    private static BigDecimal m(String monto) {
        return monto == null ? null : new BigDecimal(monto);
    }

    @Test
    @DisplayName("A-02: un renglon con solo cargo es valido y queda en centavos")
    void renglon_soloCargo_esValido() {
        var renglon = new Renglon(CUENTA, m("10"), BigDecimal.ZERO, null);

        assertThat(renglon.cargo()).isEqualTo(new BigDecimal("10.00"));
        assertThat(renglon.abono()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    @DisplayName("A-02: un abono nulo cuenta como cero")
    void renglon_abonoNulo_cuentaComoCero() {
        var renglon = new Renglon(CUENTA, null, m("5.50"), "Ventas");

        assertThat(renglon.cargo()).isEqualTo(new BigDecimal("0.00"));
        assertThat(renglon.abono()).isEqualTo(new BigDecimal("5.50"));
    }

    @ParameterizedTest(name = "cargo {0} y abono {1}")
    @CsvSource(value = {"10.00, 10.00", "0, 0", "NULL, NULL", "-5.00, 0", "0, -1.00", "0.00, 0.00"},
            nullValues = "NULL")
    @DisplayName("A-02: cargo y abono a la vez, ninguno o negativo falla con MOVIMIENTO_INVALIDO")
    void renglon_invalido_fallaConMovimientoInvalido(String cargo, String abono) {
        fallaCon(() -> new Renglon(CUENTA, m(cargo), m(abono), null), CodigoError.MOVIMIENTO_INVALIDO);
    }

    @Test
    @DisplayName("A-02: un renglon sin cuenta falla con MOVIMIENTO_INVALIDO")
    void renglon_sinCuenta_fallaConMovimientoInvalido() {
        fallaCon(() -> new Renglon(null, m("1.00"), null, null), CodigoError.MOVIMIENTO_INVALIDO);
    }

    @ParameterizedTest(name = "monto {0}")
    @CsvSource({"10.001", "1000000000000.00"})
    @DisplayName("A-02: un monto con mas de dos decimales o fuera de numeric(14,2) falla con MONTO_INVALIDO")
    void renglon_montoFueraDeRango_fallaConMontoInvalido(String monto) {
        fallaCon(() -> new Renglon(CUENTA, m(monto), null, null), CodigoError.MONTO_INVALIDO);
    }

    @Test
    @DisplayName("A-02: el mayor monto que cabe en numeric(14,2) es valido")
    void renglon_montoMaximo_esValido() {
        var renglon = new Renglon(CUENTA, m("999999999999.99"), null, null);

        assertThat(renglon.cargo()).isEqualByComparingTo("999999999999.99");
    }
}
