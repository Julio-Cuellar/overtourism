package mx.negocio360.restaurantes.contratos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Objects;

/**
 * Lo que Restaurantes lee de {@code negocio.registrado} v1: el giro. Lector tolerante: ignora lo que no usa y lo que
 * Negocios agregue (R5).
 *
 * @param giro {@code RESTAURANTE}, {@code MUSEO} o {@code MISCELANEA}
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record NegocioRegistradoV1(String giro) {

    /** Tipo del evento que se lee con este contrato. */
    public static final String TIPO = "negocio.registrado";

    public NegocioRegistradoV1 {
        Objects.requireNonNull(giro, "giro");
        if (giro.isBlank()) {
            throw new IllegalArgumentException("El giro no puede estar vacio");
        }
    }
}
