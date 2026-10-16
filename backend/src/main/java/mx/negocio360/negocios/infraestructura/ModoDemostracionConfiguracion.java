package mx.negocio360.negocios.infraestructura;

import mx.negocio360.negocios.aplicacion.puertos.ModoDemostracion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * La bandera del modo demostracion (DM-03): {@code negocios.demo.activo}, apagada por omision. Si esta encendida y el perfil
 * activo es {@code produccion} la aplicacion NO arranca: olvidar apagarla no debe dejar reseñas sin cuenta en un servidor
 * real. Un valor que no es booleano tambien impide arrancar, en lugar de tomarse como apagado.
 */
@Configuration
public class ModoDemostracionConfiguracion {

    @Bean
    public ModoDemostracion modoDemostracion(@Value("${negocios.demo.activo:false}") boolean activo,
                                             Environment entorno) {
        if (activo && entorno.acceptsProfiles(Profiles.of("produccion"))) {
            throw new IllegalStateException("negocios.demo.activo esta encendido con el perfil produccion: el modo "
                    + "demostracion permite reseñar sin cuenta y no debe existir en produccion (DM-03)");
        }
        return () -> activo;
    }
}
