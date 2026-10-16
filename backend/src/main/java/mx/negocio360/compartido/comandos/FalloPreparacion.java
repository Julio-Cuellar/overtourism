package mx.negocio360.compartido.comandos;

/**
 * Fallos del patron preparar -&gt; confirmar que detecta {@link ServicioPreparacion}. Cada modulo los traduce a su
 * propia excepcion con el codigo del mismo nombre.
 */
public enum FalloPreparacion {
    /** Token vacio, desconocido, demasiado largo, o de otro negocio u otro actor. */
    TOKEN_INVALIDO,
    /** Token vencido (5 minutos, D-23). */
    TOKEN_EXPIRADO,
    /** Token ya usado, o perdido en una carrera con otra confirmacion. */
    TOKEN_USADO,
    /** El comando guardado ya no coincide con su hash o no se puede leer. */
    COMANDO_CAMBIO
}
