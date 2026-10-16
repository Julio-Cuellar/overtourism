package mx.negocio360.compartido.graphql;

import graphql.execution.instrumentation.Instrumentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.DataFetcherExceptionResolver;
import org.springframework.graphql.server.WebGraphQlInterceptor;

/**
 * Cableado de la entrada de GraphQL comun a todos los modulos (IDN-4): el interceptor que lleva la entrada al contexto, los
 * topes de la consulta y los errores comunes. Los topes se pueden ajustar con {@code plataforma.graphql.*}.
 */
@Configuration(proxyBeanMethods = false)
public class EntradaGraphQlConfiguracion {

    @Bean
    public WebGraphQlInterceptor interceptorDeEntrada() {
        return new InterceptorDeEntrada();
    }

    @Bean
    public Instrumentation limitesDeConsulta(@Value("${plataforma.graphql.profundidad-maxima:10}") int profundidad,
                                             @Value("${plataforma.graphql.alias-maximos:10}") int alias,
                                             @Value("${plataforma.graphql.campos-maximos:200}") int campos) {
        return new LimitesDeConsultaGraphQl(profundidad, alias, campos);
    }

    @Bean
    public DataFetcherExceptionResolver resolvedorDeErroresComunes() {
        return new ResolvedorDeErroresComunes();
    }
}
