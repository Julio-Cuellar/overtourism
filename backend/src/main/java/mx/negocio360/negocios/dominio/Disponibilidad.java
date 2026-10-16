package mx.negocio360.negocios.dominio;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * El nivel de disponibilidad que declara un negocio (DI-01): libre, moderada o llena, con una nota corta. Vence a las 3
 * horas si no se renueva (DI-03) y vencida se muestra como «sin dato», nunca como libre. Negocios no calcula ocupacion:
 * guarda lo que le declaran.
 */
public record Disponibilidad(UUID negocioId, Nivel nivel, String nota, Fuente fuente, UUID declaradaPor,
                             Instant declaradaEn, Instant venceEn) {

    public enum Nivel { LIBRE, MODERADA, LLENA }

    public enum Fuente { MANUAL, VERTICAL }

    public static final Duration VIGENCIA = Duration.ofHours(3);
    public static final int MAXIMO_NOTA = 80;

    public Disponibilidad {
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(nivel, "nivel");
        Objects.requireNonNull(fuente, "fuente");
        Objects.requireNonNull(declaradaEn, "declaradaEn");
        Objects.requireNonNull(venceEn, "venceEn");
    }

    /** Lo que declara una persona del negocio. El nivel y la nota se validan aqui. */
    public static Disponibilidad declarar(UUID negocioId, String nivel, String nota, UUID declaradaPor, Instant ahora) {
        Nivel parseado;
        try {
            parseado = Nivel.valueOf(nivel == null ? "" : nivel.strip().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new NegociosException(CodigoError.DISPONIBILIDAD_INVALIDA);
        }
        String limpia = null;
        if (nota != null && !nota.isBlank()) {
            limpia = nota.strip();
            if (limpia.length() > MAXIMO_NOTA || limpia.codePoints().anyMatch(Character::isISOControl)) {
                throw new NegociosException(CodigoError.DISPONIBILIDAD_INVALIDA);
            }
        }
        return new Disponibilidad(negocioId, parseado, limpia, Fuente.MANUAL, declaradaPor, ahora, ahora.plus(VIGENCIA));
    }

    public boolean vigenteEn(Instant instante) {
        return instante.isBefore(venceEn);
    }
}
