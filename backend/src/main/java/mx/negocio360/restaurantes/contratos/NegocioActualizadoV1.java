package mx.negocio360.restaurantes.contratos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Objects;

/**
 * Lo que Restaurantes lee de {@code negocio.actualizado} v1: el giro y si el dueño lo publico. Lector tolerante (R5).
 *
 * @param giro      giro del negocio
 * @param publicado si aparece en la app del turista
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record NegocioActualizadoV1(String giro, Boolean publicado) {

    /** Tipo del evento que se lee con este contrato. */
    public static final String TIPO = "negocio.actualizado";

    public NegocioActualizadoV1 {
        Objects.requireNonNull(giro, "giro");
        Objects.requireNonNull(publicado, "publicado");
    }
}
