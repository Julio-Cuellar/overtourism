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
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;

/**
 * SG-13: lo que falla en un resolvedor sale como un codigo y un texto fijos; el mensaje de la excepcion, que puede
 * traer SQL, rutas o datos personales, no sale nunca. Lo inesperado recibe una referencia para encontrarlo en el
 * registro, y el registro tampoco lleva el mensaje.
 */
@SpringBootTest
class ResolvedorDeErroresGraphqlTest {

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

    private static void sinDatosDelError(ExecutionResult resultado) {
        assertThat(resultado.toSpecification().toString()).doesNotContain("secreto", "telefono", "2221234567", "cliente",
                "IllegalState", "anonimizar", "inventario.articulo", "password");
    }

    @Test
    @DisplayName("una excepcion inesperada sale como ERROR_INTERNO con referencia, sin el mensaje ni la clase")
    void excepcionInesperada_salePorCodigoYReferencia() {
        var resultado = ejecutar("{ fallaInternaDePrueba }");

        assertThat(resultado.getErrors()).hasSize(1);
        var error = resultado.getErrors().get(0);
        assertThat(error.getMessage()).isEqualTo("ERROR_INTERNO");
        assertThat(error.getErrorType()).isEqualTo(ErrorType.INTERNAL_ERROR);
        assertThat(error.getExtensions()).containsEntry("codigo", "ERROR_INTERNO");
        assertThat((String) error.getExtensions().get("referencia")).matches("[0-9a-f]{12}");
        assertThat(error.getPath()).containsExactly("fallaInternaDePrueba");
        sinDatosDelError(resultado);
    }

    @Test
    @DisplayName("SG-20: el registro del error lleva la referencia y la clase, pero no el mensaje de la excepcion")
    void excepcionInesperada_elRegistroNoLlevaElMensaje() {
        try (var captura = new CapturaDeLogs("mx.negocio360.compartido.graphql.ResolvedorDeErroresGraphql")) {
            var resultado = ejecutar("{ fallaInternaDePrueba }");
            String referencia = (String) resultado.getErrors().get(0).getExtensions().get("referencia");

            var lineas = captura.lineas();
            assertThat(lineas).hasSize(1);
            assertThat(lineas.get(0)).contains(referencia).contains("IllegalStateException");
            assertThat(lineas.get(0)).doesNotContain("secreto", "telefono", "2221234567", "password", "SELECT");
        }
    }

    @Test
    @DisplayName("PermisoDenegadoException sale como PERMISO_DENEGADO sin decir que permiso falto")
    void permisoDenegado_saleComoCodigoFijo() {
        var resultado = ejecutar("{ permisoDenegadoDePrueba }");

        var error = resultado.getErrors().get(0);
        assertThat(error.getMessage()).isEqualTo("PERMISO_DENEGADO");
        assertThat(error.getErrorType()).isEqualTo(ErrorType.FORBIDDEN);
        assertThat(error.getExtensions()).containsEntry("codigo", "PERMISO_DENEGADO").doesNotContainKey("referencia");
        sinDatosDelError(resultado);
    }

    @Test
    @DisplayName("una falta de sesion sale como NO_AUTENTICADO")
    void sinSesion_saleComoNoAutenticado() {
        var error = ejecutar("{ noAutenticadoDePrueba }").getErrors().get(0);

        assertThat(error.getMessage()).isEqualTo("NO_AUTENTICADO");
        assertThat(error.getErrorType()).isEqualTo(ErrorType.UNAUTHORIZED);
    }

    @Test
    @DisplayName("una sesion sin negocio activo sale como SIN_NEGOCIO_ACTIVO, distinto de NO_AUTENTICADO")
    void sinNegocio_saleComoSinNegocioActivo() {
        var error = ejecutar("{ sinNegocioDePrueba }").getErrors().get(0);

        assertThat(error.getMessage()).isEqualTo("SIN_NEGOCIO_ACTIVO");
        assertThat(error.getErrorType()).isEqualTo(ErrorType.UNAUTHORIZED);
        assertThat(error.getExtensions()).containsEntry("codigo", "SIN_NEGOCIO_ACTIVO");
    }

    @Test
    @DisplayName("un argumento que no se puede leer sale como CONSULTA_INVALIDA, no como error interno, sin repetirlo")
    void argumentoIlegible_saleComoConsultaInvalida() {
        var resultado = ejecutar("{ uuidDePrueba(id: \"no-es-un-uuid-secreto\") }");

        var error = resultado.getErrors().get(0);
        assertThat(error.getMessage()).isEqualTo("CONSULTA_INVALIDA");
        assertThat(error.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(error.getExtensions()).containsEntry("codigo", "CONSULTA_INVALIDA").doesNotContainKey("referencia");
        assertThat(resultado.getErrors().toString()).doesNotContain("secreto").doesNotContain("Exception");
    }

    @Test
    @DisplayName("la consulta invalida deja una linea de registro con la clase de la causa y sin el valor, para no esconder fallos")
    void argumentoIlegible_dejaRastroSinElValor() {
        try (var captura = new CapturaDeLogs("mx.negocio360.compartido.graphql.ResolvedorDeErroresGraphql")) {
            ejecutar("{ uuidDePrueba(id: \"no-es-un-uuid-secreto\") }");

            var lineas = captura.lineas();
            assertThat(lineas).hasSize(1);
            assertThat(lineas.get(0)).contains("CONSULTA_INVALIDA").contains("uuidDePrueba");
            assertThat(lineas.get(0)).doesNotContain("secreto", "no-es-un-uuid");
        }
    }

    @Test
    @DisplayName("un argumento bien formado pasa")
    void argumentoLegible_pasa() {
        var resultado = ejecutar("{ uuidDePrueba(id: \"0192f39f-1111-7000-8000-00000000a001\") }");

        assertThat(resultado.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("un error que llega envuelto de forma asincrona se desenvuelve y sale con su codigo")
    void errorAsincrono_seDesenvuelve() {
        var resultado = ejecutar("{ asincronoQueFallaDePrueba }");

        assertThat(resultado.getErrors().get(0).getMessage()).isEqualTo("PERMISO_DENEGADO");
        sinDatosDelError(resultado);
    }

    @Test
    @DisplayName("un modulo declara como se traducen sus errores: sale su codigo, su texto fijo y el id del recurso")
    void errorDeNegocio_loTraduceElTraductorDelModulo() {
        var resultado = ejecutar("{ errorDeNegocioDePrueba }");

        var error = resultado.getErrors().get(0);
        assertThat(error.getMessage()).isEqualTo("Ya existe ese dato.");
        assertThat(error.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(error.getExtensions()).containsEntry("codigo", "DUPLICADO_DE_PRUEBA")
                .containsEntry("recursoId", ErrorDeNegocioDePrueba.RECURSO.toString());
        sinDatosDelError(resultado);
    }

    @Test
    @DisplayName("un null en un campo que no admite null sale como ERROR_INTERNO, sin el texto del motor")
    void nuloEnCampoNoNulo_saleComoErrorInterno() {
        var resultado = ejecutar("{ nuloDePrueba }");

        assertThat(resultado.getErrors()).hasSize(1);
        assertThat(resultado.getErrors().get(0).getMessage()).isEqualTo("ERROR_INTERNO");
        assertThat(resultado.toSpecification().toString()).doesNotContain("non null", "nuloDePrueba was");
    }

    @Test
    @DisplayName("lo que sale bien sigue saliendo: un campo falla y el otro devuelve su dato")
    void resultadoParcial_conservaLoQueSalioBien() {
        var resultado = ejecutar("{ arbolDePrueba { valor } fallaInternaDePrueba }");

        assertThat(resultado.getErrors()).hasSize(1);
        assertThat(resultado.<Map<String, Object>>getData()).containsKey("arbolDePrueba");
        assertThat(((Map<?, ?>) resultado.<Map<String, Object>>getData().get("arbolDePrueba")).get("valor"))
                .isEqualTo("hoja");
    }
}
