package mx.negocio360;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import mx.negocio360.compartido.seguridad.CatalogoDePermisos;
import mx.negocio360.compartido.seguridad.PermisoDeclarado;
import mx.negocio360.identidad.dominio.RolesBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * AZ-02 y AZ-03 con los modulos reales: cada modulo declara sus permisos y los roles base solo citan permisos que
 * existen. Esta prueba vive en la raiz porque es la unica que puede ver a Identidad y a Clientes a la vez.
 */
@SpringBootTest
class CatalogoDePermisosPlataformaTest {

    @Autowired
    private List<CatalogoDePermisos> catalogos;

    private List<PermisoDeclarado> todos() {
        return catalogos.stream().flatMap(c -> c.permisos().stream()).toList();
    }

    @Test
    @DisplayName("Identidad y Clientes declaran sus permisos y cada uno solo declara los de su modulo")
    void catalogos_cadaModuloDeclaraLosSuyos() {
        Map<String, Long> porModulo = todos().stream()
                .collect(Collectors.groupingBy(PermisoDeclarado::modulo, Collectors.counting()));

        assertThat(porModulo).containsEntry("identidad", 5L).containsEntry("clientes", 11L).hasSize(2);
        for (var catalogo : catalogos) {
            assertThat(catalogo.permisos().stream().map(PermisoDeclarado::modulo).distinct()).hasSize(1);
        }
    }

    @Test
    @DisplayName("no hay dos permisos con el mismo codigo")
    void catalogos_sinCodigosRepetidos() {
        assertThat(todos().stream().map(PermisoDeclarado::codigo)).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("AZ-03: todos los permisos de los roles base existen en los catalogos de los modulos")
    void rolesBase_soloCitanPermisosDeclarados() {
        RolesBase.verificar(todos());
    }

    @Test
    @DisplayName("los permisos sensibles son los de los derechos del responsable y los de administrar al equipo")
    void catalogos_permisosSensibles() {
        var sensibles = todos().stream().filter(PermisoDeclarado::sensible).map(PermisoDeclarado::codigo).toList();

        assertThat(sensibles).containsExactlyInAnyOrder("identidad.miembro.invitar", "identidad.miembro.cambiar_rol",
                "identidad.miembro.revocar", "clientes.aviso.publicar", "clientes.cliente.anonimizar",
                "clientes.arco.gestionar", "clientes.datos.exportar");
    }
}
