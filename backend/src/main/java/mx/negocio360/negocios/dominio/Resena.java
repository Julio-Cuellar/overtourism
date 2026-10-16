package mx.negocio360.negocios.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Lo que un turista opina de un negocio (seccion 5.2). Pertenece al negocio (D-98). El autor es solo un seudonimo: en P1,
 * el id de dispositivo que genera la app (RS-11), que no identifica a nadie. El comentario es texto libre de un tercero y
 * puede traer datos personales: nunca viaja por eventos ni se imprime (SN-13).
 */
public record Resena(UUID id, UUID negocioId, UUID visitanteId, int calificacion, String comentario, Estado estado,
                     Origen origen, String respuesta, UUID respondidaPor, Instant respondidaEn, Instant creadaEn,
                     Instant editadaEn, long version) {

    /** Publicada, o oculta por la plataforma (RS-05). */
    public enum Estado { PUBLICADA, OCULTA }

    /** De donde viene el autor: un dispositivo (demostracion) o una cuenta de Visitantes (P2). */
    public enum Origen { DEMOSTRACION, VISITANTE }

    public static final int MAXIMO_COMENTARIO = 1000;
    public static final int MAXIMO_RESPUESTA = 500;

    public Resena {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(visitanteId, "visitanteId");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(origen, "origen");
        Objects.requireNonNull(creadaEn, "creadaEn");
    }

    /** RS-03: un entero de 1 a 5. */
    public static int calificacion(int valor) {
        if (valor < 1 || valor > 5) {
            throw new NegociosException(CodigoError.CALIFICACION_INVALIDA);
        }
        return valor;
    }

    /** RS-03: opcional, de hasta 1000 caracteres, sin control ni formato (salvo el salto de linea). Vacio es sin comentario. */
    public static String comentario(String entrada) {
        if (entrada == null || entrada.isBlank()) {
            return null;
        }
        var limpio = entrada.strip();
        if (limpio.length() > MAXIMO_COMENTARIO || tieneInvisibles(limpio)) {
            throw new NegociosException(CodigoError.COMENTARIO_INVALIDO);
        }
        return limpio;
    }

    /** RS-04: obligatoria, de hasta 500 caracteres, con las mismas reglas de caracteres. */
    public static String respuesta(String entrada) {
        if (entrada == null || entrada.isBlank()) {
            throw new NegociosException(CodigoError.RESPUESTA_INVALIDA);
        }
        var limpia = entrada.strip();
        if (limpia.length() > MAXIMO_RESPUESTA || tieneInvisibles(limpia)) {
            throw new NegociosException(CodigoError.RESPUESTA_INVALIDA);
        }
        return limpia;
    }

    /** RS-11: el id de dispositivo es un UUID en su forma de texto de 36 caracteres. */
    public static UUID dispositivo(String entrada) {
        if (entrada == null || entrada.length() != 36) {
            throw new NegociosException(CodigoError.DISPOSITIVO_INVALIDO);
        }
        try {
            return UUID.fromString(entrada);
        } catch (IllegalArgumentException e) {
            throw new NegociosException(CodigoError.DISPOSITIVO_INVALIDO);
        }
    }

    private static boolean tieneInvisibles(String texto) {
        return texto.codePoints().anyMatch(c -> c != '\n' && (Character.isISOControl(c)
                || Character.getType(c) == Character.FORMAT || Character.getType(c) == Character.PRIVATE_USE));
    }

    /** Sin el comentario ni el dispositivo en el texto: no deben acabar en un registro (SN-13, SN-06). */
    @Override
    public String toString() {
        return "Resena[id=" + id + ", negocioId=" + negocioId + ", estado=" + estado + "]";
    }
}
