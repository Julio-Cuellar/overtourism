package mx.negocio360.contabilidad.dominio;

import static mx.negocio360.contabilidad.fakes.Afirmaciones.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** A-01: toda poliza cuadra (cargos = abonos) y tiene al menos dos renglones. */
class AsientoTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 10, 15);
    private static final UUID CAJA = UUID.randomUUID();
    private static final UUID VENTAS = UUID.randomUUID();

    private static Renglon cargo(UUID cuenta, String monto) {
        return new Renglon(cuenta, new BigDecimal(monto), BigDecimal.ZERO, null);
    }

    private static Renglon abono(UUID cuenta, String monto) {
        return new Renglon(cuenta, BigDecimal.ZERO, new BigDecimal(monto), null);
    }

    @Test
    @DisplayName("A-01: una poliza con cargos iguales a abonos se arma y su total es la suma de cargos")
    void asiento_cuadrado_seArmaConSuTotal() {
        var asiento = new Asiento(TipoPoliza.INGRESO, FECHA, "Venta de mostrador",
                List.of(cargo(CAJA, "232.00"), abono(VENTAS, "200.00"), abono(VENTAS, "32.00")));

        assertThat(asiento.total()).isEqualByComparingTo("232.00");
        assertThat(asiento.renglones()).hasSize(3);
    }

    @Test
    @DisplayName("A-01: 100.00 de cargo y 99.99 de abono falla con POLIZA_DESCUADRADA")
    void asiento_descuadrado_fallaConPolizaDescuadrada() {
        fallaCon(() -> new Asiento(TipoPoliza.DIARIO, FECHA, "Ajuste",
                List.of(cargo(CAJA, "100.00"), abono(VENTAS, "99.99"))), CodigoError.POLIZA_DESCUADRADA);
    }

    @Test
    @DisplayName("A-01: un solo renglon falla con POLIZA_SIN_RENGLONES")
    void asiento_conUnRenglon_fallaSinRenglones() {
        fallaCon(() -> new Asiento(TipoPoliza.DIARIO, FECHA, "Ajuste", List.of(cargo(CAJA, "10.00"))),
                CodigoError.POLIZA_SIN_RENGLONES);
    }

    @Test
    @DisplayName("A-01: sin renglones falla con POLIZA_SIN_RENGLONES")
    void asiento_sinRenglones_fallaSinRenglones() {
        fallaCon(() -> new Asiento(TipoPoliza.DIARIO, FECHA, "Ajuste", List.of()), CodigoError.POLIZA_SIN_RENGLONES);
        fallaCon(() -> new Asiento(TipoPoliza.DIARIO, FECHA, "Ajuste", null), CodigoError.POLIZA_SIN_RENGLONES);
    }

    @Test
    @DisplayName("A-01: los renglones del asiento no se pueden cambiar despues de armarlo")
    void asiento_renglones_sonInmutables() {
        var asiento = new Asiento(TipoPoliza.INGRESO, FECHA, "Venta",
                List.of(cargo(CAJA, "10.00"), abono(VENTAS, "10.00")));

        assertThat(asiento.renglones()).isUnmodifiable();
    }
}
