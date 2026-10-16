package mx.negocio360.compartido.graphql;

/** Claves del contexto de una ejecucion de GraphQL que comparten las piezas de la puerta de /graphql. */
public final class ClavesDeContexto {

    /** Resumen seguro de la operacion: {@code tipo:campo1,campo2}, con nombres que el esquema ya valido (SG-20). */
    public static final String OPERACION = "n360.operacion";

    private ClavesDeContexto() {
    }
}
