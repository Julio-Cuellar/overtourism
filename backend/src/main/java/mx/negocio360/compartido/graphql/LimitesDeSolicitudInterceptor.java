package mx.negocio360.compartido.graphql;

import java.util.Objects;
import java.util.concurrent.TimeoutException;
import org.springframework.core.Ordered;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import reactor.core.publisher.Mono;

/**
 * SG-09: el tamano de la consulta se mide antes de analizarla (un texto enorme no gasta tiempo del analizador) y la
 * respuesta no espera mas del tiempo maximo. Al vencer el tiempo se responde {@code TIEMPO_AGOTADO}; el motor no puede
 * cancelar un resolvedor que ya corre, pero nadie espera su resultado. Aplica igual a HTTP y a WebSocket.
 */
public final class LimitesDeSolicitudInterceptor implements WebGraphQlInterceptor, Ordered {

    public static final String CONSULTA_MUY_LARGA = "CONSULTA_MUY_LARGA";
    public static final String TIEMPO_AGOTADO = "TIEMPO_AGOTADO";

    /** Entre el registro (por fuera) y el enmascarado de errores (por dentro). */
    public static final int ORDEN = Ordered.HIGHEST_PRECEDENCE + 10;

    private final PropiedadesGraphql limites;

    public LimitesDeSolicitudInterceptor(PropiedadesGraphql limites) {
        this.limites = Objects.requireNonNull(limites, "limites");
    }

    @Override
    public int getOrder() {
        return ORDEN;
    }

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest peticion, Chain cadena) {
        if (peticion.getDocument().length() > limites.consultaMaximaCaracteres()) {
            return Mono.just(rechazo(peticion, CONSULTA_MUY_LARGA));
        }
        return cadena.next(peticion).timeout(limites.tiempoMaximo())
                .onErrorResume(TimeoutException.class, vencido -> Mono.just(rechazo(peticion, TIEMPO_AGOTADO)));
    }

    private static WebGraphQlResponse rechazo(WebGraphQlRequest peticion, String codigo) {
        return RechazoGraphql.de(peticion, codigo);
    }
}
