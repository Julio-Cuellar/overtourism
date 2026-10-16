package mx.negocio360.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import mx.negocio360.identidad.dominio.Giro;
import mx.negocio360.identidad.dominio.RolesBase;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * V037 (R0 de Restaurantes): los permisos restaurantes.*, el rol MESERO_CAJA y la plantilla del giro RESTAURANTE quedan
 * sembrados en la base con los mismos valores que {@link RolesBase} y {@link PermisosDeRestaurantes} (AZ-02, AM-09).
 * Corre las migraciones sobre una base vacia aparte y sin contexto de Spring, igual que MigracionesPostgresTest, para que
 * no dependa del resto de la aplicacion.
 */
class PermisosDeRestaurantesMigracionPostgresTest {

    private static final String BASE = "prueba_permisos_restaurantes";
    private static String url;

    private static void comoDueno(String sql) throws Exception {
        try (var conexion = PostgresDePruebas.conexionDelDueno(); var sentencia = conexion.createStatement()) {
            conexion.setAutoCommit(true);
            sentencia.execute(sql);
        }
    }

    @BeforeAll
    static void migrarUnaBaseNueva() throws Exception {
        PostgresDePruebas.iniciar();
        comoDueno("DROP DATABASE IF EXISTS " + BASE + " WITH (FORCE)");
        comoDueno("CREATE DATABASE " + BASE);
        url = PostgresDePruebas.url().replaceFirst("/[^/?]+(\\?|$)", "/" + BASE + "$1");
        Flyway.configure()
                .dataSource(url, PostgresDePruebas.usuarioDueno(), PostgresDePruebas.contrasenaDueno())
                .schemas("plataforma").defaultSchema("plataforma").createSchemas(true)
                .placeholders(Map.of("app_user", PostgresDePruebas.usuarioApp()))
                .load().migrate();
    }

    @AfterAll
    static void borrarLaBase() throws Exception {
        comoDueno("DROP DATABASE IF EXISTS " + BASE + " WITH (FORCE)");
    }

    private static List<String> columna(String sql, Object... parametros) throws Exception {
        try (var conexion = DriverManager.getConnection(url, PostgresDePruebas.usuarioDueno(),
                PostgresDePruebas.contrasenaDueno()); var sentencia = conexion.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                sentencia.setObject(i + 1, parametros[i]);
            }
            var filas = new ArrayList<String>();
            try (var resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    filas.add(resultado.getString(1));
                }
            }
            return filas;
        }
    }

    @Test
    @DisplayName("AZ-02: los 18 permisos de Restaurantes estan sembrados con su descripcion y sin ser sensibles")
    void permisos_estanSembrados() throws Exception {
        var declarados = new PermisosDeRestaurantes().permisos();

        var enBase = columna("select codigo from identidad.permiso where modulo = 'restaurantes' order by codigo");

        assertThat(enBase).containsExactlyElementsOf(declarados.stream().map(p -> p.codigo()).sorted().toList());
        for (var permiso : declarados) {
            assertThat(columna("select modulo || '|' || descripcion || '|' || sensible from identidad.permiso where codigo = ?",
                    permiso.codigo())).as(permiso.codigo())
                    .containsExactly(permiso.modulo() + "|" + permiso.descripcion() + "|" + permiso.sensible());
        }
    }

    @Test
    @DisplayName("AM-09: el rol MESERO_CAJA existe y cada rol tiene en la base los mismos permisos que RolesBase")
    void roles_coincidenConRolesBase() throws Exception {
        assertThat(columna("select codigo from identidad.rol order by codigo"))
                .containsExactlyElementsOf(RolesBase.todos().stream().map(r -> r.codigo()).sorted().toList());
        assertThat(columna("select nombre from identidad.rol where codigo = 'MESERO_CAJA'")).containsExactly("Mesero con caja");
        for (var rol : RolesBase.todos()) {
            assertThat(columna("select permiso_codigo from identidad.rol_permiso where rol_codigo = ? order by permiso_codigo",
                    rol.codigo())).as(rol.codigo()).containsExactlyInAnyOrderElementsOf(rol.permisos());
        }
    }

    @Test
    @DisplayName("D-171: la plantilla de RESTAURANTE ofrece MESERO_CAJA despues de MESERO y conserva el orden de los demas")
    void plantilla_delRestaurante() throws Exception {
        for (var giro : Giro.values()) {
            assertThat(columna("select rol_codigo from identidad.plantilla_giro_rol where giro = ? order by orden", giro.name()))
                    .as(giro.name()).containsExactlyElementsOf(RolesBase.plantilla(giro));
        }
        assertThat(RolesBase.plantilla(Giro.RESTAURANTE)).containsSubsequence("MESERO", "MESERO_CAJA", "COCINERO");
    }
}
