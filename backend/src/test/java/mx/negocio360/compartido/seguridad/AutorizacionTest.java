package mx.negocio360.compartido.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** AZ-01 y AZ-04: todo permiso esta negado por defecto; sin permiso se responde PERMISO_DENEGADO sin decir cual falta. */
class AutorizacionTest {

    @Test
    @DisplayName("puede es verdadero solo para los permisos concedidos")
    void puede_soloLosConcedidos() {
        var autorizacion = Autorizacion.de(Set.of("clientes.sugerir"));

        assertThat(autorizacion.puede("clientes.sugerir")).isTrue();
        assertThat(autorizacion.puede("clientes.cliente.anonimizar")).isFalse();
    }

    @Test
    @DisplayName("AZ-04: sin permisos no puede nada, ni con un permiso nulo o vacio")
    void puede_sinPermisos_negadoPorDefecto() {
        var autorizacion = Autorizacion.de(Set.of());

        assertThat(autorizacion.puede("clientes.sugerir")).isFalse();
        assertThat(autorizacion.puede(null)).isFalse();
        assertThat(autorizacion.puede("")).isFalse();
    }

    @Test
    @DisplayName("exigir no hace nada con permiso y lanza PermisoDenegadoException sin permiso")
    void exigir_conYSinPermiso() {
        var autorizacion = Autorizacion.de(Set.of("clientes.sugerir"));

        assertThatCode(() -> autorizacion.exigir("clientes.sugerir")).doesNotThrowAnyException();
        assertThatThrownBy(() -> autorizacion.exigir("clientes.cliente.anonimizar"))
                .isInstanceOf(PermisoDenegadoException.class);
    }

    @Test
    @DisplayName("el mensaje de la excepcion no dice que permiso falto: el que pide no debe aprender el catalogo")
    void excepcion_noRevelaElPermiso() {
        var error = new PermisoDenegadoException("clientes.cliente.anonimizar");

        assertThat(error.getMessage()).isEqualTo("PERMISO_DENEGADO").doesNotContain("anonimizar");
        assertThat(error.permiso()).isEqualTo("clientes.cliente.anonimizar");
    }

    @Test
    @DisplayName("la autorizacion copia el conjunto: cambiarlo despues no concede nada")
    void de_copiaElConjunto() {
        var original = new HashSet<>(Set.of("clientes.sugerir"));
        var autorizacion = Autorizacion.de(original);

        original.add("clientes.cliente.anonimizar");

        assertThat(autorizacion.puede("clientes.cliente.anonimizar")).isFalse();
    }
}
