package mx.negocio360.seguridad;

import java.time.Duration;
import mx.negocio360.compartido.seguridad.PermisoDenegadoException;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

/** Solo pruebas: resolvedores de los campos de {@code prueba-seguridad.graphqls}. */
@Controller
class ContextoDePruebaControlador {

    /** Devuelve el negocio y el actor que el resolvedor recibe de la sesion, nunca del cliente. */
    @QueryMapping
    String contextoDePrueba(@ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        var contexto = entrada.exigirContexto();
        return contexto.negocioId() + "|" + contexto.actor();
    }

    @QueryMapping
    NodoDePrueba arbolDePrueba() {
        return NodoDePrueba.hoja();
    }

    @QueryMapping
    String fallaInternaDePrueba() {
        throw new IllegalStateException("password=secreto SELECT * FROM cliente WHERE telefono='2221234567'");
    }

    @QueryMapping
    String permisoDenegadoDePrueba() {
        throw new PermisoDenegadoException("clientes.cliente.anonimizar");
    }

    @QueryMapping
    String noAutenticadoDePrueba() {
        throw new AuthenticationCredentialsNotFoundException("sin sesion");
    }

    @QueryMapping
    String sinNegocioDePrueba() {
        throw new mx.negocio360.compartido.seguridad.NoAutenticadoException(
                mx.negocio360.compartido.seguridad.NoAutenticadoException.SIN_NEGOCIO_ACTIVO);
    }

    @QueryMapping
    String uuidDePrueba(@Argument java.util.UUID id) {
        return id.toString();
    }

    @QueryMapping
    Mono<String> asincronoQueFallaDePrueba() {
        return Mono.error(new PermisoDenegadoException("inventario.articulo.baja"));
    }

    @QueryMapping
    String errorDeNegocioDePrueba() {
        throw new ErrorDeNegocioDePrueba();
    }

    @QueryMapping
    String nuloDePrueba() {
        return null;
    }

    /** Cuantas veces se ejecutaron los resolvedores protegidos: una puerta que niega no debe llegar a ejecutarlos. */
    static final java.util.concurrent.atomic.AtomicInteger EJECUCIONES_PROTEGIDAS =
            new java.util.concurrent.atomic.AtomicInteger();

    @QueryMapping
    String soloConPermisoDePrueba() {
        EJECUCIONES_PROTEGIDAS.incrementAndGet();
        return "paso";
    }

    @QueryMapping
    String sensibleDePrueba() {
        EJECUCIONES_PROTEGIDAS.incrementAndGet();
        return "paso";
    }

    @org.springframework.graphql.data.method.annotation.MutationMapping
    String comandoDePrueba() {
        EJECUCIONES_PROTEGIDAS.incrementAndGet();
        return "paso";
    }

    @org.springframework.graphql.data.method.annotation.SubscriptionMapping
    reactor.core.publisher.Flux<String> eventosDePrueba(@ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return mx.negocio360.compartido.graphql.EventosPorNegocio
                .filtrar(EventosDePrueba.flujo(), entrada, EventoDePrueba::negocioId).map(EventoDePrueba::texto);
    }

    @org.springframework.graphql.data.method.annotation.SubscriptionMapping
    reactor.core.publisher.Flux<String> sensibleSuscripcionDePrueba() {
        EJECUCIONES_PROTEGIDAS.incrementAndGet();
        return reactor.core.publisher.Flux.never();
    }

    @org.springframework.graphql.data.method.annotation.SubscriptionMapping
    reactor.core.publisher.Flux<String> suscripcionQueFallaDePrueba() {
        return reactor.core.publisher.Flux.error(
                new IllegalStateException("password=secreto SELECT * FROM cliente WHERE telefono='2221234567'"));
    }

    @QueryMapping
    Mono<String> lentoDePrueba(@Argument int milisegundos) {
        return Mono.delay(Duration.ofMillis(milisegundos)).map(demora -> "listo");
    }
}
