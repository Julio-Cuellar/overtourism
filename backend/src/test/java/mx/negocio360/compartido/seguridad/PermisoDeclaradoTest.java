package mx.negocio360.compartido.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** AZ-02 · D-70: cada modulo declara sus permisos como datos con la forma {@code modulo.recurso.accion}. */
class PermisoDeclaradoTest {

    @ParameterizedTest
    @ValueSource(strings = {"identidad.miembro.invitar", "clientes.sugerir", "inventario.merma.registrar",
        "identidad.miembro.cambiar_rol"})
    @DisplayName("un codigo con dos o tres segmentos en minusculas es valido y su modulo es el primer segmento")
    void crear_codigoValido_exponeElModulo(String codigo) {
        var permiso = new PermisoDeclarado(codigo, "Descripcion", false);

        assertThat(permiso.codigo()).isEqualTo(codigo);
        assertThat(permiso.modulo()).isEqualTo(codigo.substring(0, codigo.indexOf('.')));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "clientes", "Clientes.sugerir", "clientes.a.b.c", "clientes..sugerir", ".sugerir",
        "clientes.sugerir.", "clientes.sug erir", "clientes.sugerir\n", "clientes.sugerir1"})
    @DisplayName("un codigo mal formado se rechaza")
    void crear_codigoInvalido_seRechaza(String codigo) {
        assertThatThrownBy(() -> new PermisoDeclarado(codigo, "Descripcion", false))
                .isInstanceOf(IllegalArgumentException.class).hasMessageNotContaining("\n");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("la descripcion es obligatoria")
    void crear_sinDescripcion_seRechaza(String descripcion) {
        assertThatThrownBy(() -> new PermisoDeclarado("clientes.sugerir", descripcion, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("un permiso sensible lo declara asi y exige confirmacion reforzada (D-73)")
    void crear_sensible_loConserva() {
        assertThat(new PermisoDeclarado("clientes.cliente.anonimizar", "Anonimizar", true).sensible()).isTrue();
        assertThat(new PermisoDeclarado("clientes.sugerir", "Buscar", false).sensible()).isFalse();
    }
}
