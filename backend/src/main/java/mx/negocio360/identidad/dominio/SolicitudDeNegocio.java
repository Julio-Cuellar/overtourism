package mx.negocio360.identidad.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Un negocio que Identidad pidio registrar y que el modulo de negocios todavia no confirmo (D-78, D-79). Sirve para
 * reconocer su respuesta (solo se acepta un {@code negocio.registrado} de un negocio pedido, a nombre de quien lo pidio)
 * y para mostrarle a la persona que su negocio esta pendiente.
 */
public record SolicitudDeNegocio(UUID negocioId, UUID cuentaId, String nombre, Giro giro, Instant creadaEn) {

    public SolicitudDeNegocio {
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(cuentaId, "cuentaId");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(giro, "giro");
        Objects.requireNonNull(creadaEn, "creadaEn");
    }
}
