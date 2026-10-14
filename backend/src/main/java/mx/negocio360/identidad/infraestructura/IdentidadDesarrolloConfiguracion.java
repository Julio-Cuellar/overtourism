package mx.negocio360.identidad.infraestructura;

import mx.negocio360.identidad.web.DevCanalController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

/**
 * Piezas que solo existen para desarrollar, probar y hacer demos: canal simulado, repositorios y limite de tasa en
 * memoria, y el controlador que entrega el ultimo codigo. Solo se carga si se habilita expresamente con
 * {@code identidad.canal-simulado.habilitado=true} y nunca con el perfil {@code produccion} (AS-10, SI-13): olvidar
 * el perfil no abre nada. Los datos se pierden al reiniciar. El controlador se importa aqui para que tambien exista
 * cuando no hay escaneo de componentes (por ejemplo en las pruebas de contexto); con escaneo, Spring lo registra
 * una sola vez.
 */
@Configuration
@Profile("!produccion")
@ConditionalOnProperty(name = CanalSimulado.PROPIEDAD_DE_HABILITACION, havingValue = "true")
@Import(DevCanalController.class)
public class IdentidadDesarrolloConfiguracion {

    @Bean
    public CanalSimulado canalSimulado() {
        return new CanalSimulado();
    }

    @Bean
    public RepositorioCuentasEnMemoria repositorioCuentas() {
        return new RepositorioCuentasEnMemoria();
    }

    @Bean
    public RepositorioCodigosEnMemoria repositorioCodigos() {
        return new RepositorioCodigosEnMemoria();
    }

    @Bean
    public RepositorioSesionesEnMemoria repositorioSesiones() {
        return new RepositorioSesionesEnMemoria();
    }

    @Bean
    public LimiteDeTasaEnMemoria limiteDeTasa() {
        return new LimiteDeTasaEnMemoria();
    }

}
