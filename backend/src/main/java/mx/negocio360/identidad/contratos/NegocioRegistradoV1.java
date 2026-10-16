package mx.negocio360.identidad.contratos;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;

/**
 * Lo que Identidad lee de {@code negocio.registrado} v1 (D-78), que publica el modulo de negocios cuando registra el
 * negocio que se le pidio. Lector tolerante (R5): ignora los campos que no usa. El negocio es el del sobre; los datos
 * traen la cuenta que lo solicito, el nombre y el giro. Solo vale lo que publica el modulo de negocios (origen
 * {@value #ORIGEN}). Lo que no entiende (otro tipo, otra version, otro origen o datos mal formados) devuelve vacio:
 * reintentarlo no lo arreglaria.
 */
public record NegocioRegistradoV1(UUID negocioId, UUID solicitadoPor, String nombre, String giro, UUID eventoId,
                                  UUID correlacionId) {

    public static final String TIPO = "negocio.registrado";
    public static final int VERSION = 1;
    public static final String ORIGEN = "negocios";

    public NegocioRegistradoV1 {
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(solicitadoPor, "solicitadoPor");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(giro, "giro");
        Objects.requireNonNull(eventoId, "eventoId");
        Objects.requireNonNull(correlacionId, "correlacionId");
    }

    public static Optional<NegocioRegistradoV1> de(SobreEvento sobre) {
        if (!TIPO.equals(sobre.tipo()) || sobre.version() != VERSION || !ORIGEN.equals(sobre.origen())) {
            return Optional.empty();
        }
        var datos = sobre.datos();
        if (!(datos.get("nombre") instanceof String nombre) || !(datos.get("giro") instanceof String giro)
                || !(datos.get("solicitado_por") instanceof String solicitante)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new NegocioRegistradoV1(sobre.negocioId(), UUID.fromString(solicitante), nombre, giro,
                    sobre.id(), sobre.correlacionId()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
