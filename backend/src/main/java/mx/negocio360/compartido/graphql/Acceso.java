package mx.negocio360.compartido.graphql;

/**
 * Quien puede ejecutar una operacion de la raiz del esquema (SG-01, SG-03). Es una decision explicita de quien la declara:
 * lo que no se declara se niega.
 */
public enum Acceso {
    /** Cualquiera, con o sin sesion (la app del turista). Solo para lo que se publica a proposito: vistas de solo lectura. */
    PUBLICA,
    /** Una sesion, aunque todavia no haya elegido negocio (por ejemplo ver sus altas de negocio). */
    CUENTA,
    /** Una sesion con negocio activo, y el permiso que declare la operacion o la variante del comando. */
    NEGOCIO
}
