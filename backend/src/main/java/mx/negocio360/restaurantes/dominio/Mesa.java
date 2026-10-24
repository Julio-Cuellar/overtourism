package mx.negocio360.restaurantes.dominio;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Una mesa del salon (MS-01). No guarda su estado: se deriva de su cuenta (MS-02). Lleva el token opaco de su QR
 * (QR-01), que se rota con {@link #conToken} y que deja de servir si la mesa se da de baja (MS-09). Es inmutable.
 *
 * @param id              identificador
 * @param negocioId       negocio dueño
 * @param zonaId          zona a la que pertenece
 * @param etiqueta        nombre visible, de 1 a 20 caracteres, unico entre las mesas activas del negocio
 * @param capacidad       de 1 a 20 personas
 * @param bloqueada       marca manual del personal (MS-02)
 * @param activa          falso si se dio de baja (MS-07)
 * @param qrToken         enlace opaco de 128 bits en base64url (QR-01)
 * @param qrPausadoHasta  hasta cuando el QR de esta mesa no acepta pedidos (QR-17), o nulo
 * @param version         bloqueo optimista, desde 1
 * @param actualizadaEn   ultimo cambio
 */
public record Mesa(UUID id, UUID negocioId, UUID zonaId, String etiqueta, int capacidad, boolean bloqueada, boolean activa,
                   String qrToken, Instant qrPausadoHasta, long version, Instant actualizadaEn) {

    /** Tope de mesas activas por negocio (MS-01). */
    public static final int MAX_POR_NEGOCIO = 60;
    /** Capacidad maxima de una mesa (MS-01). */
    public static final int MAX_CAPACIDAD = 20;
    private static final int MAX_ETIQUETA = 20;

    public Mesa {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(zonaId, "zonaId");
        var recortada = etiqueta == null ? "" : etiqueta.strip();
        if (recortada.isEmpty() || recortada.length() > MAX_ETIQUETA) {
            throw new RestaurantesException(CodigoError.NOMBRE_INVALIDO);
        }
        etiqueta = recortada;
        if (capacidad < 1 || capacidad > MAX_CAPACIDAD) {
            throw new RestaurantesException(CodigoError.CAPACIDAD_INVALIDA);
        }
        Objects.requireNonNull(qrToken, "qrToken");
        Objects.requireNonNull(actualizadaEn, "actualizadaEn");
    }

    /** Una mesa nueva, activa, libre de bloqueo y en la version 1. */
    public static Mesa nueva(UUID id, UUID negocioId, UUID zonaId, String etiqueta, int capacidad, String qrToken,
                             Instant ahora) {
        return new Mesa(id, negocioId, zonaId, etiqueta, capacidad, false, true, qrToken, null, 1, ahora);
    }

    /** La etiqueta como se compara para la unicidad: sin espacios sobrantes ni mayusculas. */
    public String etiquetaNormalizada() {
        return normalizar(etiqueta);
    }

    /** Normaliza una etiqueta para compararla. */
    public static String normalizar(String etiqueta) {
        return etiqueta == null ? "" : etiqueta.strip().toLowerCase(Locale.ROOT);
    }

    /** Verdadero si el QR de esta mesa esta pausado en ese momento (QR-17). */
    public boolean qrPausado(Instant ahora) {
        return qrPausadoHasta != null && ahora.isBefore(qrPausadoHasta);
    }

    /** Cambia zona, etiqueta y capacidad. */
    public Mesa editada(UUID nuevaZona, String nuevaEtiqueta, int nuevaCapacidad, Instant ahora) {
        return new Mesa(id, negocioId, nuevaZona, nuevaEtiqueta, nuevaCapacidad, bloqueada, activa, qrToken, qrPausadoHasta,
                version + 1, ahora);
    }

    /** Pone o quita la marca manual de bloqueo. */
    public Mesa conBloqueo(boolean nueva, Instant ahora) {
        return new Mesa(id, negocioId, zonaId, etiqueta, capacidad, nueva, activa, qrToken, qrPausadoHasta, version + 1, ahora);
    }

    /** Da de baja la mesa (MS-07); su QR deja de servir (MS-09). */
    public Mesa conBaja(Instant ahora) {
        return new Mesa(id, negocioId, zonaId, etiqueta, capacidad, bloqueada, false, qrToken, qrPausadoHasta, version + 1,
                ahora);
    }

    /** Cambia el token del QR: el anterior deja de servir de inmediato (QR-02). */
    public Mesa conToken(String nuevoToken, Instant ahora) {
        return new Mesa(id, negocioId, zonaId, etiqueta, capacidad, bloqueada, activa, nuevoToken, qrPausadoHasta,
                version + 1, ahora);
    }

    /** Pausa el QR hasta ese momento, o lo reanuda con nulo (QR-17). */
    public Mesa conQrPausadoHasta(Instant hasta, Instant ahora) {
        return new Mesa(id, negocioId, zonaId, etiqueta, capacidad, bloqueada, activa, qrToken, hasta, version + 1, ahora);
    }
}
