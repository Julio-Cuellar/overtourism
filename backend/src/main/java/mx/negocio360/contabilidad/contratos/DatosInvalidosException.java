package mx.negocio360.contabilidad.contratos;

/**
 * Un evento recibido no trae lo que su contrato promete. El mensaje dice que campo fallo, nunca su valor.
 */
public final class DatosInvalidosException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea el error.
     *
     * @param campo campo del contrato que falta o no tiene forma valida
     */
    public DatosInvalidosException(String campo) {
        super("Campo invalido en el evento: " + campo);
    }
}
