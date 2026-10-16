package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.ExecutionResult;
import java.net.URI;
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
 * SG-13 y SG-12: los errores que produce el motor antes de ejecutar (sintaxis, validacion, variables) dicen en sus
 * textos que campos y tipos existen o que valor se mando. Con la introspeccion cerrada no deben darlo por otra via: todo
 * sale como CONSULTA_INVALIDA, sin posiciones ni texto. Solo pasan tal cual los errores que lleva nuestro propio codigo.
 */
@SpringBootTest
class EnmascaradoDeErroresTest {

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

    private ExecutionResult ejecutar(String consulta, Map<String, Object> variables) {
        var cuerpo = Map.<String, Object>of("query", consulta, "variables", variables);
        var peticion = new WebGraphQlRequest(URI.create("/graphql"), new HttpHeaders(), new LinkedMultiValueMap<>(),
                null, SesionDePrueba.atributos(), cuerpo, "prueba", Locale.ROOT);
        return manejador.handleRequest(peticion).block().getExecutionResult();
    }

    private ExecutionResult ejecutar(String consulta) {
        return ejecutar(consulta, Map.of());
    }

    private static void invalida(ExecutionResult resultado, String... pistasQueNoDebenSalir) {
        assertThat(resultado.getErrors()).hasSize(1);
        var error = resultado.getErrors().get(0);
        assertThat(error.getMessage()).isEqualTo("CONSULTA_INVALIDA");
        assertThat(error.getExtensions()).containsEntry("codigo", "CONSULTA_INVALIDA");
        assertThat(error.getLocations()).isNullOrEmpty();
        assertThat(resultado.toSpecification().toString()).doesNotContain(pistasQueNoDebenSalir);
    }

    @Test
    @DisplayName("un campo que no existe no dice en que tipo se busco ni sugiere otros")
    void campoInexistente_noRevelaElEsquema() {
        invalida(ejecutar("{ camposecreto }"), "camposecreto", "Query", "undefined", "Validation");
    }

    @Test
    @DisplayName("un argumento que no existe no revela el campo ni sus argumentos")
    void argumentoInexistente_noRevelaNada() {
        invalida(ejecutar("{ arbolDePrueba(negocioId: \"x\") { valor } }"), "negocioId", "arbolDePrueba", "argument");
    }

    @Test
    @DisplayName("un error de sintaxis no repite el texto de la consulta")
    void errorDeSintaxis_noRepiteLaConsulta() {
        invalida(ejecutar("{ arbolDePrueba { valor $tokenraro"), "tokenraro", "Invalid Syntax", "offending");
    }

    @Test
    @DisplayName("una variable con un valor de otro tipo no repite el valor que se mando")
    void variableConValorInvalido_noRepiteElValor() {
        invalida(ejecutar("query($n: Int!) { lentoDePrueba(milisegundos: $n) }", Map.of("n", "valor-sensible-7")),
                "valor-sensible-7", "Variable", "Int");
    }

    @Test
    @DisplayName("una variable que falta tampoco da pistas")
    void variableFaltante_noDaPistas() {
        invalida(ejecutar("query($n: Int!) { lentoDePrueba(milisegundos: $n) }"), "Variable", "Int", "$n");
    }

    @Test
    @DisplayName("un fragmento que no existe no revela nombres")
    void fragmentoInexistente_noRevelaNombres() {
        invalida(ejecutar("{ arbolDePrueba { ...FragmentoSecreto } }"), "FragmentoSecreto", "Fragment");
    }

    @Test
    @DisplayName("los errores de limites son nuestros y pasan tal cual, con su codigo")
    void erroresPropios_pasanTalCual() {
        var resultado = ejecutar("{ arbolDePrueba { hijo { hijo { hijo { hijo { hijo { hijo { valor } } } } } } } }");

        assertThat(resultado.getErrors()).hasSize(1);
        assertThat(resultado.getErrors().get(0).getMessage()).isEqualTo("CONSULTA_MUY_PROFUNDA");
    }

    @Test
    @DisplayName("una consulta valida no se toca")
    void consultaValida_noSeToca() {
        var resultado = ejecutar("{ arbolDePrueba { valor } }");

        assertThat(resultado.getErrors()).isEmpty();
    }
}
