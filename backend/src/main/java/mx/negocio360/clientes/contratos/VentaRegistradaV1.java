package mx.negocio360.clientes.contratos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Lo que Clientes lee de {@code venta.registrada} v1. Lector tolerante: ignora lo que no usa y lo que Ventas agregue
 * (R5). {@code cliente_id} es opcional (venta de mostrador, H-05) y {@code total} es neto de cupon (D-65).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record VentaRegistradaV1(UUID ventaId, UUID clienteId, BigDecimal total) {

    public VentaRegistradaV1 {
        Objects.requireNonNull(ventaId, "ventaId");
        Objects.requireNonNull(total, "total");
        if (total.signum() < 0) {
            throw new IllegalArgumentException("El total de una venta no puede ser negativo: " + total);
        }
    }

    public Optional<UUID> cliente() {
        return Optional.ofNullable(clienteId);
    }
}
