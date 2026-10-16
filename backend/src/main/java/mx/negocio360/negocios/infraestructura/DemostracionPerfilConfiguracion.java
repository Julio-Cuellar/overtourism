package mx.negocio360.negocios.infraestructura;

import mx.negocio360.negocios.DemostracionApi;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Con el perfil {@code demo} la aplicacion siembra los negocios simulados al arrancar (DM-02). La siembra es idempotente.
 * Si el modo demostracion esta apagado, la aplicacion no arranca: el perfil {@code demo} sin la bandera es un error de
 * configuracion que debe verse, no una demo vacia.
 */
@Configuration
@Profile("demo & !produccion")
public class DemostracionPerfilConfiguracion {

    @Bean
    public ApplicationRunner sembrarAlArrancar(DemostracionApi demostracion) {
        return argumentos -> demostracion.sembrar();
    }
}
