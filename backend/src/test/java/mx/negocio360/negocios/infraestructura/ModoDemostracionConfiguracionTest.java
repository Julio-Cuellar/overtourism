package mx.negocio360.negocios.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import mx.negocio360.negocios.aplicacion.puertos.ModoDemostracion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** DM-03: la aplicacion no arranca con el perfil de produccion si el modo demostracion esta encendido. */
class ModoDemostracionConfiguracionTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(ModoDemostracionConfiguracion.class);

    @Test
    @DisplayName("por omision esta apagado")
    void porOmision_apagado() {
        contexto.run(c -> assertThat(c.getBean(ModoDemostracion.class).activo()).isFalse());
    }

    @Test
    @DisplayName("encendido en desarrollo funciona")
    void encendido_enDesarrollo() {
        contexto.withPropertyValues("negocios.demo.activo=true")
                .run(c -> assertThat(c.getBean(ModoDemostracion.class).activo()).isTrue());
    }

    @Test
    @DisplayName("encendido con el perfil produccion, la aplicacion no arranca")
    void encendido_enProduccion_noArranca() {
        contexto.withPropertyValues("negocios.demo.activo=true", "spring.profiles.active=produccion")
                .run(c -> assertThat(c).hasFailed().getFailure().rootCause().isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("produccion"));
    }

    @Test
    @DisplayName("apagado con el perfil produccion arranca normal")
    void apagado_enProduccion_arranca() {
        contexto.withPropertyValues("spring.profiles.active=produccion")
                .run(c -> assertThat(c.getBean(ModoDemostracion.class).activo()).isFalse());
    }

    @Test
    @DisplayName("un valor que no es booleano se rechaza en lugar de tomarse como apagado")
    void valorRaro_seRechaza() {
        contexto.withPropertyValues("negocios.demo.activo=tal-vez").run(c -> assertThat(c).hasFailed());
    }
}
