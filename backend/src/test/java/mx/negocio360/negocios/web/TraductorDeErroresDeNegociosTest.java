package mx.negocio360.negocios.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import mx.negocio360.negocios.dominio.CodigoError;
import mx.negocio360.negocios.dominio.NegociosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.execution.ErrorType;

/**
 * SG-13: como sale un {@link NegociosException} por GraphQL. El codigo y el texto son los fijos de {@link CodigoError} (nunca
 * el mensaje de la excepcion); lo que falta para publicar sale como extension; el tipo sigue el estado que ya usa la API REST.
 */
class TraductorDeErroresDeNegociosTest {

    private final TraductorDeErroresDeNegocios traductor = new TraductorDeErroresDeNegocios();

    @Test
    @DisplayName("cada codigo sale con su texto fijo y su propio nombre como codigo")
    void cadaCodigo_textoFijo() {
        for (var codigo : CodigoError.values()) {
            var traducido = traductor.traducir(new NegociosException(codigo)).orElseThrow();

            assertThat(traducido.codigo()).isEqualTo(codigo.name());
            assertThat(traducido.mensaje()).isEqualTo(codigo.mensaje()).isNotBlank();
        }
    }

    @Test
    @DisplayName("el tipo del error sigue el estado HTTP del codigo: no encontrado, sin permiso o peticion mala")
    void tipoDelError() {
        assertThat(traductor.traducir(new NegociosException(CodigoError.NEGOCIO_NO_DISPONIBLE)).orElseThrow()
                .clasificacion()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(traductor.traducir(new NegociosException(CodigoError.MODO_DEMOSTRACION_APAGADO)).orElseThrow()
                .clasificacion()).isEqualTo(ErrorType.FORBIDDEN);
        assertThat(traductor.traducir(new NegociosException(CodigoError.CONSULTA_INVALIDA)).orElseThrow()
                .clasificacion()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(traductor.traducir(new NegociosException(CodigoError.CONCURRENCIA)).orElseThrow()
                .clasificacion()).isEqualTo(ErrorType.BAD_REQUEST);
    }

    @Test
    @DisplayName("lo que falta para publicar sale como extension; sin faltantes no hay extension")
    void faltantes() {
        var incompleta = traductor.traducir(new NegociosException(CodigoError.PUBLICACION_INCOMPLETA,
                List.of("UBICACION", "HORARIO"))).orElseThrow();
        var comun = traductor.traducir(new NegociosException(CodigoError.CONCURRENCIA)).orElseThrow();

        assertThat(incompleta.extensiones()).containsEntry("faltantes", List.of("UBICACION", "HORARIO"));
        assertThat(comun.extensiones()).doesNotContainKey("faltantes");
    }

    @Test
    @DisplayName("solo reconoce las excepciones de Negocios: las demas las dejan pasar a otros traductores")
    void soloReconoceLasSuyas() {
        assertThat(traductor.traducir(new IllegalStateException("password=secreto"))).isEmpty();
        assertThat(traductor.traducir(new RuntimeException())).isEmpty();
    }
}
