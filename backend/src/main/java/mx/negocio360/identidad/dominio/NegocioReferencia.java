package mx.negocio360.identidad.dominio;

import java.util.Objects;
import java.util.UUID;

/**
 * Copia minima de un negocio, para el selector y la plantilla de roles (D-78). El dueño del dato es el modulo de
 * negocios; Identidad solo la guarda cuando ese modulo anuncia que registro el negocio.
 */
public record NegocioReferencia(UUID negocioId, String nombre, Giro giro, Estado estado) {

    public enum Estado { ACTIVO, CERRADO }

    public NegocioReferencia {
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(giro, "giro");
        Objects.requireNonNull(estado, "estado");
    }
}
