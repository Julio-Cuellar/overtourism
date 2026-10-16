package mx.negocio360.canal.web;

/**
 * Fallo del canal, antes de llegar a un modulo. Lleva un codigo fijo y su texto en espanol de Mexico; nunca datos de
 * la peticion.
 */
public final class CanalException extends RuntimeException {

    /** Codigos del canal, cada uno con su texto fijo. */
    public enum Codigo {
        /** No hay sesion valida con negocio (AZ-05). */
        NO_AUTENTICADO("Inicia sesión para continuar."),
        /** El comando existe en el esquema pero su modulo aun no esta en el canal (por ejemplo Clientes). */
        NO_DISPONIBLE("Esa operación todavía no está disponible."),
        /** El comando no trae exactamente una variante o sus campos no cumplen el esquema. */
        COMANDO_INVALIDO("Revisa los datos de la operación."),
        /** El token no pertenece a ningun modulo del canal. */
        TOKEN_INVALIDO("La confirmación no es válida. Vuelve a empezar.");

        private final String mensaje;

        Codigo(String mensaje) {
            this.mensaje = mensaje;
        }

        /**
         * Texto fijo para la persona.
         *
         * @return mensaje en espanol de Mexico
         */
        public String mensaje() {
            return mensaje;
        }
    }

    private final Codigo codigo;

    /**
     * Crea el fallo.
     *
     * @param codigo codigo fijo
     */
    public CanalException(Codigo codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    /**
     * Codigo del fallo.
     *
     * @return el codigo
     */
    public Codigo codigo() {
        return codigo;
    }
}
