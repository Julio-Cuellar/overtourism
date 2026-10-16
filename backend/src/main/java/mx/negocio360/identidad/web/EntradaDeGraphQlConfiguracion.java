package mx.negocio360.identidad.web;

import java.time.Clock;
import mx.negocio360.identidad.IdentidadApi;
import mx.negocio360.identidad.aplicacion.puertos.LimiteDeTasa;
import mx.negocio360.compartido.seguridad.LimitadorDeTasa;
import mx.negocio360.compartido.seguridad.SeguridadHttpConfiguracion;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Pone el filtro de entrada de {@code /graphql} (IDN-4). Los topes se ajustan con {@code plataforma.entrada.*}:
 * peticiones por minuto por IP (120) y por dispositivo (60). El cuerpo maximo es {@code seguridad.http.cuerpo-maximo-bytes}
 * (64 KB), el mismo de la puerta HTTP y del mensaje de WebSocket. Suma los controles de la plataforma: fallos de autenticacion
 * por IP y peticiones por cuenta (SG-10).
 */
@Configuration(proxyBeanMethods = false)
public class EntradaDeGraphQlConfiguracion {

    @Bean
    public FilterRegistrationBean<FiltroDeEntrada> filtroDeEntrada(
            IdentidadApi identidad, LimiteDeTasa tasa, Clock reloj,
            @Value("${plataforma.entrada.por-ip-por-minuto:120}") int porIp,
            @Value("${plataforma.entrada.por-dispositivo-por-minuto:60}") int porDispositivo,
            @Value("${seguridad.http.cuerpo-maximo-bytes:65536}") int cuerpo,
            @Qualifier(SeguridadHttpConfiguracion.LIMITE_DE_FALLOS_POR_IP) LimitadorDeTasa fallosPorIp,
            @Qualifier(SeguridadHttpConfiguracion.LIMITE_DE_PETICIONES_POR_CUENTA) LimitadorDeTasa peticionesPorCuenta) {
        var registro = new FilterRegistrationBean<>(new FiltroDeEntrada(identidad, tasa, reloj,
                new FiltroDeEntrada.Limites(porIp, porDispositivo, cuerpo),
                new FiltroDeEntrada.Controles(fallosPorIp, peticionesPorCuenta)));
        registro.addUrlPatterns("/graphql");
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registro;
    }
}
