package mx.negocio360.canal.web;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.identidad.IdentidadApi;

/**
 * Convierte el encabezado {@code Authorization: Bearer} en el {@link ContextoNegocio} de la peticion, preguntando a
 * Identidad (AZ-05). El negocio sale de la sesion; si la sesion aun no tiene negocio (todavia no existe el modulo de
 * negocios), se usa el negocio de demostracion <b>solo si el servidor lo configuro</b> (D-90). El cliente nunca lo
 * puede elegir.
 */
public final class ContextoDeSesion {

    private static final Pattern BEARER = Pattern.compile("(?i)^Bearer\\s+(\\S+)$");

    private final IdentidadApi identidad;
    private final UUID negocioDeDemo;

    /**
     * Arma el resolutor.
     *
     * @param identidad     API de Identidad
     * @param negocioDeDemo negocio para sesiones sin negocio, o nulo para no usar ninguno
     */
    public ContextoDeSesion(IdentidadApi identidad, UUID negocioDeDemo) {
        this.identidad = Objects.requireNonNull(identidad, "identidad");
        this.negocioDeDemo = negocioDeDemo;
    }

    /**
     * Contexto de una peticion.
     *
     * @param autorizacion valor del encabezado {@code Authorization}, puede ser nulo
     * @return negocio y actor, o vacio si no hay sesion valida o no hay negocio
     */
    public Optional<ContextoNegocio> resolver(String autorizacion) {
        if (autorizacion == null) {
            return Optional.empty();
        }
        var coincide = BEARER.matcher(autorizacion.strip());
        if (!coincide.matches()) {
            return Optional.empty();
        }
        return identidad.autenticar(coincide.group(1)).flatMap(sesion -> sesion.contextoNegocio()
                .or(() -> Optional.ofNullable(negocioDeDemo)
                        .map(negocio -> new ContextoNegocio(negocio, sesion.cuentaId().toString()))));
    }
}
