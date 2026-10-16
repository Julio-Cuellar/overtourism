package mx.negocio360.negocios.aplicacion;

import java.time.Clock;
import java.time.Instant;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** El reloj movible de las pruebas de reseñas y demostracion (expiracion de tokens, limite por hora). */
@TestConfiguration
class RelojDeResenas {

    static final Instant INICIO = Instant.parse("2026-10-15T16:30:00Z");
    static final RelojDePrueba RELOJ = new RelojDePrueba(INICIO);

    @Bean
    @Primary
    Clock relojDeResenas() {
        return RELOJ;
    }
}
