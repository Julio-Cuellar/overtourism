package mx.negocio360.contabilidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import mx.negocio360.contabilidad.ContabilidadApi;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioPolizas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * F-04: las polizas se conservan; no existe operacion que las borre ni las cambie. El plazo de cinco anos (CFF art. 30)
 * esta por verificar, y el respaldo es tema de operacion, no de codigo. La garantia en la base (sin DELETE y con
 * disparador) la prueba {@code ContabilidadPostgresTest}, que necesita Docker.
 */
class SinBorradoTest {

    private static final Pattern DESTRUCTIVO = Pattern.compile("(?i).*(borr|elimin|delete|remove|actualiz|update|"
            + "purg|limpi).*");

    private static List<String> nombres(Class<?> tipo) {
        return Arrays.stream(tipo.getMethods()).map(Method::getName).toList();
    }

    @Test
    @DisplayName("F-04: el puerto de polizas solo inserta y lee")
    void repositorioDePolizas_soloInsertaYLee() {
        assertThat(nombres(RepositorioPolizas.class)).noneMatch(n -> DESTRUCTIVO.matcher(n).matches());
    }

    @Test
    @DisplayName("F-04: la API publica no tiene operaciones para borrar ni cambiar polizas")
    void api_sinBorrado() {
        assertThat(nombres(ContabilidadApi.class)).noneMatch(n -> DESTRUCTIVO.matcher(n).matches());
    }

    @Test
    @DisplayName("F-04: ningun comando borra o edita polizas; la correccion es ReversarPoliza")
    void comandos_sinBorradoDePolizas() throws Exception {
        try (var archivos = Files.list(Path.of("src/main/java/mx/negocio360/contabilidad/aplicacion"))) {
            var comandos = archivos.map(p -> p.getFileName().toString()).filter(n -> n.endsWith("Comando.java"))
                    .toList();

            assertThat(comandos).contains("ReversarPolizaComando.java")
                    .noneMatch(n -> n.matches("(?i).*(borrar|eliminar|editar)poliza.*"));
        }
    }

    @Test
    @DisplayName("F-04: la migracion no da UPDATE ni DELETE sobre poliza ni movimiento")
    void migracion_sinPermisoDeBorrar() throws Exception {
        var sql = Files.readString(Path.of("src/main/resources/db/migration/V030__contabilidad_esquema.sql"));

        assertThat(sql).contains("GRANT SELECT, INSERT ON contabilidad.poliza, contabilidad.movimiento");
        assertThat(sql).doesNotContainPattern("GRANT[^;]*(UPDATE|DELETE)[^;]*contabilidad\\.(poliza|movimiento)[,; ]");
    }
}
