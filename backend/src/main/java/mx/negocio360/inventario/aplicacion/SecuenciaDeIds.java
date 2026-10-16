package mx.negocio360.inventario.aplicacion;

import java.time.Instant;
import java.util.UUID;
import mx.negocio360.compartido.identidad.UuidV7;

/**
 * Genera UUIDv7 estrictamente crecientes dentro del proceso. Los movimientos se leen ordenados por id (kardex y
 * cursores), y dos UUIDv7 del mismo milisegundo salen en orden arbitrario; aqui cada id usa al menos un milisegundo
 * mas que el anterior. En varias instancias el orden entre ellas sigue siendo el del reloj.
 */
public final class SecuenciaDeIds {

    private Instant ultimo = Instant.EPOCH;

    /** Una secuencia nueva; todo lo que mueve el saldo de un mismo servicio debe compartir la misma. */
    public SecuenciaDeIds() {
    }

    /** Un UUIDv7 mayor que todos los que entrego antes. */
    synchronized UUID siguiente(Instant ahora) {
        var instante = ahora.toEpochMilli() > ultimo.toEpochMilli() ? ahora : ultimo.plusMillis(1);
        ultimo = instante;
        return UuidV7.generar(instante);
    }
}
