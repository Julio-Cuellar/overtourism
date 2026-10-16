package mx.negocio360.restaurantes.contratos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Lo que Restaurantes lee de {@code venta.anulada} v1: la venta y los productos de sus lineas (D-25: sin las lineas habria
 * que mandar la reversa por cada venta del sistema). Lector tolerante (R5).
 *
 * @param ventaId id de la venta anulada
 * @param lineas  lineas de la venta
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record VentaAnuladaV1(UUID ventaId, List<LineaAnuladaV1> lineas) {

    /** Tipo del evento que se lee con este contrato. */
    public static final String TIPO = "venta.anulada";

    public VentaAnuladaV1 {
        Objects.requireNonNull(ventaId, "ventaId");
        lineas = List.copyOf(Objects.requireNonNull(lineas, "lineas"));
    }

    /** Una linea de la venta anulada: solo importa su producto. */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LineaAnuladaV1(UUID productoId) {

        public LineaAnuladaV1 {
            Objects.requireNonNull(productoId, "productoId");
        }
    }
}
