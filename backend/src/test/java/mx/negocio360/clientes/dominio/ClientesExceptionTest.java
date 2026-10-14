package mx.negocio360.clientes.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** P-06: ningun error lleva datos personales; el mensaje es solo el codigo. */
class ClientesExceptionTest {

    @Test
    @DisplayName("el mensaje es el codigo y no hay recurso")
    void nueva_soloConCodigo_mensajeEsElCodigo() {
        var excepcion = new ClientesException(CodigoError.NOMBRE_INVALIDO);

        assertThat(excepcion.getMessage()).isEqualTo("NOMBRE_INVALIDO");
        assertThat(excepcion.codigo()).isEqualTo(CodigoError.NOMBRE_INVALIDO);
        assertThat(excepcion.recursoId()).isEmpty();
    }

    @Test
    @DisplayName("puede llevar el id del recurso existente, pero el mensaje sigue siendo solo el codigo")
    void nueva_conRecurso_guardaElIdSinTocarElMensaje() {
        var existente = UUID.randomUUID();

        var excepcion = new ClientesException(CodigoError.TELEFONO_DUPLICADO, existente);

        assertThat(excepcion.recursoId()).contains(existente);
        assertThat(excepcion.getMessage()).isEqualTo("TELEFONO_DUPLICADO");
    }

    @Test
    @DisplayName("existen todos los codigos de la seccion 7.4 del diseno")
    void codigos_seccion74_estanTodos() {
        var nombres = Arrays.stream(CodigoError.values()).map(Enum::name).toList();

        assertThat(nombres).contains("NOMBRE_INVALIDO", "TELEFONO_INVALIDO", "TELEFONO_DUPLICADO", "CORREO_INVALIDO",
                "FECHA_NACIMIENTO_INVALIDA", "NOTAS_MUY_LARGAS", "CLIENTE_NO_ENCONTRADO", "CLIENTE_ANONIMIZADO",
                "SOLICITUD_NO_ENCONTRADA", "SOLICITUD_CERRADA", "FINALIDAD_INVALIDA", "CONSENTIMIENTO_INCOMPLETO",
                "AVISO_NO_VIGENTE", "AVISO_INCOMPLETO", "AVISO_VERSION_REQUERIDA", "IDENTIDAD_NO_VERIFICADA",
                "MOTIVO_REQUERIDO", "PLAZO_YA_AMPLIADO", "TOKEN_INVALIDO", "TOKEN_EXPIRADO", "TOKEN_USADO",
                "COMANDO_CAMBIO", "CONCURRENCIA", "ERROR_TEMPORAL");
    }
}
