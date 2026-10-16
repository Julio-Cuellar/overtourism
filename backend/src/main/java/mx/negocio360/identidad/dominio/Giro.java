package mx.negocio360.identidad.dominio;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

/** Giros que reconoce la plataforma: cada uno ofrece sus propios roles al personal (D-69). */
public enum Giro {
    RESTAURANTE, MISCELANEA, MUSEO;

    private static final int MAX_CARACTERES_DE_ENTRADA = 40;

    /** Reconoce el giro sin importar mayusculas, espacios ni acentos; lo desconocido devuelve vacio. */
    public static Optional<Giro> de(String entrada) {
        if (entrada == null || entrada.length() > MAX_CARACTERES_DE_ENTRADA) {
            return Optional.empty();
        }
        var limpio = Normalizer.normalize(entrada.strip(), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
        for (var giro : values()) {
            if (giro.name().equals(limpio)) {
                return Optional.of(giro);
            }
        }
        return Optional.empty();
    }
}
