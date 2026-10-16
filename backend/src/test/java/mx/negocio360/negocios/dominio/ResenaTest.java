package mx.negocio360.negocios.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** RS-03 (calificacion y comentario), RS-04 (respuesta), RS-06 (el promedio lo calcula el codigo) y RS-11 (dispositivo). */
class ResenaTest {

    private static CodigoError codigo(Runnable accion) {
        var e = org.assertj.core.api.Assertions.catchThrowableOfType(NegociosException.class, accion::run);
        assertThat(e).isNotNull();
        return e.codigo();
    }

    @Test
    @DisplayName("RS-03: la calificacion es un entero de 1 a 5")
    void calificacion_deUnoACinco() {
        for (var valida = 1; valida <= 5; valida++) {
            assertThat(Resena.calificacion(valida)).isEqualTo(valida);
        }
        assertThat(codigo(() -> Resena.calificacion(0))).isEqualTo(CodigoError.CALIFICACION_INVALIDA);
        assertThat(codigo(() -> Resena.calificacion(6))).isEqualTo(CodigoError.CALIFICACION_INVALIDA);
        assertThat(codigo(() -> Resena.calificacion(-1))).isEqualTo(CodigoError.CALIFICACION_INVALIDA);
    }

    @Test
    @DisplayName("RS-03: el comentario es opcional, de hasta 1000 caracteres y sin caracteres de control")
    void comentario_reglas() {
        assertThat(Resena.comentario(null)).isNull();
        assertThat(Resena.comentario("   ")).isNull();
        assertThat(Resena.comentario("  Muy rico  ")).isEqualTo("Muy rico");
        assertThat(Resena.comentario("Linea uno\nLinea dos")).isEqualTo("Linea uno\nLinea dos");
        assertThat(Resena.comentario("x".repeat(1000))).hasSize(1000);
        assertThat(codigo(() -> Resena.comentario("x".repeat(1001)))).isEqualTo(CodigoError.COMENTARIO_INVALIDO);
        assertThat(codigo(() -> Resena.comentario("hola\u0000"))).isEqualTo(CodigoError.COMENTARIO_INVALIDO);
        assertThat(codigo(() -> Resena.comentario("hola‮mundo"))).isEqualTo(CodigoError.COMENTARIO_INVALIDO);
    }

    @Test
    @DisplayName("RS-04: la respuesta del negocio es obligatoria y de hasta 500 caracteres")
    void respuesta_reglas() {
        assertThat(Resena.respuesta("  Gracias por venir  ")).isEqualTo("Gracias por venir");
        assertThat(Resena.respuesta("x".repeat(500))).hasSize(500);
        assertThat(codigo(() -> Resena.respuesta("x".repeat(501)))).isEqualTo(CodigoError.RESPUESTA_INVALIDA);
        assertThat(codigo(() -> Resena.respuesta(null))).isEqualTo(CodigoError.RESPUESTA_INVALIDA);
        assertThat(codigo(() -> Resena.respuesta("   "))).isEqualTo(CodigoError.RESPUESTA_INVALIDA);
        assertThat(codigo(() -> Resena.respuesta("a\u0007b"))).isEqualTo(CodigoError.RESPUESTA_INVALIDA);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "no-es-uuid", "0192f39f-3333-7000-8000-00000000f00", "0192f39f-3333-7000-8000-00000000f001x",
            "{0192f39f-3333-7000-8000-00000000f001}"})
    @DisplayName("RS-11: un id de dispositivo que no es UUID se rechaza")
    void dispositivo_invalido(String entrada) {
        assertThat(codigo(() -> Resena.dispositivo(entrada))).isEqualTo(CodigoError.DISPOSITIVO_INVALIDO);
    }

    @Test
    @DisplayName("RS-11: un UUID valido se acepta, sin importar mayusculas")
    void dispositivo_valido() {
        var id = UUID.randomUUID();

        assertThat(Resena.dispositivo(id.toString())).isEqualTo(id);
        assertThat(Resena.dispositivo(id.toString().toUpperCase())).isEqualTo(id);
    }

    @Test
    @DisplayName("RS-06: el promedio lo calcula el codigo: 5 y 3 dan 4,0; sin resenas, nulo")
    void resumen_promedio() {
        var vacio = ResumenCalificacion.de(0, 0, 0, 0, 0);
        var cincoYTres = ResumenCalificacion.de(0, 0, 1, 0, 1);

        assertThat(vacio.total()).isZero();
        assertThat(vacio.promedio()).isNull();
        assertThat(cincoYTres.total()).isEqualTo(2);
        assertThat(cincoYTres.promedio()).isEqualTo(4.0);
        assertThat(cincoYTres.distribucion()).containsExactly(0, 0, 1, 0, 1);
        assertThat(ResumenCalificacion.de(1, 0, 0, 1, 1).promedio()).isEqualTo(3.3);
    }

    @Test
    @DisplayName("el texto de una reseña no se imprime: es de un tercero y puede traer datos personales")
    void toString_noImprimeElComentario() {
        var resena = new Resena(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 5, "Llamen al 2225550101",
                Resena.Estado.PUBLICADA, Resena.Origen.DEMOSTRACION, null, null, null, java.time.Instant.EPOCH, null, 0);

        assertThat(resena.toString()).doesNotContain("2225550101").doesNotContain(resena.visitanteId().toString());
        assertThatThrownBy(() -> new Resena(null, UUID.randomUUID(), UUID.randomUUID(), 5, null,
                Resena.Estado.PUBLICADA, Resena.Origen.DEMOSTRACION, null, null, null, java.time.Instant.EPOCH, null, 0))
                .isInstanceOf(NullPointerException.class);
    }
}
