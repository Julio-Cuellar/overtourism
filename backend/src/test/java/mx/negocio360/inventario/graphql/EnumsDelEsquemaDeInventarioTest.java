package mx.negocio360.inventario.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import mx.negocio360.inventario.dominio.CausaTipo;
import mx.negocio360.inventario.dominio.TipoMovimiento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Un movimiento del kardex se serializa con el enum del esquema: si el dominio tiene un valor que el esquema no declara, la
 * consulta entera responde {@code ERROR_INTERNO} en cuanto aparece un movimiento con ese valor (asi paso con
 * {@code REVERSION}). Esta prueba compara el SDL con los enums del dominio, sin arrancar nada.
 */
class EnumsDelEsquemaDeInventarioTest {

    private static List<String> valoresDe(String enumGraphql) throws IOException {
        var sdl = new ClassPathResource("graphql/inventario.graphqls").getContentAsString(StandardCharsets.UTF_8);
        var m = Pattern.compile("enum " + enumGraphql + "\\s*\\{([^}]*)\\}").matcher(sdl);
        assertThat(m.find()).as("enum " + enumGraphql).isTrue();
        return Arrays.stream(m.group(1).trim().split("\\s+")).toList();
    }

    @Test
    @DisplayName("CausaMovimientoInventario declara todos los valores de CausaTipo, incluida REVERSION")
    void causas() throws IOException {
        assertThat(valoresDe("CausaMovimientoInventario"))
                .containsExactlyInAnyOrderElementsOf(Arrays.stream(CausaTipo.values()).map(Enum::name).toList());
    }

    @Test
    @DisplayName("TipoMovimientoInventario declara todos los valores de TipoMovimiento")
    void tipos() throws IOException {
        assertThat(valoresDe("TipoMovimientoInventario"))
                .containsExactlyInAnyOrderElementsOf(Arrays.stream(TipoMovimiento.values()).map(Enum::name).toList());
    }
}
