package mx.negocio360.clientes.contratos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Objects;
import java.util.UUID;

/** Lo que Clientes lee de {@code venta.anulada} v1: solo el id de la venta (H-03). */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record VentaAnuladaV1(UUID ventaId) {

    public VentaAnuladaV1 {
        Objects.requireNonNull(ventaId, "ventaId");
    }
}
