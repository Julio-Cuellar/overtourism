package mx.negocio360.canal.demo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Modulos vecinos <b>simulados</b> para la demo: Catalogo, Pasaporte y permisos. Solo con
 * {@code negocio360.demo.habilitada=true} y nunca con el perfil {@code produccion}.
 */
@Configuration(proxyBeanMethods = false)
@Profile("!produccion")
@ConditionalOnProperty(name = "negocio360.demo.habilitada", havingValue = "true")
public class DemoConfiguracion {

    /**
     * Catalogo de la demo.
     *
     * @return productos simulados
     */
    @Bean
    public CatalogoSimulado catalogoSimulado() {
        return new CatalogoSimulado();
    }

    /**
     * Pasaporte de la demo.
     *
     * @return cupones simulados
     */
    @Bean
    public CuponesSimulados cuponesSimulados() {
        return new CuponesSimulados();
    }

    /**
     * Permisos de la demo.
     *
     * @return permisos simulados para Ventas y Caja
     */
    @Bean
    public PermisosSimulados permisosSimulados() {
        return new PermisosSimulados();
    }
}
