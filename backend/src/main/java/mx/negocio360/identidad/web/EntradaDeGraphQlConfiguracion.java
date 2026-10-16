package mx.negocio360.identidad.web;

import java.time.Clock;
import mx.negocio360.identidad.IdentidadApi;
import mx.negocio360.identidad.aplicacion.puertos.LimiteDeTasa;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Pone el filtro de entrada de {@code /graphql} (IDN-4). Los topes se ajustan con {@code plataforma.entrada.*}:
 * peticiones por minuto por IP (120) y por dispositivo (60), y bytes del cuerpo (16 384).
 */
@Configuration(proxyBeanMethods = false)
public class EntradaDeGraphQlConfiguracion {

    @Bean
    public FilterRegistrationBean<FiltroDeEntrada> filtroDeEntrada(
            IdentidadApi identidad, LimiteDeTasa tasa, Clock reloj,
            @Value("${plataforma.entrada.por-ip-por-minuto:120}") int porIp,
            @Value("${plataforma.entrada.por-dispositivo-por-minuto:60}") int porDispositivo,
            @Value("${plataforma.entrada.cuerpo-maximo-bytes:16384}") int cuerpo) {
        var registro = new FilterRegistrationBean<>(new FiltroDeEntrada(identidad, tasa, reloj,
                new FiltroDeEntrada.Limites(porIp, porDispositivo, cuerpo)));
        registro.addUrlPatterns("/graphql");
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registro;
    }
}
