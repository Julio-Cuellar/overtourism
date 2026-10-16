package mx.negocio360.caja.dominio;

import java.util.Objects;
import java.util.UUID;

/**
 * Punto de cobro de un negocio: mostrador o taquilla.
 *
 * @param id        UUIDv7
 * @param negocioId negocio dueño
 * @param nombre    de 1 a 60 caracteres, unico por negocio sin importar mayusculas
 * @param activa    falso si ya no se usa
 */
public record Caja(UUID id, UUID negocioId, String nombre, boolean activa) {

    /** Exige id, negocio y nombre. */
    public Caja {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(nombre, "nombre");
    }
}
