package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import graphql.language.Document;
import graphql.language.FragmentDefinition;
import graphql.language.OperationDefinition;
import graphql.parser.Parser;
import java.time.Duration;
import java.util.stream.Collectors;
import mx.negocio360.compartido.graphql.MedidaDeConsulta;
import mx.negocio360.compartido.graphql.MedidorDeConsulta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SG-06 y SG-08: cuanto cuesta una consulta, medido sobre el documento ya analizado. Cada fragmento se mide una sola
 * vez, asi que una "bomba" de fragmentos (cada nivel cita dos veces al anterior) se mide en tiempo lineal y no
 * exponencial; el costo se satura en un tope en lugar de desbordarse.
 */
class MedidorDeConsultaTest {

    private static MedidaDeConsulta medir(String consulta) {
        Document documento = new Parser().parseDocument(consulta);
        var operacion = documento.getDefinitionsOfType(OperationDefinition.class).get(0);
        var fragmentos = documento.getDefinitionsOfType(FragmentDefinition.class).stream()
                .collect(Collectors.toMap(FragmentDefinition::getName, f -> f));
        return MedidorDeConsulta.medir(operacion, fragmentos);
    }

    @Test
    @DisplayName("la profundidad cuenta los campos de la rama mas larga; los de la raiz valen 1")
    void profundidad_esLaDeLaRamaMasLarga() {
        assertThat(medir("{ a }").profundidad()).isEqualTo(1);
        assertThat(medir("{ a { b { c } } d { e } }").profundidad()).isEqualTo(3);
    }

    @Test
    @DisplayName("la complejidad suma 1 por campo mas la de sus hijos: { a { b c } d } = 4")
    void complejidad_sumaUnoPorCampoMasLosHijos() {
        assertThat(medir("{ a { b c } d }").complejidad()).isEqualTo(4);
        assertThat(medir("{ __typename }").complejidad()).isEqualTo(1);
    }

    @Test
    @DisplayName("los alias se cuentan donde esten, tambien dentro de otros campos")
    void alias_seCuentanEnTodoElArbol() {
        var medida = medir("{ x: a y: a { z: b w } }");

        assertThat(medida.alias()).isEqualTo(3);
        assertThat(medida.camposRaiz()).isEqualTo(2);
    }

    @Test
    @DisplayName("los campos de la raiz incluyen los que llegan por fragmentos en linea o con nombre")
    void camposRaiz_incluyenFragmentos() {
        assertThat(medir("{ a b c }").camposRaiz()).isEqualTo(3);
        assertThat(medir("{ ... on Query { a b } c }").camposRaiz()).isEqualTo(3);
        assertThat(medir("{ ...F c } fragment F on Query { a b }").camposRaiz()).isEqualTo(3);
    }

    @Test
    @DisplayName("un fragmento citado dos veces cuesta dos veces; no suma profundidad")
    void fragmento_secuentaPorCadaUso() {
        var medida = medir("{ a { ...F ...F } } fragment F on T { b c }");

        assertThat(medida.complejidad()).isEqualTo(5);
        assertThat(medida.profundidad()).isEqualTo(2);
    }

    @Test
    @DisplayName("una bomba de 40 fragmentos se mide al instante y el costo se satura, sin desbordar")
    void bombaDeFragmentos_seMideRapidoYSeSatura() {
        var consulta = new StringBuilder("{ a { ...F40 } } fragment F0 on T { valor }");
        for (var i = 1; i <= 40; i++) {
            consulta.append(" fragment F").append(i).append(" on T { ...F").append(i - 1).append(" ...F")
                    .append(i - 1).append(" }");
        }

        var medida = assertTimeoutPreemptively(Duration.ofMillis(500), () -> medir(consulta.toString()));

        assertThat(medida.complejidad()).isEqualTo(MedidorDeConsulta.TOPE);
        assertThat(medida.profundidad()).isEqualTo(2);
    }

    @Test
    @DisplayName("un ciclo de fragmentos (que la validacion ya rechaza) no cuelga ni revienta la pila")
    void cicloDeFragmentos_noCuelga() {
        var medida = assertTimeoutPreemptively(Duration.ofMillis(500),
                () -> medir("{ a { ...F } } fragment F on T { b ...G } fragment G on T { c ...F }"));

        assertThat(medida.complejidad()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("una mutacion o suscripcion se mide igual que una consulta")
    void mutacionYSuscripcion_seMidenIgual() {
        assertThat(medir("mutation { prepararComando(input: {}) { token resumen } }").complejidad()).isEqualTo(3);
        assertThat(medir("subscription { alertas { id } }").profundidad()).isEqualTo(2);
    }

    @Test
    @DisplayName("un fragmento que no existe no cuenta (la validacion ya lo rechazo)")
    void fragmentoInexistente_noCuenta() {
        assertThat(medir("{ a ...Nada }").complejidad()).isEqualTo(1);
    }
}
