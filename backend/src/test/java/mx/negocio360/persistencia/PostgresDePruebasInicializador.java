package mx.negocio360.persistencia;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Apunta cada contexto de pruebas al PostgreSQL de {@link PostgresDePruebas}. Lo registra
 * {@code META-INF/spring.factories}, así que aplica solo a todo {@code @SpringBootTest}. La aplicación usa el rol
 * sin privilegios; Flyway usa el dueño, que es quien puede crear esquemas y tablas.
 */
public final class PostgresDePruebasInicializador implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext contexto) {
        TestPropertyValues.of(
                "spring.datasource.url=" + PostgresDePruebas.url(),
                "spring.datasource.username=" + PostgresDePruebas.usuarioApp(),
                "spring.datasource.password=" + PostgresDePruebas.contrasenaApp(),
                "spring.flyway.url=" + PostgresDePruebas.url(),
                "spring.flyway.user=" + PostgresDePruebas.usuarioDueno(),
                "spring.flyway.password=" + PostgresDePruebas.contrasenaDueno(),
                "spring.flyway.placeholders.app_user=" + PostgresDePruebas.usuarioApp()).applyTo(contexto);
    }
}
