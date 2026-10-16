package mx.negocio360.compartido.graphql;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Publisher;
import org.springframework.core.Ordered;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.graphql.server.WebSocketGraphQlRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * SG-15: tope de operaciones abiertas por cuenta en el WebSocket. Reserva un lugar antes de ejecutar; si la respuesta es un
 * flujo (una suscripcion) el lugar se libera cuando el flujo termina, se cancela (el cliente manda complete) o falla (se
 * cierra la conexion), y si es una respuesta simple se libera al instante. Si la cuenta esta en el tope, la operacion no
 * se ejecuta: {@code TOPE_DE_SUSCRIPCIONES}. Va por dentro de la autenticacion, de la que toma la cuenta.
 */
public final class ControlDeSuscripcionesInterceptor implements WebGraphQlInterceptor, Ordered {

    public static final String TOPE_DE_SUSCRIPCIONES = "TOPE_DE_SUSCRIPCIONES";
    public static final int ORDEN = Ordered.HIGHEST_PRECEDENCE + 8;

    private final ControlDeSuscripciones control;

    public ControlDeSuscripcionesInterceptor(ControlDeSuscripciones control) {
        this.control = Objects.requireNonNull(control, "control");
    }

    @Override
    public int getOrder() {
        return ORDEN;
    }

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest peticion, Chain cadena) {
        if (!(peticion instanceof WebSocketGraphQlRequest)) {
            return cadena.next(peticion);
        }
        return Mono.deferContextual(contexto -> {
            var cuenta = ContextoDeSesion.actorDe(contexto);
            if (cuenta == null) {
                return cadena.next(peticion);
            }
            if (!control.adquirir(cuenta)) {
                return Mono.just(RechazoGraphql.de(peticion, TOPE_DE_SUSCRIPCIONES));
            }
            var liberado = new AtomicBoolean();
            Runnable liberar = () -> {
                if (liberado.compareAndSet(false, true)) {
                    control.liberar(cuenta);
                }
            };
            return cadena.next(peticion)
                    .map(respuesta -> {
                        Object datos = respuesta.getData();
                        if (datos instanceof Publisher<?> flujo) {
                            return respuesta.transform(constructor -> constructor.data(
                                    Flux.from(flujo).doFinally(senal -> liberar.run())));
                        }
                        liberar.run();
                        return respuesta;
                    })
                    .doOnError(error -> liberar.run())
                    .doOnCancel(liberar)
                    // Si la cadena termina sin ninguna respuesta no hay flujo que libere el lugar.
                    .switchIfEmpty(Mono.defer(() -> {
                        liberar.run();
                        return Mono.empty();
                    }));
        });
    }

    /** Toma la cuenta de la entrada de GraphQL que escribio la autenticacion del WebSocket. */
    static final class ContextoDeSesion {

        private ContextoDeSesion() {
        }

        static String actorDe(reactor.util.context.ContextView contexto) {
            var guardado = contexto.getOrDefault(EntradaGraphQl.CLAVE, null);
            if (guardado instanceof EntradaGraphQl entrada && entrada.autenticada()) {
                return entrada.exigirCuenta().toString();
            }
            return null;
        }
    }
}
