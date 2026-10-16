package mx.negocio360.restaurantes.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * La extension de un negocio de giro RESTAURANTE (seccion 3.4 de negocios.md): nace al escuchar {@code negocio.registrado}
 * y recuerda si el negocio es visible para el turista, que es lo que decide si se publica su menu (PB-01).
 *
 * @param negocioId     el mismo id del negocio, que asigno Identidad
 * @param publicado     lo que el dueño decidio en Negocios
 * @param estado        {@code ACTIVO}, {@code SUSPENDIDO} o {@code CERRADO}
 * @param actualizadoEn momento del ultimo evento aplicado; un evento mas viejo no lo pisa
 */
public record Restaurante(UUID negocioId, boolean publicado, String estado, Instant actualizadoEn) {

    /** Estado en el que un negocio puede mostrarse al turista. */
    public static final String ACTIVO = "ACTIVO";

    public Restaurante {
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(actualizadoEn, "actualizadoEn");
    }

    /** Un restaurante recien dado de alta: activo y sin publicar. */
    public static Restaurante nuevo(UUID negocioId, Instant ahora) {
        return new Restaurante(negocioId, false, ACTIVO, ahora);
    }

    /** PB-01: el turista lo ve solo si el dueño lo publico y el negocio esta activo. */
    public boolean visible() {
        return publicado && ACTIVO.equals(estado);
    }

    /** Aplica lo ultimo que se sabe de la publicacion. */
    public Restaurante conPublicado(boolean nuevo, Instant momento) {
        return new Restaurante(negocioId, nuevo, estado, momento);
    }

    /** Aplica el ultimo estado del negocio. */
    public Restaurante conEstado(String nuevo, Instant momento) {
        return new Restaurante(negocioId, publicado, nuevo, momento);
    }
}
