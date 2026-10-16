package mx.negocio360.negocios;

import java.math.BigDecimal;
import java.util.UUID;

/** Lo minimo que otros horizontales necesitan de un negocio (J2, R6): quien es, en que estado y donde esta. */
public record VistaDeNegocio(UUID negocioId, String nombre, String giro, String estado, String zonaHoraria,
                             BigDecimal latitud, BigDecimal longitud) {
}
