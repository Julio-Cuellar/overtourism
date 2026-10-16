package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import mx.negocio360.compartido.seguridad.ConfirmacionReforzada;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;

/**
 * SG-19: el contrato con IDN-3.2. Cuando aparece un bean {@link ConfirmacionReforzada}, las operaciones sensibles se
 * abren solo para lo que ese bean confirme; mientras no exista, todas se niegan (lo prueba AutorizacionDeOperacionesTest).
 */
@SpringBootTest
@Import(ConfirmacionReforzadaTest.ConfirmaSoloUnaVez.class)
class ConfirmacionReforzadaTest {

    /** Lo que se le pregunto a la confirmacion: la cuenta, el negocio y el permiso. */
    static final AtomicReference<String> PREGUNTADO = new AtomicReference<>();
    static final AtomicReference<Boolean> RESPUESTA = new AtomicReference<>(false);

    @TestConfiguration
    static class ConfirmaSoloUnaVez {
        @Bean
        ConfirmacionReforzada confirmacionReforzada() {
            return (UUID cuenta, UUID negocio, String permiso) -> {
                PREGUNTADO.set(cuenta + "|" + negocio + "|" + permiso);
                return RESPUESTA.get();
            };
        }
    }

    @Autowired
    private WebGraphQlHandler manejador;

    @AfterEach
    void limpiar() {
        SesionDePrueba.salir();
        PREGUNTADO.set(null);
        RESPUESTA.set(false);
    }

    private java.util.List<String> codigos(String consulta) {
        var peticion = new WebGraphQlRequest(URI.create("/graphql"), new HttpHeaders(), new LinkedMultiValueMap<>(),
                null, SesionDePrueba.atributos(), Map.of("query", consulta), "prueba", Locale.ROOT);
        return manejador.handleRequest(peticion).block().getExecutionResult().getErrors().stream()
                .map(e -> (String) e.getExtensions().get("codigo")).toList();
    }

    @Test
    @DisplayName("si la confirmacion reforzada confirma, la operacion sensible pasa")
    void confirmada_pasa() {
        SesionDePrueba.entrar("clientes.cliente.anonimizar");
        RESPUESTA.set(true);

        assertThat(codigos("{ sensibleDePrueba }")).isEmpty();
        assertThat(PREGUNTADO).hasValue(SesionDePrueba.CUENTA + "|" + SesionDePrueba.NEGOCIO + "|clientes.cliente.anonimizar");
    }

    @Test
    @DisplayName("si no confirma, CONFIRMACION_REQUERIDA")
    void noConfirmada_seNiega() {
        SesionDePrueba.entrar("clientes.cliente.anonimizar");

        assertThat(codigos("{ sensibleDePrueba }")).containsExactly("CONFIRMACION_REQUERIDA");
    }

    @Test
    @DisplayName("sin el permiso ni se pregunta a la confirmacion: primero el permiso")
    void sinPermiso_noSePregunta() {
        SesionDePrueba.entrar("clientes.sugerir");
        RESPUESTA.set(true);

        assertThat(codigos("{ sensibleDePrueba }")).containsExactly("PERMISO_DENEGADO");
        assertThat(PREGUNTADO.get()).isNull();
    }

    @Test
    @DisplayName("una operacion no sensible nunca consulta a la confirmacion")
    void noSensible_noSePregunta() {
        SesionDePrueba.entrar("clientes.sugerir");

        assertThat(codigos("{ soloConPermisoDePrueba }")).isEmpty();
        assertThat(PREGUNTADO.get()).isNull();
    }
}
