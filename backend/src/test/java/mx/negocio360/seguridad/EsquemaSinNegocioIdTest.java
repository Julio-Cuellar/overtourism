package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLInputObjectType;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.execution.GraphQlSource;

/**
 * SG-02: ningun tipo, campo ni argumento del esquema permite que el cliente diga a que negocio pertenece. Las unicas
 * apariciones de {@code negocioId} son las de la superficie publica del turista (donde el negocio es el objeto consultado o
 * calificado, no el contexto del que pregunta: D-98 y D-99) y las de las salidas del alta de negocio de la propia cuenta.
 * Cualquier otra rompe la prueba: agregar una a la lista es una decision que alguien tiene que ver.
 */
@SpringBootTest
class EsquemaSinNegocioIdTest {

    private static final Pattern NEGOCIO_ID = Pattern.compile("(?i)negocio_?id");

    @Autowired
    private GraphQlSource fuente;

    @Test
    @DisplayName("el negocio sale solo del contexto autenticado: negocioId solo aparece donde esta documentado")
    void esquema_noTieneNegocioIdEnCamposNiArgumentos() {
        var encontrados = new ArrayList<String>();
        for (var tipo : fuente.schema().getAllTypesAsList()) {
            if (tipo.getName().startsWith("__")) {
                continue;
            }
            if (tipo instanceof GraphQLFieldsContainer contenedor) {
                for (var campo : contenedor.getFieldDefinitions()) {
                    if (NEGOCIO_ID.matcher(campo.getName()).matches()) {
                        encontrados.add(tipo.getName() + "." + campo.getName());
                    }
                    for (var argumento : campo.getArguments()) {
                        if (NEGOCIO_ID.matcher(argumento.getName()).matches()) {
                            encontrados.add(tipo.getName() + "." + campo.getName() + "(" + argumento.getName() + ")");
                        }
                    }
                }
            }
            if (tipo instanceof GraphQLInputObjectType entrada) {
                for (var campo : entrada.getFieldDefinitions()) {
                    if (NEGOCIO_ID.matcher(campo.getName()).matches()) {
                        encontrados.add(tipo.getName() + "." + campo.getName());
                    }
                }
            }
        }

        assertThat(encontrados).containsExactlyInAnyOrder(
                // Entradas publicas: el negocio es el objeto consultado o calificado por el turista
                "Query.resenasDeNegocio(negocioId)", "PublicarResenaInput.negocioId",
                // Menu publico del turista (D-170): el negocio es el objeto consultado y el tipo de salida devuelve su id
                "Query.menuPublico(negocioId)", "RestMenuPublico.negocioId",
                // Mesa por QR (R6): el negocio sale de la mesa que resuelve el token; el tipo de salida devuelve su id para que la
                // pagina pida el nombre y las resenas a Negocios. Ninguna entrada lo recibe.
                "RestMesaPublica.negocioId",
                // Salidas del alta de negocio de la propia cuenta: devuelven el id del negocio creado, no lo reciben
                "AltaDeNegocio.negocioId", "AvisoDeAlta.negocioId");
    }
}
