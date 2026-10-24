package mx.negocio360.restaurantes.dominio;

/**
 * El estado de una mesa, que <b>no se guarda</b>: se deriva (MS-02). {@code BLOQUEADA} es la marca manual del personal;
 * {@code OCUPADA}, tener una cuenta abierta o por cobrar; {@code LIBRE}, ninguna de las dos.
 */
public enum EstadoDeMesa {
    LIBRE,
    OCUPADA,
    BLOQUEADA
}
