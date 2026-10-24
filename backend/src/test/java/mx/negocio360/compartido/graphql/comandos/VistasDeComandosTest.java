package mx.negocio360.compartido.graphql.comandos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.comandos.ResultadoComando;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Las vistas que el despachador entrega a GraphQL: datos ordenados, fallos como resultado y token que no se imprime. */
class VistasDeComandosTest {

    @Test
    @DisplayName("ResultadoVista.de ordena los datos por clave y marca ok solo con el codigo OK")
    void resultado_ordenaDatos() {
        var id = UUID.randomUUID();
        var resultado = ResultadoComando.ok("Venta registrada.", id, Map.of("total", "66.00", "cambio", "34.00",
                "folio", "1"));

        var vista = ResultadoVista.de(resultado);

        assertThat(vista.ok()).isTrue();
        assertThat(vista.recursoId()).isEqualTo(id);
        assertThat(vista.datos()).extracting(DatoVista::clave).containsExactly("cambio", "folio", "total");
        assertThat(ResultadoVista.de(new ResultadoComando("OTRO", "x", id, Map.of())).ok()).isFalse();
    }

    @Test
    @DisplayName("ResultadoVista.fallo no trae recurso ni datos, y los datos nunca son nulos")
    void resultado_fallo() {
        var vista = ResultadoVista.fallo("RESENA_DUPLICADA", "Ya reseñaste este lugar.");

        assertThat(vista.ok()).isFalse();
        assertThat(vista.codigo()).isEqualTo("RESENA_DUPLICADA");
        assertThat(vista.recursoId()).isNull();
        assertThat(vista.datos()).isEmpty();
    }

    @Test
    @DisplayName("la vista de una preparacion no imprime el token")
    void preparacion_noImprimeElToken() {
        var vista = new PreparacionVista("SECRETO-DEL-TOKEN", "resumen", Instant.parse("2026-10-16T12:00:00Z"), List.of());

        assertThat(vista.toString()).doesNotContain("SECRETO").contains("2026-10-16");
        assertThat(vista.conToken("otro").token()).isEqualTo("otro");
    }
}
