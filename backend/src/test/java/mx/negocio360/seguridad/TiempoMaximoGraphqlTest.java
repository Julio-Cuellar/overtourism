package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import graphql.ExecutionResult;
import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;

/** SG-09: el tiempo maximo de ejecucion. Aqui se baja a 300 ms para no esperar 10 s en cada corrida. */
@SpringBootTest(properties = "seguridad.graphql.tiempo-maximo=300ms")
class TiempoMaximoGraphqlTest {

    @Autowired
    private WebGraphQlHandler manejador;

    @BeforeEach
    void entrarConSesion() {
        // Toda operacion de la raiz exige sesion con negocio activo (SG-03); estas pruebas miden otra cosa.
        SesionDePrueba.entrar();
    }

    @AfterEach
    void salir() {
        SesionDePrueba.salir();
    }

    private ExecutionResult ejecutar(String consulta) {
        var peticion = new WebGraphQlRequest(URI.create("/graphql"), new HttpHeaders(), new LinkedMultiValueMap<>(),
                null, SesionDePrueba.atributos(), Map.of("query", consulta), "prueba", Locale.ROOT);
        return manejador.handleRequest(peticion).block().getExecutionResult();
    }

    @Test
    @DisplayName("una consulta que tarda mas del maximo recibe TIEMPO_AGOTADO y la respuesta no espera a que termine")
    void consultaLenta_recibeTiempoAgotado() {
        // assertTimeoutPreemptively corre en otro hilo y la sesion de prueba es del hilo de la prueba: se entra ahi.
        var resultado = assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            SesionDePrueba.entrar();
            try {
                return ejecutar("{ lentoDePrueba(milisegundos: 5000) }");
            } finally {
                SesionDePrueba.salir();
            }
        });

        assertThat(resultado.getErrors()).hasSize(1);
        assertThat(resultado.getErrors().get(0).getMessage()).isEqualTo("TIEMPO_AGOTADO");
        assertThat(resultado.getErrors().get(0).getExtensions()).containsEntry("codigo", "TIEMPO_AGOTADO");
        assertThat(resultado.<Object>getData()).isNull();
    }

    @Test
    @DisplayName("una consulta que termina dentro del maximo responde normal")
    void consultaRapida_respondeNormal() {
        var resultado = ejecutar("{ lentoDePrueba(milisegundos: 10) }");

        assertThat(resultado.getErrors()).isEmpty();
        assertThat(resultado.<Map<String, Object>>getData()).containsEntry("lentoDePrueba", "listo");
    }
}
