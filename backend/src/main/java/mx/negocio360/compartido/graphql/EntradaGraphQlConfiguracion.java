package mx.negocio360.compartido.graphql;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlInterceptor;

/**
 * Cableado de la entrada de GraphQL comun a todos los modulos (IDN-4): el interceptor que lleva la entrada al contexto.
 * Los topes de la consulta y los errores comunes los pone {@link SeguridadGraphqlConfiguracion}.
 */
@Configuration(proxyBeanMethods = false)
public class EntradaGraphQlConfiguracion {

    @Bean
    public WebGraphQlInterceptor interceptorDeEntrada() {
        return new InterceptorDeEntrada();
    }
}
