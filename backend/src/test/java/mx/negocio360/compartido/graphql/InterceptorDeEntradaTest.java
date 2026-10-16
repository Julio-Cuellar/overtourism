package mx.negocio360.compartido.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.ExecutionInput;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import mx.negocio360.compartido.seguridad.Autorizacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import reactor.core.publisher.Mono;

/** IDN-4: la entrada que dejo el filtro llega al contexto de GraphQL; si no hay ninguna, la que llega es anonima. */
class InterceptorDeEntradaTest {

    private static EntradaGraphQl contextoQueQuedo(Map<String, Object> atributos) {
        var peticion = mock(WebGraphQlRequest.class);
        when(peticion.getAttributes()).thenReturn(atributos);
        var cadena = mock(WebGraphQlInterceptor.Chain.class);
        when(cadena.next(any())).thenReturn(Mono.empty());

        new InterceptorDeEntrada().intercept(peticion, cadena).block();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<BiFunction<ExecutionInput, ExecutionInput.Builder, ExecutionInput>> funcion =
                ArgumentCaptor.forClass(BiFunction.class);
        verify(peticion).configureExecutionInput(funcion.capture());
        var entrada = ExecutionInput.newExecutionInput("{ x }").build();
        var resultado = funcion.getValue().apply(entrada, ExecutionInput.newExecutionInput("{ x }"));
        return (EntradaGraphQl) resultado.getGraphQLContext().get(EntradaGraphQl.CLAVE);
    }

    @Test
    @DisplayName("la entrada que dejo el filtro es la que llega al contexto")
    void laEntradaDelFiltro_llegaAlContexto() {
        var entrada = EntradaGraphQl.de(UUID.randomUUID(), null, Autorizacion.de(Set.of("negocios.perfil.ver")), null);

        assertThat(contextoQueQuedo(Map.of(EntradaGraphQl.ATRIBUTO, entrada))).isSameAs(entrada);
    }

    @Test
    @DisplayName("sin entrada en la peticion, la del contexto es anonima: no tiene cuenta ni permisos (falla cerrado)")
    void sinEntrada_esAnonima() {
        var entrada = contextoQueQuedo(Map.of());

        assertThat(entrada.autenticada()).isFalse();
        assertThat(entrada.autorizacion().puede("negocios.perfil.ver")).isFalse();
    }

    @Test
    @DisplayName("un atributo que no es una EntradaGraphQl se ignora, no se obedece")
    void atributoAjeno_seIgnora() {
        assertThat(contextoQueQuedo(Map.of(EntradaGraphQl.ATRIBUTO, "soy-admin")).autenticada()).isFalse();
    }
}
