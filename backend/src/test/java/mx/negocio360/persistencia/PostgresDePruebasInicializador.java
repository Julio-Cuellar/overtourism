package mx.negocio360.persistencia;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Apunta cada contexto de pruebas al PostgreSQL de {@link PostgresDePruebas}. Lo registra
 * {@code META-INF/spring.factories}, así que aplica solo a todo {@code @SpringBootTest}. La aplicación usa el rol
 * sin privilegios; Flyway usa el dueño, que es quien puede crear esquemas y tablas.
 *
 * <p>Una prueba que no usa la base (por ejemplo la rebanada de GraphQL del canal) lo apaga con
 * {@value #SIN_BASE}{@code =true} y no necesita Docker.
 */
public final class PostgresDePruebasInicializador implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    /** Propiedad de prueba para no levantar PostgreSQL en un contexto que no lo usa. */
    public static final String SIN_BASE = "negocio360.pruebas.sin-base";

    @Override
    public void initialize(ConfigurableApplicationContext contexto) {
        if (contexto.getEnvironment().getProperty(SIN_BASE, Boolean.class, false)) {
            return;
        }
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
