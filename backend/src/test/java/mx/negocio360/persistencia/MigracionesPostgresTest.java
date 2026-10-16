package mx.negocio360.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.DriverManager;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Las migraciones corren sobre una base vacía aparte (sin Spring), para probar lo que la base de la suite ya no
 * puede mostrar: qué pasa cuando falta el rol de la aplicación.
 */
class MigracionesPostgresTest {

    private static final String BASE_TEMPORAL = "prueba_migraciones";

    private static Flyway flyway(String url, String rolDeLaAplicacion) {
        return Flyway.configure()
                .dataSource(url, PostgresDePruebas.usuarioDueno(), PostgresDePruebas.contrasenaDueno())
                .schemas("plataforma").defaultSchema("plataforma").createSchemas(true)
                .placeholders(Map.of("app_user", rolDeLaAplicacion))
                .load();
    }

    private static void ejecutarComoDueno(String sql) throws Exception {
        try (var conexion = PostgresDePruebas.conexionDelDueno(); var sentencia = conexion.createStatement()) {
            conexion.setAutoCommit(true);
            sentencia.execute(sql);
        }
    }

    private static String urlDeLaBaseTemporal() {
        return PostgresDePruebas.url().replaceFirst("/[^/?]+(\\?|$)", "/" + BASE_TEMPORAL + "$1");
    }

    private static void prepararBaseTemporal() throws Exception {
        PostgresDePruebas.iniciar();
        ejecutarComoDueno("DROP DATABASE IF EXISTS " + BASE_TEMPORAL + " WITH (FORCE)");
        ejecutarComoDueno("CREATE DATABASE " + BASE_TEMPORAL);
    }

    private static boolean consultarBoolean(String url, String sql) throws Exception {
        try (var conexion = DriverManager.getConnection(url, PostgresDePruebas.usuarioDueno(),
                PostgresDePruebas.contrasenaDueno()); var sentencia = conexion.createStatement();
             var resultado = sentencia.executeQuery(sql)) {
            resultado.next();
            return resultado.getBoolean(1);
        }
    }

    @Test
    @DisplayName("si el rol de la aplicación no existe, V001 falla con un mensaje claro en vez de omitir los permisos")
    void v001_sinElRolDeLaAplicacion_falla() throws Exception {
        prepararBaseTemporal();
        try {
            var url = urlDeLaBaseTemporal();

            assertThatThrownBy(() -> flyway(url, "rol_que_no_existe").migrate())
                    .hasMessageContaining("rol_que_no_existe")
                    .hasMessageContaining("no existe");

            assertThat(consultarBoolean(url, "select count(*) = 0 from information_schema.tables where table_schema = 'clientes'"))
                    .as("no quedó nada a medias").isTrue();
        } finally {
            ejecutarComoDueno("DROP DATABASE IF EXISTS " + BASE_TEMPORAL + " WITH (FORCE)");
        }
    }

    @Test
    @DisplayName("el rol de la aplicación puede llamarse distinto: las migraciones le dan los permisos al nombre configurado")
    void v001_conOtroNombreDeRol_concedeLosPermisosAEseRol() throws Exception {
        var rol = "app_alterno_prueba";
        ejecutarComoDueno("DO $$ BEGIN IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '" + rol
                + "') THEN CREATE ROLE " + rol + " LOGIN NOSUPERUSER NOBYPASSRLS; END IF; END $$");
        prepararBaseTemporal();
        try {
            var url = urlDeLaBaseTemporal();

            flyway(url, rol).migrate();

            assertThat(consultarBoolean(url, "select pg_has_role('" + rol + "', 'rol_clientes', 'member')")).isTrue();
        } finally {
            ejecutarComoDueno("DROP DATABASE IF EXISTS " + BASE_TEMPORAL + " WITH (FORCE)");
        }
    }
}
