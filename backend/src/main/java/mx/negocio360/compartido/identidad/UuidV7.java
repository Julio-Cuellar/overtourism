package mx.negocio360.compartido.identidad;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/**
 * Generador de UUIDv7 (RFC 9562): 48 bits de milisegundos Unix, version 7, variante RFC 4122 y bits aleatorios.
 * Los ids nuevos quedan ordenados por milisegundo (dentro del mismo milisegundo el orden es arbitrario), lo que
 * mantiene los indices de Postgres compactos (R3).
 */
public final class UuidV7 {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final int VERSION = 7;
    private static final long BITS_ALEATORIOS_A = 0x0FFFL;
    private static final long BITS_ALEATORIOS_B = 0x3FFF_FFFF_FFFF_FFFFL;
    private static final long VARIANTE_RFC_4122 = 0x8000_0000_0000_0000L;
    private static final int DESPLAZAMIENTO_TIEMPO = 16;
    private static final int DESPLAZAMIENTO_VERSION = 12;

    private UuidV7() {
    }

    public static UUID generar(Instant instante) {
        var milisegundos = instante.toEpochMilli();
        var parteAlta = (milisegundos << DESPLAZAMIENTO_TIEMPO)
                | ((long) VERSION << DESPLAZAMIENTO_VERSION)
                | (ALEATORIO.nextLong() & BITS_ALEATORIOS_A);
        var parteBaja = (ALEATORIO.nextLong() & BITS_ALEATORIOS_B) | VARIANTE_RFC_4122;
        return new UUID(parteAlta, parteBaja);
    }

    /** Instante (en milisegundos) que lleva un UUIDv7. */
    public static Instant instanteDe(UUID id) {
        if (id.version() != VERSION) {
            throw new IllegalArgumentException("El id no es UUIDv7: " + id);
        }
        return Instant.ofEpochMilli(id.getMostSignificantBits() >>> DESPLAZAMIENTO_TIEMPO);
    }
}
