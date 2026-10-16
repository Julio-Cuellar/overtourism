package mx.negocio360.compartido.graphql;

import java.util.Objects;

/**
 * Lo que exige una variante del tipo de comando (por ejemplo {@code anonimizarCliente}): un permiso, o ser publica
 * (por ejemplo el turista que publica una resena, que no tiene cuenta). Ser publica es una marca explicita: un permiso
 * ausente sin esa marca es un error, no una puerta abierta.
 *
 * @param variante el campo del tipo de entrada {@code @oneOf}
 * @param permiso  el permiso que exige, o nulo si la variante es publica
 * @param publico  verdadero si cualquiera puede ejecutarla, sin sesion
 */
public record ComandoDeclarado(String variante, String permiso, boolean publico) {

    public ComandoDeclarado {
        Objects.requireNonNull(variante, "variante");
        if (variante.isBlank()) {
            throw new IllegalArgumentException("La variante no puede estar en blanco");
        }
        if (publico) {
            if (permiso != null) {
                throw new IllegalArgumentException("Una variante publica no pide permiso");
            }
        } else {
            Objects.requireNonNull(permiso, "permiso");
            if (permiso.isBlank()) {
                throw new IllegalArgumentException("El permiso no puede estar en blanco");
            }
        }
    }

    /** Una variante que exige ese permiso. */
    public ComandoDeclarado(String variante, String permiso) {
        this(variante, permiso, false);
    }

    /** Una variante que cualquiera puede ejecutar, sin sesion. */
    public static ComandoDeclarado publico(String variante) {
        return new ComandoDeclarado(variante, null, true);
    }
}
