package mx.negocio360.clientes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.stream.Collectors;
import mx.negocio360.clientes.dominio.CodigoError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** La capa que responde al usuario no improvisa: cada codigo de error tiene su plantilla fija y sin datos personales. */
class PlantillasClientesTest {

    @Test
    @DisplayName("todo codigo de error tiene plantilla")
    void de_cadaCodigo_tienePlantillaNoVacia() {
        for (var codigo : CodigoError.values()) {
            assertThat(PlantillasClientes.de(codigo)).as(codigo.name()).isNotBlank();
        }
    }

    @Test
    @DisplayName("ninguna plantilla se repite entre codigos")
    void de_codigosDistintos_tienenTextosDistintos() {
        var textos = Arrays.stream(CodigoError.values()).map(PlantillasClientes::de).collect(Collectors.toSet());

        assertThat(textos).hasSameSizeAs(CodigoError.values());
    }

    @Test
    @DisplayName("la plantilla de telefono duplicado no menciona digitos")
    void de_telefonoDuplicado_noLlevaDigitos() {
        assertThat(PlantillasClientes.de(CodigoError.TELEFONO_DUPLICADO)).doesNotContainPattern("\\d");
    }

    @Test
    @DisplayName("los mensajes de exito existen")
    void mensajesDeExito_existen() {
        assertThat(PlantillasClientes.CLIENTE_GUARDADO).isNotBlank();
        assertThat(PlantillasClientes.CLIENTE_ACTUALIZADO).isNotBlank();
        assertThat(PlantillasClientes.AVISO_PUBLICADO).isNotBlank();
    }
}
