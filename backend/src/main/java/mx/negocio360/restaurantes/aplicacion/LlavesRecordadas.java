package mx.negocio360.restaurantes.aplicacion;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import mx.negocio360.restaurantes.dominio.CodigoError;
import mx.negocio360.restaurantes.dominio.RestaurantesException;

/**
 * Lo que se responde a una operacion con {@code Idempotency-Key} (CT-14, QR-15, D-208): repetir la misma llave con la misma
 * peticion devuelve lo mismo sin repetir el efecto; la misma llave con otra peticion es {@code LLAVE_REUTILIZADA}. Cada llave
 * se recuerda un tiempo y despues se olvida. Vive en la memoria de este proceso: con varios nodos hace falta la tabla de la
 * plataforma (HZ-25). Quien la usa debe cuidar que revisar, ejecutar y guardar ocurran bajo el mismo cerrojo.
 *
 * @param <T> lo que se devuelve al repetir
 */
final class LlavesRecordadas<T> {

    /** Una operacion ya hecha: que se pidio (huella) y que se respondio, hasta {@code expira}. */
    private record Recordada<T>(String huella, T resultado, Instant expira) {
    }

    private final Clock reloj;
    private final Duration retencion;
    private final Map<String, Recordada<T>> recordadas = new HashMap<>();

    LlavesRecordadas(Clock reloj, Duration retencion) {
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        this.retencion = Objects.requireNonNull(retencion, "retencion");
    }

    /**
     * Lo que se respondio antes a esa llave, si no ha vencido.
     *
     * @param clave  la llave junto con lo que la acota (negocio, persona, operacion)
     * @param huella lo que se pide ahora
     * @throws RestaurantesException LLAVE_REUTILIZADA si la llave se uso con otra peticion
     */
    synchronized Optional<T> buscar(String clave, String huella) {
        var ahora = reloj.instant();
        recordadas.values().removeIf(r -> !r.expira().isAfter(ahora));
        var anterior = recordadas.get(clave);
        if (anterior == null) {
            return Optional.empty();
        }
        if (!anterior.huella().equals(huella)) {
            throw new RestaurantesException(CodigoError.LLAVE_REUTILIZADA);
        }
        return Optional.of(anterior.resultado());
    }

    /** Recuerda la respuesta de una operacion que acaba de salir bien. */
    synchronized void guardar(String clave, String huella, T resultado) {
        recordadas.put(clave, new Recordada<>(huella, resultado, reloj.instant().plus(retencion)));
    }

    /** La llave sin espacios sobrantes, o nulo si no hay. */
    static String limpia(String llave) {
        return llave == null || llave.isBlank() ? null : llave.strip();
    }
}
