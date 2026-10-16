package mx.negocio360.canal.web;

import java.util.UUID;
import mx.negocio360.identidad.IdentidadApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cableado HTTP del canal: sesion a contexto (D-90), traduccion de errores y CORS para el frontend en desarrollo.
 * Solo con {@code negocio360.demo.habilitada=true} y nunca con el perfil {@code produccion}.
 *
 * <p>Propiedades: {@code negocio360.demo.negocio-id} (negocio para sesiones sin negocio; vacio = ninguno) y
 * {@code negocio360.demo.cors-origenes} (patrones de origen separados por coma; vacio = ninguno). La app movil no
 * necesita CORS; Flutter web y el navegador si.
 */
@Configuration(proxyBeanMethods = false)
@Profile("!produccion")
@ConditionalOnProperty(name = "negocio360.demo.habilitada", havingValue = "true")
public class CanalConfiguracion {

    /**
     * Resuelve la sesion de cada peticion.
     *
     * @param identidad     API de Identidad
     * @param negocioDeDemo id del negocio de demostracion, o vacio
     * @return el resolutor
     */
    @Bean
    public ContextoDeSesion contextoDeSesion(IdentidadApi identidad,
                                             @Value("${negocio360.demo.negocio-id:}") String negocioDeDemo) {
        return new ContextoDeSesion(identidad, negocioDeDemo.isBlank() ? null : UUID.fromString(negocioDeDemo));
    }

    /**
     * Pone el contexto de la sesion en cada peticion GraphQL.
     *
     * @param sesiones resolutor de la sesion
     * @return el interceptor
     */
    @Bean
    public InterceptorDeSesion interceptorDeSesion(ContextoDeSesion sesiones) {
        return new InterceptorDeSesion(sesiones);
    }

    /**
     * Errores de dominio con codigo, regla y texto fijo.
     *
     * @return el traductor
     */
    @Bean
    public ErroresDeCanal erroresDeCanal() {
        return new ErroresDeCanal();
    }

    /**
     * CORS para {@code /graphql} y {@code /auth/**}, solo con los origenes configurados.
     *
     * @param origenes patrones de origen separados por coma
     * @return la configuracion de CORS
     */
    @Bean
    public WebMvcConfigurer corsDelCanal(@Value("${negocio360.demo.cors-origenes:}") String[] origenes) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registro) {
                if (origenes.length == 0) {
                    return;
                }
                for (var ruta : new String[] {"/graphql", "/auth/**"}) {
                    registro.addMapping(ruta).allowedOriginPatterns(origenes)
                            .allowedMethods("GET", "POST", "DELETE", "PATCH").allowedHeaders("Authorization",
                                    "Content-Type");
                }
            }
        };
    }
}
