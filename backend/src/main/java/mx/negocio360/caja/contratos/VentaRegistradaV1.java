package mx.negocio360.caja.contratos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Lo que Caja lee de {@code venta.registrada} v1: la venta, su turno y sus pagos. Lector tolerante (R5).
 *
 * @param ventaId venta
 * @param turnoId turno donde se cobro
 * @param pagos   al menos uno
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record VentaRegistradaV1(UUID ventaId, UUID turnoId, List<PagoV1> pagos) {

    /** Tipo del evento que se lee con este contrato. */
    public static final String TIPO = "venta.registrada";

    /** Exige venta, turno y pagos: sin ellos el evento esta mal formado. */
    public VentaRegistradaV1 {
        Objects.requireNonNull(ventaId, "ventaId");
        Objects.requireNonNull(turnoId, "turnoId");
        pagos = List.copyOf(Objects.requireNonNull(pagos, "pagos"));
    }
}
