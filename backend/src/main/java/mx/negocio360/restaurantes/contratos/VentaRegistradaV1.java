package mx.negocio360.restaurantes.contratos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Lo que Restaurantes lee de {@code venta.registrada} v1: la venta y, de cada linea, el producto y la cantidad. Lector
 * tolerante: ignora lo que no usa y lo que Ventas agregue (R5, plan de integracion 10.4). No importa tipos de Ventas.
 *
 * @param ventaId id de la venta, la causa del consumo
 * @param lineas  lineas vendidas
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record VentaRegistradaV1(UUID ventaId, List<LineaV1> lineas) {

    /** Tipo del evento que se lee con este contrato. */
    public static final String TIPO = "venta.registrada";

    public VentaRegistradaV1 {
        Objects.requireNonNull(ventaId, "ventaId");
        lineas = List.copyOf(Objects.requireNonNull(lineas, "lineas"));
    }

    /**
     * Una linea vendida.
     *
     * @param productoId producto de Ventas (para un platillo, su id)
     * @param cantidad   mayor que 0, con a lo mas 4 decimales
     */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LineaV1(UUID productoId, BigDecimal cantidad) {

        private static final BigDecimal TOPE = new BigDecimal("1000000");

        public LineaV1 {
            Objects.requireNonNull(productoId, "productoId");
            Objects.requireNonNull(cantidad, "cantidad");
            if (cantidad.signum() <= 0 || cantidad.scale() > 4 || cantidad.compareTo(TOPE) >= 0) {
                throw new IllegalArgumentException("Cantidad fuera de contrato");
            }
        }
    }
}
