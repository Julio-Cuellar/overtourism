package mx.negocio360.compartido.graphql.comandos;

import java.util.Objects;

/**
 * Un rechazo del despachador de comandos o de una consulta que su modulo todavia no implementa: la variante no la atiende
 * nadie, el comando no trae exactamente una variante o el token no es de ningun modulo. El texto es fijo y no dice que modulos existen.
 */
public final class ComandoGraphQlException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Codigos del despachador. */
    public enum Codigo {
        /** El comando no trae exactamente una variante, o sus campos no se pueden leer. */
        COMANDO_INVALIDO("El comando no es válido."),
        /** Ningun modulo atiende esa variante todavia. */
        COMANDO_NO_DISPONIBLE("Esa operación todavía no está disponible."),
        /** La consulta esta en el esquema pero su modulo todavia no la implementa. */
        OPERACION_NO_DISPONIBLE("Esa operación todavía no está disponible."),
        /** Un argumento de la consulta no es aceptable (por ejemplo un texto de busqueda demasiado corto). */
        CONSULTA_INVALIDA("La consulta no es válida."),
        /** El token esta vacio, es de otro modulo o no se reconoce. */
        TOKEN_INVALIDO("La confirmación no es válida. Vuelve a empezar.");

        private final String mensaje;

        Codigo(String mensaje) {
            this.mensaje = mensaje;
        }

        /** Texto fijo para la persona. */
        public String mensaje() {
            return mensaje;
        }
    }

    private final Codigo codigo;

    /**
     * Rechazo con su codigo; el mensaje de la excepcion es solo el codigo.
     *
     * @param codigo codigo fijo
     */
    public ComandoGraphQlException(Codigo codigo) {
        super(Objects.requireNonNull(codigo, "codigo").name());
        this.codigo = codigo;
    }

    /** Codigo fijo del rechazo. */
    public Codigo codigo() {
        return codigo;
    }
}
