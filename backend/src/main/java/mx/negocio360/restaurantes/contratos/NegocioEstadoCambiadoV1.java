package mx.negocio360.restaurantes.contratos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Objects;
import java.util.Set;

/**
 * Lo que Restaurantes lee de {@code negocio.estado_cambiado} v1: el estado nuevo. Lector tolerante (R5).
 *
 * @param estadoNuevo {@code ACTIVO}, {@code SUSPENDIDO} o {@code CERRADO}
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record NegocioEstadoCambiadoV1(String estadoNuevo) {

    /** Tipo del evento que se lee con este contrato. */
    public static final String TIPO = "negocio.estado_cambiado";
    private static final Set<String> ESTADOS = Set.of("ACTIVO", "SUSPENDIDO", "CERRADO");

    public NegocioEstadoCambiadoV1 {
        Objects.requireNonNull(estadoNuevo, "estadoNuevo");
        if (!ESTADOS.contains(estadoNuevo)) {
            throw new IllegalArgumentException("Estado desconocido");
        }
    }
}
