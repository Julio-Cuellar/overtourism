package mx.negocio360.negocios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import mx.negocio360.negocios.DemostracionApi;
import mx.negocio360.negocios.ResenasPublicasApi;
import mx.negocio360.negocios.dominio.CodigoError;
import mx.negocio360.negocios.dominio.NegociosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** DM-03: el modo demostracion esta apagado por omision, y apagado no se puede reseñar sin cuenta ni sembrar la demo. */
@SpringBootTest
class DemostracionApagadaTest {

    @Autowired
    private DemostracionApi demo;
    @Autowired
    private ResenasPublicasApi resenas;

    @Test
    @DisplayName("sin la bandera, preparar una reseña falla con MODO_DEMOSTRACION_APAGADO")
    void resenarSinCuenta_fallaConElModoApagado() {
        var error = org.assertj.core.api.Assertions.catchThrowable(() -> resenas.prepararResena(
                UUID.randomUUID().toString(), UUID.randomUUID(), 5, null));

        assertThat(error).isInstanceOf(NegociosException.class);
        assertThat(((NegociosException) error).codigo()).isEqualTo(CodigoError.MODO_DEMOSTRACION_APAGADO);
    }

    @Test
    @DisplayName("sin la bandera, confirmar una reseña, sembrar y restablecer fallan igual")
    void confirmarSembrarYRestablecer_fallan() {
        for (Runnable accion : new Runnable[] {() -> resenas.confirmarResena(UUID.randomUUID().toString(), "x"),
                demo::sembrar, demo::restablecer}) {
            var error = org.assertj.core.api.Assertions.catchThrowable(accion::run);
            assertThat(((NegociosException) error).codigo()).isEqualTo(CodigoError.MODO_DEMOSTRACION_APAGADO);
        }
    }

    @Test
    @DisplayName("leer las reseñas publicas no depende del modo: sigue funcionando apagado")
    void leer_noDependeDelModo() {
        assertThat(resenas.resenasDeNegocio(UUID.randomUUID(), 20, null).resenas()).isEmpty();
    }
}
