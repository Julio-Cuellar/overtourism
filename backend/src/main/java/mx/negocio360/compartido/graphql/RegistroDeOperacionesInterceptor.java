package mx.negocio360.compartido.graphql;

import graphql.GraphQLError;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import reactor.core.publisher.Mono;

/**
 * SG-20: una linea de registro por operacion en el logger {@code auditoria.graphql}: la cuenta, el negocio, la operacion
 * (tipo y campos de la raiz), como salio y cuanto tardo. Nunca el texto de la consulta, las variables ni el nombre de la
 * operacion que escribe el cliente: pueden traer datos personales o saltos de linea para falsificar renglones. La
 * operacion sale de nombres que el esquema ya valido; los errores, de nuestros codigos. Va por fuera de todo, asi ve la
 * respuesta final.
 */
public final class RegistroDeOperacionesInterceptor implements WebGraphQlInterceptor, Ordered {

    public static final String NOMBRE_DEL_REGISTRO = "auditoria.graphql";
    private static final Logger REGISTRO = LoggerFactory.getLogger(NOMBRE_DEL_REGISTRO);
    private static final Pattern SEGURO = Pattern.compile("[A-Za-z0-9_.:,-]{1,100}");
    private static final String NO_EJECUTADA = "no_ejecutada";

    /** Despues de la autenticacion del WebSocket, que escribe la entrada en el contexto de Reactor que aqui se lee. */
    public static final int ORDEN = Ordered.HIGHEST_PRECEDENCE + 5;

    @Override
    public int getOrder() {
        return ORDEN;
    }

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest peticion, Chain cadena) {
        var inicio = System.nanoTime();
        // En HTTP la entrada la dejo el filtro de /graphql como atributo de la peticion.
        var deLaPeticion = peticion.getAttributes().get(EntradaGraphQl.ATRIBUTO);
        return Mono.deferContextual(contexto -> {
            // En un WebSocket la deja la autenticacion de la conexion en el contexto de Reactor.
            var guardada = contexto.getOrDefault(EntradaGraphQl.CLAVE, null);
            var entrada = guardada instanceof EntradaGraphQl e ? e : deLaPeticion instanceof EntradaGraphQl h ? h : null;
            var cuenta = entrada == null ? "-" : entrada.cuenta().map(Object::toString).map(RegistroDeOperacionesInterceptor::seguro)
                    .orElse("-");
            var negocio = entrada == null ? "-" : entrada.negocio().map(Object::toString)
                    .map(RegistroDeOperacionesInterceptor::seguro).orElse("-");
            var quien = "cuenta=" + cuenta + " negocio=" + negocio;
            return cadena.next(peticion)
                    .doOnNext(respuesta -> registrar(quien, respuesta, inicio))
                    .doOnError(error -> REGISTRO.info("{} operacion={} resultado=ERROR errores={} ms={}", quien,
                            NO_EJECUTADA, ResolvedorDeErroresGraphql.ERROR_INTERNO, milisegundos(inicio)));
        });
    }

    private static void registrar(String quien, WebGraphQlResponse respuesta, long inicio) {
        var operacion = respuesta.getExecutionInput().getGraphQLContext().<String>get(ClavesDeContexto.OPERACION);
        var codigos = new LinkedHashSet<String>();
        for (var error : respuesta.getExecutionResult().getErrors()) {
            codigos.add(codigo(error));
        }
        REGISTRO.info("{} operacion={} resultado={} errores={} ms={}", quien, operacion == null ? NO_EJECUTADA
                        : seguro(operacion), codigos.isEmpty() ? "OK" : "ERROR",
                codigos.isEmpty() ? "-" : String.join(",", codigos), milisegundos(inicio));
    }

    private static String codigo(GraphQLError error) {
        var extensiones = error.getExtensions();
        return extensiones != null && extensiones.get("codigo") instanceof String codigo ? seguro(codigo)
                : "DESCONOCIDO";
    }

    /** Solo caracteres que no pueden falsificar una linea ni abrir un campo nuevo; lo demas se reemplaza. */
    private static String seguro(String texto) {
        return SEGURO.matcher(texto).matches() ? texto : "?";
    }

    private static long milisegundos(long inicioNanos) {
        return (System.nanoTime() - inicioNanos) / 1_000_000;
    }
}
