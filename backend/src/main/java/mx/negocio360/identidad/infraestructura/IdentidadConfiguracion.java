package mx.negocio360.identidad.infraestructura;

import java.time.Clock;
import mx.negocio360.identidad.IdentidadApi;
import mx.negocio360.identidad.IdentidadServicio;
import mx.negocio360.identidad.aplicacion.PoliticaDeAcceso;
import mx.negocio360.identidad.aplicacion.puertos.CanalDeCodigos;
import mx.negocio360.identidad.aplicacion.puertos.LimiteDeTasa;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioCodigos;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioCuentas;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioSesiones;
import mx.negocio360.identidad.web.CabecerasDeSeguridad;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cableado de Identidad que existe en todos los perfiles. Necesita un canal, repositorios y un limite de tasa: en
 * desarrollo y demo los da {@link IdentidadDesarrolloConfiguracion}; en produccion todavia no hay implementaciones
 * (Postgres es IDN-1.1 y el canal real IDN-DOC.1), asi que la aplicacion no arranca, a proposito (AS-10).
 */
@Configuration
@EnableConfigurationProperties(PropiedadesDePolitica.class)
public class IdentidadConfiguracion {

    @Bean
    public PoliticaDeAcceso politicaDeAcceso(PropiedadesDePolitica propiedades) {
        return propiedades.comoPolitica();
    }

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock relojDelSistema() {
        return Clock.systemUTC();
    }

    /** SI-17: cabeceras de no-cache en todo lo que sirve el acceso. */
    @Bean
    public FilterRegistrationBean<CabecerasDeSeguridad> cabecerasDeSeguridad() {
        var registro = new FilterRegistrationBean<>(new CabecerasDeSeguridad());
        registro.addUrlPatterns("/auth/*", "/cuenta", "/dev/*");
        return registro;
    }

    @Bean
    public IdentidadApi identidadApi(RepositorioCuentas cuentas, RepositorioCodigos codigos, RepositorioSesiones sesiones,
                                     CanalDeCodigos canal, LimiteDeTasa limite, Clock reloj, PoliticaDeAcceso politica) {
        return new IdentidadServicio(cuentas, codigos, sesiones, canal, limite, reloj, politica);
    }
}
