package mx.negocio360.persistencia;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Un PostgreSQL real en Docker para toda la suite de pruebas. Se levanta una sola vez por ejecución y lo comparten
 * todos los contextos de Spring; Testcontainers lo apaga al terminar. Hay dos roles, igual que en el entorno real: el
 * dueño (superusuario del contenedor), que corre las migraciones, y {@value #ROL_APP}, que usa la aplicación y NO es
 * superusuario ni puede saltarse la seguridad por fila. Las contraseñas se generan al azar en cada ejecución.
 */
public final class PostgresDePruebas {

    public static final String ROL_APP = "negocio360_app";
    private static final String IMAGEN = "postgres:17-alpine";
    private static final String BASE = "negocio360";
    private static final String DUENO = "negocio360";

    private static final String CONTRASENA_DUENO = UUID.randomUUID().toString();
    private static final String CONTRASENA_APP = UUID.randomUUID().toString();
    private static PostgreSQLContainer<?> contenedor;

    private PostgresDePruebas() {
    }

    /** Arranca el contenedor la primera vez y crea el rol de la aplicación. */
    public static synchronized PostgreSQLContainer<?> iniciar() {
        if (contenedor == null) {
            var nuevo = new PostgreSQLContainer<>(IMAGEN)
                    .withDatabaseName(BASE).withUsername(DUENO).withPassword(CONTRASENA_DUENO);
            nuevo.start();
            contenedor = nuevo;
            crearRolDeLaAplicacion();
        }
        return contenedor;
    }

    public static String url() {
        return iniciar().getJdbcUrl();
    }

    public static String usuarioDueno() {
        return DUENO;
    }

    public static String contrasenaDueno() {
        return CONTRASENA_DUENO;
    }

    public static String usuarioApp() {
        return ROL_APP;
    }

    public static String contrasenaApp() {
        return CONTRASENA_APP;
    }

    /** Conexión como dueño de la base: sirve para cosas que la aplicación no puede hacer. */
    public static Connection conexionDelDueno() throws SQLException {
        return DriverManager.getConnection(url(), DUENO, CONTRASENA_DUENO);
    }

    /** Conexión con el mismo rol que usa la aplicación. */
    public static Connection conexionDeLaAplicacion() throws SQLException {
        return DriverManager.getConnection(url(), ROL_APP, CONTRASENA_APP);
    }

    private static void crearRolDeLaAplicacion() {
        try (var conexion = DriverManager.getConnection(contenedor.getJdbcUrl(), DUENO, CONTRASENA_DUENO);
             var sentencia = conexion.createStatement()) {
            sentencia.execute("CREATE ROLE " + ROL_APP + " LOGIN PASSWORD '" + CONTRASENA_APP
                    + "' NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS");
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo crear el rol de la aplicación en la base de pruebas", e);
        }
    }
}
