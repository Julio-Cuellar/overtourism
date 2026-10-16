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

/**
 * SG-06, SG-08 y SG-09 contra el motor real: profundidad 7, complejidad 100, 10 alias, 5 campos raiz y 10000
 * caracteres. Cada limite se prueba en su valor exacto (pasa) y en el siguiente (se rechaza), y el rechazo es un
 * error fijo que no revela el limite ni ejecuta ningun resolvedor.
 */
@SpringBootTest
class LimitesGraphqlTest {

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

    private static void rechazada(ExecutionResult resultado, String codigo) {
        assertThat(resultado.getErrors()).hasSize(1);
        var error = resultado.getErrors().get(0);
        assertThat(error.getMessage()).isEqualTo(codigo);
        assertThat(error.getExtensions()).containsEntry("codigo", codigo);
        assertThat(resultado.<Object>getData()).isNull();
    }

    private static void aceptada(ExecutionResult resultado) {
        assertThat(resultado.getErrors()).isEmpty();
        assertThat(resultado.<Map<String, Object>>getData()).isNotNull();
    }

    private static String anidada(int hijos) {
        return "{ arbolDePrueba " + "{ hijo ".repeat(hijos) + "{ valor }" + " }".repeat(hijos) + " }";
    }

    @Test
    @DisplayName("SG-06: profundidad 7 pasa; profundidad 8 se rechaza")
    void profundidad_enElTopeYUnoMas() {
        aceptada(ejecutar(anidada(5)));
        rechazada(ejecutar(anidada(6)), "CONSULTA_MUY_PROFUNDA");
    }

    @Test
    @DisplayName("SG-06: complejidad 100 pasa; complejidad 101 se rechaza")
    void complejidad_enElTopeYUnoMas() {
        aceptada(ejecutar("{ arbolDePrueba { " + "valor ".repeat(99) + "} }"));
        rechazada(ejecutar("{ arbolDePrueba { " + "valor ".repeat(100) + "} }"), "CONSULTA_MUY_COSTOSA");
    }

    @Test
    @DisplayName("SG-08: 10 alias pasan; 11 se rechazan")
    void alias_enElTopeYUnoMas() {
        aceptada(ejecutar("{ arbolDePrueba { " + alias(10) + " } }"));
        rechazada(ejecutar("{ arbolDePrueba { " + alias(11) + " } }"), "DEMASIADOS_ALIAS");
    }

    private static String alias(int cuantos) {
        var texto = new StringBuilder();
        for (var i = 1; i <= cuantos; i++) {
            texto.append("v").append(i).append(": valor ");
        }
        return texto.toString();
    }

    @Test
    @DisplayName("SG-08: 5 campos en la raiz pasan; 6 se rechazan, aunque cada uno lleve su alias")
    void camposRaiz_enElTopeYUnoMas() {
        aceptada(ejecutar(raiz(5)));
        rechazada(ejecutar(raiz(6)), "DEMASIADOS_CAMPOS_RAIZ");
    }

    private static String raiz(int cuantos) {
        var texto = new StringBuilder("{ ");
        for (var i = 1; i <= cuantos; i++) {
            texto.append("r").append(i).append(": arbolDePrueba { valor } ");
        }
        return texto.append("}").toString();
    }

    @Test
    @DisplayName("SG-08: la bomba de fragmentos se rechaza como demasiado costosa y rapido")
    void bombaDeFragmentos_seRechazaRapido() {
        var consulta = new StringBuilder("{ arbolDePrueba { ...F20 } } fragment F0 on NodoDePrueba { valor }");
        for (var i = 1; i <= 20; i++) {
            consulta.append(" fragment F").append(i).append(" on NodoDePrueba { ...F").append(i - 1).append(" ...F")
                    .append(i - 1).append(" }");
        }

        var resultado = assertTimeoutPreemptively(Duration.ofSeconds(3), () -> ejecutar(consulta.toString()));

        rechazada(resultado, "CONSULTA_MUY_COSTOSA");
    }

    @Test
    @DisplayName("SG-13: el rechazo no revela cuanto vale el limite ni que consulta lo provoco")
    void rechazo_noRevelaElLimiteNiLaConsulta() {
        var resultado = ejecutar(anidada(6));

        var texto = resultado.getErrors().toString();
        assertThat(texto).doesNotContain("7", "8", "hijo", "arbolDePrueba");
    }

    @Test
    @DisplayName("una consulta normal no se afecta: se ejecuta y devuelve sus datos")
    void consultaNormal_seEjecuta() {
        var resultado = ejecutar("{ arbolDePrueba { valor hijo { valor } } }");

        assertThat(resultado.getErrors()).isEmpty();
        assertThat(resultado.<Map<String, Map<String, Object>>>getData().get("arbolDePrueba"))
                .containsEntry("valor", "hoja").containsEntry("hijo", null);
    }

    @Test
    @DisplayName("SG-09: una consulta de mas de 10000 caracteres se rechaza sin analizarla; con 10000 pasa")
    void tamanoDeLaConsulta_enElTopeYUnoMas() {
        var base = "{ arbolDePrueba { valor } }";
        var exacta = base + " ".repeat(10_000 - base.length());
        assertThat(exacta.length()).isEqualTo(10_000);

        aceptada(ejecutar(exacta));
        rechazada(ejecutar(exacta + " "), "CONSULTA_MUY_LARGA");
    }

    @Test
    @DisplayName("SG-09: una consulta enorme no gasta tiempo: se rechaza por tamano antes de analizarla")
    void consultaEnorme_seRechazaAlInstante() {
        var enorme = "{ arbolDePrueba { valor } }" + " valor".repeat(10_000);

        var resultado = assertTimeoutPreemptively(Duration.ofMillis(500), () -> ejecutar(enorme));

        rechazada(resultado, "CONSULTA_MUY_LARGA");
    }

    @Test
    @DisplayName("un error de sintaxis sale como CONSULTA_INVALIDA (SG-13), distinto de los codigos de limites")
    void sintaxisInvalida_noSeConfundeConUnLimite() {
        var resultado = ejecutar("{ arbolDePrueba { ");

        assertThat(resultado.getErrors()).hasSize(1);
        assertThat(resultado.getErrors().get(0).getMessage()).isEqualTo("CONSULTA_INVALIDA")
                .isNotIn("CONSULTA_MUY_PROFUNDA", "CONSULTA_MUY_COSTOSA", "CONSULTA_MUY_LARGA", "DEMASIADOS_ALIAS",
                        "DEMASIADOS_CAMPOS_RAIZ");
    }
}
